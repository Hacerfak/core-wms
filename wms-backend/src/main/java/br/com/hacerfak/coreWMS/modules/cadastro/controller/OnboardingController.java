package br.com.hacerfak.coreWMS.modules.cadastro.controller;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.Empresa;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.EmpresaRepository;
import br.com.hacerfak.coreWMS.modules.cadastro.service.CertificadoService;
import br.com.hacerfak.coreWMS.modules.cadastro.service.TenantProvisioningService;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.UserRole;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.Usuario;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.UsuarioEmpresa;
import br.com.hacerfak.coreWMS.modules.seguranca.repository.UsuarioEmpresaRepository;
import br.com.hacerfak.coreWMS.modules.seguranca.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final CertificadoService certificadoService;
    private final TenantProvisioningService provisioningService;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;

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

            // 6. Vincula o Usuário Logado como ADMIN
            String loginUsuario = SecurityContextHolder.getContext().getAuthentication().getName();

            // Usamos orElseThrow porque o usuário TEM que estar logado para chegar aqui
            Usuario usuario = usuarioRepository.findByLogin(loginUsuario)
                    .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado"));

            UsuarioEmpresa vinculo = UsuarioEmpresa.builder()
                    .usuario(usuario)
                    .empresa(novaEmpresa)
                    .role(UserRole.ADMIN)
                    .build();

            usuarioEmpresaRepository.save(vinculo);

            return ResponseEntity
                    .ok("Empresa " + dados.getRazaoSocial() + " criada com sucesso! Faça login novamente.");

        } catch (Exception e) {
            // Se der erro ao salvar no banco, idealmente deveríamos desfazer a criação do
            // DB,
            // mas para o MVP, apenas logamos o erro.
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao salvar dados da empresa: " + e.getMessage());
        }
    }
}