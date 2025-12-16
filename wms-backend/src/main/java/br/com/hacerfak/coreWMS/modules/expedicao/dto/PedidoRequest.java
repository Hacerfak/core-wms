package br.com.hacerfak.coreWMS.modules.expedicao.dto;

import java.math.BigDecimal;
import java.util.List;

public record PedidoRequest(
        String codigoExterno, // ID do pedido no site/ERP
        Long clienteId, // ID do Parceiro (Destinatário)
        List<ItemPedidoRequest> itens) {
    public record ItemPedidoRequest(
            Long produtoId,
            BigDecimal quantidade) {
    }
}
