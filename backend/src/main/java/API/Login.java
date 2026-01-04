package API;

import java.io.IOException;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Login extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleLogin(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleLogin(request, response);
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String usuario = request.getParameter("User");
        if (usuario == null) {
            usuario = request.getParameter("nombre_usuario");
        }
        String password = request.getParameter("password");
        if (password == null) {
            password = request.getParameter("contrasena");
        }

        if (usuario == null || password == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "credenciales_incompletas");
            return;
        }

        try {
            AuthService.AuthResult result = AuthService.authenticate(usuario, password);
            JsonObjectBuilder body = Json.createObjectBuilder();
            if (result != null) {
                AuthService.applySession(request, result);
                body.add("status", "yes")
                        .add("tipo", result.nombreRol)
                        .add("id_usuario", result.idUsuario)
                        .add("id_rol", result.idRol)
                        .add("nombre_usuario", result.nombreUsuario);
            } else {
                body.add("status", "no")
                        .add("tipo", "nodefinido");
            }
            ResponseUtil.writeOk(response, body.build());
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_autenticacion");
        }
    }
}
