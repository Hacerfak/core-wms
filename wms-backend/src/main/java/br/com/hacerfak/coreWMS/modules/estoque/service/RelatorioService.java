package br.com.hacerfak.coreWMS.modules.estoque.service;

import br.com.hacerfak.coreWMS.modules.estoque.dto.relatorio.AcuracidadeDTO;
import br.com.hacerfak.coreWMS.modules.estoque.dto.relatorio.AgingDTO;
import br.com.hacerfak.coreWMS.modules.estoque.dto.relatorio.KardexDTO;
import br.com.hacerfak.coreWMS.modules.estoque.dto.relatorio.OcupacaoDTO;
import br.com.hacerfak.coreWMS.modules.estoque.dto.relatorio.PosicaoEstoqueDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RelatorioService {

    private final EntityManager entityManager;

    // --- 1. POSIÇÃO DE ESTOQUE (Já existente) ---
    @SuppressWarnings("unchecked")
    public List<PosicaoEstoqueDTO> gerarRelatorioPosicaoEstoque() {
        String sql = """
                    SELECT
                        p.sku, p.nome,
                        l.codigo as local,
                        e.lpn, e.lote,
                        CAST(e.data_validade AS VARCHAR),
                        e.status_qualidade,
                        e.quantidade
                    FROM tb_estoque_saldo e
                    JOIN tb_produto p ON e.produto_id = p.id
                    JOIN tb_localizacao l ON e.localizacao_id = l.id
                    WHERE e.quantidade > 0
                    ORDER BY l.codigo, p.sku
                """;

        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> rows = query.getResultList();

        return rows.stream().map(row -> new PosicaoEstoqueDTO(
                (String) row[0], (String) row[1], (String) row[2],
                (String) row[3], (String) row[4], (String) row[5],
                (String) row[6], (BigDecimal) row[7])).collect(Collectors.toList());
    }

    // --- 2. KARDEX DO PRODUTO (Rastreabilidade) ---
    @SuppressWarnings("unchecked")
    public List<KardexDTO> gerarKardexProduto(Long produtoId) {
        String sql = """
                    SELECT
                        m.data_criacao,
                        m.tipo,
                        COALESCE(m.lpn, m.observacao, 'N/A') as doc,
                        m.usuario_responsavel,
                        CASE WHEN m.tipo IN ('ENTRADA', 'AJUSTE_POSITIVO', 'DESBLOQUEIO') THEN m.quantidade ELSE 0 END as entrada,
                        CASE WHEN m.tipo IN ('SAIDA', 'AJUSTE_NEGATIVO', 'BLOQUEIO', 'PERDA_QUEBRA') THEN m.quantidade ELSE 0 END as saida,
                        m.saldo_anterior,
                        m.saldo_atual
                    FROM tb_movimento_estoque m
                    WHERE m.produto_id = :produtoId
                    ORDER BY m.data_criacao DESC -- Mais recente primeiro
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("produtoId", produtoId);
        List<Object[]> rows = query.getResultList();

        return rows.stream().map(row -> new KardexDTO(
                ((Timestamp) row[0]).toLocalDateTime(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (BigDecimal) row[4],
                (BigDecimal) row[5],
                (BigDecimal) row[6],
                (BigDecimal) row[7])).collect(Collectors.toList());
    }

    // --- 3. AGING (Validade / Shelf Life) ---
    @SuppressWarnings("unchecked")
    public List<AgingDTO> gerarRelatorioAging(Integer diasParaVencer) {
        // Se diasParaVencer for nulo, traz tudo. Se informado, traz só o que vence
        // nesse prazo.
        String filtroData = (diasParaVencer != null)
                ? "AND e.data_validade <= CURRENT_DATE + INTERVAL '" + diasParaVencer + " day'"
                : "";

        String sql = """
                SELECT
                    p.sku, p.nome, e.lote, e.data_validade, e.quantidade, e.status_qualidade
                FROM tb_estoque_saldo e
                JOIN tb_produto p ON e.produto_id = p.id
                WHERE e.quantidade > 0
                AND e.data_validade IS NOT NULL
                """ + filtroData + """
                    ORDER BY e.data_validade ASC
                """;

        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> rows = query.getResultList();
        LocalDate hoje = LocalDate.now();

        return rows.stream().map(row -> {
            LocalDate validade = ((java.sql.Date) row[3]).toLocalDate();
            long dias = ChronoUnit.DAYS.between(hoje, validade);

            String faixa = "OK";
            if (dias < 0)
                faixa = "VENCIDO";
            else if (dias <= 30)
                faixa = "CRITICO";
            else if (dias <= 60)
                faixa = "ALERTA";

            return new AgingDTO(
                    (String) row[0], (String) row[1], (String) row[2],
                    validade, (int) dias, (BigDecimal) row[4], (String) row[5], faixa);
        }).collect(Collectors.toList());
    }

    // --- 4. ACURACIDADE (IRA - Inventory Record Accuracy) ---
    @SuppressWarnings("unchecked")
    public List<AcuracidadeDTO> gerarRelatorioAcuracidade() {
        // Compara Snapshot do Sistema vs Quantidade Final da Contagem
        String sql = """
                    SELECT
                        i.id, i.descricao, i.data_agendada,
                        COUNT(tc.id) as total_itens,
                        SUM(CASE WHEN tc.divergente = false THEN 1 ELSE 0 END) as corretos,
                        SUM(CASE WHEN tc.divergente = true THEN 1 ELSE 0 END) as divergentes
                    FROM tb_inventario i
                    JOIN tb_tarefa_contagem tc ON tc.inventario_id = i.id
                    WHERE i.status = 'FINALIZADO'
                    GROUP BY i.id, i.descricao, i.data_agendada
                    ORDER BY i.data_agendada DESC
                """;

        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> rows = query.getResultList();

        return rows.stream().map(row -> {
            long total = ((Number) row[3]).longValue();
            long corretos = ((Number) row[4]).longValue();
            long divergentes = ((Number) row[5]).longValue(); // Para debug se quiser

            BigDecimal acuracidade = (total > 0)
                    ? BigDecimal.valueOf(corretos).divide(BigDecimal.valueOf(total), 4, java.math.RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            return new AcuracidadeDTO(
                    (Long) row[0], (String) row[1], ((java.sql.Date) row[2]).toLocalDate(),
                    total, corretos, divergentes, acuracidade);
        }).collect(Collectors.toList());
    }

    // --- 5. OCUPAÇÃO DO ARMAZÉM ---
    @SuppressWarnings("unchecked")
    public List<OcupacaoDTO> gerarRelatorioOcupacao() {
        // Conta locais ativos e quantos têm saldo > 0
        String sql = """
                    SELECT
                        a.nome as area,
                        l.tipo,
                        COUNT(l.id) as total_posicoes,
                        COUNT(DISTINCT CASE WHEN e.id IS NOT NULL AND e.quantidade > 0 THEN l.id END) as ocupadas
                    FROM tb_localizacao l
                    LEFT JOIN tb_area a ON l.area_id = a.id
                    LEFT JOIN tb_estoque_saldo e ON e.localizacao_id = l.id
                    WHERE l.ativo = true
                    GROUP BY a.nome, l.tipo
                """;

        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> rows = query.getResultList();

        return rows.stream().map(row -> {
            long total = ((Number) row[2]).longValue();
            long ocupadas = ((Number) row[3]).longValue();
            long vazias = total - ocupadas;
            double taxa = (total > 0) ? ((double) ocupadas / total) * 100 : 0.0;

            return new OcupacaoDTO(
                    (String) row[0], (String) row[1], total, ocupadas, vazias, taxa);
        }).collect(Collectors.toList());
    }
}