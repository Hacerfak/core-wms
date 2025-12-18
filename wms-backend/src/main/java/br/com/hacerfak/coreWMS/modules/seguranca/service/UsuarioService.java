package br.com.hacerfak.coreWMS.modules.seguranca.service;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Empresa;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.EmpresaRepository;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.*;
import br.com.hacerfak.coreWMS.modules.seguranca.dto.*;
import br.com.hacerfak.coreWMS.modules.seguranca.repository.*;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final EmpresaRepository empresaRepository;
    private final PerfilRepository perfilRepository;
    private final UsuarioPerfilRepository usuarioPerfilRepository;
    private final PasswordEncoder passwordEncoder;

    // --- 1. VERIFICAÇÃO PRÉVIA ---
    public VerificarUsuarioDTO verificarExistencia(String login) {
        String tenantOriginal = TenantContext.getTenant();
        TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID); // Busca no Master
        try {
            return usuarioRepository.findByLogin(login)
                    .map(u -> new VerificarUsuarioDTO(true, u.getId(), u.getLogin()))
                    .orElse(new VerificarUsuarioDTO(false, null, login));
        } finally {
            TenantContext.setTenant(tenantOriginal);
        }
    }

    // --- 2. LISTAGEM (Mantida) ---
    public List<UsuarioDTO> listarPorTenantAtual() {
        String tenantId = TenantContext.getTenant();
        List<UsuarioDTO> resultado = new ArrayList<>();

        TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);
        try {
            Empresa empresa = empresaRepository.findByTenantId(tenantId)
                    .orElseThrow(() -> new EntityNotFoundException("Empresa não encontrada"));

            // JOIN FETCH para performance
            List<UsuarioEmpresa> vinculos = usuarioEmpresaRepository.findByEmpresaId(empresa.getId());

            for (UsuarioEmpresa vinculo : vinculos) {
                resultado.add(new UsuarioDTO(
                        vinculo.getUsuario().getId(),
                        vinculo.getUsuario().getLogin(),
                        "Carregando...",
                        vinculo.getUsuario().isAtivo()));
            }
        } finally {
            TenantContext.setTenant(tenantId);
        }

        // Busca perfis locais
        for (int i = 0; i < resultado.size(); i++) {
            UsuarioDTO u = resultado.get(i);
            if (u.login().equalsIgnoreCase("master")) {
                resultado.set(i, new UsuarioDTO(u.id(), u.login(), "MASTER", true));
                continue;
            }
            var perfis = usuarioPerfilRepository.findByUsuarioId(u.id());
            if (!perfis.isEmpty()) {
                resultado.set(i, new UsuarioDTO(u.id(), u.login(), perfis.get(0).getPerfil().getNome(), u.ativo()));
            } else {
                resultado.set(i, new UsuarioDTO(u.id(), u.login(), "Sem Perfil", u.ativo()));
            }
        }
        return resultado;
    }

    // --- Helper para pegar o ID do usuário logado ---
    private Long getUsuarioLogadoId() {
        try {
            Usuario usuario = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return usuario.getId();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao identificar usuário logado.");
        }
    }

    // --- EXCLUSÃO LOCAL (Admin da Empresa) ---
    public void removerAcessoLocal(Long usuarioAlvoId) {
        // TRAVA DE SEGURANÇA: Não pode se remover
        if (getUsuarioLogadoId().equals(usuarioAlvoId)) {
            throw new IllegalArgumentException("Você não pode remover seu próprio acesso.");
        }

        String tenantAtual = TenantContext.getTenant();

        // 1. Remove perfil no banco do tenant
        var perfis = usuarioPerfilRepository.findByUsuarioId(usuarioAlvoId);
        usuarioPerfilRepository.deleteAll(perfis);

        // 2. Remove vínculo no banco Master
        TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);
        try {
            Empresa empresa = empresaRepository.findByTenantId(tenantAtual)
                    .orElseThrow(() -> new EntityNotFoundException("Empresa atual não identificada no Master"));

            var vinculo = usuarioEmpresaRepository.findByEmpresaId(empresa.getId()).stream()
                    .filter(v -> v.getUsuario().getId().equals(usuarioAlvoId))
                    .findFirst()
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Vínculo não encontrado ou usuário não pertence a esta empresa"));

            usuarioEmpresaRepository.delete(vinculo);
        } finally {
            TenantContext.setTenant(tenantAtual);
        }
    }

    // --- EXCLUSÃO GLOBAL (Master Admin) ---
    public void excluirUsuarioGlobal(Long usuarioAlvoId) {
        // TRAVA DE SEGURANÇA
        if (getUsuarioLogadoId().equals(usuarioAlvoId)) {
            throw new IllegalArgumentException("Você não pode excluir sua própria conta globalmente.");
        }

        String tenantOriginal = TenantContext.getTenant();

        try {
            TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);
            Usuario usuario = usuarioRepository.findById(usuarioAlvoId)
                    .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

            List<UsuarioEmpresa> acessos = new ArrayList<>(usuario.getAcessos());

            for (UsuarioEmpresa acesso : acessos) {
                String tenantDestino = acesso.getEmpresa().getTenantId();
                try {
                    TenantContext.setTenant(tenantDestino);
                    var perfisLocais = usuarioPerfilRepository.findByUsuarioId(usuarioAlvoId);
                    if (!perfisLocais.isEmpty()) {
                        usuarioPerfilRepository.deleteAll(perfisLocais);
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao limpar tenant " + tenantDestino);
                }
            }

            TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);
            usuarioRepository.deleteById(usuarioAlvoId);

        } finally {
            TenantContext.setTenant(tenantOriginal);
        }
    }

    public void atualizarUsuario(Long id, CriarUsuarioRequest request) {
        String tenantId = TenantContext.getTenant();
        boolean isMasterUser = false;

        // 1. Atualizar dados Globais (Login e Senha) no Banco Master
        TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);
        try {
            Usuario usuario = usuarioRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));

            // Verifica se é o Master/Admin para pular lógica de perfil depois
            if (usuario.getRole() == UserRole.ADMIN) {
                isMasterUser = true;
            }

            // Verifica se mudou o login e se já existe
            if (!usuario.getLogin().equals(request.login())) {
                if (isMasterUser) {
                    // Opcional: Bloquear mudança de login do Master se quiser
                    // throw new IllegalArgumentException("Não é permitido alterar o login do
                    // Master.");
                }
                if (usuarioRepository.findByLogin(request.login()).isPresent()) {
                    throw new IllegalArgumentException("Este login já está em uso por outro usuário.");
                }
                usuario.setLogin(request.login());
            }

            // Atualiza senha apenas se foi informada
            if (request.senha() != null && !request.senha().isBlank()) {
                usuario.setSenha(passwordEncoder.encode(request.senha()));
            }

            usuarioRepository.save(usuario);
        } finally {
            TenantContext.setTenant(tenantId);
        }

        // 2. Atualizar Perfil no Banco Local (Tenant)
        // Se for Master, NÃO mexe em perfis locais (ele não precisa)
        // Se não veio perfilId no request (ex: edição parcial), ignoramos também
        if (!isMasterUser && request.perfilId() != null) {
            var perfisAntigos = usuarioPerfilRepository.findByUsuarioId(id);
            usuarioPerfilRepository.deleteAll(perfisAntigos);

            Perfil novoPerfil = perfilRepository.findById(request.perfilId())
                    .orElseThrow(() -> new EntityNotFoundException("Perfil não encontrado."));

            UsuarioPerfil usuarioPerfil = UsuarioPerfil.builder()
                    .usuarioId(id)
                    .perfil(novoPerfil)
                    .build();

            usuarioPerfilRepository.save(usuarioPerfil);
        }
    }

    // --- 3. CRIAÇÃO OU VÍNCULO (O Coração da mudança) ---
    public void salvarUsuarioParaEmpresa(CriarUsuarioRequest request) {
        String tenantId = TenantContext.getTenant();

        if (tenantId == null || tenantId.equals("wms_master")) {
            throw new IllegalArgumentException("Selecione uma empresa para adicionar usuários.");
        }

        Usuario usuarioGlobal;

        // --- PASSO A: MASTER DB (Identidade) ---
        TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);
        try {
            Empresa empresa = empresaRepository.findByTenantId(tenantId)
                    .orElseThrow(() -> new EntityNotFoundException("Empresa não encontrada"));

            Optional<Usuario> existente = usuarioRepository.findByLogin(request.login());

            if (existente.isPresent()) {
                // CENÁRIO 1: Usuário já existe -> Apenas vincula
                usuarioGlobal = existente.get();

                // Verifica se já está nesta empresa
                if (usuarioEmpresaRepository.existsByUsuarioIdAndEmpresaId(usuarioGlobal.getId(), empresa.getId())) {
                    // Se já existe, não é erro, apenas seguimos para atualizar o perfil (opcional)
                    // ou lançamos erro. Vamos permitir para atualizar perfil.
                } else {
                    // Cria vínculo com a empresa
                    UsuarioEmpresa vinculo = UsuarioEmpresa.builder()
                            .usuario(usuarioGlobal)
                            .empresa(empresa)
                            .role(UserRole.USER)
                            .build();
                    usuarioEmpresaRepository.save(vinculo);
                }
            } else {
                // CENÁRIO 2: Usuário NOVO -> Cria e vincula
                if (request.senha() == null || request.senha().isBlank()) {
                    throw new IllegalArgumentException("Senha obrigatória para novos usuários.");
                }

                usuarioGlobal = Usuario.builder()
                        .login(request.login())
                        .senha(passwordEncoder.encode(request.senha()))
                        .role(UserRole.USER) // <--- MUDANÇA: Sempre nasce como USER comum
                        .ativo(true)
                        .build();
                usuarioRepository.save(usuarioGlobal);

                // O vínculo global também é genérico
                UsuarioEmpresa vinculo = UsuarioEmpresa.builder()
                        .usuario(usuarioGlobal)
                        .empresa(empresa)
                        .role(UserRole.USER) // <--- MUDANÇA: A role aqui é irrelevante agora
                        .build();
                usuarioEmpresaRepository.save(vinculo);
            }
        } finally {
            TenantContext.setTenant(tenantId);
        }

        // --- PASSO B: TENANT DB (Perfil) ---
        // Agora, no banco da empresa, definimos o papel dele (Gerente, Operador, etc)
        Perfil perfil = perfilRepository.findById(request.perfilId())
                .orElseThrow(() -> new EntityNotFoundException("Perfil não encontrado"));

        // Remove perfil anterior se houver (para permitir troca de perfil)
        var perfilAnterior = usuarioPerfilRepository.findByUsuarioId(usuarioGlobal.getId());
        if (!perfilAnterior.isEmpty()) {
            usuarioPerfilRepository.deleteAll(perfilAnterior);
        }

        UsuarioPerfil novoPerfil = UsuarioPerfil.builder()
                .usuarioId(usuarioGlobal.getId())
                .perfil(perfil)
                .build();

        usuarioPerfilRepository.save(novoPerfil);
    }
}