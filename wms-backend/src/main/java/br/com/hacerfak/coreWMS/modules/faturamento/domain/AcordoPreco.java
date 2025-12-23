package br.com.hacerfak.coreWMS.modules.faturamento.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Parceiro;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tb_acordo_preco")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AcordoPreco extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Parceiro cliente;

    @ManyToOne
    @JoinColumn(name = "servico_id", nullable = false)
    private Servico servico;

    @Column(nullable = false)
    private BigDecimal precoUnitario;

    private LocalDate vigenciaInicio;
    private LocalDate vigenciaFim;
}