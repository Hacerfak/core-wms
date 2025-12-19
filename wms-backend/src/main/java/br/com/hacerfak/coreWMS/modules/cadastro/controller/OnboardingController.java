package br.com.hacerfak.coreWMS.modules.cadastro.controller;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.Empresa;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.EmpresaRepository;
import br.com.hacerfak.coreWMS.modules.cadastro.service.CertificadoService;
import br.com.hacerfak.coreWMS.modules.cadastro.service.TenantProvisioningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.*;
import br.com.hacerfak.coreWMS.modules.seguranca.repository.*;

@RestController
@RequestMapping("/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final CertificadoService certificadoService;
    private final TenantProvisioningService provisioningService;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final PerfilRepository perfilRepository;
    private final UsuarioPerfilRepository usuarioPerfilRepository;

    @PostMapping("/upload-certificado")
    public ResponseEntity<?> uploadCertificado(
            @RequestParam("file") MultipartFile file,
            @RequestParam("senha") String senha) {

        String tenantOriginal = TenantContext.getTenant();

        try {
            // 1. Extrair dados do certificado
            var dados = certificadoService.extrairDados(file, senha);

            // --- FORÇA O CONTEXTO MASTER PARA VALIDAR/SALVAR EMPRESA ---
            TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);

            // 2. Valida se já existe
            if (empresaRepository.existsByCnpj(dados.getCnpj())) {
                throw new IllegalArgumentException("Empresa com CNPJ " + dados.getCnpj() + " já está cadastrada.");
            }

            // 3. Gera ID do Tenant
            String cnpjLimpo = dados.getCnpj().replaceAll("\\D", "");
            String tenantId = "tenant_" + cnpjLimpo;

            // 4. Cria Infra (Banco e Config)
            provisioningService.criarBancoDeDados(tenantId);
            provisioningService.inicializarConfiguracao(tenantId, dados.getRazaoSocial(), dados.getCnpj());

            // 5. Salva a Empresa no Banco Master
            Empresa novaEmpresa = Empresa.builder()
                    .razaoSocial(dados.getRazaoSocial())
                    .cnpj(cnpjLimpo)
                    .tenantId(tenantId)
                    .nomeCertificado(file.getOriginalFilename())
                    .validadeCertificado(dados.getValidade())
                    .ativo(true)
                    .build();

            empresaRepository.save(novaEmpresa);

            // 6. Vincula o Usuário Logado
            String loginUsuario = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByLogin(loginUsuario).orElseThrow();

            UsuarioEmpresa vinculo = UsuarioEmpresa.builder()
                    .usuario(usuario)
                    .empresa(novaEmpresa)
                    .role(UserRole.USER)
                    .build();
            usuarioEmpresaRepository.save(vinculo);

            // 7. Perfil Administrador no Tenant
            try {
                TenantContext.setTenant(tenantId);

                Perfil perfilAdmin = perfilRepository.findAll().stream()
                        .filter(p -> p.getNome().equalsIgnoreCase("Administrador Local")
                                || p.getNome().equalsIgnoreCase("Administrador"))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException(
                                "Perfil de Administrador não encontrado no novo banco."));

                UsuarioPerfil up = UsuarioPerfil.builder()
                        .usuarioId(usuario.getId())
                        .perfil(perfilAdmin)
                        .build();

                usuarioPerfilRepository.save(up);

            } finally {
                if (tenantOriginal != null) {
                    TenantContext.setTenant(tenantOriginal);
                } else {
                    TenantContext.clear();
                }
            }

            return ResponseEntity.ok("Ambiente criado com sucesso!");

        } catch (IllegalArgumentException e) {
            // Erros de validação (Senha errada, CNPJ duplicado) -> 400 Bad Request
            // O front vai receber apenas a mensagem limpa
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (Exception e) {
            // Erros técnicos inesperados -> 500 Internal Server Error
            e.printStackTrace();
            if (tenantOriginal != null) {
                TenantContext.setTenant(tenantOriginal);
            } else {
                TenantContext.clear();
            }
            return ResponseEntity.internalServerError().body("Erro interno no Onboarding: " + e.getMessage());
        }
    }
}