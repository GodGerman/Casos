package API;

import java.io.IOException;
import java.io.PrintWriter;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.servlet.http.HttpServletResponse;

public final class ResponseUtil {
    private ResponseUtil() {
    }

    public static void writeJson(HttpServletResponse response, JsonStructure json, int status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.print(json.toString());
        }
    }

    public static void writeOk(HttpServletResponse response, JsonStructure json) throws IOException {
        writeJson(response, json, HttpServletResponse.SC_OK);
    }

    public static void writeError(HttpServletResponse response, int status, String mensaje) throws IOException {
        JsonObject body = Json.createObjectBuilder()
                .add("ok", false)
                .add("mensaje", mensaje == null ? "" : mensaje)
                .build();
        writeJson(response, body, status);
    }
}
