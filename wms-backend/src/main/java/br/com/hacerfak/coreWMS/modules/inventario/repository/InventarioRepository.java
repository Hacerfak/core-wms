package br.com.hacerfak.coreWMS.modules.inventario.repository;

import br.com.hacerfak.coreWMS.modules.inventario.domain.Inventario;
import br.com.hacerfak.coreWMS.modules.inventario.domain.StatusInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Long> {
    List<Inventario> findByStatus(StatusInventario status);
}