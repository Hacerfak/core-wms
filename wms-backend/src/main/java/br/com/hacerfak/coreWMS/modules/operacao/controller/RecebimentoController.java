package br.com.hacerfak.coreWMS.modules.operacao.controller;

import br.com.hacerfak.coreWMS.modules.operacao.domain.Recebimento;
import br.com.hacerfak.coreWMS.modules.operacao.dto.ConferenciaRequest;
import br.com.hacerfak.coreWMS.modules.operacao.repository.RecebimentoRepository;
import br.com.hacerfak.coreWMS.modules.operacao.repository.VolumeRecebimentoRepository;
import br.com.hacerfak.coreWMS.modules.operacao.service.NfeImportService;
import br.com.hacerfak.coreWMS.modules.operacao.service.RecebimentoService;
import br.com.hacerfak.coreWMS.modules.operacao.dto.RecebimentoResumoDTO;
import br.com.hacerfak.coreWMS.modules.operacao.dto.VolumeResumoDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication; // Importação para autenticação

import java.util.List;

@RestController
@RequestMapping("/api/recebimentos")
@RequiredArgsConstructor
public class RecebimentoController {

    private final RecebimentoRepository recebimentoRepository;
    private final NfeImportService nfeImportService;
    private final RecebimentoService recebimentoService;
    @Autowired
    private VolumeRecebimentoRepository volumeRepository; // Injete o repositório

    // --- 1. VISÃO GERAL (Dashboard) ---
    @GetMapping
    public ResponseEntity<List<RecebimentoResumoDTO>> listar() { // <--- Mudou o tipo de retorno
        // Usa a busca otimizada que não carrega itens nem produtos profundos
        return ResponseEntity.ok(recebimentoRepository.findAllResumo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Recebimento> buscarPorId(@PathVariable Long id) {
        // Usa o método novo que carrega os itens
        return recebimentoRepository.findByIdComItens(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- 2. IMPORTAÇÃO XML (Passo Inicial) ---
    @PostMapping(value = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Recebimento> importarNfe(@RequestParam("file") MultipartFile file) {
        // Processa o XML, cria o Recebimento e os Itens previstos
        Recebimento recebimento = nfeImportService.importarXml(file);
        return ResponseEntity.ok(recebimento);
    }

    // --- 3. CONFERÊNCIA CEGA & GERAÇÃO DE LPN (Passo Operacional) ---
    // O operador conta e o sistema gera uma etiqueta (LPN) para colar no
    // pallet/caixa
    @PostMapping("/{id}/volume")
    public ResponseEntity<String> gerarVolume(
            @PathVariable Long id,
            @RequestBody @Valid ConferenciaRequest dto,
            Authentication authentication) { // <--- 1. Injeta o usuário

        String usuarioLogado = authentication.getName(); // <--- 2. Pega o login

        // 3. Passa para o service
        String lpnGerado = recebimentoService.gerarVolume(id, dto, usuarioLogado);

        return ResponseEntity.ok(lpnGerado);
    }

    // --- 4. FINALIZAÇÃO (Confronto) ---
    // Valida se a soma dos volumes bate com a Nota Fiscal
    @PostMapping("/{id}/finalizar")
    public ResponseEntity<String> finalizar(@PathVariable Long id) {
        try {
            recebimentoService.finalizarConferencia(id);
            return ResponseEntity.ok("Conferência finalizada com sucesso! Volumes liberados para armazenagem.");
        } catch (IllegalStateException e) {
            // Retorna erro 400 se houver divergência (contagem != nota)
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao finalizar: " + e.getMessage());
        }
    }

    // --- 5. GESTÃO ---
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelarRecebimento(@PathVariable Long id) {
        return recebimentoRepository.findById(id).map(recebimento -> {
            // Regra de proteção: Não apaga se já virou estoque oficial
            if ("FINALIZADO".equals(recebimento.getStatus().toString())) {
                return ResponseEntity.badRequest()
                        .body("Não é possível excluir recebimento FINALIZADO (Estoque já gerado/liberado).");
            }
            recebimentoRepository.delete(recebimento);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        recebimentoService.cancelarConferencia(id);
        return ResponseEntity.noContent().build();
    }

    // NOVO: Lista os LPNs de um item específico
    @GetMapping("/{id}/produtos/{produtoId}/volumes")
    public ResponseEntity<List<VolumeResumoDTO>> listarVolumesDoItem(
            @PathVariable Long id,
            @PathVariable Long produtoId) {

        // Usa o novo método otimizado
        List<VolumeResumoDTO> volumes = volumeRepository.findResumoByRecebimentoAndProduto(id, produtoId);

        return ResponseEntity.ok(volumes);
    }
}