package br.com.hacerfak.coreWMS.modules.cadastro.controller;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.EmpresaConfig;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.EmpresaConfigRepository;
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
@RequestMapping("/api/empresa-config")
@RequiredArgsConstructor
public class EmpresaConfigController {

    private final EmpresaConfigRepository repository;

    @GetMapping
    public ResponseEntity<EmpresaConfig> getDadosEmpresa() {
        return repository.findById(1L).map(empresa -> {
            // Remove dados sensíveis da resposta JSON
            empresa.setCertificadoSenha(null);
            empresa.setCertificadoArquivo(null);
            return ResponseEntity.ok(empresa);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('CONFIG_GERENCIAR') or hasRole('ADMIN')")
    public ResponseEntity<EmpresaConfig> atualizarDados(@RequestBody EmpresaConfig dto) {
        return repository.findById(1L).map(empresa -> {
            // Atualiza todos os campos de texto
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

            // Endereço
            empresa.setCep(dto.getCep());
            empresa.setLogradouro(dto.getLogradouro());
            empresa.setNumero(dto.getNumero());
            empresa.setComplemento(dto.getComplemento());
            empresa.setBairro(dto.getBairro());
            empresa.setCidade(dto.getCidade());
            empresa.setUf(dto.getUf()); // Atualiza a UF principal

            // Configs
            empresa.setRecebimentoCegoObrigatorio(dto.isRecebimentoCegoObrigatorio());
            empresa.setPermiteEstoqueNegativo(dto.isPermiteEstoqueNegativo());

            return ResponseEntity.ok(repository.save(empresa));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/certificado", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('CONFIG_GERENCIAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> uploadCertificado(
            @RequestParam("file") MultipartFile file,
            @RequestParam("senha") String senha) {

        try {
            EmpresaConfig config = repository.findById(1L).orElseThrow();

            // 1. Extrai Validade do PFX
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(file.getInputStream(), senha.toCharArray());

            Enumeration<String> aliases = ks.aliases();
            if (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
                config.setValidadeCertificado(cert.getNotAfter().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime());
            }

            // 2. Salva Binário, Senha e Nome
            config.setCertificadoArquivo(file.getBytes());
            config.setCertificadoSenha(senha);
            config.setNomeCertificado(file.getOriginalFilename());

            repository.save(config);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao processar certificado: Senha inválida ou arquivo corrompido.");
        }
    }
}