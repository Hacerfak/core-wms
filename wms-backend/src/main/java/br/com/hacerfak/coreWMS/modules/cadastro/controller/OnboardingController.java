package br.com.hacerfak.coreWMS.modules.cadastro.controller;

import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.core.service.CryptoService;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Empresa;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.EmpresaDados;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.EmpresaDadosRepository;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.EmpresaRepository;
import br.com.hacerfak.coreWMS.modules.cadastro.service.CertificadoService;
import br.com.hacerfak.coreWMS.modules.cadastro.service.TenantProvisioningService;
import br.com.hacerfak.coreWMS.modules.integracao.dto.CnpjResponse;
import br.com.hacerfak.coreWMS.modules.integracao.service.SefazService;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.Perfil;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.UserRole;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.Usuario;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.UsuarioEmpresa;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.UsuarioPerfil;
import br.com.hacerfak.coreWMS.modules.seguranca.repository.PerfilRepository;
import br.com.hacerfak.coreWMS.modules.seguranca.repository.UsuarioEmpresaRepository;
import br.com.hacerfak.coreWMS.modules.seguranca.repository.UsuarioPerfilRepository;
import br.com.hacerfak.coreWMS.modules.seguranca.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final CertificadoService certificadoService;
    private final TenantProvisioningService provisioningService;
    private final SefazService sefazService;
    private final CryptoService cryptoService;

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;

    private final PerfilRepository perfilRepository;
    private final UsuarioPerfilRepository usuarioPerfilRepository;
    private final EmpresaDadosRepository empresaDadosRepository;

    @PostMapping("/upload-certificado")
    public ResponseEntity<?> uploadCertificado(
            @RequestParam("file") MultipartFile file,
            @RequestParam("senha") String senha,
            @RequestParam(value = "uf", defaultValue = "SP") String uf) {

        String tenantOriginal = TenantContext.getTenant();
        String tenantIdGerado = null;

        try {
            var dadosCert = certificadoService.extrairDados(file, senha);
            String cnpjLimpo = dadosCert.getCnpj().replaceAll("\\D", "");

            TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);

            if (empresaRepository.existsByCnpj(cnpjLimpo)) {
                throw new IllegalArgumentException("Empresa com CNPJ " + dadosCert.getCnpj() + " já cadastrada.");
            }

            tenantIdGerado = "tenant_" + cnpjLimpo;

            // --- TRANSAÇÃO DISTRIBUÍDA MANUAL ---
            // 1. Cria DB Físico
            provisioningService.criarBancoDeDados(tenantIdGerado);
            provisioningService.inicializarConfiguracao(tenantIdGerado, dadosCert.getRazaoSocial(), cnpjLimpo);

            // 2. Salva no Master
            Empresa novaEmpresa = Empresa.builder()
                    .razaoSocial(dadosCert.getRazaoSocial())
                    .cnpj(cnpjLimpo)
                    .tenantId(tenantIdGerado)
                    .nomeCertificado(file.getOriginalFilename())
                    .validadeCertificado(dadosCert.getValidade())
                    .ativo(true)
                    .build();
            empresaRepository.save(novaEmpresa);

            String loginUsuario = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByLogin(loginUsuario).orElseThrow();

            UsuarioEmpresa vinculo = UsuarioEmpresa.builder()
                    .usuario(usuario)
                    .empresa(novaEmpresa)
                    .role(UserRole.USER) // Role genérico
                    .build();
            usuarioEmpresaRepository.save(vinculo);

            // 3. Configura Tenant (Dados Sensíveis)
            try {
                TenantContext.setTenant(tenantIdGerado);

                Perfil perfilAdmin = perfilRepository.findAll().stream()
                        .filter(p -> p.getNome().toUpperCase().contains("ADMIN"))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Perfil Admin não encontrado no template."));

                UsuarioPerfil up = UsuarioPerfil.builder()
                        .usuarioId(usuario.getId())
                        .perfil(perfilAdmin)
                        .build();
                usuarioPerfilRepository.save(up);

                EmpresaDados config = empresaDadosRepository.findById(1L).orElseThrow();
                config.setCertificadoArquivo(file.getBytes());

                // --- CRIPTOGRAFIA APLICADA ---
                config.setCertificadoSenha(cryptoService.encrypt(senha));

                config.setNomeCertificado(file.getOriginalFilename());
                config.setValidadeCertificado(dadosCert.getValidade().atStartOfDay());
                config.setUf(uf);

                empresaDadosRepository.save(config);

                if (!uf.equalsIgnoreCase("MA")) {
                    try {
                        CnpjResponse dadosSefaz = sefazService.consultarCadastro(uf, cnpjLimpo, null);
                        config.setRazaoSocial(dadosSefaz.getRazaoSocial());
                        config.setNomeFantasia(dadosSefaz.getNomeFantasia());
                        config.setInscricaoEstadual(dadosSefaz.getIe());
                        config.setRegimeTributario(dadosSefaz.getRegimeTributario());
                        config.setCnaePrincipal(dadosSefaz.getCnaePrincipal());
                        config.setCep(dadosSefaz.getCep());
                        config.setLogradouro(dadosSefaz.getLogradouro());
                        config.setNumero(dadosSefaz.getNumero());
                        config.setComplemento(dadosSefaz.getComplemento());
                        config.setBairro(dadosSefaz.getBairro());
                        config.setCidade(dadosSefaz.getCidade());
                        config.setUf(dadosSefaz.getUf());
                        empresaDadosRepository.save(config);
                    } catch (Exception ex) {
                        System.err.println(">>> Aviso: Falha na consulta SEFAZ (Ignorado): " + ex.getMessage());
                    }
                }

            } catch (Exception e) {
                // Erro na configuração interna do tenant, mas banco já criado.
                // Re-lança para cair no catch externo e fazer rollback.
                throw e;
            }

            return ResponseEntity.ok("Ambiente criado com sucesso!");

        } catch (Exception e) {
            e.printStackTrace();

            // --- ROLLBACK DE COMPENSAÇÃO ---
            if (tenantIdGerado != null) {
                // 1. Remove do Master se foi salvo
                try {
                    TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);
                    // O ideal seria deletar o registro da tabela tb_empresa, mas precisaria buscar
                    // pelo tenantId
                    // Se o banco foi criado mas deu erro antes de salvar no master, o drop resolve.
                } catch (Exception exRollback) {
                }

                // 2. Dropa o banco físico
                provisioningService.dropDatabase(tenantIdGerado);
            }

            // Restaura contexto
            if (tenantOriginal != null)
                TenantContext.setTenant(tenantOriginal);
            else
                TenantContext.clear();

            return ResponseEntity.internalServerError().body("Erro ao criar ambiente: " + e.getMessage());
        } finally {
            // Garante retorno ao contexto original em caso de sucesso
            if (tenantOriginal != null)
                TenantContext.setTenant(tenantOriginal);
            else
                TenantContext.clear();
        }
    }
}