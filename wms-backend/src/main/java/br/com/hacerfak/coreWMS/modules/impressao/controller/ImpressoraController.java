package br.com.hacerfak.coreWMS.modules.impressao.controller;

import br.com.hacerfak.coreWMS.modules.impressao.domain.Impressora;
import br.com.hacerfak.coreWMS.modules.impressao.dto.ImpressoraRequest;
import br.com.hacerfak.coreWMS.modules.impressao.repository.ImpressoraRepository;
import br.com.hacerfak.coreWMS.modules.impressao.service.ImpressaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/impressao/impressoras")
@RequiredArgsConstructor
public class ImpressoraController {

    private final ImpressoraRepository impressoraRepository;
    private final ImpressaoService impressaoService;

    @GetMapping
    @PreAuthorize("hasAuthority('CONFIG_SISTEMA') or hasRole('ADMIN')")
    public ResponseEntity<List<Impressora>> listar() {
        return ResponseEntity.ok(impressoraRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONFIG_SISTEMA') or hasRole('ADMIN')")
    public ResponseEntity<Impressora> criar(@RequestBody @Valid ImpressoraRequest dto) {
        return ResponseEntity.ok(impressaoService.cadastrarImpressora(dto));
    }

    @PostMapping("/{id}/teste")
    @PreAuthorize("hasAuthority('CONFIG_SISTEMA') or hasRole('ADMIN')")
    public ResponseEntity<Void> testarImpressao(@PathVariable Long id) {
        // ZPL Simples de Teste
        String zplTeste = "^XA^FO50,50^ADN,36,20^FDTESTE DE CONEXAO^FS^FO50,100^ADN,18,10^FD" +
                LocalDateTime.now().toString() + "^FS^XZ";

        // Pega o usuário logado do contexto de segurança (simplificado aqui)
        String usuario = "ADMIN_TESTE";

        System.out.println(">>> DEBUG CONTROLLER: Recebi pedido de teste para ID: " + id);

        impressaoService.enviarParaFila(zplTeste, id, usuario, "TESTE_SISTEMA");
        return ResponseEntity.ok().build();
    }
}