package br.com.hacerfak.coreWMS.modules.inventario.domain;

import br.com.hacerfak.coreWMS.core.domain.workflow.Tarefa;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Localizacao;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_tarefa_contagem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TarefaContagem extends Tarefa {

    @ManyToOne
    @JoinColumn(name = "inventario_id", nullable = false)
    private Inventario inventario;

    @ManyToOne
    @JoinColumn(name = "localizacao_id", nullable = false)
    private Localizacao localizacao;

    // Opcional: Se for inventário focado em um produto específico
    @ManyToOne
    @JoinColumn(name = "produto_foco_id")
    private Produto produtoFoco;

    // --- SNAPSHOT (Foto do sistema no momento da geração) ---
    // Isso é crucial para auditoria: "O sistema dizia que tinha 10"
    @Column(nullable = false)
    private BigDecimal saldoSistemaSnapshot;

    // --- EXECUÇÃO ---
    private BigDecimal quantidadeContada1;
    private String usuarioContagem1;

    private BigDecimal quantidadeContada2; // Divergência
    private String usuarioContagem2;

    private BigDecimal quantidadeContada3; // Tira-teima
    private String usuarioContagem3;

    // Qual foi a contagem final considerada válida?
    private BigDecimal quantidadeFinal;

    private boolean divergente; // Se contagem != sistema
}