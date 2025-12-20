package br.com.hacerfak.coreWMS.modules.cadastro.controller;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.EmpresaDados;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.EmpresaDadosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.util.Enumeration;

@RestController
@RequestMapping("/api/empresa-dados") // Endpoint Renomeado
@RequiredArgsConstructor
public class EmpresaDadosController {

    private final EmpresaDadosRepository repository;

    @GetMapping
    public ResponseEntity<EmpresaDados> getDadosEmpresa() {
        return repository.findById(1L).map(empresa -> {
            empresa.setCertificadoSenha(null);
            empresa.setCertificadoArquivo(null);
            return ResponseEntity.ok(empresa);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('CONFIG_GERENCIAR') or hasRole('ADMIN')")
    public ResponseEntity<EmpresaDados> atualizarDados(@RequestBody EmpresaDados dto) {
        return repository.findById(1L).map(empresa -> {
            // Atualiza APENAS dados cadastrais
            empresa.setRazaoSocial(dto.getRazaoSocial());
            empresa.setNomeFantasia(dto.getNomeFantasia());
            empresa.setCnpj(dto.getCnpj());
            empresa.setInscricaoEstadual(dto.getInscricaoEstadual());
            empresa.setInscricaoMunicipal(dto.getInscricaoMunicipal());
            empresa.setCnaePrincipal(dto.getCnaePrincipal());
            empresa.setRegimeTributario(dto.getRegimeTributario());

            empresa.setEmail(dto.getEmail());
            empresa.setTelefone(dto.getTelefone());
            empresa.setWebsite(dto.getWebsite());

            empresa.setCep(dto.getCep());
            empresa.setLogradouro(dto.getLogradouro());
            empresa.setNumero(dto.getNumero());
            empresa.setComplemento(dto.getComplemento());
            empresa.setBairro(dto.getBairro());
            empresa.setCidade(dto.getCidade());
            empresa.setUf(dto.getUf());

            // NÃO ATUALIZA MAIS CONFIGS AQUI (foram para SistemaConfigController)

            return ResponseEntity.ok(repository.save(empresa));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/certificado", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('CONFIG_GERENCIAR') or hasRole('ADMIN')")
    public ResponseEntity<EmpresaDados> uploadCertificado(
            @RequestParam("file") MultipartFile file,
            @RequestParam("senha") String senha) {

        try {
            EmpresaDados config = repository.findById(1L).orElseThrow();

            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(file.getInputStream(), senha.toCharArray());

            Enumeration<String> aliases = ks.aliases();
            if (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
                config.setValidadeCertificado(cert.getNotAfter().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime());
                config.setNomeCertificado(file.getOriginalFilename());
            }

            config.setCertificadoArquivo(file.getBytes());
            config.setCertificadoSenha(senha);

            repository.save(config);

            // Retorna o objeto atualizado (sem senha)
            config.setCertificadoSenha(null);
            config.setCertificadoArquivo(null);

            return ResponseEntity.ok(config);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao processar certificado: " + e.getMessage());
        }
    }
}