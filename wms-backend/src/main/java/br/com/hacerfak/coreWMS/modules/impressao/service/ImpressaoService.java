package br.com.hacerfak.coreWMS.modules.impressao.service;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.impressao.domain.*;
import br.com.hacerfak.coreWMS.modules.impressao.dto.ImpressoraRequest;
import br.com.hacerfak.coreWMS.modules.impressao.dto.PrintJobDTO;
import br.com.hacerfak.coreWMS.modules.impressao.repository.FilaImpressaoRepository;
import br.com.hacerfak.coreWMS.modules.impressao.repository.ImpressoraRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImpressaoService {

    private final FilaImpressaoRepository filaRepository;
    private final ImpressoraRepository impressoraRepository;
    private final StringRedisTemplate redisTemplate;

    /**
     * Enfileira um ZPL gerado para ser impresso pelo Agente.
     */
    @Transactional
    public Long enviarParaFila(String zpl, Long impressoraId, String usuario, String origem) {
        // Busca por ID (findById) em vez de Nome
        Impressora impressora = impressoraRepository.findById(impressoraId)
                .orElseThrow(() -> new EntityNotFoundException("Impressora não encontrada com ID: " + impressoraId));

        if (!impressora.isAtivo()) {
            throw new IllegalArgumentException("A impressora '" + impressora.getNome() + "' está inativa no sistema.");
        }

        FilaImpressao item = FilaImpressao.builder()
                .zplConteudo(zpl)
                .impressoraAlvo(impressora)
                .status(StatusImpressao.PENDENTE)
                .usuarioSolicitante(usuario)
                .origem(origem)
                .tentativas(0)
                .build();

        FilaImpressao salvo = filaRepository.save(item);

        // 2. OTIMIZAÇÃO: Notificar Agente via Redis (Inbox Pattern)
        // Se a impressora não tem agente (impressora de rede direta), processamos
        // diferente.
        // Assumindo que a impressora está vinculada a um agente pelo Hostname ou
        // Config.
        // Vamos supor que você vai vincular Impressora -> Agente.
        // Por simplificação: vamos enviar para uma chave global ou específica do
        // agente.

        // Estratégia: Chave = "print_jobs:NOME_DO_AGENTE"
        // Como descobrir o agente da impressora? Idealmente a tabela Impressora deve
        // ter um "agente_id".
        // Se não tiver, vamos assumir um broadcast ou buscar o agente pelo IP/Rede.

        // Exemplo simplificado: Buscando agente pelo vínculo (adicione esse campo na
        // entidade se precisar)
        // String agenteQueueKey = "wms:print:inbox:" +
        // impressora.getAgente().getNome();

        // Se não tiver vínculo direto ainda, usaremos uma fila global e o agente filtra
        // (menos otimizado)
        // OU, melhor: O Controller do Agente busca direto no Redis.

        // Vamos gravar o ID do Job na lista de tarefas pendentes
        String redisKey = "wms:print:jobs:pending";
        redisTemplate.opsForList().rightPush(redisKey, String.valueOf(salvo.getId()));

        return salvo.getId();
    }

    /**
     * CRUD de Impressoras
     */
    @Transactional
    public Impressora cadastrarImpressora(ImpressoraRequest dto) {
        // Mapear DTO -> Entity (Lógica simplificada, adicione busca de
        // Armazem/Depositante se necessário)
        Impressora imp = Impressora.builder()
                .nome(dto.nome())
                .descricao(dto.descricao())
                .tipoConexao(dto.tipoConexao())
                .enderecoIp(dto.enderecoIp())
                .porta(dto.porta() != null ? dto.porta() : 9100)
                .caminhoCompartilhamento(dto.caminhoCompartilhamento())
                .ativo(true)
                .build();

        return impressoraRepository.save(imp);
    }

    /**
     * Chamado pelo AGENTE: Busca trabalhos pendentes
     */
    @Transactional(readOnly = true)
    public List<PrintJobDTO> buscarTrabalhosPendentes() {
        return filaRepository.buscarPendentesComImpressora().stream()
                .map(f -> PrintJobDTO.builder()
                        .id(f.getId())
                        .zpl(f.getZplConteudo())
                        .tipoConexao(f.getImpressoraAlvo().getTipoConexao().name())
                        .ip(f.getImpressoraAlvo().getEnderecoIp())
                        .porta(f.getImpressoraAlvo().getPorta())
                        .caminhoCompartilhamento(f.getImpressoraAlvo().getCaminhoCompartilhamento())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Chamado pelo AGENTE: Atualiza status (Sucesso/Erro)
     */
    @Transactional
    public void atualizarStatusFila(Long idFila, boolean sucesso, String mensagemErro) {
        FilaImpressao item = filaRepository.findById(idFila).orElse(null);
        if (item == null)
            return;

        if (sucesso) {
            item.setStatus(StatusImpressao.CONCLUIDO);
        } else {
            item.setTentativas(item.getTentativas() + 1);
            item.setMensagemErro(mensagemErro);
            // Se falhou 3x, marca como ERRO, senão volta pra PENDENTE pra tentar de novo?
            // Por segurança, vamos marcar ERRO direto e requerer intervenção humana ou
            // retry manual
            item.setStatus(StatusImpressao.ERRO);
        }
        filaRepository.save(item);
    }
}