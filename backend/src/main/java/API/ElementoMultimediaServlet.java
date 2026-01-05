package API;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

@WebServlet(name = "ElementoMultimediaServlet", urlPatterns = {"/api/elemento-multimedia"})
public class ElementoMultimediaServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id_usuario_sesion = getSessionUserId(request);
        Integer id_rol_sesion = getSessionRoleId(request);
        boolean es_admin = isAdmin(id_rol_sesion);

        Integer id_elemento = parseInt(request.getParameter("id_elemento"));
        if (id_elemento == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "id_elemento_requerido");
            return;
        }
        if (!es_admin && !isOwnerElement(id_elemento.intValue(), id_usuario_sesion)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        String sql = "SELECT em.id_elemento, em.id_archivo, em.tipo_uso, em.fecha_creacion, "
                + "am.tipo_media, am.titulo, am.ruta_archivo "
                + "FROM elemento_multimedia em "
                + "INNER JOIN archivos_multimedia am ON am.id_archivo = em.id_archivo "
                + "WHERE em.id_elemento = ? ORDER BY em.id_archivo";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id_elemento.intValue());
            try (ResultSet rs = ps.executeQuery()) {
                JsonArrayBuilder items = Json.createArrayBuilder();
                while (rs.next()) {
                    JsonObjectBuilder item = Json.createObjectBuilder();
                    item.add("id_elemento", rs.getInt("id_elemento"));
                    item.add("id_archivo", rs.getInt("id_archivo"));
                    item.add("tipo_uso", rs.getString("tipo_uso"));
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
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_elemento_multimedia");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id_usuario_sesion = getSessionUserId(request);
        Integer id_rol_sesion = getSessionRoleId(request);
        boolean es_admin = isAdmin(id_rol_sesion);

        JsonObject payload = JsonUtil.readJsonObject(request);
        Integer id_elemento = JsonUtil.getInt(payload, "id_elemento");
        Integer id_archivo = JsonUtil.getInt(payload, "id_archivo");
        String tipo_uso = normalizeTipoUso(JsonUtil.getString(payload, "tipo_uso"));

        if (id_elemento == null || id_archivo == null || tipo_uso == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "datos_incompletos");
            return;
        }
        if (!es_admin && !isOwnerElement(id_elemento.intValue(), id_usuario_sesion)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        String sql = "INSERT INTO elemento_multimedia (id_elemento, id_archivo, tipo_uso) VALUES (?,?,?)";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id_elemento.intValue());
            ps.setInt(2, id_archivo.intValue());
            ps.setString(3, tipo_uso);
            ps.executeUpdate();
            JsonObjectBuilder body = Json.createObjectBuilder().add("ok", true);
            ResponseUtil.writeOk(response, body.build());
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_crear_elemento_multimedia");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id_usuario_sesion = getSessionUserId(request);
        Integer id_rol_sesion = getSessionRoleId(request);
        boolean es_admin = isAdmin(id_rol_sesion);

        Integer id_elemento = parseInt(request.getParameter("id_elemento"));
        Integer id_archivo = parseInt(request.getParameter("id_archivo"));
        if (id_elemento == null || id_archivo == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "datos_incompletos");
            return;
        }
        if (!es_admin && !isOwnerElement(id_elemento.intValue(), id_usuario_sesion)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        String sql = "DELETE FROM elemento_multimedia WHERE id_elemento = ? AND id_archivo = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id_elemento.intValue());
            ps.setInt(2, id_archivo.intValue());
            int deleted = ps.executeUpdate();
            if (deleted == 0) {
                ResponseUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "relacion_no_encontrada");
                return;
            }
            JsonObjectBuilder body = Json.createObjectBuilder().add("ok", true);
            ResponseUtil.writeOk(response, body.build());
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_eliminar_elemento_multimedia");
        }
    }

    private boolean isOwnerElement(int id_elemento, Integer id_usuario_sesion) {
        if (id_usuario_sesion == null) {
            return false;
        }
        String sql = "SELECT d.id_usuario FROM diagramas_uml d "
                + "INNER JOIN elementos_diagrama e ON e.id_diagrama = d.id_diagrama "
                + "WHERE e.id_elemento = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id_elemento);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_usuario") == id_usuario_sesion.intValue();
                }
            }
        } catch (Exception ex) {
            return false;
        }
        return false;
    }

    private String normalizeTipoUso(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            return null;
        }
        String normalized = tipo.trim().toUpperCase();
        if ("ICONO".equals(normalized) || "FONDO".equals(normalized) || "ADJUNTO".equals(normalized)) {
            return normalized;
        }
        return null;
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

    private boolean isAdmin(Integer id_rol) {
        return id_rol != null && id_rol.intValue() == 1;
    }
}
