package br.com.hacerfak.coreWMS.modules.cadastro.controller;

import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.UserRole;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.Usuario;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.UsuarioEmpresa;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.UsuarioPerfil;
import br.com.hacerfak.coreWMS.modules.seguranca.dto.EmpresaResumoDTO;
import br.com.hacerfak.coreWMS.modules.seguranca.repository.UsuarioPerfilRepository;
import br.com.hacerfak.coreWMS.modules.seguranca.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/empresas")
@RequiredArgsConstructor
public class EmpresaController {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioPerfilRepository usuarioPerfilRepository;

    @GetMapping("/meus-acessos")
    public ResponseEntity<List<EmpresaResumoDTO>> listarMinhasEmpresas() {
        // Guarda o estado atual para restaurar no final (boa prática)
        String tenantOriginal = TenantContext.getTenant();

        // 1. FORÇA O CONTEXTO MASTER (Onde estão os usuários e vínculos)
        TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);

        try {
            // Pega o usuário logado
            String login = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByLogin(login).orElseThrow();

            List<EmpresaResumoDTO> resultado = new ArrayList<>();

            // 2. Itera sobre as empresas para buscar o Perfil Real em cada banco
            // (Igual ao que o Login faz)
            for (UsuarioEmpresa acesso : usuario.getAcessos()) {
                if (!acesso.getEmpresa().isAtivo())
                    continue;

                String tenantId = acesso.getEmpresa().getTenantId();
                String nomePerfil = "Carregando...";

                try {
                    // A. Troca para o banco da empresa específica
                    TenantContext.setTenant(tenantId);

                    // B. Verifica o perfil local
                    if (usuario.getRole() == UserRole.ADMIN) {
                        nomePerfil = "MASTER";
                    } else {
                        List<UsuarioPerfil> perfis = usuarioPerfilRepository.findByUsuarioId(usuario.getId());
                        if (!perfis.isEmpty()) {
                            nomePerfil = perfis.get(0).getPerfil().getNome();
                        } else {
                            nomePerfil = "Sem Perfil";
                        }
                    }
                } catch (Exception e) {
                    nomePerfil = "Erro leitura";
                    System.err.println("Erro ao ler perfil do tenant " + tenantId + ": " + e.getMessage());
                } finally {
                    // C. IMPORTANTE: Volta para o Master para continuar o loop
                    TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);
                }

                resultado.add(new EmpresaResumoDTO(
                        acesso.getEmpresa().getId(),
                        acesso.getEmpresa().getRazaoSocial(),
                        tenantId,
                        nomePerfil));
            }

            return ResponseEntity.ok(resultado);

        } finally {
            // Restaura contexto original
            if (tenantOriginal != null) {
                TenantContext.setTenant(tenantOriginal);
            } else {
                TenantContext.clear();
            }
        }
    }
}