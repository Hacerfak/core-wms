package br.com.hacerfak.coreWMS.modules.portaria.controller;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Localizacao;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.SolicitacaoSaida;
import br.com.hacerfak.coreWMS.modules.expedicao.repository.SolicitacaoSaidaRepository;
import br.com.hacerfak.coreWMS.modules.operacao.service.RecebimentoWorkflowService;
import br.com.hacerfak.coreWMS.modules.portaria.domain.Agendamento;
import br.com.hacerfak.coreWMS.modules.portaria.domain.StatusAgendamento;
import br.com.hacerfak.coreWMS.modules.portaria.domain.Turno;
import br.com.hacerfak.coreWMS.modules.portaria.dto.AgendamentoRequest;
import br.com.hacerfak.coreWMS.modules.portaria.dto.TurnoRequest;
import br.com.hacerfak.coreWMS.modules.portaria.repository.AgendamentoRepository;
import br.com.hacerfak.coreWMS.modules.portaria.repository.TurnoRepository;
import br.com.hacerfak.coreWMS.modules.portaria.service.PortariaService;
import br.com.hacerfak.coreWMS.modules.sistema.service.AnexoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/portaria")
@RequiredArgsConstructor
public class PortariaController {

    private final PortariaService portariaService;
    private final TurnoRepository turnoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final AnexoService anexoService;
    private final SolicitacaoSaidaRepository solicitacaoSaidaRepository;
    private final RecebimentoWorkflowService recebimentoWorkflowService;

    // =================================================================================
    // 1. GESTÃO DE TURNOS
    // =================================================================================

    @GetMapping("/turnos")
    @PreAuthorize("hasAuthority('CONFIG_GERENCIAR') or hasRole('ADMIN')")
    public ResponseEntity<List<Turno>> listarTurnos() {
        return ResponseEntity.ok(turnoRepository.findAll());
    }

    @PostMapping("/turnos")
    @PreAuthorize("hasAuthority('CONFIG_GERENCIAR') or hasRole('ADMIN')")
    public ResponseEntity<Turno> criarTurno(@RequestBody @Valid TurnoRequest dto) {
        Turno turno = Turno.builder()
                .nome(dto.nome())
                .inicio(dto.inicio())
                .fim(dto.fim())
                .diasSemana(dto.diasSemana())
                .ativo(dto.ativo() != null ? dto.ativo() : true)
                .build();
        return ResponseEntity.ok(turnoRepository.save(turno));
    }

    @DeleteMapping("/turnos/{id}")
    @PreAuthorize("hasAuthority('CONFIG_GERENCIAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> excluirTurno(@PathVariable Long id) {
        turnoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // =================================================================================
    // 2. AGENDAMENTOS (Agenda)
    // =================================================================================

    @GetMapping("/agenda")
    @PreAuthorize("hasAuthority('PORTARIA_AGENDAR') or hasRole('ADMIN')")
    public ResponseEntity<List<Agendamento>> listarAgenda(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {

        LocalDate target = data != null ? data : LocalDate.now();
        LocalDateTime inicio = target.atStartOfDay();
        LocalDateTime fim = target.atTime(LocalTime.MAX);

        return ResponseEntity
                .ok(agendamentoRepository.findByDataPrevistaInicioBetweenOrderByDataPrevistaInicioAsc(inicio, fim));
    }

    @PostMapping("/agenda")
    @PreAuthorize("hasAuthority('PORTARIA_AGENDAR') or hasRole('ADMIN')")
    public ResponseEntity<Agendamento> criarAgendamento(@RequestBody @Valid AgendamentoRequest dto) {
        return ResponseEntity.ok(portariaService.criarAgendamentoManual(dto));
    }

    @PostMapping(value = "/agenda/{id}/vincular-xml", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('RECEBIMENTO_IMPORTAR_XML') or hasRole('ADMIN')")
    public ResponseEntity<Void> vincularXml(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        portariaService.vincularXmlAgendamento(id, file);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/solicitacoes-saida-pendentes")
    @PreAuthorize("hasAuthority('PORTARIA_AGENDAR') or hasRole('ADMIN')")
    public ResponseEntity<List<SolicitacaoSaida>> listarSaidasPendentes() {
        return ResponseEntity.ok(solicitacaoSaidaRepository.findPendentesDeAgendamento());
    }

    @PostMapping("/agenda/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        portariaService.cancelarAgendamento(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/agenda/{id}/no-show")
    public ResponseEntity<Void> noShow(@PathVariable Long id) {
        portariaService.registrarNoShow(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/agenda/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        portariaService.excluirAgendamento(id);
        return ResponseEntity.noContent().build();
    }

    // =================================================================================
    // 3. OPERAÇÃO DE PORTARIA (Check-in / Check-out)
    // =================================================================================

    @GetMapping("/patio")
    @PreAuthorize("hasAuthority('PORTARIA_OPERAR') or hasRole('ADMIN')")
    public ResponseEntity<List<Agendamento>> listarPatio() {
        return ResponseEntity.ok(agendamentoRepository.findVeiculosNoPatio());
    }

    @PostMapping("/checkin/{codigoReserva}")
    @PreAuthorize("hasAuthority('PORTARIA_OPERAR') or hasRole('ADMIN')")
    public ResponseEntity<Agendamento> realizarCheckin(
            @PathVariable String codigoReserva,
            @RequestParam String placa,
            @RequestParam String motorista,
            @RequestParam String cpf,
            @RequestParam(required = false) Long docaId) { // <--- Parâmetro Doca

        Agendamento agendamento = agendamentoRepository.findByCodigoReserva(codigoReserva)
                .orElseThrow(() -> new EntityNotFoundException("Reserva não encontrada: " + codigoReserva));

        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            throw new IllegalStateException("Status inválido para check-in. Status atual: " + agendamento.getStatus());
        }

        // 1. Atualiza Doca no Agendamento (se informada no Check-in)
        if (docaId != null) {
            Localizacao doca = new Localizacao();
            doca.setId(docaId);
            agendamento.setDoca(doca);
        }

        // 2. PROPAGAÇÃO E GATILHO DE WORKFLOW (A Correção Principal)
        if (agendamento.getSolicitacaoEntrada() != null) {
            // Se tem doca (seja definida agora ou no agendamento prévio)
            if (agendamento.getDoca() != null) {
                // Chama o serviço que: Seta a Doca na Solicitação + Muda Status para AGUARDANDO
                // + Gera Tarefa
                recebimentoWorkflowService.vincularDoca(
                        agendamento.getSolicitacaoEntrada().getId(),
                        agendamento.getDoca().getId());
            }
        }
        // Futuro: Lógica similar para Saída/Expedição se necessário

        agendamento.setDataChegada(LocalDateTime.now());
        agendamento.setPlacaVeiculo(placa);
        agendamento.setNomeMotoristaAvulso(motorista);
        agendamento.setCpfMotoristaAvulso(cpf);
        agendamento.setStatus(StatusAgendamento.NA_PORTARIA); // Ou NA_DOCA se já foi direto

        // Se já tiver doca, podemos considerar status NA_DOCA direto?
        // Geralmente Check-in é "Entrou no Pátio". O "Encostou na Doca" é outro evento.
        // Vamos manter NA_PORTARIA, mas a doca já fica reservada.

        return ResponseEntity.ok(agendamentoRepository.save(agendamento));
    }

    @PostMapping(value = "/checkout/{codigoReserva}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PORTARIA_OPERAR') or hasRole('ADMIN')")
    public ResponseEntity<Agendamento> realizarCheckout(
            @PathVariable String codigoReserva,
            @RequestParam(value = "assinatura", required = false) MultipartFile assinatura) {

        Agendamento agendamento = agendamentoRepository.findByCodigoReserva(codigoReserva)
                .orElseThrow(() -> new EntityNotFoundException("Reserva não encontrada"));

        // Valida se está no pátio ou na doca
        if (agendamento.getStatus() == StatusAgendamento.FINALIZADO ||
                agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new IllegalStateException("Agendamento já encerrado.");
        }

        // Salva a assinatura se enviada (comprovante de entrega/saída)
        if (assinatura != null && !assinatura.isEmpty()) {
            anexoService.uploadArquivo(
                    assinatura,
                    "AGENDAMENTO",
                    agendamento.getId(),
                    "Assinatura de Checkout - Motorista: " + agendamento.getNomeMotoristaAvulso());
        }

        agendamento.setDataSaida(LocalDateTime.now());
        agendamento.setStatus(StatusAgendamento.FINALIZADO);

        return ResponseEntity.ok(agendamentoRepository.save(agendamento));
    }

    @PostMapping("/operacao/{id}/encostar")
    @PreAuthorize("hasAuthority('PORTARIA_OPERAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> encostarVeiculo(
            @PathVariable Long id,
            @RequestParam(required = false) Long docaId) {

        portariaService.encostarVeiculo(id, docaId);
        return ResponseEntity.ok().build();
    }
}