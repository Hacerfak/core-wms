package br.com.hacerfak.coreWMS.modules.portaria.service;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusSolicitacao;
import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.SolicitacaoSaida;
import br.com.hacerfak.coreWMS.modules.expedicao.repository.SolicitacaoSaidaRepository;
import br.com.hacerfak.coreWMS.modules.operacao.domain.SolicitacaoEntrada;
import br.com.hacerfak.coreWMS.modules.operacao.repository.SolicitacaoEntradaRepository;
import br.com.hacerfak.coreWMS.modules.operacao.service.NfeImportService;
import br.com.hacerfak.coreWMS.modules.portaria.domain.*;
import br.com.hacerfak.coreWMS.modules.portaria.dto.AgendamentoRequest;
import br.com.hacerfak.coreWMS.modules.portaria.repository.AgendamentoRepository;
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

        // Lógica de Entrada
        if (dto.tipo() == TipoAgendamento.ENTRADA) {
            SolicitacaoEntrada sol = SolicitacaoEntrada.builder()
                    .codigoExterno("AGEND-" + codigo)
                    .status(StatusSolicitacao.CRIADA)
                    .dataEmissao(LocalDateTime.now())
                    .build();

            sol = solicitacaoEntradaRepository.save(sol);
            agendamento.setSolicitacaoEntrada(sol);
        }

        // Lógica de Saída
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

        SolicitacaoEntrada novaSolicitacaoComItens = nfeImportService.importarXml(file);
        SolicitacaoEntrada solAntiga = agendamento.getSolicitacaoEntrada();

        agendamento.setSolicitacaoEntrada(novaSolicitacaoComItens);
        agendamento.setXmlVinculado(true);
        agendamentoRepository.save(agendamento);

        if (solAntiga != null && solAntiga.getItens().isEmpty()) {
            solicitacaoEntradaRepository.delete(solAntiga);
        }
    }

    // --- MÉTODOS DE AÇÃO (CORRIGIDOS) ---

    @Transactional
    public void cancelarAgendamento(Long id) {
        // Correção: repository -> agendamentoRepository
        Agendamento a = agendamentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado"));

        if (a.getStatus() == StatusAgendamento.FINALIZADO)
            throw new IllegalStateException("Não pode cancelar agendamento finalizado.");

        a.setStatus(StatusAgendamento.CANCELADO);
        agendamentoRepository.save(a);
    }

    @Transactional
    public void registrarNoShow(Long id) {
        // Correção: repository -> agendamentoRepository
        Agendamento a = agendamentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado"));

        a.setStatus(StatusAgendamento.NO_SHOW);
        agendamentoRepository.save(a);
    }

    @Transactional
    public void excluirAgendamento(Long id) {
        // Correção: repository -> agendamentoRepository
        Agendamento a = agendamentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado"));

        if (a.getStatus() != StatusAgendamento.CANCELADO)
            throw new IllegalStateException("Apenas agendamentos cancelados podem ser excluídos.");

        agendamentoRepository.delete(a);
    }
}