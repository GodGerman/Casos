package API;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(name = "DiagramaMultimediaServlet", urlPatterns = {"/api/diagrama-multimedia"})
public class DiagramaMultimediaServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer sessionUserId = getSessionUserId(request);
        Integer sessionRoleId = getSessionRoleId(request);
        boolean admin = isAdmin(sessionRoleId);

        Integer idDiagrama = parseInt(request.getParameter("id_diagrama"));
        if (idDiagrama == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "id_diagrama_requerido");
            return;
        }
        if (!admin && !isOwnerDiagram(idDiagrama.intValue(), sessionUserId)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        String sql = "SELECT dm.id_diagrama, dm.id_archivo, dm.descripcion, dm.orden, dm.fecha_creacion, "
                + "am.tipo_media, am.titulo, am.ruta_archivo "
                + "FROM diagrama_multimedia dm "
                + "INNER JOIN archivos_multimedia am ON am.id_archivo = dm.id_archivo "
                + "WHERE dm.id_diagrama = ? ORDER BY dm.orden, dm.id_archivo";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idDiagrama.intValue());
            try (ResultSet rs = ps.executeQuery()) {
                JsonArrayBuilder items = Json.createArrayBuilder();
                while (rs.next()) {
                    JsonObjectBuilder item = Json.createObjectBuilder();
                    item.add("id_diagrama", rs.getInt("id_diagrama"));
                    item.add("id_archivo", rs.getInt("id_archivo"));
                    JsonUtil.add(item, "descripcion", rs.getString("descripcion"));
                    item.add("orden", rs.getInt("orden"));
                    item.add("tipo_media", rs.getString("tipo_media"));
                    JsonUtil.add(item, "titulo", rs.getString("titulo"));
                    JsonUtil.add(item, "ruta_archivo", rs.getString("ruta_archivo"));
                    items.add(item);
                }
                JsonObjectBuilder body = Json.createObjectBuilder()
                        .add("ok", true)
                        .add("multimedia", items);
                ResponseUtil.writeOk(response, body.build());
            }
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_diagrama_multimedia");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer sessionUserId = getSessionUserId(request);
        Integer sessionRoleId = getSessionRoleId(request);
        boolean admin = isAdmin(sessionRoleId);

        JsonObject payload = JsonUtil.readJsonObject(request);
        Integer idDiagrama = JsonUtil.getInt(payload, "id_diagrama");
        Integer idArchivo = JsonUtil.getInt(payload, "id_archivo");
        String descripcion = JsonUtil.getString(payload, "descripcion");
        Integer orden = JsonUtil.getInt(payload, "orden");

        if (idDiagrama == null || idArchivo == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "datos_incompletos");
            return;
        }
        if (!admin && !isOwnerDiagram(idDiagrama.intValue(), sessionUserId)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }
        if (orden == null) {
            orden = 0;
        }

        String sql = "INSERT INTO diagrama_multimedia (id_diagrama, id_archivo, descripcion, orden) VALUES (?,?,?,?)";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idDiagrama.intValue());
            ps.setInt(2, idArchivo.intValue());
            if (descripcion == null || descripcion.trim().isEmpty()) {
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setString(3, descripcion);
            }
            ps.setInt(4, orden.intValue());
            ps.executeUpdate();
            JsonObjectBuilder body = Json.createObjectBuilder().add("ok", true);
            ResponseUtil.writeOk(response, body.build());
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_crear_diagrama_multimedia");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer sessionUserId = getSessionUserId(request);
        Integer sessionRoleId = getSessionRoleId(request);
        boolean admin = isAdmin(sessionRoleId);

        Integer idDiagrama = parseInt(request.getParameter("id_diagrama"));
        Integer idArchivo = parseInt(request.getParameter("id_archivo"));
        if (idDiagrama == null || idArchivo == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "datos_incompletos");
            return;
        }
        if (!admin && !isOwnerDiagram(idDiagrama.intValue(), sessionUserId)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        String sql = "DELETE FROM diagrama_multimedia WHERE id_diagrama = ? AND id_archivo = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idDiagrama.intValue());
            ps.setInt(2, idArchivo.intValue());
            int deleted = ps.executeUpdate();
            if (deleted == 0) {
                ResponseUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "relacion_no_encontrada");
                return;
            }
            JsonObjectBuilder body = Json.createObjectBuilder().add("ok", true);
            ResponseUtil.writeOk(response, body.build());
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_eliminar_diagrama_multimedia");
        }
    }

    private boolean isOwnerDiagram(int idDiagrama, Integer sessionUserId) {
        if (sessionUserId == null) {
            return false;
        }
        String sql = "SELECT id_usuario FROM diagramas_uml WHERE id_diagrama = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idDiagrama);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_usuario") == sessionUserId.intValue();
                }
            }
        } catch (Exception ex) {
            return false;
        }
        return false;
    }

    private Integer parseInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer getSessionUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute("id_usuario");
        return value instanceof Integer ? (Integer) value : null;
    }

    private Integer getSessionRoleId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute("id_rol");
        return value instanceof Integer ? (Integer) value : null;
    }

    private boolean isAdmin(Integer idRol) {
        return idRol != null && idRol.intValue() == 1;
    }
}
