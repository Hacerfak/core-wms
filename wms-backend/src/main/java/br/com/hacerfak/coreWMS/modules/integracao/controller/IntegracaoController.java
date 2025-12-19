package br.com.hacerfak.coreWMS.modules.integracao.controller;

import br.com.hacerfak.coreWMS.modules.integracao.dto.CnpjResponse;
import br.com.hacerfak.coreWMS.modules.integracao.service.SefazService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/integracao")
@RequiredArgsConstructor
public class IntegracaoController {

    private final SefazService sefazService;

    // Agora exige UF, pois a Consulta Cadastro é Estadual
    @GetMapping("/sefaz/{uf}/cnpj/{cnpj}")
    public ResponseEntity<CnpjResponse> consultarPorCnpj(@PathVariable String uf, @PathVariable String cnpj) {
        return ResponseEntity.ok(sefazService.consultarCadastro(uf, cnpj, null));
    }

    @GetMapping("/sefaz/{uf}/ie/{ie}")
    public ResponseEntity<CnpjResponse> consultarPorIe(@PathVariable String uf, @PathVariable String ie) {
        return ResponseEntity.ok(sefazService.consultarCadastro(uf, null, ie));
    }
}