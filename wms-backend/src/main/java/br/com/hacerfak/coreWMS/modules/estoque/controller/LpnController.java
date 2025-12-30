package br.com.hacerfak.coreWMS.modules.estoque.controller;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ProdutoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Localizacao;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Lpn;
import br.com.hacerfak.coreWMS.modules.estoque.repository.LocalizacaoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.repository.LpnRepository;
import br.com.hacerfak.coreWMS.modules.estoque.service.LpnService;
import br.com.hacerfak.coreWMS.modules.impressao.service.ImpressaoService;
import br.com.hacerfak.coreWMS.modules.impressao.service.ZplGeneratorService;
import br.com.hacerfak.coreWMS.modules.operacao.dto.AddItemLpnRequest;
import br.com.hacerfak.coreWMS.modules.operacao.dto.GerarLpnMassaRequest;
import br.com.hacerfak.coreWMS.modules.operacao.dto.GerarLpnVaziaRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estoque/lpns")
@RequiredArgsConstructor
public class LpnController {

    private final LpnService lpnService; // Service Principal
    private final LpnRepository lpnRepository;
    private final ZplGeneratorService zplService;
    private final ImpressaoService impressaoService;

    // Repositórios auxiliares para lookup de entidades
    private final ProdutoRepository produtoRepository;
    private final LocalizacaoRepository localizacaoRepository;

    // --- OPERAÇÃO ---

    /**
     * Gera LPNs vazias para impressão antecipada.
     */
    @PostMapping("/gerar-vazias")
    @PreAuthorize("hasAuthority('ESTOQUE_MOVIMENTAR') or hasRole('ADMIN')")
    public ResponseEntity<List<String>> gerarLpnsVazias(@RequestBody @Valid GerarLpnVaziaRequest dto) {
        String usuario = getUsuarioLogado();
        List<String> codigos = lpnService.gerarLpnsVazias(dto.quantidade(), dto.formatoId(), dto.solicitacaoId(),
                usuario);
        return ResponseEntity.ok(codigos);
    }

    /**
     * Gera LPNs em massa (Carga Fechada) já com conteúdo e estoque.
     */
    @PostMapping("/gerar-massa")
    @PreAuthorize("hasAuthority('ESTOQUE_MOVIMENTAR') or hasRole('ADMIN')")
    public ResponseEntity<List<String>> gerarLpnsMassa(@RequestBody @Valid GerarLpnMassaRequest dto) {
        String usuario = getUsuarioLogado();

        // Busca entidades necessárias para passar ao Service
        Produto produto = produtoRepository.findByCodigoBarras(dto.sku())
                .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado"));

        Localizacao local = localizacaoRepository.findById(dto.localizacaoId())
                .orElseThrow(() -> new EntityNotFoundException("Localização não encontrada"));

        List<String> codigos = lpnService.gerarLpnsComConteudo(
                produto,
                dto.qtdPorVolume(),
                dto.qtdVolumes(),
                dto.lote(),
                dto.dataValidade(),
                dto.numeroSerie(),
                local,
                dto.solicitacaoId(),
                dto.formatoId(),
                usuario);

        return ResponseEntity.ok(codigos);
    }

    /**
     * Adiciona Item na LPN (Bipagem / Conferência).
     */
    @PostMapping("/{codigo}/itens")
    @PreAuthorize("hasAuthority('ESTOQUE_MOVIMENTAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> adicionarItem(
            @PathVariable String codigo,
            @RequestBody @Valid AddItemLpnRequest dto) {

        String usuario = getUsuarioLogado();
        lpnService.adicionarItem(codigo, dto, dto.formatoId(), usuario);
        return ResponseEntity.ok().build();
    }

    /**
     * Finaliza a LPN (Fecha o volume para armazenagem).
     */
    @PostMapping("/{codigo}/fechar")
    @PreAuthorize("hasAuthority('ESTOQUE_MOVIMENTAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> fecharLpn(@PathVariable String codigo) {
        String usuario = getUsuarioLogado();
        lpnService.fecharLpn(codigo, usuario);
        return ResponseEntity.ok().build();
    }

    // --- CONSULTAS E IMPRESSÃO (Mantidos do seu código original) ---

    @GetMapping
    @PreAuthorize("hasAuthority('ESTOQUE_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<List<Lpn>> listarLpns() {
        return ResponseEntity.ok(lpnRepository.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ESTOQUE_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<Lpn> buscarPorId(@PathVariable Long id) {
        return lpnRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new EntityNotFoundException("LPN não encontrada"));
    }

    @GetMapping("/codigo/{codigo}")
    @PreAuthorize("hasAuthority('ESTOQUE_VISUALIZAR') or hasAuthority('RECEBIMENTO_CONFERIR') or hasRole('ADMIN')")
    public ResponseEntity<Lpn> buscarPorCodigo(@PathVariable String codigo) {
        return lpnRepository.findByCodigo(codigo)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new EntityNotFoundException("LPN não encontrada com o código: " + codigo));
    }

    @GetMapping(value = "/{id}/etiqueta/preview", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> visualizarEtiqueta(@PathVariable Long id,
            @RequestParam(required = false) Long templateId) {
        Lpn lpn = lpnRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("LPN não encontrada"));
        String zpl = zplService.gerarZplParaLpn(templateId, lpn);
        return ResponseEntity.ok(zpl);
    }

    @PostMapping("/{id}/imprimir")
    @PreAuthorize("hasAuthority('ESTOQUE_MOVIMENTAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> enviarParaImpressora(
            @PathVariable Long id,
            @RequestParam(required = false) Long templateId,
            @RequestParam Long impressoraId) {

        Lpn lpn = lpnRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("LPN não encontrada"));
        String zpl = zplService.gerarZplParaLpn(templateId, lpn);
        String usuario = getUsuarioLogado();
        impressaoService.enviarParaFila(zpl, impressoraId, usuario, "LPN_" + lpn.getCodigo());

        return ResponseEntity.ok().build();
    }

    // Helper para pegar usuário
    private String getUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "SISTEMA";
    }
}