package br.com.hacerfak.coreWMS.modules.impressao.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PrintJobDTO {
    private Long id; // ID da fila
    private String zpl;

    // Dados da Impressora para o Agente saber onde conectar
    private String tipoConexao; // REDE, USB
    private String ip;
    private Integer porta;
    private String caminhoCompartilhamento; // \\PC\Zebra
}