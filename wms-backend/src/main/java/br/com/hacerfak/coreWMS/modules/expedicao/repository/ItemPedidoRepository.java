package br.com.hacerfak.coreWMS.modules.expedicao.repository;

import br.com.hacerfak.coreWMS.modules.expedicao.domain.ItemPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {

    // Busca todos os itens de um pedido específico
    List<ItemPedido> findByPedidoId(Long pedidoId);
}
