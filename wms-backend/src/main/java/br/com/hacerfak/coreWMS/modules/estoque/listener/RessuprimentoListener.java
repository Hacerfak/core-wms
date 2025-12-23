package br.com.hacerfak.coreWMS.modules.estoque.listener;

import br.com.hacerfak.coreWMS.modules.estoque.domain.ConfiguracaoPicking;
import br.com.hacerfak.coreWMS.modules.estoque.event.EstoqueMovimentadoEvent;
import br.com.hacerfak.coreWMS.modules.estoque.repository.ConfiguracaoPickingRepository;
import br.com.hacerfak.coreWMS.modules.estoque.service.RessuprimentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RessuprimentoListener {

    private final ConfiguracaoPickingRepository configPickingRepository;
    private final RessuprimentoService ressuprimentoService;

    @Async // Executa em thread separada para não travar a movimentação (API fica rápida)
    @EventListener
    public void handleEstoqueMovimentado(EstoqueMovimentadoEvent event) {
        // Só nos interessa SAÍDAS (diminuição de estoque) para acionar ressuprimento
        if (!"SAIDA".equals(event.tipoMovimento())) {
            return;
        }

        // Verifica se o local movimentado é um endereço de picking configurado
        Optional<ConfiguracaoPicking> configOpt = configPickingRepository
                .findByProdutoIdAndLocalizacaoId(event.produtoId(), event.localizacaoId());

        if (configOpt.isPresent()) {
            ConfiguracaoPicking config = configOpt.get();

            // Verifica se o saldo caiu abaixo do ponto de reposição
            if (event.saldoResultante().compareTo(config.getPontoRessuprimento()) <= 0) {
                System.out.println("Gatilho de Evento: Acionando ressuprimento para " + event.localizacaoId());
                // Chama o serviço existente (que já tem as validações de não duplicar tarefa)
                // Precisamos expor ou adaptar o método 'verificarNecessidade' no Service
                // Como ele é privado lá, o ideal é chamar o processamento geral ou refatorar o
                // Service
                // para aceitar um config específico público.

                // Vamos assumir que refatoramos o RessuprimentoService para ter este método
                // público:
                ressuprimentoService.analisarConfiguracaoEspecifica(config);
            }
        }
    }
}