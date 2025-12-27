package br.com.hacerfak.coreWMS.modules.impressao.listener;

import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Lpn;
import br.com.hacerfak.coreWMS.modules.estoque.event.LpnCriadaEvent;
import br.com.hacerfak.coreWMS.modules.estoque.repository.LpnRepository;
import br.com.hacerfak.coreWMS.modules.impressao.domain.Impressora;
import br.com.hacerfak.coreWMS.modules.impressao.repository.ImpressoraRepository;
import br.com.hacerfak.coreWMS.modules.impressao.service.ImpressaoService;
import br.com.hacerfak.coreWMS.modules.impressao.service.ZplGeneratorService;
import br.com.hacerfak.coreWMS.modules.sistema.repository.SistemaConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImpressaoListener {

    private final ImpressaoService impressaoService;
    private final ZplGeneratorService zplService;
    private final SistemaConfigRepository configRepository;
    private final LpnRepository lpnRepository;
    private final ImpressoraRepository impressoraRepository;

    @Async
    @EventListener
    @Transactional // Cria nova transação para não afetar a original se falhar
    public void handleLpnCriada(LpnCriadaEvent event) {
        // Restaura o contexto do tenant na thread assíncrona
        TenantContext.setTenant(event.tenantId());
        try {
            // 1. Verifica se a impressão automática está ligada
            boolean imprimirAuto = configRepository.findById("IMPRESSAO_AUTOMATICA_ETIQUETA")
                    .map(c -> Boolean.parseBoolean(c.getValor()))
                    .orElse(false);

            if (imprimirAuto) {
                log.info(">>> IMPRESSÃO AUTO: Iniciando para LPN {}", event.codigoLpn());

                // 2. Busca os dados completos da LPN
                Lpn lpn = lpnRepository.findById(event.lpnId()).orElse(null);
                if (lpn == null) {
                    log.warn("LPN {} não encontrada para impressão.", event.lpnId());
                    return;
                }

                // 3. Define qual impressora usar
                Long impressoraId = resolverImpressoraPadrao();
                if (impressoraId == null) {
                    log.warn("Nenhuma impressora ativa encontrada para impressão automática.");
                    return;
                }

                // 4. Gera o ZPL usando o template padrão (passando null no templateId)
                // O serviço ZplGeneratorService já tem a lógica de buscar o template padrão de
                // LPN
                String zpl = zplService.gerarZplParaLpn(null, lpn);

                // 5. Envia para a fila de impressão
                impressaoService.enviarParaFila(zpl, impressoraId, "SISTEMA", "AUTO_RECEBIMENTO");
            }
        } catch (Exception e) {
            log.error("Erro na impressão automática da LPN " + event.codigoLpn(), e);
        } finally {
            // Limpa o contexto para não poluir a thread do pool
            TenantContext.clear();
        }
    }

    /**
     * Tenta encontrar a impressora configurada como padrão.
     * Se não houver configuração, usa a primeira impressora ativa encontrada.
     */
    private Long resolverImpressoraPadrao() {
        // Tenta pegar o ID configurado no banco
        Optional<String> configId = configRepository.findById("IMPRESSORA_PADRAO_ID")
                .map(c -> c.getValor());

        if (configId.isPresent()) {
            try {
                return Long.parseLong(configId.get());
            } catch (NumberFormatException ignored) {
                log.warn("Configuração IMPRESSORA_PADRAO_ID inválida: {}", configId.get());
            }
        }

        // Fallback: Pega a primeira impressora ativa do sistema
        List<Impressora> ativas = impressoraRepository.findByAtivoTrue();
        if (!ativas.isEmpty()) {
            return ativas.get(0).getId();
        }

        return null;
    }
}