package br.com.hacerfak.coreWMS.modules.operacao.controller;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.operacao.domain.Agendamento;
import br.com.hacerfak.coreWMS.modules.operacao.domain.StatusAgendamento;
import br.com.hacerfak.coreWMS.modules.operacao.repository.AgendamentoRepository;
import br.com.hacerfak.coreWMS.modules.sistema.service.AnexoService; // <--- Injeção nova
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/portaria")
@RequiredArgsConstructor
public class PortariaController {

    private final AgendamentoRepository agendamentoRepository;
    private final AnexoService anexoService; // Serviço de anexos

    // --- CHECK-IN (Entrada) ---
    @PostMapping("/checkin/{codigoReserva}")
    public ResponseEntity<Agendamento> realizarCheckin(
            @PathVariable String codigoReserva,
            @RequestParam String placa,
            @RequestParam String motorista,
            @RequestParam String cpf) {

        Agendamento agendamento = agendamentoRepository.findAll().stream()
                .filter(a -> a.getCodigoReserva().equals(codigoReserva))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Reserva não encontrada: " + codigoReserva));

        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            throw new IllegalStateException("Status inválido para check-in. Status atual: " + agendamento.getStatus());
        }

        agendamento.setDataChegada(LocalDateTime.now());
        agendamento.setPlacaVeiculo(placa);
        agendamento.setNomeMotoristaAvulso(motorista);
        agendamento.setCpfMotoristaAvulso(cpf);
        agendamento.setStatus(StatusAgendamento.NA_PORTARIA);

        return ResponseEntity.ok(agendamentoRepository.save(agendamento));
    }

    // --- CHECK-OUT (Saída com Assinatura) ---
    @PostMapping(value = "/checkout/{codigoReserva}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Agendamento> realizarCheckout(
            @PathVariable String codigoReserva,
            @RequestParam(value = "assinatura", required = false) MultipartFile assinatura) {

        Agendamento agendamento = agendamentoRepository.findAll().stream()
                .filter(a -> a.getCodigoReserva().equals(codigoReserva))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Reserva não encontrada"));

        // Valida se está no pátio ou na doca
        if (agendamento.getStatus() == StatusAgendamento.FINALIZADO ||
                agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new IllegalStateException("Agendamento já encerrado.");
        }

        // Salva a assinatura se foi enviada
        if (assinatura != null && !assinatura.isEmpty()) {
            anexoService.uploadArquivo(
                    assinatura,
                    "AGENDAMENTO", // Tipo da entidade
                    agendamento.getId(), // ID da entidade
                    "Assinatura de Checkout - Motorista: " + agendamento.getNomeMotoristaAvulso());
        }

        agendamento.setDataSaida(LocalDateTime.now());
        agendamento.setStatus(StatusAgendamento.FINALIZADO);

        return ResponseEntity.ok(agendamentoRepository.save(agendamento));
    }

    // --- LISTAGEM DO PÁTIO ---
    @GetMapping("/patio")
    public ResponseEntity<List<Agendamento>> listarVeiculosNoPatio() {
        // Retorna quem já fez checkin mas não saiu
        List<Agendamento> noPatio = agendamentoRepository.findAll().stream()
                .filter(a -> a.getStatus() == StatusAgendamento.NA_PORTARIA ||
                        a.getStatus() == StatusAgendamento.NA_DOCA)
                .toList();
        return ResponseEntity.ok(noPatio);
    }
}