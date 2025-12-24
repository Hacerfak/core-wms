package br.com.hacerfak.coreWMS.modules.impressao.service;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Parceiro;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Lpn;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.VolumeExpedicao;
import br.com.hacerfak.coreWMS.modules.impressao.domain.EtiquetaTemplate;
import br.com.hacerfak.coreWMS.modules.impressao.domain.TipoFinalidadeEtiqueta;
import br.com.hacerfak.coreWMS.modules.impressao.repository.EtiquetaTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ZplGeneratorService {

    private final EtiquetaTemplateRepository templateRepository;

    @Transactional(readOnly = true)
    public String gerarZplParaLpn(Long templateId, Lpn lpn) {
        EtiquetaTemplate template = resolverTemplate(templateId, TipoFinalidadeEtiqueta.LPN);

        Map<String, String> variaveis = new HashMap<>();
        variaveis.put("LPN_CODIGO", lpn.getCodigo());
        variaveis.put("TIPO", lpn.getTipo().name());

        // Se for pallet mono-produto, pega dados do primeiro item
        if (!lpn.getItens().isEmpty()) {
            var item = lpn.getItens().get(0);
            variaveis.put("SKU", item.getProduto().getSku());
            variaveis.put("DESC", item.getProduto().getNome());

            // Soma total se for o mesmo produto, ou "VÁRIOS" se mix
            boolean mix = lpn.getItens().stream().map(i -> i.getProduto().getId()).distinct().count() > 1;

            if (mix) {
                variaveis.put("SKU", "MIX");
                variaveis.put("DESC", "Pallet Misto");
                variaveis.put("QTD", String.valueOf(lpn.getItens().size()) + " ITENS");
            } else {
                variaveis.put("QTD", item.getQuantidade().toString());
            }

            variaveis.put("LOTE", item.getLote() != null ? item.getLote() : "");
            variaveis.put("VALIDADE", item.getDataValidade() != null ? item.getDataValidade().toString() : "");
        } else {
            variaveis.put("SKU", "VAZIO");
            variaveis.put("DESC", "LPN Vazia");
            variaveis.put("QTD", "0");
            variaveis.put("LOTE", "");
            variaveis.put("VALIDADE", "");
        }

        return substituirVariaveis(template.getZplCodigo(), variaveis);
    }

    @Transactional(readOnly = true)
    public String gerarZplParaVolume(Long templateId, VolumeExpedicao volume) {
        EtiquetaTemplate template = resolverTemplate(templateId, TipoFinalidadeEtiqueta.VOLUME_EXPEDICAO);
        Parceiro cliente = volume.getSolicitacao().getCliente();

        Map<String, String> variaveis = new HashMap<>();
        variaveis.put("RASTREIO", volume.getCodigoRastreio());
        variaveis.put("PEDIDO", volume.getSolicitacao().getCodigoExterno());
        variaveis.put("ROTA", volume.getSolicitacao().getRota() != null ? volume.getSolicitacao().getRota() : "GERAL");
        variaveis.put("DESTINATARIO", cliente.getNome());
        variaveis.put("PESO", volume.getPesoBruto() != null ? volume.getPesoBruto().toString() : "0.00");

        // --- Endereço Completo ---
        String enderecoCompleto = montarEnderecoCompleto(cliente);
        variaveis.put("ENDERECO_COMPLETO", enderecoCompleto);

        // Campos individuais caso o ZPL precise separado
        variaveis.put("CIDADE", cliente.getCidade() != null ? cliente.getCidade() : "");
        variaveis.put("UF", cliente.getUf() != null ? cliente.getUf() : "");
        variaveis.put("CEP", cliente.getCep() != null ? cliente.getCep() : "");

        return substituirVariaveis(template.getZplCodigo(), variaveis);
    }

    private String montarEnderecoCompleto(Parceiro p) {
        // Ex: Av. Brasil, 1500 - Galpão B - Centro, São Paulo/SP - CEP: 01000-000
        StringBuilder sb = new StringBuilder();
        if (p.getLogradouro() != null)
            sb.append(p.getLogradouro());
        if (p.getNumero() != null)
            sb.append(", ").append(p.getNumero());
        if (p.getComplemento() != null && !p.getComplemento().isBlank())
            sb.append(" - ").append(p.getComplemento());
        if (p.getBairro() != null)
            sb.append(" - ").append(p.getBairro());

        if (p.getCidade() != null) {
            sb.append(", ").append(p.getCidade());
            if (p.getUf() != null)
                sb.append("/").append(p.getUf());
        }

        if (p.getCep() != null)
            sb.append(" - CEP: ").append(p.getCep());

        return sb.toString();
    }

    private EtiquetaTemplate resolverTemplate(Long templateId, TipoFinalidadeEtiqueta tipo) {
        if (templateId != null) {
            return templateRepository.findById(templateId)
                    .orElseThrow(() -> new EntityNotFoundException("Template não encontrado"));
        }
        return templateRepository.findFirstByTipoFinalidadeAndPadraoTrue(tipo)
                .orElseThrow(() -> new EntityNotFoundException("Nenhum template padrão encontrado para " + tipo));
    }

    private String substituirVariaveis(String zplOriginal, Map<String, String> variaveis) {
        String zplProcessado = zplOriginal;
        for (Map.Entry<String, String> entry : variaveis.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String valor = entry.getValue() != null ? entry.getValue() : "";
            zplProcessado = zplProcessado.replace(placeholder, valor);
        }
        return zplProcessado;
    }
}