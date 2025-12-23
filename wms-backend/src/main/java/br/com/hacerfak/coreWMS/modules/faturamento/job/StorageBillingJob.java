package br.com.hacerfak.coreWMS.modules.faturamento.job;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.Parceiro;
import br.com.hacerfak.coreWMS.modules.estoque.repository.EstoqueSaldoRepository;
import br.com.hacerfak.coreWMS.modules.faturamento.service.FaturamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StorageBillingJob {

    private final EstoqueSaldoRepository saldoRepository;
    private final FaturamentoService faturamentoService;

    // Executa todo dia às 23:55
    @Scheduled(cron = "0 55 23 * * ?")
    @Transactional
    public void calcularArmazenagemDiaria() {
        System.out.println("--- JOB FATURAMENTO: Iniciando cálculo de armazenagem diária ---");

        // Retorna lista de Arrays: [Parceiro, Long(quantidade)]
        List<Object[]> ocupacao = saldoRepository.contarPalletsPorCliente();

        for (Object[] row : ocupacao) {
            try {
                Parceiro cliente = (Parceiro) row[0];
                Long qtdPallets = (Long) row[1];

                if (cliente != null && qtdPallets > 0) {
                    // Gera o apontamento automático
                    faturamentoService.registrarCobrancaAutomatica(
                            cliente,
                            "ARMAZENAGEM_PALLET_DIA", // Este código deve existir no tb_servico
                            BigDecimal.valueOf(qtdPallets),
                            "Saldo Fechamento Dia");
                }
            } catch (Exception e) {
                System.err.println("Erro ao calcular armazenagem para um cliente: " + e.getMessage());
                // Continua para o próximo cliente
            }
        }
        System.out.println("--- JOB FATURAMENTO: Finalizado ---");
    }
}