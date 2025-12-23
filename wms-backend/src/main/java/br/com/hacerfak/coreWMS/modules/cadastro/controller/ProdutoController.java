package br.com.hacerfak.coreWMS.modules.cadastro.controller;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.cadastro.dto.ProdutoRequest;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ProdutoRepository;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ParceiroRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoRepository repository;
    private final ParceiroRepository parceiroRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('PRODUTO_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<Page<Produto>> listarTodos(
            @PageableDefault(page = 0, size = 20, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(repository.findAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUTO_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<Produto> buscarPorId(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PRODUTO_CRIAR') or hasRole('ADMIN')")
    @CacheEvict(value = "produtos", allEntries = true)
    public ResponseEntity<Produto> criar(@RequestBody @Valid ProdutoRequest dto) {
        // Assume parceiro ID 1 por enquanto (Multitenant resolve isso depois)
        var parceiro = parceiroRepository.findById(1L).orElseThrow();

        if (repository.existsBySku(dto.sku())) {
            throw new IllegalArgumentException("SKU já cadastrado: " + dto.sku());
        }

        Produto novoProduto = Produto.builder()
                .sku(dto.sku())
                .nome(dto.nome())
                .ean13(dto.ean13())
                .dun14(dto.dun14())
                .unidadeMedida(dto.unidadeMedida())
                .pesoBrutoKg(dto.pesoBrutoKg())
                .ncm(dto.ncm())
                .cest(dto.cest())
                .valorUnitarioPadrao(dto.valorUnitarioPadrao())
                .ativo(dto.ativo() != null ? dto.ativo() : true)
                .controlaLote(dto.controlaLote() != null ? dto.controlaLote() : false)
                .controlaValidade(dto.controlaValidade() != null ? dto.controlaValidade() : false)
                .controlaSerie(dto.controlaSerie() != null ? dto.controlaSerie() : false)
                .unidadeArmazenagem(dto.unidadeArmazenagem())
                .fatorConversao(dto.fatorConversao() != null ? dto.fatorConversao() : 1)
                .depositante(parceiro)
                .build();

        return ResponseEntity.ok(repository.save(novoProduto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUTO_EDITAR') or hasRole('ADMIN')")
    @CacheEvict(value = "produtos", allEntries = true)
    public ResponseEntity<Produto> atualizar(@PathVariable Long id, @RequestBody @Valid ProdutoRequest dto) {
        return repository.findById(id).map(produto -> {
            produto.setNome(dto.nome());
            produto.setEan13(dto.ean13());
            produto.setDun14(dto.dun14());
            produto.setUnidadeMedida(dto.unidadeMedida());
            produto.setPesoBrutoKg(dto.pesoBrutoKg());
            produto.setNcm(dto.ncm());
            produto.setCest(dto.cest());
            produto.setValorUnitarioPadrao(dto.valorUnitarioPadrao());

            if (dto.ativo() != null)
                produto.setAtivo(dto.ativo());
            if (dto.controlaLote() != null)
                produto.setControlaLote(dto.controlaLote());
            if (dto.controlaValidade() != null)
                produto.setControlaValidade(dto.controlaValidade());
            if (dto.controlaSerie() != null)
                produto.setControlaSerie(dto.controlaSerie());

            produto.setUnidadeArmazenagem(dto.unidadeArmazenagem());
            if (dto.fatorConversao() != null)
                produto.setFatorConversao(dto.fatorConversao());

            return ResponseEntity.ok(repository.save(produto));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUTO_EXCLUIR') or hasRole('ADMIN')")
    @CacheEvict(value = "produtos", allEntries = true)
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