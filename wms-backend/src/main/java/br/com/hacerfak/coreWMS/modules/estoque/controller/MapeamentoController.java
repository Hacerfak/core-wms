package br.com.hacerfak.coreWMS.modules.estoque.controller;

import br.com.hacerfak.coreWMS.modules.estoque.domain.*;
import br.com.hacerfak.coreWMS.modules.estoque.dto.*;
import br.com.hacerfak.coreWMS.modules.estoque.service.MapeamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.cache.annotation.CacheEvict;

import java.util.List;

@RestController
@RequestMapping("/api/mapeamento")
@RequiredArgsConstructor
public class MapeamentoController {

    private final MapeamentoService service;

    // --- ARMAZÉNS ---

    @GetMapping("/armazens")
    @PreAuthorize("hasAuthority('LOCALIZACAO_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<List<Armazem>> listarArmazens() {
        return ResponseEntity.ok(service.listarArmazens());
    }

    @PostMapping("/armazens")
    @PreAuthorize("hasAuthority('LOCALIZACAO_GERENCIAR') or hasRole('ADMIN')")
    public ResponseEntity<Armazem> salvarArmazem(@RequestBody ArmazemRequest dto) {
        return ResponseEntity.ok(service.salvarArmazem(dto));
    }

    @DeleteMapping("/armazens/{id}")
    @PreAuthorize("hasAuthority('LOCALIZACAO_EXCLUIR') or hasRole('ADMIN')")
    public ResponseEntity<Void> excluirArmazem(@PathVariable Long id) {
        service.excluirArmazem(id);
        return ResponseEntity.noContent().build();
    }

    // --- ÁREAS ---

    @GetMapping("/areas/{armazemId}")
    @PreAuthorize("hasAuthority('LOCALIZACAO_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<List<Area>> listarAreas(@PathVariable Long armazemId) {
        return ResponseEntity.ok(service.listarAreas(armazemId));
    }

    @PostMapping("/areas")
    @PreAuthorize("hasAuthority('LOCALIZACAO_GERENCIAR') or hasRole('ADMIN')")
    public ResponseEntity<Area> salvarArea(@RequestBody AreaRequest dto) {
        return ResponseEntity.ok(service.salvarArea(dto));
    }

    @DeleteMapping("/areas/{id}")
    @PreAuthorize("hasAuthority('LOCALIZACAO_EXCLUIR') or hasRole('ADMIN')")
    public ResponseEntity<Void> excluirArea(@PathVariable Long id) {
        service.excluirArea(id);
        return ResponseEntity.noContent().build();
    }

    // --- LOCALIZAÇÕES ---

    @GetMapping("/locais/{areaId}")
    @PreAuthorize("hasAuthority('LOCALIZACAO_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<List<Localizacao>> listarLocais(@PathVariable Long areaId) {
        return ResponseEntity.ok(service.listarLocais(areaId));
    }

    // NOVO ENDPOINT: Listagem Global com Filtro
    @GetMapping("/locais")
    @PreAuthorize("hasAuthority('LOCALIZACAO_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<List<Localizacao>> listarLocaisGlobal(
            @RequestParam(required = false) TipoLocalizacao tipo) {
        return ResponseEntity.ok(service.listarTodosLocais(tipo));
    }

    @PostMapping("/locais")
    @PreAuthorize("hasAuthority('LOCALIZACAO_GERENCIAR') or hasRole('ADMIN')")
    @CacheEvict(value = "locais", allEntries = true)
    public ResponseEntity<Localizacao> salvarLocal(@RequestBody LocalizacaoRequest dto) {
        return ResponseEntity.ok(service.salvarLocal(dto));
    }

    @DeleteMapping("/locais/{id}")
    @PreAuthorize("hasAuthority('LOCALIZACAO_EXCLUIR') or hasRole('ADMIN')")
    @CacheEvict(value = "locais", allEntries = true)
    public ResponseEntity<Void> excluirLocal(@PathVariable Long id) {
        service.excluirLocal(id);
        return ResponseEntity.noContent().build();
    }

    // --- UTILITÁRIOS (Scan) ---

    @GetMapping("/scan/{enderecoCompleto}")
    @PreAuthorize("hasAuthority('LOCALIZACAO_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<Localizacao> buscarPorBarcode(@PathVariable String enderecoCompleto) {
        return ResponseEntity.ok(service.buscarPorEnderecoCompleto(enderecoCompleto));
    }

    // --- NOVO: IMPORTAÇÃO ---
    @PostMapping(value = "/locais/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('LOCALIZACAO_GERENCIAR') or hasRole('ADMIN')")
    @CacheEvict(value = "locais", allEntries = true)
    public ResponseEntity<Void> importarLocais(@RequestParam("file") MultipartFile file) {
        service.importarLocalizacoes(file);
        return ResponseEntity.ok().build();
    }

    // --- NOVO: BULK UPDATE ---
    @PostMapping("/locais/bulk-update")
    @PreAuthorize("hasAuthority('LOCALIZACAO_GERENCIAR') or hasRole('ADMIN')")
    @CacheEvict(value = "locais", allEntries = true)
    public ResponseEntity<Void> atualizarMassa(@RequestBody LocalizacaoBulkUpdateDTO dto) {
        service.atualizarEmMassa(dto);
        return ResponseEntity.ok().build();
    }
}