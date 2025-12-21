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

        // 2. Itera sobre as empresas (PERFORMANCE FIX: Sem abrir conexão com cada
        // tenant)
        for (UsuarioEmpresa acesso : usuario.getAcessos()) {
            if (!acesso.getEmpresa().isAtivo())
                continue;

            // Retornamos o Role genérico (ADMIN/USER) que está na tabela de vínculo do
            // Master.
            // O nome específico do "Perfil" (ex: "Gerente de Estoque") será carregado
            // apenas quando o usuário selecionar a empresa.
            String perfilExibicao = acesso.getRole() == UserRole.ADMIN ? "Administrador" : "Colaborador";

            acessos.add(new EmpresaResumoDTO(
                    acesso.getEmpresa().getId(),
                    acesso.getEmpresa().getRazaoSocial(),
                    acesso.getEmpresa().getCnpj(),
                    acesso.getEmpresa().getTenantId(),
                    perfilExibicao));
        }

        var token = tokenService.generateToken(usuario, null, List.of());

        // CORREÇÃO: Passando usuario.getId()
        return new LoginResponseDTO(token, usuario.getId(), usuario.getLogin(), usuario.getRole().name(), acessos);
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

        TenantContext.setTenant(tenantId);
        List<String> authorities = new ArrayList<>();

        try {
            if (usuario.getRole() == UserRole.ADMIN) {
                authorities.add("ROLE_ADMIN");
                for (PermissaoEnum p : PermissaoEnum.values()) {
                    authorities.add(p.name());
                }
            } else {
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

        // CORREÇÃO: Passando usuario.getId()
        return new LoginResponseDTO(tokenComTenant, usuario.getId(), usuario.getLogin(), usuario.getRole().name(),
                List.of());
    }

    public LoginResponseDTO atualizarCredenciais() {
        String tenantAtual = TenantContext.getTenant();
        TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);
        Usuario usuario;
        try {
            String login = SecurityContextHolder.getContext().getAuthentication().getName();
            usuario = usuarioRepository.findByLogin(login).orElseThrow();
        } finally {
            TenantContext.setTenant(tenantAtual);
        }

        List<String> authorities = new ArrayList<>();
        if (usuario.getRole() == UserRole.ADMIN) {
            authorities.add("ROLE_ADMIN");
            for (PermissaoEnum p : PermissaoEnum.values()) {
                authorities.add(p.name());
            }
        } else {
            var perfis = usuarioPerfilRepository.findByUsuarioId(usuario.getId());
            for (UsuarioPerfil up : perfis) {
                if (up.getPerfil().isAtivo()) {
                    up.getPerfil().getPermissoes().forEach(p -> authorities.add(p.name()));
                }
            }
        }

        var novoToken = tokenService.generateToken(usuario, tenantAtual, authorities);

        // CORREÇÃO: Passando usuario.getId()
        return new LoginResponseDTO(novoToken, usuario.getId(), usuario.getLogin(), usuario.getRole().name(),
                List.of());
    }
}