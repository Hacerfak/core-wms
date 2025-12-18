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

        try {
            // 1. Extrair dados do certificado
            var dados = certificadoService.extrairDados(file, senha);

            // 2. Valida se já existe
            if (empresaRepository.existsByCnpj(dados.getCnpj())) {
                return ResponseEntity.badRequest().body("Empresa com CNPJ " + dados.getCnpj() + " já cadastrada.");
            }

            // 3. Gera ID do Tenant
            String cnpjLimpo = dados.getCnpj().replaceAll("\\D", "");
            String tenantId = "tenant_" + cnpjLimpo;

            // 4. Cria o Banco de Dados Físico e Roda Migrations
            provisioningService.criarBancoDeDados(tenantId);

            // 5. Inicializa a tabela de configuração interna do tenant
            provisioningService.inicializarConfiguracao(tenantId, dados.getRazaoSocial(), dados.getCnpj());

            // 6. Salva a Empresa no Banco Master
            Empresa novaEmpresa = Empresa.builder()
                    .razaoSocial(dados.getRazaoSocial())
                    .cnpj(cnpjLimpo)
                    .tenantId(tenantId)
                    .nomeCertificado(file.getOriginalFilename())
                    .validadeCertificado(dados.getValidade())
                    .ativo(true)
                    .build();

            empresaRepository.save(novaEmpresa);

            // 7. Vincula o Usuário Logado
            String loginUsuario = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByLogin(loginUsuario).orElseThrow();

            UsuarioEmpresa vinculo = UsuarioEmpresa.builder()
                    .usuario(usuario)
                    .empresa(novaEmpresa)
                    .role(UserRole.USER)
                    .build();
            usuarioEmpresaRepository.save(vinculo);

            // 8. CRÍTICO: Entrar no Tenant e dar o perfil de Administrador
            try {
                TenantContext.setTenant(tenantId);

                // Busca o perfil que a migration deve ter criado
                Perfil perfilAdmin = perfilRepository.findAll().stream()
                        .filter(p -> p.getNome().equalsIgnoreCase("Administrador Local")
                                || p.getNome().equalsIgnoreCase("Administrador"))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException(
                                "Perfil de Administrador não encontrado no novo banco. Verifique as migrations V17+."));

                UsuarioPerfil up = UsuarioPerfil.builder()
                        .usuarioId(usuario.getId())
                        .perfil(perfilAdmin)
                        .build();

                usuarioPerfilRepository.save(up);

            } finally {
                TenantContext.clear();
            }

            return ResponseEntity.ok("Empresa criada e usuário vinculado como Administrador!");

        } catch (Exception e) {
            e.printStackTrace();
            // Retorna o erro detalhado para o frontend mostrar no Alert
            return ResponseEntity.internalServerError().body("Erro no Onboarding: " + e.getMessage());
        }
    }
}