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

/**
 * Servlet de autenticacion que maneja login y logout por /api/auth/*.
 *
 * Expone endpoints JSON para iniciar/cerrar sesion. El login crea
 * HttpSession y devuelve datos basicos del usuario/rol.
 *
 */
@WebServlet(name = "AuthServlet", urlPatterns = {"/api/auth/login", "/api/auth/logout"})
public class AuthServlet extends HttpServlet {

    /**
     * Procesa login (credenciales en JSON o form) y logout.
     * No retorna valor; escribe JSON y puede responder 400/401/500 segun validacion.
     *
     * Flujo:
     *
     * - Si la ruta es /logout, invalida la sesion y responde ok.
     * - Si es /login, lee JSON o parametros form para compatibilidad.
     * - Valida credenciales, crea sesion y responde datos del usuario.
     *
     *
     * @param request request HTTP actual.
     * @param response response HTTP actual.
     * @throws ServletException si el contenedor falla.
     * @throws IOException si falla la escritura de la respuesta.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if ("/api/auth/logout".equals(path)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            // Logout siempre responde OK para simplificar el frontend.
            JsonObject body = Json.createObjectBuilder().add("ok", true).build();
            ResponseUtil.writeOk(response, body);
            return;
        }

        // Login: admite JSON en body o parametros form para compatibilidad legacy.
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

            // Crea sesion y retorna datos basicos del usuario.
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
