package br.com.hacerfak.coreWMS.modules.sistema.repository;

import br.com.hacerfak.coreWMS.modules.sistema.domain.Anexo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnexoRepository extends JpaRepository<Anexo, Long> {
    // Busca anexos de uma entidade específica (ex: Fotos da Tarefa de Divergência
    // #50)
    List<Anexo> findByEntidadeTipoAndEntidadeId(String entidadeTipo, Long entidadeId);
}