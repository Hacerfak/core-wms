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
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
        var auth = authenticationManager.authenticate(usernamePassword);
        Usuario usuario = (Usuario) auth.getPrincipal();

        // Lista de empresas (Apenas leitura do Master)
        List<EmpresaResumoDTO> acessos = usuario.getAcessos().stream()
                .filter(acesso -> acesso.getEmpresa().isAtivo())
                .map(acesso -> new EmpresaResumoDTO(
                        acesso.getEmpresa().getId(),
                        acesso.getEmpresa().getRazaoSocial(),
                        acesso.getEmpresa().getTenantId(),
                        acesso.getRole().name()))
                .toList();

        // Token inicial sem tenant e sem permissões específicas
        var token = tokenService.generateToken(usuario, null, List.of());

        return new LoginResponseDTO(token, usuario.getLogin(), acessos);
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
        return new LoginResponseDTO(tokenComTenant, usuario.getLogin(), List.of());
    }
}