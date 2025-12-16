package br.com.hacerfak.coreWMS.modules.sistema.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_configuracao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Configuracao {
    @Id
    private String chave;
    private String valor;
    private String descricao;
}