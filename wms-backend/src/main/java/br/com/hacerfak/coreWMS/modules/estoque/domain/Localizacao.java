package br.com.hacerfak.coreWMS.modules.estoque.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import br.com.hacerfak.coreWMS.core.domain.BaseEntity;

@Entity
@Table(name = "tb_localizacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Localizacao extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String codigo; // Ex: A-01-01 (Rua-Predio-Nivel)

    private String tipo; // PICKING, PULMAO, DOCA, STAGE

    // --- CORREÇÕES AQUI ---

    @Builder.Default // Garante que nasce desbloqueado
    private boolean bloqueado = false;

    @Builder.Default // Garante que nasce ativo
    private boolean ativo = true;

    // Se você tiver capacidade definida, use o Default também
    @Column(precision = 19, scale = 4) // Isso ajuda o Hibernate a entender
    private BigDecimal capacidadePesoKg;
}