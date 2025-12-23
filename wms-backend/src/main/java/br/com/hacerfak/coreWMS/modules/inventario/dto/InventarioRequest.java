package br.com.hacerfak.coreWMS.modules.inventario.dto;

import br.com.hacerfak.coreWMS.modules.inventario.domain.TipoInventario;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record InventarioRequest(
        @NotNull String descricao,
        @NotNull TipoInventario tipo,
        LocalDate dataAgendada,
        boolean cego,

        // Filtros para gerar tarefas
        List<Long> localizacoesIds, // Para inventário geográfico
        List<Long> produtosIds // Para inventário rotativo
) {
}