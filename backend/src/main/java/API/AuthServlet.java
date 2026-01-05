package API;

import java.io.IOException;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(name = "AuthServlet", urlPatterns = {"/api/auth/login", "/api/auth/logout"})
public class AuthServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if ("/api/auth/logout".equals(path)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            JsonObject body = Json.createObjectBuilder().add("ok", true).build();
            ResponseUtil.writeOk(response, body);
            return;
        }

        JsonObject payload = JsonUtil.readJsonObject(request);
        String nombre_usuario = JsonUtil.getString(payload, "nombre_usuario");
        String contrasena = JsonUtil.getString(payload, "contrasena");
        if (nombre_usuario == null) {
            nombre_usuario = request.getParameter("nombre_usuario");
        }
        if (contrasena == null) {
            contrasena = request.getParameter("contrasena");
        }

        if (nombre_usuario == null || contrasena == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "credenciales_incompletas");
            return;
        }

        try {
            AuthService.AuthResult result = AuthService.authenticate(nombre_usuario, contrasena);
            if (result == null) {
                ResponseUtil.writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "credenciales_invalidas");
                return;
            }

            AuthService.applySession(request, result);

            JsonObjectBuilder builder = Json.createObjectBuilder()
                    .add("ok", true)
                    .add("id_usuario", result.id_usuario)
                    .add("id_rol", result.id_rol)
                    .add("nombre_usuario", result.nombre_usuario)
                    .add("nombre_rol", result.nombre_rol);
            ResponseUtil.writeOk(response, builder.build());
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_autenticacion");
        }
    }
}
