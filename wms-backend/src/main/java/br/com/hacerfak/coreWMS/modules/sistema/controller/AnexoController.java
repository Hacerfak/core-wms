package br.com.hacerfak.coreWMS.modules.sistema.controller;

import br.com.hacerfak.coreWMS.modules.sistema.domain.Anexo;
import br.com.hacerfak.coreWMS.modules.sistema.repository.AnexoRepository;
import br.com.hacerfak.coreWMS.modules.sistema.service.AnexoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/anexos")
@RequiredArgsConstructor
public class AnexoController {

    private final AnexoService anexoService;
    private final AnexoRepository anexoRepository;

    @PostMapping("/upload")
    public ResponseEntity<Anexo> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("entidadeTipo") String entidadeTipo,
            @RequestParam("entidadeId") Long entidadeId,
            @RequestParam(value = "descricao", required = false) String descricao) {

        Anexo anexo = anexoService.uploadArquivo(file, entidadeTipo, entidadeId, descricao);
        return ResponseEntity.ok(anexo);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        Anexo anexo = anexoRepository.findById(id).orElseThrow();
        byte[] dados = anexoService.recuperarArquivo(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + anexo.getNomeArquivo() + "\"")
                .contentType(MediaType.parseMediaType(anexo.getContentType()))
                .body(dados);
    }

    @GetMapping("/listar")
    public ResponseEntity<List<Anexo>> listar(
            @RequestParam String entidadeTipo,
            @RequestParam Long entidadeId) {
        return ResponseEntity.ok(anexoRepository.findByEntidadeTipoAndEntidadeId(entidadeTipo, entidadeId));
    }
}