package br.com.hacerfak.coreWMS.modules.cadastro.controller;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.Parceiro;
import br.com.hacerfak.coreWMS.modules.cadastro.dto.ParceiroRequest; // Import do DTO
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ParceiroRepository;
import jakarta.validation.Valid; // Import para validação
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/parceiros")
@RequiredArgsConstructor
public class ParceiroController {

    private final ParceiroRepository repository;

    @GetMapping
    @PreAuthorize("hasAuthority('PARCEIRO_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<List<Parceiro>> listar() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PARCEIRO_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<Parceiro> buscarPorId(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- CREATE COM DTO ---
    @PostMapping
    @PreAuthorize("hasAuthority('PARCEIRO_CRIAR') or hasRole('ADMIN')")
    public ResponseEntity<Parceiro> criar(@RequestBody @Valid ParceiroRequest dto) {
        Parceiro parceiro = Parceiro.builder()
                .nome(dto.nome())
                .documento(dto.documento())
                .ie(dto.ie())
                .nomeFantasia(dto.nomeFantasia())
                .ativo(dto.ativo() != null ? dto.ativo() : true)
                .recebimentoCego(dto.recebimentoCego() != null ? dto.recebimentoCego() : false)
                // Endereço
                .cep(dto.cep())
                .logradouro(dto.logradouro())
                .numero(dto.numero())
                .bairro(dto.bairro())
                .cidade(dto.cidade())
                .uf(dto.uf())
                // Contato
                .telefone(dto.telefone())
                .email(dto.email())
                .crt(dto.crt())
                .tipo(dto.tipo() != null ? dto.tipo() : "AMBOS") // Default seguro se vier nulo da API
                .build();

        return ResponseEntity.ok(repository.save(parceiro));
    }

    // --- UPDATE COM DTO ---
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PARCEIRO_EDITAR') or hasRole('ADMIN')")
    public ResponseEntity<Parceiro> atualizar(@PathVariable Long id, @RequestBody @Valid ParceiroRequest dto) {
        return repository.findById(id).map(parceiro -> {
            parceiro.setNome(dto.nome());
            parceiro.setDocumento(dto.documento());
            parceiro.setIe(dto.ie());
            parceiro.setNomeFantasia(dto.nomeFantasia());

            // Configurações
            if (dto.ativo() != null)
                parceiro.setAtivo(dto.ativo());
            if (dto.recebimentoCego() != null)
                parceiro.setRecebimentoCego(dto.recebimentoCego());

            // Endereço
            parceiro.setCep(dto.cep());
            parceiro.setLogradouro(dto.logradouro());
            parceiro.setNumero(dto.numero());
            parceiro.setBairro(dto.bairro());
            parceiro.setCidade(dto.cidade());
            parceiro.setUf(dto.uf());

            // Contato
            parceiro.setTelefone(dto.telefone());
            parceiro.setEmail(dto.email());
            parceiro.setCrt(dto.crt());
            parceiro.setTipo(dto.tipo());

            return ResponseEntity.ok(repository.save(parceiro));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PARCEIRO_EXCLUIR') or hasRole('ADMIN')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (!repository.existsById(id))
            return ResponseEntity.notFound().build();
        try {
            repository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}