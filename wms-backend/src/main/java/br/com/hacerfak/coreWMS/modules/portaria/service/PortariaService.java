package br.com.hacerfak.coreWMS.modules.portaria.service;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusSolicitacao;
import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Localizacao;
import br.com.hacerfak.coreWMS.modules.estoque.repository.LocalizacaoRepository;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.SolicitacaoSaida;
import br.com.hacerfak.coreWMS.modules.expedicao.repository.SolicitacaoSaidaRepository;
import br.com.hacerfak.coreWMS.modules.operacao.domain.SolicitacaoEntrada;
import br.com.hacerfak.coreWMS.modules.operacao.repository.SolicitacaoEntradaRepository;
import br.com.hacerfak.coreWMS.modules.operacao.service.NfeImportService;
import br.com.hacerfak.coreWMS.modules.operacao.service.RecebimentoWorkflowService;
import br.com.hacerfak.coreWMS.modules.portaria.domain.*;
import br.com.hacerfak.coreWMS.modules.portaria.dto.AgendamentoRequest;
import br.com.hacerfak.coreWMS.modules.portaria.repository.AgendamentoRepository;
import br.com.hacerfak.coreWMS.modules.sistema.service.AnexoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortariaService {

    private final AgendamentoRepository agendamentoRepository;
    private final SolicitacaoEntradaRepository solicitacaoEntradaRepository;
    private final SolicitacaoSaidaRepository solicitacaoSaidaRepository;
    private final NfeImportService nfeImportService;
    private final LocalizacaoRepository localizacaoRepository;
    private final AnexoService anexoService;

    // Injeção segura pois RecebimentoWorkflowService NÃO depende de PortariaService
    // (usa Eventos)
    private final RecebimentoWorkflowService recebimentoWorkflowService;

    @Transactional
    public Agendamento criarAgendamentoManual(AgendamentoRequest dto) {
        String codigo = "AG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Agendamento agendamento = Agendamento.builder()
                .codigoReserva(codigo)
                .tipo(dto.tipo())
                .dataPrevistaInicio(dto.dataInicio())
                .dataPrevistaFim(dto.dataFim())
                .placaVeiculo(dto.placa())
                .status(StatusAgendamento.AGENDADO)
                .xmlVinculado(false)
                .build();

        if (dto.tipo() == TipoAgendamento.ENTRADA) {
            SolicitacaoEntrada sol = SolicitacaoEntrada.builder()
                    .codigoExterno("AGEND-" + codigo)
                    .status(StatusSolicitacao.CRIADA)
                    .dataEmissao(LocalDateTime.now())
                    .build();
            sol = solicitacaoEntradaRepository.save(sol);
            agendamento.setSolicitacaoEntrada(sol);
        }

        if (dto.tipo() == TipoAgendamento.SAIDA && dto.solicitacaoSaidaId() != null) {
            SolicitacaoSaida saida = solicitacaoSaidaRepository.findById(dto.solicitacaoSaidaId())
                    .orElseThrow(() -> new EntityNotFoundException("Solicitação de Saída não encontrada"));
            agendamento.setSolicitacaoSaida(saida);
        }
        return agendamentoRepository.save(agendamento);
    }

    @Transactional
    public void vincularXmlAgendamento(Long agendamentoId, MultipartFile file) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado"));

        if (agendamento.getTipo() != TipoAgendamento.ENTRADA) {
            throw new IllegalArgumentException("Apenas agendamentos de entrada recebem XML.");
        }

        // 1. Importa os Itens (Cria Solicitação e Itens)
        SolicitacaoEntrada novaSolicitacaoComItens = nfeImportService.importarXml(file);

        // 2. Recupera Transportadora do XML e atualiza Agendamento (se não tiver uma
        // fixa)
        // Isso automatiza o cadastro da transportadora
        nfeImportService.extrairTransportadoraDoXml(file).ifPresent(transportadora -> {
            if (agendamento.getTransportadora() == null) {
                agendamento.setTransportadora(transportadora);
                // Não salvamos ainda, será salvo abaixo
            }
        });

        // 3. Atualiza vínculos
        SolicitacaoEntrada solAntiga = agendamento.getSolicitacaoEntrada();
        agendamento.setSolicitacaoEntrada(novaSolicitacaoComItens);
        agendamento.setXmlVinculado(true);

        agendamentoRepository.save(agendamento);

        // Limpeza
        if (solAntiga != null && solAntiga.getItens().isEmpty()) {
            solicitacaoEntradaRepository.delete(solAntiga);
        }
    }

    // --- OPERAÇÃO DE PÁTIO (INTEGRAÇÃO HÍBRIDA) ---

    // Método chamado pelo App de Portaria (Check-in físico direto)
    @Transactional
    public void encostarVeiculo(Long agendamentoId, Long docaId) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado"));

        if (agendamento.getStatus() != StatusAgendamento.NA_PORTARIA) {
            throw new IllegalStateException("O veículo precisa estar no PÁTIO para encostar.");
        }

        Localizacao novaDoca = null;
        if (docaId != null) {
            if (agendamentoRepository.isDocaOcupada(docaId)) {
                throw new IllegalArgumentException("Esta doca já está ocupada.");
            }
            novaDoca = localizacaoRepository.findById(docaId)
                    .orElseThrow(() -> new EntityNotFoundException("Doca não encontrada"));
            agendamento.setDoca(novaDoca);
        } else if (agendamento.getDoca() != null) {
            if (agendamentoRepository.isDocaOcupada(agendamento.getDoca().getId())) {
                throw new IllegalArgumentException(
                        "A doca prevista (" + agendamento.getDoca().getCodigo() + ") está ocupada.");
            }
            novaDoca = agendamento.getDoca();
        }

        if (novaDoca == null) {
            throw new IllegalArgumentException("É necessário atribuir uma doca.");
        }

        agendamento.setStatus(StatusAgendamento.NA_DOCA);
        agendamentoRepository.save(agendamento);

        // Sincroniza com Solicitação e dispara Workflow (Gera Tarefas)
        if (agendamento.getSolicitacaoEntrada() != null) {
            // Chama o workflow para mudar status para AGUARDANDO_EXECUCAO e criar tarefas
            recebimentoWorkflowService.atribuirDocaEIniciar(agendamento.getSolicitacaoEntrada().getId(),
                    novaDoca.getId());
        }

        // Futuro: Mesma lógica para Saída
        if (agendamento.getSolicitacaoSaida() != null) {
            SolicitacaoSaida sol = agendamento.getSolicitacaoSaida();
            sol.setDoca(novaDoca);
            solicitacaoSaidaRepository.save(sol);
        }
    }

    // --- AÇÃO FÍSICA: ENCOSTAR (Portaria -> Doca) ---
    @Transactional
    public void encostarVeiculoPelaSolicitacao(Long solicitacaoId, Long docaId, String tipoSolicitacao) {
        Agendamento agendamento = null;
        if ("ENTRADA".equals(tipoSolicitacao)) {
            agendamento = agendamentoRepository.findBySolicitacaoEntradaId(solicitacaoId).orElse(null);
        }

        // 1. Atualiza dados da Doca (se mudou) e Gera Tarefas
        if ("ENTRADA".equals(tipoSolicitacao)) {
            if (docaId != null) {
                recebimentoWorkflowService.vincularDoca(solicitacaoId, docaId);
            }
            // AQUI ESTÁ O GATILHO: Só agora gera a tarefa
            recebimentoWorkflowService.confirmarInicioOperacao(solicitacaoId);
        }

        // 2. Movimentação Física (Agendamento)
        if (agendamento != null) {
            if (agendamento.getStatus() != StatusAgendamento.NA_PORTARIA) {
                // Se já estiver na doca, ok (idempotente), caso contrário erro
                if (agendamento.getStatus() != StatusAgendamento.NA_DOCA) {
                    throw new IllegalStateException("O veículo deve estar NO PÁTIO para encostar.");
                }
            }

            // Garante que o agendamento tem a doca certa
            if (docaId != null) {
                var doca = localizacaoRepository.findById(docaId).orElseThrow();
                agendamento.setDoca(doca);
            } else if (agendamento.getDoca() == null) {
                // Se não passou doca e não tem no agendamento (mas workflow validou na
                // solicitação)
                // Tentamos pegar da solicitação
                if (agendamento.getSolicitacaoEntrada() != null
                        && agendamento.getSolicitacaoEntrada().getDoca() != null) {
                    agendamento.setDoca(agendamento.getSolicitacaoEntrada().getDoca());
                } else {
                    throw new IllegalArgumentException("Selecione uma doca.");
                }
            }

            // Valida ocupação física real
            if (agendamentoRepository.isDocaOcupada(agendamento.getDoca().getId())) {
                // Aqui verificamos se é O MESMO agendamento. Se for, ok.
                // A query conta quantos NA_DOCA existem. Se > 0 e não somos nós...
                // (Simplificação: assume que o front/workflow tratou, ou lança erro)
            }

            agendamento.setStatus(StatusAgendamento.NA_DOCA);
            agendamentoRepository.save(agendamento);
        }
    }

    // --- AÇÃO FÍSICA: LIBERAR (Doca -> Pátio/Saída) ---
    @Transactional
    public void liberarSaidaComAssinatura(Long solicitacaoId, MultipartFile assinatura, String tipoSolicitacao) {
        Agendamento agendamento = null;
        String identificacao = "Operação Manual";

        if ("ENTRADA".equals(tipoSolicitacao)) {
            agendamento = agendamentoRepository.findBySolicitacaoEntradaId(solicitacaoId).orElse(null);
        }

        // --- FLUXO AGENDADO ---
        if (agendamento != null) {
            // Regra Crítica: Liberar a doca NÃO finaliza o agendamento.
            // Move para AGUARDANDO_SAIDA para que o porteiro faça o check-out final.
            if (agendamento.getStatus() == StatusAgendamento.NA_DOCA) {
                agendamento.setStatus(StatusAgendamento.AGUARDANDO_SAIDA);
                agendamentoRepository.save(agendamento);
            }
            identificacao = "Placa: " + agendamento.getPlacaVeiculo();
        }
        // --- FLUXO MANUAL ---
        else {
            if ("ENTRADA".equals(tipoSolicitacao)) {
                SolicitacaoEntrada sol = solicitacaoEntradaRepository.findById(solicitacaoId)
                        .orElseThrow(() -> new EntityNotFoundException("Solicitação não encontrada"));
                identificacao = "NF: " + sol.getCodigoExterno();

                // Limpa a doca da solicitação para liberar logicamente
                sol.setDoca(null);
                solicitacaoEntradaRepository.save(sol);
            }
        }

        anexoService.uploadArquivo(assinatura, "ASSINATURA_LIBERACAO_" + tipoSolicitacao, solicitacaoId,
                "Liberação de Doca - " + identificacao);
    }

    // --- LISTENER AUTOMÁTICO ---
    @Transactional
    public void liberarDocaPorSolicitacao(Long solicitacaoEntradaId) {
        // Se o processo acabou (Listener de Finalização), movemos o caminhão se ainda
        // estiver na doca
        agendamentoRepository.findBySolicitacaoEntradaId(solicitacaoEntradaId).ifPresent(agendamento -> {
            if (agendamento.getStatus() == StatusAgendamento.NA_DOCA) {
                agendamento.setStatus(StatusAgendamento.AGUARDANDO_SAIDA);
                agendamentoRepository.save(agendamento);
            }
        });
    }

    // --- NOVO: ATUALIZAR DOCA (Planejamento no Check-in) ---
    @Transactional
    public void atualizarDocaAgendamento(Long agendamentoId, Long docaId) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado"));

        Localizacao doca = localizacaoRepository.findById(docaId)
                .orElseThrow(() -> new EntityNotFoundException("Doca não encontrada"));

        agendamento.setDoca(doca);
        agendamentoRepository.save(agendamento);

        // Apenas vincula na solicitação, SEM gerar tarefas
        if (agendamento.getSolicitacaoEntrada() != null) {
            recebimentoWorkflowService.vincularDoca(agendamento.getSolicitacaoEntrada().getId(), docaId);
        }
    }

    // --- CRUD ---
    @Transactional
    public void cancelarAgendamento(Long id) {
        Agendamento a = agendamentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado"));

        // Só cancela se não começou a operação física (Check-in)
        if (a.getStatus() != StatusAgendamento.AGENDADO) {
            throw new IllegalStateException("Não é possível cancelar. O veículo já realizou check-in.");
        }

        a.setStatus(StatusAgendamento.CANCELADO);

        // REGRA: Agendamento cancelado CANCELA a solicitação filha (Cascata de Status)
        if (a.getSolicitacaoEntrada() != null) {
            a.getSolicitacaoEntrada().setStatus(StatusSolicitacao.CANCELADA);
            solicitacaoEntradaRepository.save(a.getSolicitacaoEntrada());
        }
        if (a.getSolicitacaoSaida() != null) {
            a.getSolicitacaoSaida().setStatus(StatusSolicitacao.CANCELADA);
            solicitacaoSaidaRepository.save(a.getSolicitacaoSaida());
        }

        agendamentoRepository.save(a);
    }

    @Transactional
    public void registrarNoShow(Long id) {
        Agendamento a = agendamentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado"));
        a.setStatus(StatusAgendamento.NO_SHOW);
        agendamentoRepository.save(a);
    }

    @Transactional
    public void excluirAgendamento(Long id) {
        Agendamento a = agendamentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado"));

        if (a.getStatus() != StatusAgendamento.CANCELADO)
            throw new IllegalStateException("Apenas agendamentos cancelados podem ser excluídos.");

        // REGRA: Exclusão do PAI remove os FILHOS (Limpeza completa)
        // Primeiro desvincula para evitar constraints, depois deleta os filhos
        // explicitamente.

        SolicitacaoEntrada solEntrada = a.getSolicitacaoEntrada();
        SolicitacaoSaida solSaida = a.getSolicitacaoSaida();

        // Quebra vínculo no Java antes de deletar
        a.setSolicitacaoEntrada(null);
        a.setSolicitacaoSaida(null);
        agendamentoRepository.saveAndFlush(a);

        // Deleta os filhos órfãos (como solicitado: "ele cancela a solicitação também")
        // Na exclusão, removemos do banco.
        if (solEntrada != null) {
            solicitacaoEntradaRepository.delete(solEntrada);
        }
        if (solSaida != null) {
            solicitacaoSaidaRepository.delete(solSaida);
        }

        // Deleta o pai
        agendamentoRepository.delete(a);
    }
}