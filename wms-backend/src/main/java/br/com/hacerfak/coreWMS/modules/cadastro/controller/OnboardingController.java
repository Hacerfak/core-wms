package br.com.hacerfak.coreWMS.modules.cadastro.controller;

import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Empresa;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.EmpresaConfig;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.EmpresaConfigRepository;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.EmpresaRepository;
import br.com.hacerfak.coreWMS.modules.cadastro.service.CertificadoService;
import br.com.hacerfak.coreWMS.modules.cadastro.service.TenantProvisioningService;
import br.com.hacerfak.coreWMS.modules.integracao.dto.CnpjResponse;
import br.com.hacerfak.coreWMS.modules.integracao.service.SefazService; // <--- Importante
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
    private final SefazService sefazService; // <--- Injetar Serviço SEFAZ

    // Repositórios MASTER
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;

    // Repositórios TENANT (o Spring resolve via TenantContext)
    private final PerfilRepository perfilRepository;
    private final UsuarioPerfilRepository usuarioPerfilRepository;
    private final EmpresaConfigRepository empresaConfigRepository;

    @PostMapping("/upload-certificado")
    public ResponseEntity<?> uploadCertificado(
            @RequestParam("file") MultipartFile file,
            @RequestParam("senha") String senha,
            @RequestParam(value = "uf", defaultValue = "SP") String uf) { // <--- Recebe UF

        String tenantOriginal = TenantContext.getTenant();

        try {
            // 1. Extrair dados básicos do certificado (offline)
            var dadosCert = certificadoService.extrairDados(file, senha);
            String cnpjLimpo = dadosCert.getCnpj().replaceAll("\\D", "");

            // --- CONTEXTO MASTER ---
            TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);

            // 2. Valida duplicidade
            if (empresaRepository.existsByCnpj(cnpjLimpo)) {
                throw new IllegalArgumentException("Empresa com CNPJ " + dadosCert.getCnpj() + " já cadastrada.");
            }

            // 3. Cria Banco de Dados
            String tenantId = "tenant_" + cnpjLimpo;
            provisioningService.criarBancoDeDados(tenantId);

            // Inicializa a tabela ID 1 com dados básicos extraídos do certificado
            provisioningService.inicializarConfiguracao(tenantId, dadosCert.getRazaoSocial(), cnpjLimpo);

            // 4. Salva Empresa no Master
            Empresa novaEmpresa = Empresa.builder()
                    .razaoSocial(dadosCert.getRazaoSocial())
                    .cnpj(cnpjLimpo)
                    .tenantId(tenantId)
                    .nomeCertificado(file.getOriginalFilename())
                    .validadeCertificado(dadosCert.getValidade())
                    .ativo(true)
                    .build();
            empresaRepository.save(novaEmpresa);

            // 5. Vincula Usuário Logado
            String loginUsuario = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByLogin(loginUsuario).orElseThrow();

            UsuarioEmpresa vinculo = UsuarioEmpresa.builder()
                    .usuario(usuario)
                    .empresa(novaEmpresa)
                    .role(UserRole.USER)
                    .build();
            usuarioEmpresaRepository.save(vinculo);

            // --- CONTEXTO TENANT ---
            // Agora entramos no banco novo para configurar tudo
            try {
                TenantContext.setTenant(tenantId);

                // A. Perfil de Admin
                Perfil perfilAdmin = perfilRepository.findAll().stream()
                        .filter(p -> p.getNome().toUpperCase().contains("ADMIN"))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Perfil Admin não encontrado no template."));

                UsuarioPerfil up = UsuarioPerfil.builder()
                        .usuarioId(usuario.getId())
                        .perfil(perfilAdmin)
                        .build();
                usuarioPerfilRepository.save(up);

                // B. SALVA CERTIFICADO NO BANCO LOCAL
                EmpresaConfig config = empresaConfigRepository.findById(1L).orElseThrow();
                config.setCertificadoArquivo(file.getBytes());
                config.setCertificadoSenha(senha);
                config.setNomeCertificado(file.getOriginalFilename());
                // Validade (converter LocalDate para LocalDateTime no inicio do dia)
                config.setValidadeCertificado(dadosCert.getValidade().atStartOfDay());
                config.setUf(uf); // Salva a UF selecionada inicialmente

                empresaConfigRepository.save(config); // Persiste o certificado para poder usar na consulta

                // C. CONSULTA SEFAZ E AUTO-PREENCHE (O Pulo do Gato!)
                if (!uf.equalsIgnoreCase("MA")) {
                    try {
                        System.out.println(">>> Onboarding: Consultando SEFAZ para " + cnpjLimpo + " na UF " + uf);

                        // O SefazService vai ler o certificado que ACABAMOS de salvar no banco (ID 1)
                        CnpjResponse dadosSefaz = sefazService.consultarCadastro(uf, cnpjLimpo, null);

                        // Atualiza com dados oficiais
                        config.setRazaoSocial(dadosSefaz.getRazaoSocial());
                        config.setNomeFantasia(dadosSefaz.getNomeFantasia());
                        config.setInscricaoEstadual(dadosSefaz.getIe());
                        config.setRegimeTributario(dadosSefaz.getRegimeTributario());
                        config.setCnaePrincipal(dadosSefaz.getCnaePrincipal());

                        // Endereço
                        config.setCep(dadosSefaz.getCep());
                        config.setLogradouro(dadosSefaz.getLogradouro());
                        config.setNumero(dadosSefaz.getNumero());
                        config.setComplemento(dadosSefaz.getComplemento());
                        config.setBairro(dadosSefaz.getBairro());
                        config.setCidade(dadosSefaz.getCidade());
                        config.setUf(dadosSefaz.getUf()); // Atualiza UF se a SEFAZ retornar diferente

                        empresaConfigRepository.save(config);

                        // Opcional: Atualizar também o nome no Master (EmpresaRepository) para ficar
                        // bonito na lista
                        // Mas isso exigiria trocar de contexto de novo. Deixa assim por enquanto.

                    } catch (Exception ex) {
                        System.err.println(">>> Aviso: Falha na consulta SEFAZ durante onboarding: " + ex.getMessage());
                        // Não aborta o processo, o usuário pode corrigir manualmente depois
                    }
                }

            } finally {
                // Restaura contexto
                if (tenantOriginal != null) {
                    TenantContext.setTenant(tenantOriginal);
                } else {
                    TenantContext.clear();
                }
            }

            return ResponseEntity.ok("Ambiente criado com sucesso! Dados importados da SEFAZ.");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            // Restaura contexto em caso de erro
            if (tenantOriginal != null)
                TenantContext.setTenant(tenantOriginal);
            else
                TenantContext.clear();

            return ResponseEntity.internalServerError().body("Erro no Onboarding: " + e.getMessage());
        }
    }
}