package br.com.hacerfak.coreWMS.modules.operacao.dto;

import br.com.hacerfak.coreWMS.modules.operacao.domain.StatusRecebimento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecebimentoResumoDTO {
    private Long id;
    private String numNotaFiscal;
    private String fornecedor; // Apenas o nome (String)
    private LocalDateTime dataCriacao;
    private LocalDateTime dataEmissao;
    private StatusRecebimento status;
}