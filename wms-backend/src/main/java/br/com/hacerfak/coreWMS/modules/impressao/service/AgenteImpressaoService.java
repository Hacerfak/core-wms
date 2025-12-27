package br.com.hacerfak.coreWMS.modules.impressao.service;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.impressao.domain.AgenteImpressao;
import br.com.hacerfak.coreWMS.modules.impressao.dto.AgenteRequest;
import br.com.hacerfak.coreWMS.modules.impressao.repository.AgenteImpressaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgenteImpressaoService {

    private final AgenteImpressaoRepository repository;

    @Transactional
    public AgenteImpressao criarAgente(AgenteRequest request) {
        if (repository.existsByNome(request.nome())) {
            throw new IllegalArgumentException("Já existe um agente com este nome.");
        }

        // Gera uma chave estilo Stripe/AWS: wms_live_sk_<random>
        String apiKey = "wms_live_sk_" + UUID.randomUUID().toString().replace("-", "");

        AgenteImpressao agente = AgenteImpressao.builder()
                .nome(request.nome())
                .descricao(request.descricao())
                .hostname(request.hostname())
                .apiKey(apiKey)
                .ativo(true)
                .build();

        return repository.save(agente);
    }

    // --- NOVA FUNCIONALIDADE: EDIÇÃO ---
    @Transactional
    @CacheEvict(value = "agentes-key", allEntries = true) // Limpa cache pois pode mudar status/chave
    public AgenteImpressao atualizarAgente(Long id, AgenteRequest request, Boolean ativo) {
        AgenteImpressao agente = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agente não encontrado"));

        // Se mudou o nome, verifica duplicidade
        if (!agente.getNome().equals(request.nome()) && repository.existsByNome(request.nome())) {
            throw new IllegalArgumentException("Já existe outro agente com este nome.");
        }

        agente.setNome(request.nome());
        agente.setDescricao(request.descricao());
        agente.setHostname(request.hostname());

        if (ativo != null) {
            agente.setAtivo(ativo);
        }

        return repository.save(agente);
    }

    // --- CORREÇÃO: EXCLUSÃO REAL (HARD DELETE) ---
    @Transactional
    @CacheEvict(value = "agentes-key", allEntries = true)
    public void excluirAgente(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Agente não encontrado");
        }
        repository.deleteById(id);
    }

    // Método chamado pelo Filtro para registrar heartbeat (sem transaction pesada)
    @Transactional
    public void registrarHeartbeat(AgenteImpressao agente, String versao) {
        LocalDateTime agora = LocalDateTime.now();
        // Otimização: Só atualiza no banco se passou mais de 15 min para não spammar
        // update
        if (agente.getUltimoHeartbeat() == null ||
                agente.getUltimoHeartbeat().isBefore(java.time.LocalDateTime.now().minusMinutes(15))) {

            String versaoFinal = (versao != null && !versao.isBlank()) ? versao : "1.0.0";
            repository.registrarHeartbeatSemAuditoria(agente.getId(), agora, versaoFinal);
        }
    }
}