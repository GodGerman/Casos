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

@WebServlet(name = "RolesServlet", urlPatterns = {"/api/roles"})
public class RolesServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        JsonArrayBuilder roles = Json.createArrayBuilder();
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
