package br.com.hacerfak.coreWMS.modules.seguranca.service;

import br.com.hacerfak.coreWMS.core.multitenant.TenantContext; // <--- IMPORTANTE
import br.com.hacerfak.coreWMS.modules.seguranca.domain.Usuario;
import br.com.hacerfak.coreWMS.modules.seguranca.dto.AuthenticationDTO;
import br.com.hacerfak.coreWMS.modules.seguranca.dto.EmpresaResumoDTO;
import br.com.hacerfak.coreWMS.modules.seguranca.dto.LoginResponseDTO;
import br.com.hacerfak.coreWMS.modules.seguranca.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;

    // --- LOGICA DE LOGIN ---
    public LoginResponseDTO login(AuthenticationDTO data) {
        // ... (Mantenha o código de login igual, ele já funciona pois o token inicial
        // não tem tenant) ...
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
        var auth = authenticationManager.authenticate(usernamePassword);

        Usuario usuario = (Usuario) auth.getPrincipal();

        List<EmpresaResumoDTO> acessos = usuario.getAcessos().stream()
                .filter(acesso -> acesso.getEmpresa().isAtivo())
                .map(acesso -> new EmpresaResumoDTO(
                        acesso.getEmpresa().getId(),
                        acesso.getEmpresa().getRazaoSocial(),
                        acesso.getEmpresa().getTenantId(),
                        acesso.getRole().name()))
                .toList();

        var token = tokenService.generateToken(usuario, null);

        return new LoginResponseDTO(token, usuario.getLogin(), acessos);
    }

    // --- LÓGICA DE SELEÇÃO DE EMPRESA (CORRIGIDA) ---
    public LoginResponseDTO selecionarEmpresa(String tenantId) {
        // 1. Salva o contexto atual (só por precaução)
        String tenantAtual = TenantContext.getTenant();

        // 2. FORÇA O CONTEXTO PARA O MASTER
        // Precisamos acessar a tabela 'tb_usuario' e 'tb_empresa' que só existem no
        // Master
        TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);

        try {
            // Agora a busca vai rodar no banco correto (Master)
            String login = SecurityContextHolder.getContext().getAuthentication().getName();

            Usuario usuario = usuarioRepository.findByLogin(login)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            // Valida se ele realmente tem acesso à empresa solicitada
            boolean temAcesso = usuario.getAcessos().stream()
                    .anyMatch(a -> a.getEmpresa().getTenantId().equals(tenantId) && a.getEmpresa().isAtivo());

            if (!temAcesso) {
                throw new RuntimeException("Acesso negado a esta empresa");
            }

            // Gera NOVO Token (COM Tenant)
            var tokenComTenant = tokenService.generateToken(usuario, tenantId);

            return new LoginResponseDTO(tokenComTenant, usuario.getLogin(), List.of());

        } finally {
            // 3. (Opcional) Restaura o contexto anterior para não afetar o resto da
            // requisição
            // Embora neste caso a requisição acabe aqui.
            TenantContext.setTenant(tenantAtual);
        }
    }
}