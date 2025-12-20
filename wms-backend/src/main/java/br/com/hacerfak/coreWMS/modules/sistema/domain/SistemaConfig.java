package br.com.hacerfak.coreWMS.modules.sistema.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_sistema_config") // Renomeado
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SistemaConfig {

    @Id
    private String chave; // Ex: RECEBIMENTO_CEGO_OBRIGATORIO

    private String valor; // Ex: "true", "100", "AZUL"

    private String descricao; // Ex: "Define se o recebimento oculta quantidades"

    private String tipo; // "BOOLEAN", "STRING", "INTEGER" (Facilita o front)
}