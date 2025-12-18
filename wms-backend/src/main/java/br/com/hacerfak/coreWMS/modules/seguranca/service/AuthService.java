package br.com.hacerfak.coreWMS.modules.seguranca.service;

import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.*;
import br.com.hacerfak.coreWMS.modules.seguranca.dto.*;
import br.com.hacerfak.coreWMS.modules.seguranca.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioPerfilRepository usuarioPerfilRepository;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;

    public LoginResponseDTO login(AuthenticationDTO data) {
        // 1. Autentica no Spring Security (Banco Master)
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
        var auth = authenticationManager.authenticate(usernamePassword);
        Usuario usuario = (Usuario) auth.getPrincipal();

        List<EmpresaResumoDTO> acessos = new ArrayList<>();

        // 2. Itera sobre as empresas para buscar o Perfil Específico em cada banco
        // (Isso resolve o bug de mostrar o mesmo perfil em todas)
        for (UsuarioEmpresa acesso : usuario.getAcessos()) {
            if (!acesso.getEmpresa().isAtivo())
                continue;

            String tenantId = acesso.getEmpresa().getTenantId();
            String nomePerfilExibicao = "Carregando...";

            // Guarda o contexto atual (Master)
            String contextoOriginal = TenantContext.getTenant();

            try {
                // A. Troca para o banco da empresa específica
                TenantContext.setTenant(tenantId);

                // B. Se for o ADMIN Global (God Mode), o nome é fixo
                if (usuario.getRole() == UserRole.ADMIN) {
                    nomePerfilExibicao = "MASTER";
                } else {
                    // C. Busca o perfil configurado neste banco
                    List<UsuarioPerfil> perfisLocais = usuarioPerfilRepository.findByUsuarioId(usuario.getId());

                    if (!perfisLocais.isEmpty()) {
                        nomePerfilExibicao = perfisLocais.get(0).getPerfil().getNome();
                    } else {
                        // Caso raro: tem acesso a empresa mas não tem perfil local criado ainda
                        nomePerfilExibicao = "Sem Perfil Definido";
                    }
                }
            } catch (Exception e) {
                nomePerfilExibicao = "Erro ao carregar perfil";
                System.err.println("Erro ao buscar perfil no tenant " + tenantId + ": " + e.getMessage());
            } finally {
                // D. IMPORTANTE: Volta para o banco Master para continuar o loop
                TenantContext.setTenant(contextoOriginal);
            }

            acessos.add(new EmpresaResumoDTO(
                    acesso.getEmpresa().getId(),
                    acesso.getEmpresa().getRazaoSocial(),
                    tenantId,
                    nomePerfilExibicao // Agora enviamos o nome real (ex: "Conferente Senior")
            ));
        }

        // Gera o token inicial
        var token = tokenService.generateToken(usuario, null, List.of());

        return new LoginResponseDTO(token, usuario.getLogin(), usuario.getRole().name(), acessos);
    }

    public LoginResponseDTO selecionarEmpresa(String tenantId) {
        String tenantAtual = TenantContext.getTenant();
        TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);

        Usuario usuario;
        try {
            String login = SecurityContextHolder.getContext().getAuthentication().getName();
            usuario = usuarioRepository.findByLogin(login).orElseThrow();

            boolean temAcesso = usuario.getAcessos().stream()
                    .anyMatch(a -> a.getEmpresa().getTenantId().equals(tenantId) && a.getEmpresa().isAtivo());

            if (!temAcesso)
                throw new RuntimeException("Acesso negado");

        } finally {
            TenantContext.setTenant(tenantAtual);
        }

        // --- CALCULA PERMISSÕES NO BANCO DO TENANT ---
        TenantContext.setTenant(tenantId);
        List<String> authorities = new ArrayList<>();

        try {
            // GOD MODE: Se for ADMIN global, libera tudo
            if (usuario.getRole() == UserRole.ADMIN) {
                authorities.add("ROLE_ADMIN");
                for (PermissaoEnum p : PermissaoEnum.values()) {
                    authorities.add(p.name());
                }
            } else {
                // Usuário comum: Busca permissões do perfil
                var perfis = usuarioPerfilRepository.findByUsuarioId(usuario.getId());
                for (UsuarioPerfil up : perfis) {
                    if (up.getPerfil().isAtivo()) {
                        up.getPerfil().getPermissoes().forEach(p -> authorities.add(p.name()));
                    }
                }
            }
        } finally {
            TenantContext.clear();
        }

        var tokenComTenant = tokenService.generateToken(usuario, tenantId, authorities);
        return new LoginResponseDTO(tokenComTenant, usuario.getLogin(), usuario.getRole().name(), List.of());
    }

    // NOVO MÉTODO: Recarrega permissões sem mudar de empresa
    public LoginResponseDTO atualizarCredenciais() {
        String tenantAtual = TenantContext.getTenant();

        // 1. Identifica usuário logado
        TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);
        Usuario usuario;
        try {
            String login = SecurityContextHolder.getContext().getAuthentication().getName();
            usuario = usuarioRepository.findByLogin(login).orElseThrow();
        } finally {
            TenantContext.setTenant(tenantAtual);
        }

        // 2. Recalcula permissões no Tenant Atual (Igual ao selecionarEmpresa)
        List<String> authorities = new ArrayList<>();

        if (usuario.getRole() == UserRole.ADMIN) {
            authorities.add("ROLE_ADMIN");
            for (PermissaoEnum p : PermissaoEnum.values()) {
                authorities.add(p.name());
            }
        } else {
            // Busca permissões atualizadas no banco
            var perfis = usuarioPerfilRepository.findByUsuarioId(usuario.getId());
            for (UsuarioPerfil up : perfis) {
                if (up.getPerfil().isAtivo()) {
                    up.getPerfil().getPermissoes().forEach(p -> authorities.add(p.name()));
                }
            }
        }

        // 3. Gera novo token
        var novoToken = tokenService.generateToken(usuario, tenantAtual, authorities);

        // Retorna estrutura padrão, mas só o token importa aqui
        return new LoginResponseDTO(novoToken, usuario.getLogin(), usuario.getRole().name(), List.of());
    }
}