package br.com.hacerfak.coreWMS.modules.seguranca.repository;

import br.com.hacerfak.coreWMS.modules.seguranca.domain.UsuarioEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioEmpresaRepository extends JpaRepository<UsuarioEmpresa, Long> {

    // Verifica se já existe o vínculo para não duplicar
    boolean existsByUsuarioIdAndEmpresaId(Long usuarioId, Long empresaId);

    // Busca todos os vínculos de uma empresa específica (usado para desvincular)
    List<UsuarioEmpresa> findByEmpresaId(Long empresaId);

    // --- O MÉTODO QUE FALTAVA ---
    // Remove todos os vínculos de um usuário específico (usado ao excluir o
    // usuário)
    void deleteByUsuarioId(Long usuarioId);
}