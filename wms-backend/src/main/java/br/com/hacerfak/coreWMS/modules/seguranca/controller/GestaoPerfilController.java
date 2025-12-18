package br.com.hacerfak.coreWMS.modules.seguranca.controller;

import br.com.hacerfak.coreWMS.modules.seguranca.domain.Perfil;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.PermissaoEnum;
import br.com.hacerfak.coreWMS.modules.seguranca.repository.PerfilRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gestao-perfis")
@RequiredArgsConstructor
public class GestaoPerfilController {

    private final PerfilRepository perfilRepository;

    // 1. LISTAR TODAS AS PERMISSÕES DISPONÍVEIS (Para o Front montar os Checkboxes)
    @GetMapping("/permissoes-disponiveis")
    public ResponseEntity<Map<String, List<String>>> listarPermissoesDisponiveis() {
        // Agrupa por prefixo para ficar bonito na tela (Ex: RECEBIMENTO_*, ESTOQUE_*)
        Map<String, List<String>> agrupado = Arrays.stream(PermissaoEnum.values())
                .map(Enum::name)
                .collect(Collectors.groupingBy(nome -> nome.split("_")[0])); // Pega a primeira palavra antes do _

        return ResponseEntity.ok(agrupado);
    }

    // 2. LISTAR PERFIS
    @GetMapping
    public ResponseEntity<List<Perfil>> listarPerfis() {
        return ResponseEntity.ok(perfilRepository.findAll());
    }

    // 3. CRIAR / EDITAR PERFIL
    @PostMapping
    @PreAuthorize("hasAuthority('PERFIL_GERENCIAR') or hasRole('ADMIN')")
    public ResponseEntity<Perfil> salvarPerfil(@RequestBody Perfil perfil) {
        // Se for edição, mantém o ID. Se novo, ID vem null.
        return ResponseEntity.ok(perfilRepository.save(perfil));
    }

    // 4. BUSCAR POR ID
    @GetMapping("/{id}")
    public ResponseEntity<Perfil> buscarPorId(@PathVariable Long id) {
        return perfilRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 5. EXCLUIR
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERFIL_GERENCIAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        perfilRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}