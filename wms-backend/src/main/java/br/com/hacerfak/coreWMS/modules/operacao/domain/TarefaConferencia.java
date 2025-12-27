package br.com.hacerfak.coreWMS.modules.operacao.domain;

import br.com.hacerfak.coreWMS.core.domain.workflow.Tarefa;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tb_tarefa_conferencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TarefaConferencia extends Tarefa {

    @ManyToOne
    @JoinColumn(name = "solicitacao_id", nullable = false)
    @JsonIgnoreProperties({ "tarefasConferencia", "itens", "hibernateLazyInitializer", "handler" })
    private SolicitacaoEntrada solicitacaoPai;

    private boolean cega; // Se true, não mostra qtd para o operador

    // Aqui poderíamos ter uma lista de "VolumesConferidos" vinculados a esta tarefa
    // para saber QUESTA tarefa gerou X volumes.
}