package API;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
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

@WebServlet(name = "DiagramasServlet", urlPatterns = {"/api/diagramas"})
public class DiagramasServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer sessionUserId = getSessionUserId(request);
        Integer sessionRoleId = getSessionRoleId(request);
        boolean admin = isAdmin(sessionRoleId);

        Integer idDiagrama = parseInt(request.getParameter("id_diagrama"));
        if (idDiagrama != null) {
            String sql = "SELECT id_diagrama, id_usuario, nombre, descripcion, estado, ancho_lienzo, alto_lienzo, "
                    + "configuracion_json, fecha_creacion, fecha_actualizacion "
                    + "FROM diagramas_uml WHERE id_diagrama = ?";
            try (Connection con = DbUtil.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idDiagrama.intValue());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int ownerId = rs.getInt("id_usuario");
                        if (!admin && (sessionUserId == null || ownerId != sessionUserId.intValue())) {
                            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
                            return;
                        }
                        JsonObjectBuilder body = Json.createObjectBuilder()
                                .add("ok", true)
                                .add("diagrama", buildDiagrama(rs));
                        ResponseUtil.writeOk(response, body.build());
                        return;
                    }
                }
                ResponseUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "diagrama_no_encontrado");
            } catch (Exception ex) {
                ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_diagramas");
            }
            return;
        }

        Integer idUsuario = parseInt(request.getParameter("id_usuario"));
        if (!admin) {
            idUsuario = sessionUserId;
        }
        if (idUsuario == null && !admin) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        String sql = "SELECT id_diagrama, id_usuario, nombre, descripcion, estado, ancho_lienzo, alto_lienzo, "
                + "configuracion_json, fecha_creacion, fecha_actualizacion "
                + "FROM diagramas_uml ";
        if (idUsuario != null) {
            sql += "WHERE id_usuario = ? ";
        }
        sql += "ORDER BY id_diagrama";

        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (idUsuario != null) {
                ps.setInt(1, idUsuario.intValue());
            }
            try (ResultSet rs = ps.executeQuery()) {
                JsonArrayBuilder diagramas = Json.createArrayBuilder();
                while (rs.next()) {
                    diagramas.add(buildDiagrama(rs));
                }
                JsonObjectBuilder body = Json.createObjectBuilder()
                        .add("ok", true)
                        .add("diagramas", diagramas);
                ResponseUtil.writeOk(response, body.build());
            }
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_diagramas");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer sessionUserId = getSessionUserId(request);
        Integer sessionRoleId = getSessionRoleId(request);
        boolean admin = isAdmin(sessionRoleId);

        JsonObject payload = JsonUtil.readJsonObject(request);
        Integer idUsuario = JsonUtil.getInt(payload, "id_usuario");
        String nombre = JsonUtil.getString(payload, "nombre");
        String descripcion = JsonUtil.getString(payload, "descripcion");
        String estado = normalizeEstado(JsonUtil.getString(payload, "estado"));
        Integer ancho = JsonUtil.getInt(payload, "ancho_lienzo");
        Integer alto = JsonUtil.getInt(payload, "alto_lienzo");
        String configuracion = JsonUtil.getString(payload, "configuracion_json");

        if (!admin) {
            idUsuario = sessionUserId;
        }
        if (idUsuario == null || nombre == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "datos_incompletos");
            return;
        }
        if (estado == null) {
            estado = "ACTIVO";
        }
        if (ancho == null) {
            ancho = 1280;
        }
        if (alto == null) {
            alto = 720;
        }

        String sql = "INSERT INTO diagramas_uml (id_usuario, nombre, descripcion, estado, ancho_lienzo, alto_lienzo, "
                + "configuracion_json) VALUES (?,?,?,?,?,?,?)";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idUsuario.intValue());
            ps.setString(2, nombre);
            if (descripcion == null || descripcion.trim().isEmpty()) {
                ps.setNull(3, Types.LONGVARCHAR);
            } else {
                ps.setString(3, descripcion);
            }
            ps.setString(4, estado);
            ps.setInt(5, ancho.intValue());
            ps.setInt(6, alto.intValue());
            if (configuracion == null || configuracion.trim().isEmpty()) {
                ps.setNull(7, Types.LONGVARCHAR);
            } else {
                ps.setString(7, configuracion);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                JsonObjectBuilder body = Json.createObjectBuilder().add("ok", true);
                if (keys.next()) {
                    body.add("id_diagrama", keys.getInt(1));
                }
                ResponseUtil.writeOk(response, body.build());
            }
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_crear_diagrama");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer sessionUserId = getSessionUserId(request);
        Integer sessionRoleId = getSessionRoleId(request);
        boolean admin = isAdmin(sessionRoleId);

        JsonObject payload = JsonUtil.readJsonObject(request);
        Integer idDiagrama = JsonUtil.getInt(payload, "id_diagrama");
        String nombre = JsonUtil.getString(payload, "nombre");
        String descripcion = JsonUtil.getString(payload, "descripcion");
        String estado = normalizeEstado(JsonUtil.getString(payload, "estado"));
        Integer ancho = JsonUtil.getInt(payload, "ancho_lienzo");
        Integer alto = JsonUtil.getInt(payload, "alto_lienzo");
        String configuracion = JsonUtil.getString(payload, "configuracion_json");

        if (idDiagrama == null || nombre == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "datos_incompletos");
            return;
        }
        if (estado == null) {
            estado = "ACTIVO";
        }
        if (ancho == null || alto == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "dimensiones_requeridas");
            return;
        }

        if (!admin && !isOwnerDiagram(idDiagrama.intValue(), sessionUserId)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        String sql = "UPDATE diagramas_uml SET nombre = ?, descripcion = ?, estado = ?, ancho_lienzo = ?, alto_lienzo = ?, "
                + "configuracion_json = ? WHERE id_diagrama = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            if (descripcion == null || descripcion.trim().isEmpty()) {
                ps.setNull(2, Types.LONGVARCHAR);
            } else {
                ps.setString(2, descripcion);
            }
            ps.setString(3, estado);
            ps.setInt(4, ancho.intValue());
            ps.setInt(5, alto.intValue());
            if (configuracion == null || configuracion.trim().isEmpty()) {
                ps.setNull(6, Types.LONGVARCHAR);
            } else {
                ps.setString(6, configuracion);
            }
            ps.setInt(7, idDiagrama.intValue());
            int updated = ps.executeUpdate();
            if (updated == 0) {
                ResponseUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "diagrama_no_encontrado");
                return;
            }
            JsonObjectBuilder body = Json.createObjectBuilder().add("ok", true);
            ResponseUtil.writeOk(response, body.build());
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_actualizar_diagrama");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
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

        String sql = "DELETE FROM diagramas_uml WHERE id_diagrama = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idDiagrama.intValue());
            int deleted = ps.executeUpdate();
            if (deleted == 0) {
                ResponseUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "diagrama_no_encontrado");
                return;
            }
            JsonObjectBuilder body = Json.createObjectBuilder().add("ok", true);
            ResponseUtil.writeOk(response, body.build());
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_eliminar_diagrama");
        }
    }

    private JsonObjectBuilder buildDiagrama(ResultSet rs) throws Exception {
        JsonObjectBuilder diagrama = Json.createObjectBuilder();
        diagrama.add("id_diagrama", rs.getInt("id_diagrama"));
        diagrama.add("id_usuario", rs.getInt("id_usuario"));
        diagrama.add("nombre", rs.getString("nombre"));
        JsonUtil.add(diagrama, "descripcion", rs.getString("descripcion"));
        diagrama.add("estado", rs.getString("estado"));
        diagrama.add("ancho_lienzo", rs.getInt("ancho_lienzo"));
        diagrama.add("alto_lienzo", rs.getInt("alto_lienzo"));
        JsonUtil.add(diagrama, "configuracion_json", rs.getString("configuracion_json"));
        Timestamp creado = rs.getTimestamp("fecha_creacion");
        Timestamp actualizado = rs.getTimestamp("fecha_actualizacion");
        JsonUtil.add(diagrama, "fecha_creacion", creado == null ? null : creado.toString());
        JsonUtil.add(diagrama, "fecha_actualizacion", actualizado == null ? null : actualizado.toString());
        return diagrama;
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

    private String normalizeEstado(String estado) {
        if (estado == null || estado.trim().isEmpty()) {
            return null;
        }
        String normalized = estado.trim().toUpperCase();
        if ("BORRADOR".equals(normalized) || "ACTIVO".equals(normalized) || "ARCHIVADO".equals(normalized)) {
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

    private boolean isAdmin(Integer idRol) {
        return idRol != null && idRol.intValue() == 1;
    }
}
