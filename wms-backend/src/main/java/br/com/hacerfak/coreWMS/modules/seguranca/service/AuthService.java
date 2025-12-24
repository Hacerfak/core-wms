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
import org.springframework.transaction.annotation.Transactional;

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
        // A autenticação chama o AuthorizationService que já corrigimos acima.
        // O objeto 'usuario' retornado aqui já terá a lista 'acessos' preenchida.
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
        var auth = authenticationManager.authenticate(usernamePassword);
        Usuario usuario = (Usuario) auth.getPrincipal();

        List<EmpresaResumoDTO> acessos = new ArrayList<>();

        for (UsuarioEmpresa acesso : usuario.getAcessos()) {
            if (!acesso.getEmpresa().isAtivo())
                continue;

            String perfilExibicao = acesso.getRole() == UserRole.ADMIN ? "Administrador" : "Colaborador";

            acessos.add(new EmpresaResumoDTO(
                    acesso.getEmpresa().getId(),
                    acesso.getEmpresa().getRazaoSocial(),
                    acesso.getEmpresa().getCnpj(),
                    acesso.getEmpresa().getTenantId(),
                    perfilExibicao));
        }

        var token = tokenService.generateToken(usuario, null, List.of());
        return new LoginResponseDTO(token, usuario.getId(), usuario.getLogin(), usuario.getRole().name(), acessos);
    }

    @Transactional(readOnly = true) // Adicione Transactional para garantir sessão aberta
    public LoginResponseDTO selecionarEmpresa(String tenantId) {
        String tenantAtual = TenantContext.getTenant();
        TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);

        Usuario usuario;
        try {
            String login = SecurityContextHolder.getContext().getAuthentication().getName();

            // CORREÇÃO: Usar findByLoginWithAcessos para evitar LazyException ao checar
            // permissão
            usuario = usuarioRepository.findByLoginWithAcessos(login).orElseThrow();

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

    @Transactional(readOnly = true)
    public LoginResponseDTO atualizarCredenciais() {
        String tenantAtual = TenantContext.getTenant();
        TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);
        Usuario usuario;
        try {
            String login = SecurityContextHolder.getContext().getAuthentication().getName();
            // CORREÇÃO: Carrega usuário completo
            usuario = usuarioRepository.findByLoginWithAcessos(login).orElseThrow();
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