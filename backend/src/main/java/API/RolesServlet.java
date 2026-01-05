package API;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet de lectura de roles disponibles en el sistema.
 *
 * Expone un catalogo de roles para interfaces de administracion.
 * No requiere payload y retorna todos los roles ordenados.
 *
 */
@WebServlet(name = "RolesServlet", urlPatterns = {"/api/roles"})
public class RolesServlet extends HttpServlet {

    /**
     * Lista todos los roles registrados.
     * No retorna valor; escribe JSON y puede responder 500 en error de BD.
     *
     * Se ejecuta SELECT simple y construye un arreglo JSON.
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
        JsonArrayBuilder roles = Json.createArrayBuilder();
        // Catalogo base para menus de administracion/registro.
        String sql = "SELECT id_rol, nombre_rol, descripcion FROM roles ORDER BY id_rol";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                JsonObjectBuilder row = Json.createObjectBuilder();
                row.add("id_rol", rs.getInt("id_rol"));
                row.add("nombre_rol", rs.getString("nombre_rol"));
                String descripcion = rs.getString("descripcion");
                JsonUtil.add(row, "descripcion", descripcion);
                roles.add(row);
            }
            JsonObjectBuilder body = Json.createObjectBuilder()
                    .add("ok", true)
                    .add("roles", roles);
            ResponseUtil.writeOk(response, body.build());
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_roles");
        }
    }
}
