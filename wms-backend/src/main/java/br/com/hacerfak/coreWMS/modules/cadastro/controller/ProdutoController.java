package br.com.hacerfak.coreWMS.modules.cadastro.controller;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.cadastro.dto.ProdutoRequest;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ProdutoRepository;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ParceiroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoRepository repository;
    private final ParceiroRepository parceiroRepository; // Caso precise validar depositante

    // --- READ (Listar Todos) ---
    @GetMapping
    @PreAuthorize("hasAuthority('PRODUTO_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<Page<Produto>> listarTodos(
            @PageableDefault(page = 0, size = 10, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(repository.findAll(pageable));
    }

    // --- READ (Buscar por ID) ---
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUTO_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<Produto> buscarPorId(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- CREATE ---
    @PostMapping
    @PreAuthorize("hasAuthority('PRODUTO_CRIAR') or hasRole('ADMIN')")
    public ResponseEntity<Produto> criar(@RequestBody ProdutoRequest dto) {
        // Validação (Simplificada)
        // Em prod, buscaríamos o Parceiro pelo ID vindo do DTO (se houver)
        // Aqui assumimos o Parceiro Padrão (ID 1) se não informado, para
        // compatibilidade
        var parceiro = parceiroRepository.findById(1L).orElseThrow();

        if (repository.existsBySku(dto.sku())) { // Nota: Ajustar se usar multi-tenant
            return ResponseEntity.badRequest().build();
        }

        Produto novoProduto = Produto.builder()
                .nome(dto.nome())
                .sku(dto.sku())
                .ean13(dto.ean13())
                .unidadeMedida(dto.unidadeMedida())
                .pesoBrutoKg(dto.pesoBrutoKg())
                .ncm(dto.ncm())
                .depositante(parceiro)
                .ativo(true)
                .build();

        return ResponseEntity.ok(repository.save(novoProduto));
    }

    // --- UPDATE ---
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUTO_EDITAR') or hasRole('ADMIN')")
    public ResponseEntity<Produto> atualizar(@PathVariable Long id, @RequestBody Produto produtoAtualizado) {
        return repository.findById(id).map(produto -> {
            produto.setNome(produtoAtualizado.getNome());
            produto.setEan13(produtoAtualizado.getEan13());
            produto.setUnidadeMedida(produtoAtualizado.getUnidadeMedida());
            produto.setPesoBrutoKg(produtoAtualizado.getPesoBrutoKg());
            produto.setNcm(produtoAtualizado.getNcm());
            // Não alteramos SKU nem Depositante por segurança
            return ResponseEntity.ok(repository.save(produto));
        }).orElse(ResponseEntity.notFound().build());
    }

    // --- DELETE (Físico ou Lógico) ---
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUTO_EXCLUIR') or hasRole('ADMIN')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (!repository.existsById(id))
            return ResponseEntity.notFound().build();

        // Em WMS, geralmente não deletamos produto movimentado. Apenas inativamos.
        repository.deleteById(id); // Se tiver FK (estoque), o banco vai bloquear (ConstraintViolation), o que é
                                   // seguro.
        return ResponseEntity.noContent().build();
    }
}