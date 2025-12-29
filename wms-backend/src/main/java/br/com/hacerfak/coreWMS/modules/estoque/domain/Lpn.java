package br.com.hacerfak.coreWMS.modules.estoque.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_lpn")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Lpn extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String codigo; // O código de barras da etiqueta (ex: "LPN-123456")

    @ManyToOne(optional = false)
    @JoinColumn(name = "formato_lpn_id", nullable = false)
    private FormatoLpn formato;

    // Método auxiliar para manter compatibilidade de código, se necessário
    public String getTipoDescricao() {
        return formato != null ? formato.getDescricao() : "Desconhecido";
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusLpn status; // EM_MONTAGEM, ARMAZENADO, EM_MOVIMENTACAO, EXPEDIDO

    // Onde essa LPN está fisicamente agora?
    @ManyToOne
    @JoinColumn(name = "localizacao_atual_id")
    @JsonIgnoreProperties({ "area", "hibernateLazyInitializer", "handler" })
    private Localizacao localizacaoAtual;

    // Mapeamos apenas o ID para evitar acoplamento direto com o módulo de
    // 'operacao'
    @Column(name = "solicitacao_entrada_id")
    private Long solicitacaoEntradaId;

    // Conteúdo da LPN (Mix de produtos)
    @Builder.Default
    @OneToMany(mappedBy = "lpn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LpnItem> itens = new ArrayList<>();

    // Métodos auxiliares
    public void adicionarItem(LpnItem item) {
        item.setLpn(this);
        this.itens.add(item);
    }
}