package br.com.hacerfak.coreWMS.modules.seguranca.service;

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
        // 1. Autentica no Spring Security (Valida senha)
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
        var auth = authenticationManager.authenticate(usernamePassword);

        Usuario usuario = (Usuario) auth.getPrincipal();

        // 2. Gera a lista de empresas ativas
        List<EmpresaResumoDTO> acessos = usuario.getAcessos().stream()
                .filter(acesso -> acesso.getEmpresa().isAtivo())
                .map(acesso -> new EmpresaResumoDTO(
                        acesso.getEmpresa().getId(),
                        acesso.getEmpresa().getRazaoSocial(),
                        acesso.getEmpresa().getTenantId(),
                        acesso.getRole().name()))
                .toList();

        // 3. Gera Token INICIAL (Sem Tenant)
        var token = tokenService.generateToken(usuario, null);

        return new LoginResponseDTO(token, usuario.getLogin(), acessos);
    }

    // --- LÓGICA DE SELEÇÃO DE EMPRESA ---
    public LoginResponseDTO selecionarEmpresa(String tenantId) {
        // Pega o usuário já autenticado pelo Token atual
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

        // Retorna o novo token (a lista de empresas pode ir vazia ou repetida,
        // opcional)
        return new LoginResponseDTO(tokenComTenant, usuario.getLogin(), List.of());
    }
}