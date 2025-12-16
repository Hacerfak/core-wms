package br.com.hacerfak.coreWMS.modules.expedicao.repository;

import br.com.hacerfak.coreWMS.modules.expedicao.domain.PedidoSaida;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoSaidaRepository extends JpaRepository<PedidoSaida, Long> {

    /**
     * Busca um pedido pelo código que veio do ERP/Loja (Ex: "PED-102030").
     * Útil para evitar duplicidade na integração.
     */
    Optional<PedidoSaida> findByCodigoPedidoExterno(String codigoPedidoExterno);

    /**
     * Busca pedidos por status.
     * Útil para o painel de monitoramento: "Quais pedidos estão parados em
     * ALOCADO?"
     */
    List<PedidoSaida> findByStatus(StatusPedido status);
}
