package br.com.hacerfak.coreWMS.modules.impressao.service;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.impressao.domain.*;
import br.com.hacerfak.coreWMS.modules.impressao.dto.ImpressoraRequest;
import br.com.hacerfak.coreWMS.modules.impressao.dto.PrintJobDTO;
import br.com.hacerfak.coreWMS.modules.impressao.repository.FilaImpressaoRepository;
import br.com.hacerfak.coreWMS.modules.impressao.repository.ImpressoraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImpressaoService {

    private final FilaImpressaoRepository filaRepository;
    private final ImpressoraRepository impressoraRepository;

    /**
     * Enfileira um ZPL gerado para ser impresso pelo Agente.
     */
    @Transactional
    public Long enviarParaFila(String zpl, String nomeImpressoraAlvo, String usuario, String origem) {
        Impressora impressora = impressoraRepository.findByNome(nomeImpressoraAlvo)
                .orElseThrow(() -> new EntityNotFoundException("Impressora não encontrada: " + nomeImpressoraAlvo));

        if (!impressora.isAtivo()) {
            throw new IllegalArgumentException("A impressora '" + nomeImpressoraAlvo + "' está inativa no sistema.");
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