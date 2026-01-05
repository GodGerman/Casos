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

/**
 * Helpers para leer JSON desde HttpServletRequest y construir respuestas con nulls.
 *
 * Centraliza parseo tolerante (nulls, tipos inesperados) y escritura de
 * valores opcionales para evitar duplicar validaciones en cada servlet.
 *
 */
public final class JsonUtil {
    /**
     * Constructor privado para evitar instanciacion.
     */
    private JsonUtil() {
    }

    /**
     * Lee el body y devuelve el JsonObject si el payload es un objeto.
     *
     * Se crea un JsonReader sobre el InputStream y valida el tipo
     * de la estructura leida. Si el JSON es invalido, retorna null.
     *
     *
     * @param request request HTTP con body JSON.
     * @return JsonObject o null si el body es vacio o no es un objeto.
     * @throws IOException si falla la lectura del stream.
     */
    public static JsonObject readJsonObject(HttpServletRequest request) throws IOException {
        try (JsonReader reader = Json.createReader(request.getInputStream())) {
            JsonStructure structure = reader.read();
            if (structure != null && structure.getValueType() == JsonValue.ValueType.OBJECT) {
                return (JsonObject) structure;
            }
        } catch (JsonException ex) {
            // JSON malformado: se reporta como null para que el servlet valide.
            return null;
        }
        return null;
    }

    /**
     * Obtiene un String desde un JsonObject, tolerando tipos no string.
     *
     * Se si el valor no es STRING, usa toString() para soportar
     * numeros u otros tipos simples enviados por el cliente.
     *
     *
     * @param obj objeto origen.
     * @param key clave a leer.
     * @return valor como String o null si no existe/esta vacio.
     */
    public static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.containsKey(key) || obj.isNull(key)) {
            return null;
        }
        if (obj.get(key).getValueType() == JsonValue.ValueType.STRING) {
            return obj.getString(key, null);
        }
        return obj.get(key).toString();
    }

    /**
     * Convierte un campo JSON a Integer.
     *
     * Se reusa {@link #getString(JsonObject, String)} y parsea
     * el valor numerico con validacion basica.
     *
     *
     * @param obj objeto origen.
     * @param key clave a leer.
     * @return Integer o null si el valor es invalido/no existe.
     */
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

    /**
     * Convierte un campo JSON a Long.
     *
     * Se reusa {@link #getString(JsonObject, String)} y parsea
     * el valor numerico con validacion basica.
     *
     *
     * @param obj objeto origen.
     * @param key clave a leer.
     * @return Long o null si el valor es invalido/no existe.
     */
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

    /**
     * Convierte un campo JSON a BigDecimal.
     *
     * Se reusa {@link #getString(JsonObject, String)} y parsea
     * con BigDecimal para mantener precision en decimales.
     *
     *
     * @param obj objeto origen.
     * @param key clave a leer.
     * @return BigDecimal o null si el valor es invalido/no existe.
     */
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

    /**
     * Agrega un String al JsonObjectBuilder, escribiendo null si aplica.
     *
     * Se si el valor es null, usa addNull para conservar la clave
     * en la respuesta y evitar inconsistencias en el frontend.
     *
     *
     * @param builder builder destino.
     * @param key clave a escribir.
     * @param value valor string o null.
     * @return builder actualizado.
     */
    public static JsonObjectBuilder add(JsonObjectBuilder builder, String key, String value) {
        if (value == null) {
            return builder.addNull(key);
        }
        return builder.add(key, value);
    }

    /**
     * Agrega un Integer al JsonObjectBuilder, escribiendo null si aplica.
     *
     * Se normaliza nulls a JSON null para mantener contrato.
     *
     *
     * @param builder builder destino.
     * @param key clave a escribir.
     * @param value valor integer o null.
     * @return builder actualizado.
     */
    public static JsonObjectBuilder add(JsonObjectBuilder builder, String key, Integer value) {
        if (value == null) {
            return builder.addNull(key);
        }
        return builder.add(key, value.intValue());
    }

    /**
     * Agrega un Long al JsonObjectBuilder, escribiendo null si aplica.
     *
     * Se normaliza nulls a JSON null para mantener contrato.
     *
     *
     * @param builder builder destino.
     * @param key clave a escribir.
     * @param value valor long o null.
     * @return builder actualizado.
     */
    public static JsonObjectBuilder add(JsonObjectBuilder builder, String key, Long value) {
        if (value == null) {
            return builder.addNull(key);
        }
        return builder.add(key, value.longValue());
    }

    /**
     * Agrega un BigDecimal al JsonObjectBuilder, escribiendo null si aplica.
     *
     * Se normaliza nulls a JSON null para mantener contrato.
     *
     *
     * @param builder builder destino.
     * @param key clave a escribir.
     * @param value valor decimal o null.
     * @return builder actualizado.
     */
    public static JsonObjectBuilder add(JsonObjectBuilder builder, String key, BigDecimal value) {
        if (value == null) {
            return builder.addNull(key);
        }
        return builder.add(key, value);
    }
}
