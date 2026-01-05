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

/**
 * Servlet para administrar la relacion entre diagramas y archivos multimedia.
 *
 * Maneja la tabla puente diagrama_multimedia y expone el listado de archivos
 * asociados con metadatos de archivos_multimedia.
 *
 */
@WebServlet(name = "DiagramaMultimediaServlet", urlPatterns = {"/api/diagrama-multimedia"})
public class DiagramaMultimediaServlet extends HttpServlet {

    /**
     * Lista multimedia asociada a un diagrama.
     * No retorna valor; responde 400/403/500 segun validaciones.
     *
     * Se valida el id_diagrama, se verifica la propiedad, se ejecuta un JOIN
     * con archivos_multimedia y devuelve el listado.
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
        Integer id_usuario_sesion = getSessionUserId(request);
        Integer id_rol_sesion = getSessionRoleId(request);
        boolean es_admin = isAdmin(id_rol_sesion);

        Integer id_diagrama = parseInt(request.getParameter("id_diagrama"));
        if (id_diagrama == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "id_diagrama_requerido");
            return;
        }
        if (!es_admin && !isOwnerDiagram(id_diagrama.intValue(), id_usuario_sesion)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        // Join para devolver metadatos del archivo.
        String sql = "SELECT dm.id_diagrama, dm.id_archivo, dm.descripcion, dm.orden, dm.fecha_creacion, "
                + "am.tipo_media, am.titulo, am.ruta_archivo "
                + "FROM diagrama_multimedia dm "
                + "INNER JOIN archivos_multimedia am ON am.id_archivo = dm.id_archivo "
                + "WHERE dm.id_diagrama = ? ORDER BY dm.orden, dm.id_archivo";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id_diagrama.intValue());
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

    /**
     * Crea una asociacion diagrama-archivo con orden opcional.
     * No retorna valor; responde 400/403/500 segun validaciones.
     *
     * Se validan ids, se fuerza orden default, se inserta en tabla puente.
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
        Integer id_usuario_sesion = getSessionUserId(request);
        Integer id_rol_sesion = getSessionRoleId(request);
        boolean es_admin = isAdmin(id_rol_sesion);

        JsonObject payload = JsonUtil.readJsonObject(request);
        Integer id_diagrama = JsonUtil.getInt(payload, "id_diagrama");
        Integer id_archivo = JsonUtil.getInt(payload, "id_archivo");
        String descripcion = JsonUtil.getString(payload, "descripcion");
        Integer orden = JsonUtil.getInt(payload, "orden");

        if (id_diagrama == null || id_archivo == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "datos_incompletos");
            return;
        }
        if (!es_admin && !isOwnerDiagram(id_diagrama.intValue(), id_usuario_sesion)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }
        if (orden == null) {
            orden = 0;
        }

        // Inserta relacion en tabla puente.
        String sql = "INSERT INTO diagrama_multimedia (id_diagrama, id_archivo, descripcion, orden) VALUES (?,?,?,?)";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id_diagrama.intValue());
            ps.setInt(2, id_archivo.intValue());
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

    /**
     * Elimina la asociacion entre diagrama y archivo.
     * No retorna valor; responde 400/403/404/500 segun validaciones.
     *
     * Se validan ids, se verifica la propiedad y se ejecuta DELETE.
     *
     *
     * @param request request HTTP actual.
     * @param response response HTTP actual.
     * @throws ServletException si el contenedor falla.
     * @throws IOException si falla la escritura de respuesta.
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id_usuario_sesion = getSessionUserId(request);
        Integer id_rol_sesion = getSessionRoleId(request);
        boolean es_admin = isAdmin(id_rol_sesion);

        Integer id_diagrama = parseInt(request.getParameter("id_diagrama"));
        Integer id_archivo = parseInt(request.getParameter("id_archivo"));
        if (id_diagrama == null || id_archivo == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "datos_incompletos");
            return;
        }
        if (!es_admin && !isOwnerDiagram(id_diagrama.intValue(), id_usuario_sesion)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        String sql = "DELETE FROM diagrama_multimedia WHERE id_diagrama = ? AND id_archivo = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id_diagrama.intValue());
            ps.setInt(2, id_archivo.intValue());
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

    /**
     * Verifica si el diagrama pertenece al usuario de la sesion.
     *
     * Se consulta id_usuario propietario y compara con sesion.
     *
     *
     * @param id_diagrama id del diagrama.
     * @param id_usuario_sesion id del usuario autenticado.
     * @return true si es propietario; false si no coincide o hay error.
     */
    private boolean isOwnerDiagram(int id_diagrama, Integer id_usuario_sesion) {
        if (id_usuario_sesion == null) {
            return false;
        }
        String sql = "SELECT id_usuario FROM diagramas_uml WHERE id_diagrama = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id_diagrama);
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

    /**
     * Parsea un entero desde query string.
     *
     * Se recorta el texto y se parsea con manejo de NumberFormatException.
     *
     *
     * @param value texto recibido.
     * @return Integer o null si no es valido.
     */
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

    /**
     * Obtiene id_usuario de la sesion si existe.
     *
     * Se lee el atributo "id_usuario" y valida tipo Integer.
     *
     *
     * @param request request HTTP actual.
     * @return id_usuario o null si no hay sesion.
     */
    private Integer getSessionUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute("id_usuario");
        return value instanceof Integer ? (Integer) value : null;
    }

    /**
     * Obtiene id_rol de la sesion si existe.
     *
     * Se lee el atributo "id_rol" y valida tipo Integer.
     *
     *
     * @param request request HTTP actual.
     * @return id_rol o null si no hay sesion.
     */
    private Integer getSessionRoleId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute("id_rol");
        return value instanceof Integer ? (Integer) value : null;
    }

    /**
     * Determina si el rol corresponde a administrador (id_rol = 1).
     *
     * Se usa como regla simple de autorizacion en todos los servlets.
     *
     *
     * @param id_rol id del rol.
     * @return true si es admin, false en caso contrario.
     */
    private boolean isAdmin(Integer id_rol) {
        return id_rol != null && id_rol.intValue() == 1;
    }
}
