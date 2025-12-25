package br.com.hacerfak.coreWMS.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class DiffUtils {

    private final ObjectMapper objectMapper;

    // Campos técnicos que não interessam para a auditoria de negócio
    private final List<String> IGNORAR_CAMPOS = List.of(
            "dataAtualizacao", "dataCriacao", "atualizadoPor", "criadoPor",
            "hibernateLazyInitializer", "handler", "versao", "version",
            "dataFinalizacao", "senha", "authorities", "accountNonExpired",
            "accountNonLocked", "credentialsNonExpired", "enabled", "username");

    public DiffUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String gerarDiff(Object antigo, Object novo) {
        try {
            if (antigo == null && novo == null)
                return null;

            // CREATE: Salva o objeto novo limpo
            if (antigo == null) {
                JsonNode tree = objectMapper.valueToTree(novo);
                return simplificarJson(tree).toString();
            }

            // DELETE: Salva o backup limpo
            if (novo == null) {
                ObjectNode node = objectMapper.createObjectNode();
                JsonNode tree = objectMapper.valueToTree(antigo);
                node.set("BACKUP_EXCLUSAO", simplificarJson(tree));
                return node.toString();
            }

            // UPDATE: Tenta gerar o Diff campo a campo
            JsonNode oldNode = simplificarJson(objectMapper.valueToTree(antigo));
            JsonNode newNode = simplificarJson(objectMapper.valueToTree(novo));
            ObjectNode diffNode = objectMapper.createObjectNode();

            Iterator<String> fieldNames = newNode.fieldNames();

            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode valOld = oldNode.get(fieldName);
                JsonNode valNew = newNode.get(fieldName);

                // Se houver diferença, registra
                if (!isEquals(valOld, valNew)) {
                    ObjectNode change = diffNode.putObject(fieldName);
                    change.set("de", valOld);
                    change.set("para", valNew);
                }
            }

            // Fallback: Se não detectou diff (ex: campos ignorados), mas houve update,
            // retorna o estado atual limpo para registro.
            if (diffNode.isEmpty()) {
                return newNode.toString();
            }

            return diffNode.toString();

        } catch (Exception e) {
            log.error("Erro ao gerar Diff de auditoria", e);
            return "{\"erro\": \"Falha técnica ao gerar diff\"}";
        }
    }

    /**
     * Remove campos técnicos e simplifica objetos aninhados (Associações)
     * para evitar poluição (ex: transforma Objeto Parceiro em "ID: 1")
     */
    private JsonNode simplificarJson(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;

            // 1. Remove campos ignorados desta camada
            objectNode.remove(IGNORAR_CAMPOS);

            // 2. Itera sobre os campos restantes
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode valor = entry.getValue();

                // Se o campo for um Objeto (Associação/Entity) e tiver ID inside
                if (valor.isObject() && valor.has("id")) {
                    // Substitui o objeto gigante por apenas o ID ou uma referência curta
                    String ref = "ID: " + valor.get("id").asText();

                    // Tenta pegar um nome ou código para ficar mais legível
                    if (valor.has("nome"))
                        ref += " (" + valor.get("nome").asText() + ")";
                    else if (valor.has("codigo"))
                        ref += " (" + valor.get("codigo").asText() + ")";
                    else if (valor.has("sku"))
                        ref += " (" + valor.get("sku").asText() + ")";

                    entry.setValue(new TextNode(ref));
                }
                // Se for objeto mas sem ID, processa recursivamente
                else if (valor.isObject()) {
                    simplificarJson(valor);
                }
                // Se for Array, processa os itens
                else if (valor.isArray()) {
                    valor.forEach(this::simplificarJson);
                }
            }
        }
        return node;
    }

    private boolean isEquals(JsonNode n1, JsonNode n2) {
        if (n1 == null && n2 == null)
            return true;
        if (n1 == null || n2 == null)
            return false;
        return n1.equals(n2);
    }
}