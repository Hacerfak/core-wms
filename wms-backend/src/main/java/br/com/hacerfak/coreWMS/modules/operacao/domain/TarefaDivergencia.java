package br.com.hacerfak.coreWMS.modules.operacao.domain;

import br.com.hacerfak.coreWMS.core.domain.workflow.Tarefa;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_tarefa_divergencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TarefaDivergencia extends Tarefa {

    @ManyToOne
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private SolicitacaoEntrada solicitacao;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Enumerated(EnumType.STRING)
    private TipoDivergencia tipo; // FALTA, SOBRA, AVARIA_DETECTADA

    private BigDecimal quantidadeDivergente;

    // Campo para o supervisor escrever a tratativa (ex: "Acordado com fornecedor
    // crédito na próxima fatura")
    private String resolucao;
}