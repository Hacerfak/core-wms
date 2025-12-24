package br.com.hacerfak.coreWMS.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

@Component
@Slf4j
public class DiffUtils {

    private final ObjectMapper objectMapper;

    // Campos que geram ruído e devem ser ignorados no Diff
    private final List<String> IGNORAR_CAMPOS = List.of(
            "dataAtualizacao", "dataCriacao", "atualizadoPor", "criadoPor",
            "hibernateLazyInitializer", "handler", "versao");

    public DiffUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Gera um JSON contendo APENAS as diferenças entre o estado antigo e o novo.
     * Formato: { "campo": { "de": "valorAntigo", "para": "valorNovo" } }
     */
    public String gerarDiff(Object antigo, Object novo) {
        try {
            if (antigo == null && novo == null)
                return null;

            // Se for criação (antigo null), salva o objeto novo inteiro
            if (antigo == null) {
                return objectMapper.writeValueAsString(limparCamposTecnicos(objectMapper.valueToTree(novo)));
            }

            // Se for exclusão (novo null), salva o antigo inteiro (backup)
            if (novo == null) {
                ObjectNode node = objectMapper.createObjectNode();
                node.set("BACKUP_EXCLUSAO", limparCamposTecnicos(objectMapper.valueToTree(antigo)));
                return node.toString();
            }

            JsonNode oldNode = objectMapper.valueToTree(antigo);
            JsonNode newNode = objectMapper.valueToTree(novo);
            ObjectNode diffNode = objectMapper.createObjectNode();

            Iterator<String> fieldNames = newNode.fieldNames();

            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();

                // Pula campos técnicos
                if (IGNORAR_CAMPOS.contains(fieldName))
                    continue;

                JsonNode valOld = oldNode.get(fieldName);
                JsonNode valNew = newNode.get(fieldName);

                // Se houver diferença, registra
                if (!isEquals(valOld, valNew)) {
                    ObjectNode change = diffNode.putObject(fieldName);
                    change.set("de", valOld);
                    change.set("para", valNew);
                }
            }

            if (diffNode.isEmpty())
                return null; // Nada mudou (evita log vazio)

            return diffNode.toString();

        } catch (Exception e) {
            log.error("Erro ao gerar Diff de auditoria", e);
            return "{\"erro\": \"Falha ao gerar diff\"}";
        }
    }

    private boolean isEquals(JsonNode n1, JsonNode n2) {
        if (n1 == null && n2 == null)
            return true;
        if (n1 == null || n2 == null)
            return false;
        return n1.equals(n2);
    }

    private JsonNode limparCamposTecnicos(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            objectNode.remove(IGNORAR_CAMPOS);
        }
        return node;
    }
}