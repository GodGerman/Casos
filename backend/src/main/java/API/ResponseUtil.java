package API;

import java.io.IOException;
import java.io.PrintWriter;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.servlet.http.HttpServletResponse;

/**
 * Utilidades para escribir respuestas JSON consistentes en el backend.
 *
 * Centraliza el seteo de status, content-type y serializacion JSON para
 * que todos los servlets entreguen el mismo formato de salida.
 *
 */
public final class ResponseUtil {
    /**
     * Constructor privado para evitar instanciacion.
     */
    private ResponseUtil() {
    }

    /**
     * Escribe un JSON con status HTTP especifico.
     * No retorna valor; escribe directamente en la respuesta.
     *
     * Se setea status/headers y serializa el JsonStructure
     * usando su representacion toString().
     *
     *
     * @param response response HTTP destino.
     * @param json payload JSON (objeto o arreglo).
     * @param status codigo HTTP a retornar.
     * @throws IOException si falla la escritura del body.
     */
    public static void writeJson(HttpServletResponse response, JsonStructure json, int status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.print(json.toString());
        }
    }

    /**
     * Escribe un JSON con status 200 OK.
     * No retorna valor; escribe directamente en la respuesta.
     *
     * Se delega en {@link #writeJson(HttpServletResponse, JsonStructure, int)}.
     *
     *
     * @param response response HTTP destino.
     * @param json payload JSON.
     * @throws IOException si falla la escritura del body.
     */
    public static void writeOk(HttpServletResponse response, JsonStructure json) throws IOException {
        writeJson(response, json, HttpServletResponse.SC_OK);
    }

    /**
     * Escribe un error estandar { ok: false, mensaje } con status indicado.
     * No retorna valor; escribe directamente en la respuesta.
     *
     * Se construye un JsonObject con ok=false y el mensaje y
     * lo envia con el status recibido.
     *
     *
     * @param response response HTTP destino.
     * @param status codigo HTTP de error.
     * @param mensaje mensaje de error para el cliente.
     * @throws IOException si falla la escritura del body.
     */
    public static void writeError(HttpServletResponse response, int status, String mensaje) throws IOException {
        JsonObject body = Json.createObjectBuilder()
                .add("ok", false)
                .add("mensaje", mensaje == null ? "" : mensaje)
                .build();
        writeJson(response, body, status);
    }
}
