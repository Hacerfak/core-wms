package br.com.hacerfak.coreWMS.modules.estoque.domain;

import br.com.hacerfak.coreWMS.core.domain.workflow.Tarefa;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tb_tarefa_armazenagem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TarefaArmazenagem extends Tarefa {

    // O que precisa ser guardado? Uma LPN.
    @ManyToOne
    @JoinColumn(name = "lpn_id", nullable = false)
    private Lpn lpn;

    // De onde ela sai? (Geralmente a Doca ou Stage de Entrada)
    @ManyToOne
    @JoinColumn(name = "origem_id")
    private Localizacao origem;

    // Para onde ela vai? (Sugerido pelo sistema ou escolhido pelo operador)
    @ManyToOne
    @JoinColumn(name = "destino_sugerido_id")
    private Localizacao destinoSugerido;

    // ID da solicitação de entrada original (para rastreabilidade)
    private Long solicitacaoEntradaId;
}