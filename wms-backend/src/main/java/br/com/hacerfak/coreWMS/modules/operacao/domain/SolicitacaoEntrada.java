package br.com.hacerfak.coreWMS.modules.operacao.domain;

import br.com.hacerfak.coreWMS.core.domain.workflow.Solicitacao;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Parceiro;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Localizacao;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_solicitacao_entrada")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SolicitacaoEntrada extends Solicitacao {

    // Dados específicos de Entrada que vieram do antigo Recebimento

    @Column(name = "nota_fiscal")
    private String notaFiscal;

    @Column(length = 44)
    private String chaveAcesso;

    @ManyToOne
    @JoinColumn(name = "fornecedor_id")
    private Parceiro fornecedor;

    private LocalDateTime dataEmissao;

    @ManyToOne
    @JoinColumn(name = "doca_id")
    @JsonIgnoreProperties({ "area", "armazem", "hibernateLazyInitializer", "handler" })
    private Localizacao doca;

    // Itens Previstos (XML)
    @Builder.Default
    @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemSolicitacaoEntrada> itens = new ArrayList<>();

    // Tarefas vinculadas a esta solicitação (Conferência, Inspeção, etc)
    @Builder.Default
    @OneToMany(mappedBy = "solicitacaoPai", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<TarefaConferencia> tarefasConferencia = new ArrayList<>();
}