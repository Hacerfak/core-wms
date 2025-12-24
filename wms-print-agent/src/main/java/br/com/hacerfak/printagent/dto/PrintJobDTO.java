package br.com.hacerfak.printagent.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrintJobDTO {
    private Long id;
    private String zpl;
    private String tipoConexao; // REDE, COMPARTILHAMENTO, USB
    private String ip;
    private Integer porta;
    private String caminhoCompartilhamento;
}