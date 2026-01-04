package API;

import java.io.IOException;
import java.math.BigDecimal;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.servlet.http.HttpServletRequest;

public final class JsonUtil {
    private JsonUtil() {
    }

    public static JsonObject readJsonObject(HttpServletRequest request) throws IOException {
        try (JsonReader reader = Json.createReader(request.getInputStream())) {
            JsonStructure structure = reader.read();
            if (structure != null && structure.getValueType() == JsonValue.ValueType.OBJECT) {
                return (JsonObject) structure;
            }
        } catch (JsonException ex) {
            return null;
        }
        return null;
    }

    public static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.containsKey(key) || obj.isNull(key)) {
            return null;
        }
        if (obj.get(key).getValueType() == JsonValue.ValueType.STRING) {
            return obj.getString(key, null);
        }
        return obj.get(key).toString();
    }

    public static Integer getInt(JsonObject obj, String key) {
        String value = getString(obj, key);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static Long getLong(JsonObject obj, String key) {
        String value = getString(obj, key);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static BigDecimal getDecimal(JsonObject obj, String key) {
        String value = getString(obj, key);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static JsonObjectBuilder add(JsonObjectBuilder builder, String key, String value) {
        if (value == null) {
            return builder.addNull(key);
        }
        return builder.add(key, value);
    }

    public static JsonObjectBuilder add(JsonObjectBuilder builder, String key, Integer value) {
        if (value == null) {
            return builder.addNull(key);
        }
        return builder.add(key, value.intValue());
    }

    public static JsonObjectBuilder add(JsonObjectBuilder builder, String key, Long value) {
        if (value == null) {
            return builder.addNull(key);
        }
        return builder.add(key, value.longValue());
    }

    public static JsonObjectBuilder add(JsonObjectBuilder builder, String key, BigDecimal value) {
        if (value == null) {
            return builder.addNull(key);
        }
        return builder.add(key, value);
    }
}
