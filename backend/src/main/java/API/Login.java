package API;

import java.io.IOException;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet legacy para autenticacion via /Login (GET/POST).
 *
 * Mantiene compatibilidad con clientes antiguos que envian User/password
 * y esperan un JSON con campos "status" y "tipo".
 *
 */
public class Login extends HttpServlet {

    /**
     * Delegado a handleLogin para login por GET.
     * No retorna valor; escribe JSON legacy en la respuesta.
     *
     * Se delega para reutilizar la logica comun de validacion.
     *
     *
     * @param request request HTTP actual.
     * @param response response HTTP actual.
     * @throws ServletException si el contenedor falla.
     * @throws IOException si falla la escritura de respuesta.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleLogin(request, response);
    }

    /**
     * Delegado a handleLogin para login por POST.
     * No retorna valor; escribe JSON legacy en la respuesta.
     *
     * Se delega para reutilizar la logica comun de validacion.
     *
     *
     * @param request request HTTP actual.
     * @param response response HTTP actual.
     * @throws ServletException si el contenedor falla.
     * @throws IOException si falla la escritura de respuesta.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleLogin(request, response);
    }

    /**
     * Valida credenciales enviadas por query/form y genera respuesta JSON legacy.
     * No retorna valor; puede responder 400 si faltan credenciales o 500 en error.
     *
     * Se lee parametros legacy/nuevos, valida presencia,
     * autentica via {@link AuthService}, crea sesion si es exitoso y
     * construye un JSON compatible con clientes antiguos.
     *
     *
     * @param request request HTTP actual.
     * @param response response HTTP actual.
     * @throws IOException si falla la escritura de respuesta.
     */
    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Acepta nombres legacy y nuevos para usuario/contrasena.
        String nombre_usuario = request.getParameter("User");
        if (nombre_usuario == null) {
            nombre_usuario = request.getParameter("nombre_usuario");
        }
        String contrasena = request.getParameter("password");
        if (contrasena == null) {
            contrasena = request.getParameter("contrasena");
        }

        if (nombre_usuario == null || contrasena == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "credenciales_incompletas");
            return;
        }

        try {
            AuthService.AuthResult result = AuthService.authenticate(nombre_usuario, contrasena);
            JsonObjectBuilder body = Json.createObjectBuilder();
            if (result != null) {
                // En login legacy tambien se crea sesion para /api/*.
                AuthService.applySession(request, result);
                body.add("status", "yes")
                        .add("tipo", result.nombre_rol)
                        .add("nombre_rol", result.nombre_rol)
                        .add("id_usuario", result.id_usuario)
                        .add("id_rol", result.id_rol)
                        .add("nombre_usuario", result.nombre_usuario);
            } else {
                body.add("status", "no")
                        .add("tipo", "nodefinido")
                        .add("nombre_rol", "nodefinido");
            }
            ResponseUtil.writeOk(response, body.build());
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_autenticacion");
        }
    }
}
