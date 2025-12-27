package br.com.hacerfak.coreWMS.modules.seguranca.service;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.modules.auditoria.service.AuditService;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Empresa;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.EmpresaRepository;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.*;
import br.com.hacerfak.coreWMS.modules.seguranca.dto.*;
import br.com.hacerfak.coreWMS.modules.seguranca.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final EmpresaRepository empresaRepository;
    private final PerfilRepository perfilRepository;
    private final UsuarioPerfilRepository usuarioPerfilRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformTransactionManager transactionManager;
    private final AuditService auditService;

    // --- HELPERS ---
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private <T> T callAsMaster(java.util.function.Supplier<T> action) {
        String original = TenantContext.getTenant();
        try {
            TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);
            return action.get();
        } finally {
            if (original != null)
                TenantContext.setTenant(original);
            else
                TenantContext.clear();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void runAsMaster(Runnable action) {
        String original = TenantContext.getTenant();
        try {
            TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);
            action.run();
        } finally {
            if (original != null)
                TenantContext.setTenant(original);
            else
                TenantContext.clear();
        }
    }

    // --- CRUD USUÁRIO ---

    public List<UsuarioDTO> listarTodosGlobal() {
        return callAsMaster(() -> usuarioRepository.findAll().stream().map(u -> new UsuarioDTO(
                u.getId(), u.getLogin(), u.getEmail(), u.getNome(),
                u.getRole() == UserRole.ADMIN ? "MASTER ADMIN" : "Usuário Comum",
                u.isAtivo(), u.getRole() == UserRole.ADMIN)).collect(Collectors.toList()));
    }

    public UsuarioDTO buscarPorId(Long id) {
        return callAsMaster(() -> {
            Usuario u = usuarioRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
            return new UsuarioDTO(
                    u.getId(), u.getLogin(), u.getEmail(), u.getNome(),
                    u.getRole() == UserRole.ADMIN ? "MASTER" : "USER", u.isAtivo(), u.getRole() == UserRole.ADMIN);
        });
    }

    public UsuarioDTO salvarUsuarioGlobal(Long id, CriarUsuarioRequest req) {
        return callAsMaster(() -> {
            return new TransactionTemplate(transactionManager).execute(status -> {
                Usuario usuario;
                boolean isSelf = false;

                Usuario logado = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                boolean isManager = logado.getRole() == UserRole.ADMIN;

                if (id != null) {
                    usuario = usuarioRepository.findById(id)
                            .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
                    if (usuario.getId().equals(logado.getId()))
                        isSelf = true;

                    if (!usuario.getLogin().equals(req.login())
                            && usuarioRepository.findByLoginWithAcessos(req.login()).isPresent()) {
                        throw new IllegalArgumentException("Login já em uso.");
                    }
                } else {
                    if (usuarioRepository.findByLoginWithAcessos(req.login()).isPresent())
                        throw new IllegalArgumentException("Login já existe.");
                    usuario = new Usuario();
                    usuario.setRole(UserRole.USER);
                }

                usuario.setNome(req.nome());
                usuario.setLogin(req.login());
                usuario.setEmail(req.email());

                if (isSelf && !isManager) {
                    // Ignora status se for auto-edição de usuário comum
                } else {
                    usuario.setAtivo(req.ativo() != null ? req.ativo() : true);
                }

                if (req.senha() != null && !req.senha().isBlank()) {
                    usuario.setSenha(passwordEncoder.encode(req.senha()));
                } else if (id == null) {
                    throw new IllegalArgumentException("Senha é obrigatória para novos usuários.");
                }

                Usuario salvo = usuarioRepository.save(usuario);
                // Note: auditService.registrarLogManual não é estritamente necessário aqui pois
                // o save() dispara o Listener,
                // mas para consistência em ações críticas pode ser mantido ou removido.
                // O Listener Global já pega o INSERT/UPDATE aqui.
                return new UsuarioDTO(salvo.getId(), salvo.getLogin(), salvo.getEmail(), salvo.getNome(), "USER",
                        salvo.isAtivo(), false);
            });
        });
    }

    public void excluirUsuarioGlobal(Long id) {
        Usuario alvo = callAsMaster(() -> usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado")));

        Usuario logado = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long logadoId = logado.getId();

        if ("master".equalsIgnoreCase(alvo.getLogin()) || alvo.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("O usuário MASTER não pode ser excluído.");
        }
        if (logadoId.equals(id)) {
            throw new IllegalArgumentException("Não é possível excluir o próprio usuário.");
        }

        List<UsuarioEmpresa> acessos = callAsMaster(() -> new ArrayList<>(alvo.getAcessos()));
        for (UsuarioEmpresa acesso : acessos) {
            String tenant = acesso.getEmpresa().getTenantId();
            try {
                TenantContext.setTenant(tenant);
                var perfis = usuarioPerfilRepository.findByUsuarioId(id);
                usuarioPerfilRepository.deleteAll(perfis);
            } catch (Exception e) {
            }
        }

        runAsMaster(() -> {
            new TransactionTemplate(transactionManager).execute(status -> {
                // --- CAPTURA CONTEXTO ANTES ---
                String usuarioLogado = SecurityContextHolder.getContext().getAuthentication().getName();
                // IP e UA podem ser nulos aqui se não estivermos num contexto Web, ou
                // capturamos via helpers se possível
                // Como é um service, simplificamos ou injetamos HttpServletRequest se
                // necessário.
                // Para simplicidade, passamos valores fixos ou nulos (o AuditService tratará)

                // CORREÇÃO: Passando os novos parâmetros
                auditService.registrarLog(
                        "DELETE",
                        "Usuario",
                        String.valueOf(id),
                        "Exclusão Global de Usuário: " + alvo.getNome() + " (" + alvo.getLogin() + ")",
                        TenantContext.DEFAULT_TENANT_ID, // Estamos no master
                        usuarioLogado,
                        "MANUAL", // IP
                        "BACKEND" // UserAgent
                );

                usuarioEmpresaRepository.deleteByUsuarioId(id);
                usuarioRepository.deleteById(id);
                return null;
            });
        });
    }

    // --- VÍNCULOS ---

    public List<Perfil> listarPerfisPorEmpresa(Long empresaId) {
        Empresa empresa = callAsMaster(() -> empresaRepository.findById(empresaId).orElseThrow());
        String original = TenantContext.getTenant();
        try {
            TenantContext.setTenant(empresa.getTenantId());
            return perfilRepository.findAll();
        } finally {
            if (original != null)
                TenantContext.setTenant(original);
            else
                TenantContext.clear();
        }
    }

    public List<EmpresaResumoDTO> listarEmpresasDoUsuario(Long usuarioId) {
        String originalTenant = TenantContext.getTenant();
        TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);
        try {
            // CORREÇÃO: Usar findById NÃO resolve pois findById padrão é LAZY nos acessos.
            // Precisamos buscar via login ou criar um findByIdWithAcessos.
            // Como já temos o ID, vamos buscar o usuário e forçar a inicialização ou usar
            // query.

            // Opção 1 (Simples): findById + Hibernate.initialize (dentro de @Transactional)
            // Opção 2 (Performance): Criar findByIdWithAcessos no repository.

            // Vamos usar a opção existente via Login para aproveitar o método criado:
            Usuario uSimples = usuarioRepository.findById(usuarioId).orElseThrow();
            Usuario usuario = usuarioRepository.findByLoginWithAcessos(uSimples.getLogin()).orElseThrow();

            List<EmpresaResumoDTO> lista = new ArrayList<>();

            for (UsuarioEmpresa vinculo : usuario.getAcessos()) {
                String tenant = vinculo.getEmpresa().getTenantId();
                String perfilNome = "Usuário";
                try {
                    TenantContext.setTenant(tenant);
                    var perfis = usuarioPerfilRepository.findByUsuarioId(usuarioId);
                    if (!perfis.isEmpty())
                        perfilNome = perfis.get(0).getPerfil().getNome();
                } catch (Exception e) {
                    perfilNome = "Erro leitura";
                }

                TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);
                lista.add(new EmpresaResumoDTO(vinculo.getEmpresa().getId(), vinculo.getEmpresa().getRazaoSocial(),
                        vinculo.getEmpresa().getCnpj(), vinculo.getEmpresa().getTenantId(), perfilNome));
            }
            return lista;
        } finally {
            if (originalTenant != null)
                TenantContext.setTenant(originalTenant);
            else
                TenantContext.clear();
        }
    }

    public void vincularEmpresa(Long usuarioId, Long empresaId, Long perfilId) {
        Usuario usuarioMaster = callAsMaster(() -> usuarioRepository.findById(usuarioId).orElseThrow());
        Empresa empresaMaster = callAsMaster(() -> empresaRepository.findById(empresaId).orElseThrow());

        runAsMaster(() -> {
            new TransactionTemplate(transactionManager).execute(status -> {
                if (!usuarioEmpresaRepository.existsByUsuarioIdAndEmpresaId(usuarioId, empresaId)) {
                    usuarioEmpresaRepository.save(UsuarioEmpresa.builder()
                            .usuario(usuarioMaster).empresa(empresaMaster).role(UserRole.USER).build());
                }
                return null;
            });
        });

        String tenantDestino = empresaMaster.getTenantId();
        String original = TenantContext.getTenant();
        try {
            TenantContext.setTenant(tenantDestino);
            new TransactionTemplate(transactionManager).execute(status -> {
                Perfil perfilAlvo = perfilRepository.findById(perfilId)
                        .orElseThrow(() -> new EntityNotFoundException("Perfil não encontrado nesta empresa."));
                var antigos = usuarioPerfilRepository.findByUsuarioId(usuarioId);
                usuarioPerfilRepository.deleteAll(antigos);
                usuarioPerfilRepository.save(UsuarioPerfil.builder().usuarioId(usuarioId).perfil(perfilAlvo).build());
                return null;
            });
        } finally {
            if (original != null)
                TenantContext.setTenant(original);
            else
                TenantContext.clear();
        }
    }

    public void desvincularEmpresa(Long usuarioId, Long empresaId) {
        Usuario usuarioAlvo = callAsMaster(() -> usuarioRepository.findById(usuarioId).orElseThrow());
        Long logadoId = ((Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();

        if (logadoId.equals(usuarioId)) {
            throw new IllegalArgumentException("Você não pode remover seus próprios vínculos/acessos.");
        }

        if (usuarioAlvo.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Não é possível remover vínculos do usuário Master (Admin Global).");
        }

        Empresa empresa = callAsMaster(() -> empresaRepository.findById(empresaId).orElseThrow());
        String tenantDestino = empresa.getTenantId();
        String original = TenantContext.getTenant();
        try {
            TenantContext.setTenant(tenantDestino);
            usuarioPerfilRepository.deleteAll(usuarioPerfilRepository.findByUsuarioId(usuarioId));
        } catch (Exception e) {
        } finally {
            if (original != null)
                TenantContext.setTenant(original);
            else
                TenantContext.clear();
        }

        runAsMaster(() -> {
            new TransactionTemplate(transactionManager).execute(status -> {
                var vinculo = usuarioEmpresaRepository.findByEmpresaId(empresaId).stream()
                        .filter(v -> v.getUsuario().getId().equals(usuarioId)).findFirst();
                vinculo.ifPresent(usuarioEmpresaRepository::delete);
                return null;
            });
        });
    }
}