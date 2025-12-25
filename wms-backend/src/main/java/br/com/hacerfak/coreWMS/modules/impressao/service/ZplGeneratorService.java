package br.com.hacerfak.coreWMS.modules.impressao.service;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Parceiro;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Localizacao;
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

        if (!lpn.getItens().isEmpty()) {
            var item = lpn.getItens().get(0);
            variaveis.put("SKU", item.getProduto().getSku());
            variaveis.put("DESC", item.getProduto().getNome());

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

        String enderecoCompleto = montarEnderecoCompleto(cliente);
        variaveis.put("ENDERECO_COMPLETO", enderecoCompleto);
        variaveis.put("CIDADE", cliente.getCidade() != null ? cliente.getCidade() : "");
        variaveis.put("UF", cliente.getUf() != null ? cliente.getUf() : "");
        variaveis.put("CEP", cliente.getCep() != null ? cliente.getCep() : "");

        return substituirVariaveis(template.getZplCodigo(), variaveis);
    }

    // --- NOVO: Etiqueta de Produto ---
    @Transactional(readOnly = true)
    public String gerarZplParaProduto(Long templateId, Produto produto) {
        EtiquetaTemplate template = resolverTemplate(templateId, TipoFinalidadeEtiqueta.PRODUTO);

        Map<String, String> variaveis = new HashMap<>();
        variaveis.put("SKU", produto.getSku());
        variaveis.put("NOME", produto.getNome());
        variaveis.put("EAN", produto.getEan13() != null ? produto.getEan13() : "");
        variaveis.put("DUN", produto.getDun14() != null ? produto.getDun14() : "");
        variaveis.put("UN", produto.getUnidadeMedida());
        variaveis.put("DEPOSITANTE", produto.getDepositante().getNome());

        // Exemplo: formatar preço se necessário, aqui passando bruto
        variaveis.put("PRECO",
                produto.getValorUnitarioPadrao() != null ? produto.getValorUnitarioPadrao().toString() : "");

        return substituirVariaveis(template.getZplCodigo(), variaveis);
    }

    // --- NOVO: Etiqueta de Localização ---
    @Transactional(readOnly = true)
    public String gerarZplParaLocalizacao(Long templateId, Localizacao local) {
        EtiquetaTemplate template = resolverTemplate(templateId, TipoFinalidadeEtiqueta.LOCALIZACAO);

        Map<String, String> variaveis = new HashMap<>();
        variaveis.put("CODIGO", local.getCodigo()); // Ex: 01-02-03
        variaveis.put("ENDERECO_COMPLETO", local.getEnderecoCompleto()); // Ex: CD1-RUA1-01-02-03
        variaveis.put("AREA", local.getArea().getNome());
        variaveis.put("ARMAZEM", local.getArea().getArmazem().getNome());
        variaveis.put("TIPO", local.getTipo().name());

        // Verifica digito verificador se tiver lógica customizada (opcional)
        // variaveis.put("DIGITO", calcularDigito(local.getId()));

        return substituirVariaveis(template.getZplCodigo(), variaveis);
    }

    // --- Métodos Auxiliares ---

    private String montarEnderecoCompleto(Parceiro p) {
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