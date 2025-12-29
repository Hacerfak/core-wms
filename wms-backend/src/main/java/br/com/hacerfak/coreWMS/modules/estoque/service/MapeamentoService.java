package br.com.hacerfak.coreWMS.modules.estoque.service;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.estoque.domain.*;
import br.com.hacerfak.coreWMS.modules.estoque.dto.*;
import br.com.hacerfak.coreWMS.modules.estoque.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MapeamentoService {

    private final ArmazemRepository armazemRepository;
    private final AreaRepository areaRepository;
    private final LocalizacaoRepository localizacaoRepository;
    private final LpnRepository lpnRepository;

    // =================================================================================
    // 1. ARMAZÉNS
    // =================================================================================

    public List<Armazem> listarArmazens() {
        return armazemRepository.findAll();
    }

    @Transactional
    public Armazem salvarArmazem(ArmazemRequest dto) {
        Armazem armazem;
        boolean inativando = false;

        // --- LÓGICA DE EDIÇÃO ---
        if (dto.id() != null) {
            armazem = armazemRepository.findById(dto.id())
                    .orElseThrow(() -> new EntityNotFoundException("Armazém não encontrado"));

            // Validação extra: Se mudou o código, verifica se o novo código já existe em
            // OUTRO armazém
            if (!armazem.getCodigo().equals(dto.codigo()) && armazemRepository.existsByCodigo(dto.codigo())) {
                throw new IllegalArgumentException("Já existe outro armazém com o código " + dto.codigo());
            }
        } else {
            // --- LÓGICA DE CRIAÇÃO ---
            if (armazemRepository.existsByCodigo(dto.codigo())) {
                throw new IllegalArgumentException("Já existe um armazém com o código " + dto.codigo());
            }
            armazem = new Armazem();
        }

        armazem.setCodigo(dto.codigo());
        armazem.setNome(dto.nome());
        armazem.setEnderecoCompleto(dto.enderecoCompleto());
        armazem.setAtivo(dto.ativo() != null ? dto.ativo() : true);

        Armazem salvo = armazemRepository.save(armazem);

        // APLICA CASCATA DE INATIVAÇÃO
        if (inativando) {
            processarInativacaoCascataArmazem(salvo);
        }

        return salvo;
    }

    // Método auxiliar para cascata do Armazém -> Áreas
    private void processarInativacaoCascataArmazem(Armazem armazem) {
        List<Area> areas = areaRepository.findByArmazemId(armazem.getId());
        for (Area area : areas) {
            if (area.isAtivo()) {
                area.setAtivo(false);
                areaRepository.save(area);
                // Propaga para os filhos da área
                processarInativacaoCascataArea(area);
            }
        }
    }

    @Transactional
    public void excluirArmazem(Long id) {
        if (!areaRepository.findByArmazemId(id).isEmpty()) {
            throw new IllegalStateException("Não é possível excluir armazém que possui áreas vinculadas.");
        }
        armazemRepository.deleteById(id);
    }

    // =================================================================================
    // 2. ÁREAS
    // =================================================================================

    public List<Area> listarAreas(Long armazemId) {
        return areaRepository.findByArmazemId(armazemId);
    }

    @Transactional
    public Area salvarArea(AreaRequest dto) {
        Armazem armazem = armazemRepository.findById(dto.armazemId())
                .orElseThrow(() -> new EntityNotFoundException("Armazém pai não encontrado"));

        Area area;
        boolean inativando = false;
        boolean codigoMudou = false; // Flag para controlar a cascata

        if (dto.id() != null) {
            area = areaRepository.findById(dto.id())
                    .orElseThrow(() -> new EntityNotFoundException("Área não encontrada"));

            // Verifica se o código está mudando (Ex: De 'DOC' para 'DOCA')
            if (!area.getCodigo().equals(dto.codigo())) {
                codigoMudou = true;
            }
        } else {
            area = new Area();
        }

        area.setArmazem(armazem);
        area.setCodigo(dto.codigo()); // Atualiza o código na memória
        area.setNome(dto.nome());
        area.setAtivo(dto.ativo() != null ? dto.ativo() : true);

        Area areaSalva = areaRepository.save(area);

        // APLICA CASCATA DE INATIVAÇÃO
        if (inativando) {
            processarInativacaoCascataArea(areaSalva);
        }

        // --- EFEITO CASCATA ---
        // Se o código da área mudou, precisamos atualizar o enderecoCompleto de TODAS
        // as posições filhas
        if (codigoMudou) {
            List<Localizacao> filhos = localizacaoRepository.findByAreaIdOrderByCodigoAsc(areaSalva.getId());
            for (Localizacao loc : filhos) {
                // Como 'loc' tem referência para 'area' (JPA), e 'area' foi atualizada acima,
                // chamar gerarEnderecoCompleto() vai pegar o novo código da área.
                loc.gerarEnderecoCompleto();
                localizacaoRepository.save(loc);
            }
        }

        return areaSalva;
    }

    // Método auxiliar para cascata da Área -> Localizações (COM PROTEÇÃO DE LPN)
    private void processarInativacaoCascataArea(Area area) {
        List<Localizacao> locais = localizacaoRepository.findByAreaId(area.getId());

        for (Localizacao loc : locais) {
            // Só tenta inativar se estiver ativo
            if (loc.isAtivo()) {
                // REGRA DE OURO: Verifica se tem LPN (Estoque)
                boolean possuiEstoque = lpnRepository.existsByLocalizacaoAtualId(loc.getId());

                if (!possuiEstoque) {
                    loc.setAtivo(false);
                    localizacaoRepository.save(loc);
                } else {
                    log.info("Cascata de inativação ignorada para o local {} pois possui LPNs associadas.",
                            loc.getEnderecoCompleto());
                    // Opcional: Poderíamos lançar um aviso, mas em cascata geralmente
                    // apenas pulamos silenciosamente para não travar a inativação do pai.
                }
            }
        }
    }

    @Transactional
    public void excluirArea(Long id) {
        if (!localizacaoRepository.findByAreaIdOrderByCodigoAsc(id).isEmpty()) {
            throw new IllegalStateException("Não é possível excluir área que possui endereços vinculados.");
        }
        areaRepository.deleteById(id);
    }

    // =================================================================================
    // 3. LOCALIZAÇÕES (CORRIGIDO PARA EDIÇÃO)
    // =================================================================================

    public List<Localizacao> listarLocais(Long areaId) {
        return localizacaoRepository.findByAreaIdOrderByCodigoAsc(areaId);
    }

    @Transactional
    public Localizacao salvarLocal(LocalizacaoRequest dto) {
        Area area = areaRepository.findById(dto.areaId())
                .orElseThrow(() -> new EntityNotFoundException("Área não encontrada"));

        Localizacao local;

        if (dto.id() != null) {
            local = localizacaoRepository.findById(dto.id())
                    .orElseThrow(() -> new EntityNotFoundException("Localização não encontrada"));
        } else {
            local = new Localizacao();
        }

        local.setArea(area);
        local.setCodigo(dto.codigo());
        local.setDescricao(dto.descricao());

        // Tipos
        local.setTipo(dto.tipo() != null ? dto.tipo() : TipoLocalizacao.DOCA);

        // --- CORREÇÃO AQUI: Salvando tipoEstrutura e capacidadeMaxima ---
        local.setTipoEstrutura(dto.tipoEstrutura() != null ? dto.tipoEstrutura() : TipoEstrutura.BLOCADO);
        local.setCapacidadeMaxima(dto.capacidadeMaxima() != null ? dto.capacidadeMaxima() : 10);
        // ----------------------------------------------------------------

        // Regras
        local.setVirtual(dto.virtual() != null ? dto.virtual() : false);
        local.setPermiteMultiLpn(dto.permiteMultiLpn() != null ? dto.permiteMultiLpn() : true);

        // Capacidades
        local.setCapacidadeLpn(dto.capacidadeLpn() != null ? dto.capacidadeLpn() : 10);
        local.setCapacidadePesoKg(dto.capacidadePesoKg());

        // Status
        local.setBloqueado(dto.bloqueado() != null ? dto.bloqueado() : false);
        local.setAtivo(dto.ativo() != null ? dto.ativo() : true);

        return localizacaoRepository.save(local);
    }

    @Transactional
    public void excluirLocal(Long id) {
        // Futuro: Verificar saldo antes de deletar
        localizacaoRepository.deleteById(id);
    }

    public Localizacao buscarPorEnderecoCompleto(String endereco) {
        return localizacaoRepository.findByEnderecoCompleto(endereco)
                .orElseThrow(() -> new EntityNotFoundException("Endereço não encontrado: " + endereco));
    }

    public List<Localizacao> listarTodosLocais(TipoLocalizacao tipoFiltro) {
        if (tipoFiltro != null) {
            return localizacaoRepository.findByTipoAndAtivoTrue(tipoFiltro);
        }
        return localizacaoRepository.findByAtivoTrue();
    }

    // =================================================================================
    // 4. FUNCIONALIDADES AVANÇADAS (IMPORTAÇÃO E BULK)
    // =================================================================================

    // --- IMPORTAÇÃO COMPLETA ---
    @Transactional
    public void importarLocalizacoes(MultipartFile file) {
        log.info("Iniciando importação de localizações...");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean headerSkipped = false;
            int count = 0;

            while ((line = reader.readLine()) != null) {
                // Limpa caracteres invisíveis (como BOM do Excel) no início da linha
                line = line.replace("\uFEFF", "").trim();

                if (line.isEmpty())
                    continue;

                // Detecção automática de separador (prioriza ponto e vírgula, senão usa
                // vírgula)
                String separador = line.contains(";") ? ";" : ",";

                // Verifica Cabeçalho (independente do separador)
                if (!headerSkipped) {
                    if (line.toUpperCase().contains("ARMAZEM") || line.toUpperCase().contains("CODIGO")) {
                        headerSkipped = true;
                        continue;
                    }
                    headerSkipped = true;
                }

                // Faz o split usando o separador detectado
                // O parâmetro -1 garante que colunas vazias no final não sejam descartadas
                String[] cols = line.split(separador, -1);

                // Validação mínima de colunas
                if (cols.length < 3) {
                    log.warn("Linha ignorada por falta de colunas (Esperado >= 3, Encontrado {}): {}", cols.length,
                            line);
                    continue;
                }

                // Remove aspas duplas que o Excel pode colocar em volta dos campos
                for (int i = 0; i < cols.length; i++) {
                    cols[i] = cols[i].replace("\"", "").trim();
                }

                String codArmazem = cols[0].toUpperCase();
                String codArea = cols[1].toUpperCase();
                String codLocal = cols[2].toUpperCase();

                // --- PARSE DOS CAMPOS ---
                String descricao = getCol(cols, 3, "");
                String tipoStr = getCol(cols, 4, "ARMAZENAGEM");
                String estStr = getCol(cols, 5, "PORTA_PALLET");

                // Numéricos
                int capLpn = parseInt(getCol(cols, 6, "1"), 1);
                BigDecimal capKg = parseBigDecimal(getCol(cols, 7, "1000"), new BigDecimal("1000"));
                int capMax = parseInt(getCol(cols, 8, "1"), 1); // Empilhamento

                // Booleanos (Default: Ativo=SIM, Resto=NAO)
                boolean ativo = parseBoolean(getCol(cols, 9, "SIM"));
                boolean bloqueado = parseBoolean(getCol(cols, 10, "NAO"));
                boolean virtual = parseBoolean(getCol(cols, 11, "NAO"));
                boolean multiLpn = parseBoolean(getCol(cols, 12, "SIM"));

                // 1. Hierarquia: Armazém (Find or Create)
                Armazem armazem = armazemRepository.findByCodigo(codArmazem)
                        .orElseGet(() -> armazemRepository.save(Armazem.builder()
                                .codigo(codArmazem).nome("Armazém " + codArmazem).enderecoCompleto("Importado")
                                .ativo(true).build()));

                // 2. Hierarquia: Área (Find or Create)
                Area area = areaRepository.findByArmazemIdAndCodigo(armazem.getId(), codArea)
                        .orElseGet(() -> areaRepository.save(Area.builder()
                                .armazem(armazem).codigo(codArea).nome("Área " + codArea).ativo(true).build()));

                // 3. Localização (Upsert)
                Localizacao local = localizacaoRepository.findByAreaIdAndCodigo(area.getId(), codLocal)
                        .orElse(new Localizacao());

                local.setArea(area);
                local.setCodigo(codLocal);

                if (!descricao.isEmpty())
                    local.setDescricao(descricao);

                local.setTipo(parseTipoLocal(tipoStr));
                local.setTipoEstrutura(parseTipoEstrutura(estStr));

                local.setCapacidadeLpn(capLpn);
                local.setCapacidadePesoKg(capKg);
                local.setCapacidadeMaxima(capMax); // Altura / Empilhamento

                local.setAtivo(ativo);
                local.setBloqueado(bloqueado);
                local.setVirtual(virtual);
                local.setPermiteMultiLpn(multiLpn);

                local.gerarEnderecoCompleto();

                localizacaoRepository.save(local);
                count++;
            }
            log.info("Importação concluída. {} registros processados.", count);
        } catch (Exception e) {
            log.error("Erro importação", e);
            throw new RuntimeException("Erro ao processar arquivo: " + e.getMessage());
        }
    }

    @Transactional
    public void atualizarEmMassa(LocalizacaoBulkUpdateDTO dto) {
        List<Localizacao> locais = localizacaoRepository.findAllById(dto.ids());

        for (Localizacao loc : locais) {
            // Só atualiza se o campo vier preenchido no DTO (não nulo)
            if (dto.tipo() != null)
                loc.setTipo(dto.tipo());
            if (dto.estrutura() != null)
                loc.setTipoEstrutura(dto.estrutura());
            if (dto.capacidadeLpn() != null)
                loc.setCapacidadeLpn(dto.capacidadeLpn());
            if (dto.capacidadePeso() != null)
                loc.setCapacidadePesoKg(dto.capacidadePeso());

            // Regra de empilhamento
            if (dto.capacidadeMaxima() != null) {
                if (loc.getTipoEstrutura() == TipoEstrutura.BLOCADO ||
                        loc.getTipoEstrutura() == TipoEstrutura.DRIVE_IN ||
                        loc.getTipoEstrutura() == TipoEstrutura.PUSH_BACK) {
                    loc.setCapacidadeMaxima(dto.capacidadeMaxima());
                }
            }

            if (dto.ativo() != null)
                loc.setAtivo(dto.ativo());
            if (dto.bloqueado() != null)
                loc.setBloqueado(dto.bloqueado());
            if (dto.virtualLocation() != null)
                loc.setVirtual(dto.virtualLocation());
            if (dto.permiteMultiLpn() != null)
                loc.setPermiteMultiLpn(dto.permiteMultiLpn());

            localizacaoRepository.save(loc);
        }
    }

    // --- Parsers Seguros ---
    private TipoLocalizacao parseTipoLocal(String val) {
        try {
            return TipoLocalizacao.valueOf(val);
        } catch (Exception e) {
            return TipoLocalizacao.ARMAZENAGEM;
        }
    }

    private TipoEstrutura parseTipoEstrutura(String val) {
        try {
            return TipoEstrutura.valueOf(val);
        } catch (Exception e) {
            return TipoEstrutura.PORTA_PALLET;
        }
    }

    // --- HELPERS PARA CSV ---
    private String getCol(String[] cols, int index, String def) {
        if (index >= cols.length)
            return def;
        String val = cols[index].trim();
        return val.isEmpty() ? def : val;
    }

    private int parseInt(String val, int def) {
        try {
            return Integer.parseInt(val);
        } catch (Exception e) {
            return def;
        }
    }

    private BigDecimal parseBigDecimal(String val, BigDecimal def) {
        try {
            return new BigDecimal(val);
        } catch (Exception e) {
            return def;
        }
    }

    private boolean parseBoolean(String val) {
        if (val == null)
            return false;
        val = val.toUpperCase();
        return val.equals("S") || val.equals("SIM") || val.equals("TRUE") || val.equals("1")
                || val.equals("VERDADEIRO");
    }
}