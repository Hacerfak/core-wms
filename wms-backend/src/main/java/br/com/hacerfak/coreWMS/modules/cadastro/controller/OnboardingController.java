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
    // @Transactional <--- REMOVA ESTA LINHA (Causa do erro)
    public ResponseEntity<?> uploadCertificado(
            @RequestParam("file") MultipartFile file,
            @RequestParam("senha") String senha) {

        // 1. Extrair dados do certificado
        var dados = certificadoService.extrairDados(file, senha);

        // 2. Valida se já existe
        if (empresaRepository.existsByCnpj(dados.getCnpj())) {
            return ResponseEntity.badRequest().body("Empresa com CNPJ " + dados.getCnpj() + " já cadastrada.");
        }

        // 3. Gera ID do Tenant
        String cnpjLimpo = dados.getCnpj().replaceAll("\\D", "");
        String tenantId = "tenant_" + cnpjLimpo;

        // 4. Cria o Banco de Dados Físico (AGORA VAI FUNCIONAR)
        // Isso precisa rodar fora de transação
        provisioningService.criarBancoDeDados(tenantId);

        // --- NOVO: Inicializa a tabela de configuração interna do tenant ---
        provisioningService.inicializarConfiguracao(tenantId, dados.getRazaoSocial(), dados.getCnpj());

        try {
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

            // 6. Vincula o Usuário Logado (que é o Master Admin criando a empresa) ou cria
            // o primeiro user
            // OBS: Se quem está criando a empresa é o ADMIN do sistema, ele já tem God
            // Mode.
            // Mas se for um auto-cadastro, precisamos dar o perfil de ADMIN LOCAL.

            String loginUsuario = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByLogin(loginUsuario).orElseThrow();

            // Vínculo Global (Genérico)
            UsuarioEmpresa vinculo = UsuarioEmpresa.builder()
                    .usuario(usuario)
                    .empresa(novaEmpresa)
                    .role(UserRole.USER) // <--- É apenas um usuário dessa empresa
                    .build();
            usuarioEmpresaRepository.save(vinculo);

            // 7. CRÍTICO: Entrar no Tenant e dar o perfil de Administrador
            try {
                TenantContext.setTenant(tenantId); // Muda para o banco novo

                // Busca o perfil que a migration V17 criou
                Perfil perfilAdmin = perfilRepository.findAll().stream()
                        .filter(p -> p.getNome().equalsIgnoreCase("Administrador Local")
                                || p.getNome().equalsIgnoreCase("Administrador"))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException(
                                "Perfil de Administrador não encontrado no template do tenant."));

                UsuarioPerfil up = UsuarioPerfil.builder()
                        .usuarioId(usuario.getId())
                        .perfil(perfilAdmin)
                        .build();

                usuarioPerfilRepository.save(up);

            } finally {
                TenantContext.clear(); // Limpa o contexto
            }

            return ResponseEntity.ok("Empresa criada e usuário vinculado como Administrador!");

        } catch (Exception e) {
            // Se der erro ao salvar no banco, idealmente deveríamos desfazer a criação do
            // DB,
            // mas para o MVP, apenas logamos o erro.
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao salvar dados da empresa: " + e.getMessage());
        }
    }
}