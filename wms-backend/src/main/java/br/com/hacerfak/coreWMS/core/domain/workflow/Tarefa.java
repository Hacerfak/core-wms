package br.com.hacerfak.coreWMS.core.domain.workflow;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder; // Importante

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@SuperBuilder // <--- MUDANÇA
@NoArgsConstructor
@AllArgsConstructor
public abstract class Tarefa extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default // Funciona dentro do SuperBuilder também
    private StatusTarefa status = StatusTarefa.PENDENTE;

    @Column(name = "usuario_atribuido")
    private String usuarioAtribuido;

    @Column(name = "inicio_execucao")
    private LocalDateTime inicioExecucao;

    @Column(name = "fim_execucao")
    private LocalDateTime fimExecucao;

    public void iniciar(String usuario) {
        this.status = StatusTarefa.EM_EXECUCAO;
        this.usuarioAtribuido = usuario;
        this.inicioExecucao = LocalDateTime.now();
    }

    public void concluir() {
        this.status = StatusTarefa.CONCLUIDA;
        this.fimExecucao = LocalDateTime.now();
    }
}