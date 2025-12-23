package br.com.hacerfak.coreWMS.modules.cadastro.controller;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.Parceiro;
import br.com.hacerfak.coreWMS.modules.cadastro.dto.ParceiroRequest;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ParceiroRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parceiros")
@RequiredArgsConstructor
public class ParceiroController {

    private final ParceiroRepository repository;

    @GetMapping
    @PreAuthorize("hasAuthority('CADASTRO_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<List<Parceiro>> listar() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CADASTRO_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<Parceiro> buscarPorId(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CADASTRO_EDITAR') or hasRole('ADMIN')")
    public ResponseEntity<Parceiro> criar(@RequestBody @Valid ParceiroRequest dto) {
        Parceiro parceiro = Parceiro.builder()
                .nome(dto.nome())
                .cpfCnpj(dto.documento().replaceAll("\\D", "")) // Limpa CNPJ
                .ie(dto.ie())
                .nomeFantasia(dto.nomeFantasia())
                .crt(dto.crt())
                .tipo(dto.tipo() != null ? dto.tipo() : "AMBOS")

                // Configs
                .ativo(dto.ativo() != null ? dto.ativo() : true)
                .recebimentoCego(dto.recebimentoCego() != null ? dto.recebimentoCego() : false)
                .padraoControlaLote(dto.padraoControlaLote() != null ? dto.padraoControlaLote() : false)
                .padraoControlaValidade(dto.padraoControlaValidade() != null ? dto.padraoControlaValidade() : false)
                .padraoControlaSerie(dto.padraoControlaSerie() != null ? dto.padraoControlaSerie() : false)

                // Endereço
                .cep(dto.cep())
                .logradouro(dto.logradouro())
                .numero(dto.numero())
                .complemento(dto.complemento()) // <--- Adicionado
                .bairro(dto.bairro())
                .cidade(dto.cidade())
                .uf(dto.uf())

                // Contato
                .telefone(dto.telefone())
                .email(dto.email())
                .build();

        return ResponseEntity.ok(repository.save(parceiro));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CADASTRO_EDITAR') or hasRole('ADMIN')")
    public ResponseEntity<Parceiro> atualizar(@PathVariable Long id, @RequestBody @Valid ParceiroRequest dto) {
        return repository.findById(id).map(parceiro -> {
            parceiro.setNome(dto.nome());
            parceiro.setCpfCnpj(dto.documento().replaceAll("\\D", ""));
            parceiro.setIe(dto.ie());
            parceiro.setNomeFantasia(dto.nomeFantasia());
            parceiro.setCrt(dto.crt());
            parceiro.setTipo(dto.tipo());

            // Configurações
            if (dto.ativo() != null)
                parceiro.setAtivo(dto.ativo());
            if (dto.recebimentoCego() != null)
                parceiro.setRecebimentoCego(dto.recebimentoCego());
            if (dto.padraoControlaLote() != null)
                parceiro.setPadraoControlaLote(dto.padraoControlaLote());
            if (dto.padraoControlaValidade() != null)
                parceiro.setPadraoControlaValidade(dto.padraoControlaValidade());
            if (dto.padraoControlaSerie() != null)
                parceiro.setPadraoControlaSerie(dto.padraoControlaSerie());

            // Endereço
            parceiro.setCep(dto.cep());
            parceiro.setLogradouro(dto.logradouro());
            parceiro.setNumero(dto.numero());
            parceiro.setComplemento(dto.complemento()); // <--- Adicionado
            parceiro.setBairro(dto.bairro());
            parceiro.setCidade(dto.cidade());
            parceiro.setUf(dto.uf());

            // Contato
            parceiro.setTelefone(dto.telefone());
            parceiro.setEmail(dto.email());

            return ResponseEntity.ok(repository.save(parceiro));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}