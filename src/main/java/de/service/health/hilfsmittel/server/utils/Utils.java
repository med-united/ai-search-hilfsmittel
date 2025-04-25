package de.service.health.hilfsmittel.server.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Utils {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static String clean(String source) {
        return source
            .replace("\\", "")
            .replace("\r\n", "")
            .replace("\n", "")
            .replace("\r", "")
            .replace("\t", " ")
            .replace("\"", "'");
    }

    public static ObjectNode asObjectNode(Object object) {
        return OBJECT_MAPPER.valueToTree(object);
    }

    public static String asString(Object object) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(object);
    }

    public static JsonNode extractJsonNode(Object entity) {
        try {
            return switch (entity) {
                case InputStream is -> OBJECT_MAPPER.readTree(new String(is.readAllBytes(), UTF_8));
                case String payload -> OBJECT_MAPPER.readTree(payload);
                case Collection<?> collection -> createArrayNode(collection);
                case ObjectNode objectNode -> objectNode;
                case ArrayNode arrayNode -> arrayNode;
                case null, default -> {
                    Map<String, String> map = Map.of("entity", entity == null ? "NULL" : "Type: " + entity.getClass().getName());
                    yield createObjectNode(map);
                }
            };
        } catch (Exception e) {
            ObjectNode node = OBJECT_MAPPER.createObjectNode();
            node.put("error", e.getMessage());
            return node;
        }
    }

    public static ArrayNode createArrayNode(Collection<?> items) {
        ArrayNode arrayNode = OBJECT_MAPPER.createArrayNode();
        items.forEach(obj -> {
            if (obj instanceof JsonNode jsonNode) {
                arrayNode.add(jsonNode);
            } else {
                arrayNode.add(extractJsonNode(obj));
            }
        });
        return arrayNode;
    }

    public static JsonNode createObjectNode(Map<String, ?> attributes) {
        ObjectNode node = OBJECT_MAPPER.createObjectNode();
        attributes.forEach((key, value) -> {
            switch (value) {
                case JsonNode jsonNode -> node.set(key, jsonNode);
                case Short shortValue -> node.put(key, shortValue);
                case Integer intValue -> node.put(key, intValue);
                case Long longValue -> node.put(key, longValue);
                case Float floatValue -> node.put(key, floatValue);
                case Double doubleValue -> node.put(key, doubleValue);
                case BigInteger biValue -> node.put(key, biValue);
                case BigDecimal bdValue -> node.put(key, bdValue);
                case String strValue -> node.put(key, strValue);
                case Boolean boolValue -> node.put(key, boolValue);
                case byte[] bytes -> node.put(key, bytes);
                case null, default -> node.put(key, String.valueOf(value));
            }
        });
        return node;
    }

    public static Throwable getOriginalCause(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    private Utils() {
    }
}
