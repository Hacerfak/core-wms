package br.com.hacerfak.coreWMS.modules.estoque.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_lpn")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lpn extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String codigo; // O código de barras da etiqueta (ex: "LPN-123456")

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoLpn tipo; // PALLET, CAIXA, GAIOLA

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusLpn status; // EM_MONTAGEM, ARMAZENADO, EM_MOVIMENTACAO, EXPEDIDO

    // Onde essa LPN está fisicamente agora?
    @ManyToOne
    @JoinColumn(name = "localizacao_atual_id")
    private Localizacao localizacaoAtual;

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