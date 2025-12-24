package br.com.hacerfak.coreWMS.modules.cadastro.controller;

import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Empresa;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.EmpresaRepository;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.UserRole;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.Usuario;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.UsuarioEmpresa;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.UsuarioPerfil;
import br.com.hacerfak.coreWMS.modules.seguranca.dto.EmpresaResumoDTO;
import br.com.hacerfak.coreWMS.modules.seguranca.repository.UsuarioPerfilRepository;
import br.com.hacerfak.coreWMS.modules.seguranca.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioPerfilRepository usuarioPerfilRepository;

    @GetMapping("/meus-acessos")
    public ResponseEntity<List<EmpresaResumoDTO>> listarMeusAcessos() {
        String tenantOriginal = TenantContext.getTenant();
        TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);

        try {
            String login = SecurityContextHolder.getContext().getAuthentication().getName();

            // --- CORREÇÃO CRÍTICA AQUI ---
            // Usar 'findByLoginWithAcessos' (com JOIN FETCH) em vez de 'findByLogin'
            // Isso evita o erro: org.hibernate.LazyInitializationException
            Usuario usuario = usuarioRepository.findByLoginWithAcessos(login).orElseThrow();

            List<EmpresaResumoDTO> resultado = new ArrayList<>();

            for (UsuarioEmpresa acesso : usuario.getAcessos()) {
                if (!acesso.getEmpresa().isAtivo())
                    continue;

                String tenantId = acesso.getEmpresa().getTenantId();
                String nomePerfil = "Usuário";

                try {
                    // Troca contexto para buscar o nome do perfil específico daquela empresa
                    TenantContext.setTenant(tenantId);
                    List<UsuarioPerfil> perfis = usuarioPerfilRepository.findByUsuarioId(usuario.getId());
                    if (!perfis.isEmpty()) {
                        nomePerfil = perfis.get(0).getPerfil().getNome();
                    } else if (acesso.getRole() == UserRole.ADMIN) {
                        nomePerfil = "Administrador Global";
                    }
                } catch (Exception e) {
                    nomePerfil = "Erro ao carregar";
                }

                // Restaura para Master para continuar o loop e adicionar ao DTO
                TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);

                resultado.add(new EmpresaResumoDTO(
                        acesso.getEmpresa().getId(),
                        acesso.getEmpresa().getRazaoSocial(),
                        acesso.getEmpresa().getCnpj(),
                        tenantId,
                        nomePerfil));
            }
            return ResponseEntity.ok(resultado);
        } finally {
            // Garante que o contexto original (ou limpeza) ocorra
            if (tenantOriginal != null)
                TenantContext.setTenant(tenantOriginal);
            else
                TenantContext.clear();
        }
    }

    @GetMapping("/lista-simples")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('USUARIO_EDITAR')")
    public ResponseEntity<List<EmpresaResumoDTO>> listarTodasSimples() {
        String original = TenantContext.getTenant();
        TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);
        try {
            List<Empresa> empresas = empresaRepository.findAll();
            return ResponseEntity.ok(empresas.stream()
                    .filter(Empresa::isAtivo)
                    .map(e -> new EmpresaResumoDTO(
                            e.getId(),
                            e.getRazaoSocial(),
                            e.getCnpj(),
                            e.getTenantId(),
                            "ATIVO"))
                    .collect(Collectors.toList()));
        } finally {
            if (original != null)
                TenantContext.setTenant(original);
            else
                TenantContext.clear();
        }
    }
}