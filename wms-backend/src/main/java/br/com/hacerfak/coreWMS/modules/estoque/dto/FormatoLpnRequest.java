package br.com.hacerfak.coreWMS.modules.estoque.dto;

import br.com.hacerfak.coreWMS.modules.estoque.domain.FormatoLpn;
import br.com.hacerfak.coreWMS.modules.estoque.domain.TipoSuporte;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record FormatoLpnRequest(
        Long id,

        @NotBlank(message = "O código é obrigatório") String codigo,

        @NotBlank(message = "A descrição é obrigatória") String descricao,

        @NotNull(message = "O tipo base é obrigatório") TipoSuporte tipoBase,

        @Positive(message = "Altura deve ser maior que zero") BigDecimal alturaM,

        @Positive(message = "Largura deve ser maior que zero") BigDecimal larguraM,

        @Positive(message = "Profundidade deve ser maior que zero") BigDecimal profundidadeM,

        @PositiveOrZero BigDecimal pesoSuportadoKg,

        @PositiveOrZero BigDecimal taraKg,

        Boolean ativo) {
    // Método auxiliar para conversão DTO -> Entity
    public FormatoLpn toEntity() {
        return FormatoLpn.builder()
                .id(this.id) // Se vier nulo, o Hibernate entende como novo insert
                .codigo(this.codigo)
                .descricao(this.descricao)
                .tipoBase(this.tipoBase)
                .alturaM(this.alturaM)
                .larguraM(this.larguraM)
                .profundidadeM(this.profundidadeM)
                .pesoSuportadoKg(this.pesoSuportadoKg != null ? this.pesoSuportadoKg : BigDecimal.ZERO)
                .taraKg(this.taraKg != null ? this.taraKg : BigDecimal.ZERO)
                .ativo(this.ativo != null ? this.ativo : true)
                .build();
    }
}