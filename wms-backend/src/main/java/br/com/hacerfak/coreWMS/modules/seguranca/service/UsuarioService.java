package br.com.hacerfak.coreWMS.modules.seguranca.service;

import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Empresa;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.EmpresaRepository;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.*;
import br.com.hacerfak.coreWMS.modules.seguranca.repository.*;
import br.com.hacerfak.coreWMS.modules.seguranca.dto.*;
import lombok.RequiredArgsConstructor;
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

    public List<UsuarioDTO> listarPorTenantAtual() {
        String tenantId = TenantContext.getTenant();
        List<UsuarioDTO> resultado = new ArrayList<>();

        // 1. Busca usuários vinculados à empresa no Master
        TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);
        try {
            Empresa empresa = empresaRepository.findByTenantId(tenantId).orElseThrow();
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

        // 2. Busca os perfis locais para enriquecer a lista
        for (UsuarioDTO u : resultado) {
            // O Admin Global não tem perfil local, tratamos isso
            if (u.login().equals("admin")) {
                resultado.set(resultado.indexOf(u), new UsuarioDTO(u.id(), u.login(), "SUPER ADMIN", true));
                continue;
            }

            var perfis = usuarioPerfilRepository.findByUsuarioId(u.id());
            if (!perfis.isEmpty()) {
                String nomePerfil = perfis.get(0).getPerfil().getNome();
                // Atualiza o DTO com o nome do perfil correto
                resultado.set(resultado.indexOf(u), new UsuarioDTO(u.id(), u.login(), nomePerfil, u.ativo()));
            }
        }

        return resultado;
    }

    public void criarUsuarioParaEmpresa(CriarUsuarioRequest request) {
        String tenantId = TenantContext.getTenant();

        // --- MASTER DB ---
        TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);
        Usuario usuario;
        try {
            Empresa empresa = empresaRepository.findByTenantId(tenantId).orElseThrow();
            Optional<Usuario> existente = usuarioRepository.findByLogin(request.login());

            if (existente.isPresent()) {
                usuario = existente.get();
                if (usuarioEmpresaRepository.existsByUsuarioIdAndEmpresaId(usuario.getId(), empresa.getId())) {
                    throw new IllegalArgumentException("Usuário já existe nesta empresa.");
                }
            } else {
                usuario = Usuario.builder()
                        .login(request.login())
                        .senha(passwordEncoder.encode(request.senha()))
                        .role(UserRole.OPERADOR)
                        .ativo(true)
                        .build();
                usuarioRepository.save(usuario);
            }

            UsuarioEmpresa vinculo = UsuarioEmpresa.builder()
                    .usuario(usuario)
                    .empresa(empresa)
                    .role(UserRole.OPERADOR)
                    .build();
            usuarioEmpresaRepository.save(vinculo);

        } finally {
            TenantContext.setTenant(tenantId);
        }

        // --- TENANT DB ---
        Perfil perfil = perfilRepository.findById(request.perfilId())
                .orElseThrow(() -> new RuntimeException("Perfil não encontrado"));

        UsuarioPerfil usuarioPerfil = UsuarioPerfil.builder()
                .usuarioId(usuario.getId())
                .perfil(perfil)
                .build();

        usuarioPerfilRepository.save(usuarioPerfil);
    }
}