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

@WebServlet(name = "ConexionesServlet", urlPatterns = {"/api/conexiones"})
public class ConexionesServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer sessionUserId = getSessionUserId(request);
        Integer sessionRoleId = getSessionRoleId(request);
        boolean admin = isAdmin(sessionRoleId);

        Integer idConexion = parseInt(request.getParameter("id_conexion"));
        if (idConexion != null) {
            String sql = "SELECT id_conexion, id_diagrama, id_elemento_origen, id_elemento_destino, tipo_conexion, "
                    + "etiqueta, puntos_json, estilo_json, fecha_creacion, fecha_actualizacion "
                    + "FROM conexiones_diagrama WHERE id_conexion = ?";
            try (Connection con = DbUtil.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idConexion.intValue());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int idDiagrama = rs.getInt("id_diagrama");
                        if (!admin && !isOwnerDiagram(idDiagrama, sessionUserId)) {
                            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
                            return;
                        }
                        JsonObjectBuilder body = Json.createObjectBuilder()
                                .add("ok", true)
                                .add("conexion", buildConexion(rs));
                        ResponseUtil.writeOk(response, body.build());
                        return;
                    }
                }
                ResponseUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "conexion_no_encontrada");
            } catch (Exception ex) {
                ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_conexiones");
            }
            return;
        }

        Integer idDiagrama = parseInt(request.getParameter("id_diagrama"));
        if (idDiagrama == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "id_diagrama_requerido");
            return;
        }
        if (!admin && !isOwnerDiagram(idDiagrama.intValue(), sessionUserId)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        String sql = "SELECT id_conexion, id_diagrama, id_elemento_origen, id_elemento_destino, tipo_conexion, "
                + "etiqueta, puntos_json, estilo_json, fecha_creacion, fecha_actualizacion "
                + "FROM conexiones_diagrama WHERE id_diagrama = ? ORDER BY id_conexion";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idDiagrama.intValue());
            try (ResultSet rs = ps.executeQuery()) {
                JsonArrayBuilder conexiones = Json.createArrayBuilder();
                while (rs.next()) {
                    conexiones.add(buildConexion(rs));
                }
                JsonObjectBuilder body = Json.createObjectBuilder()
                        .add("ok", true)
                        .add("conexiones", conexiones);
                ResponseUtil.writeOk(response, body.build());
            }
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_conexiones");
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
        Integer idElementoOrigen = JsonUtil.getInt(payload, "id_elemento_origen");
        Integer idElementoDestino = JsonUtil.getInt(payload, "id_elemento_destino");
        String tipoConexion = normalizeTipoConexion(JsonUtil.getString(payload, "tipo_conexion"));
        String etiqueta = JsonUtil.getString(payload, "etiqueta");
        String puntosJson = JsonUtil.getString(payload, "puntos_json");
        String estiloJson = JsonUtil.getString(payload, "estilo_json");

        if (idDiagrama == null || idElementoOrigen == null || idElementoDestino == null || tipoConexion == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "datos_incompletos");
            return;
        }
        if (!admin && !isOwnerDiagram(idDiagrama.intValue(), sessionUserId)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        String sql = "INSERT INTO conexiones_diagrama (id_diagrama, id_elemento_origen, id_elemento_destino, "
                + "tipo_conexion, etiqueta, puntos_json, estilo_json) VALUES (?,?,?,?,?,?,?)";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idDiagrama.intValue());
            ps.setInt(2, idElementoOrigen.intValue());
            ps.setInt(3, idElementoDestino.intValue());
            ps.setString(4, tipoConexion);
            if (etiqueta == null || etiqueta.trim().isEmpty()) {
                ps.setNull(5, Types.VARCHAR);
            } else {
                ps.setString(5, etiqueta);
            }
            if (puntosJson == null || puntosJson.trim().isEmpty()) {
                ps.setNull(6, Types.LONGVARCHAR);
            } else {
                ps.setString(6, puntosJson);
            }
            if (estiloJson == null || estiloJson.trim().isEmpty()) {
                ps.setNull(7, Types.LONGVARCHAR);
            } else {
                ps.setString(7, estiloJson);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                JsonObjectBuilder body = Json.createObjectBuilder().add("ok", true);
                if (keys.next()) {
                    body.add("id_conexion", keys.getInt(1));
                }
                ResponseUtil.writeOk(response, body.build());
            }
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_crear_conexion");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer sessionUserId = getSessionUserId(request);
        Integer sessionRoleId = getSessionRoleId(request);
        boolean admin = isAdmin(sessionRoleId);

        JsonObject payload = JsonUtil.readJsonObject(request);
        Integer idConexion = JsonUtil.getInt(payload, "id_conexion");
        Integer idDiagrama = JsonUtil.getInt(payload, "id_diagrama");
        Integer idElementoOrigen = JsonUtil.getInt(payload, "id_elemento_origen");
        Integer idElementoDestino = JsonUtil.getInt(payload, "id_elemento_destino");
        String tipoConexion = normalizeTipoConexion(JsonUtil.getString(payload, "tipo_conexion"));
        String etiqueta = JsonUtil.getString(payload, "etiqueta");
        String puntosJson = JsonUtil.getString(payload, "puntos_json");
        String estiloJson = JsonUtil.getString(payload, "estilo_json");

        if (idConexion == null || idDiagrama == null || idElementoOrigen == null || idElementoDestino == null || tipoConexion == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "datos_incompletos");
            return;
        }
        if (!admin && !isOwnerDiagram(idDiagrama.intValue(), sessionUserId)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        String sql = "UPDATE conexiones_diagrama SET id_diagrama = ?, id_elemento_origen = ?, id_elemento_destino = ?, "
                + "tipo_conexion = ?, etiqueta = ?, puntos_json = ?, estilo_json = ? WHERE id_conexion = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idDiagrama.intValue());
            ps.setInt(2, idElementoOrigen.intValue());
            ps.setInt(3, idElementoDestino.intValue());
            ps.setString(4, tipoConexion);
            if (etiqueta == null || etiqueta.trim().isEmpty()) {
                ps.setNull(5, Types.VARCHAR);
            } else {
                ps.setString(5, etiqueta);
            }
            if (puntosJson == null || puntosJson.trim().isEmpty()) {
                ps.setNull(6, Types.LONGVARCHAR);
            } else {
                ps.setString(6, puntosJson);
            }
            if (estiloJson == null || estiloJson.trim().isEmpty()) {
                ps.setNull(7, Types.LONGVARCHAR);
            } else {
                ps.setString(7, estiloJson);
            }
            ps.setInt(8, idConexion.intValue());
            int updated = ps.executeUpdate();
            if (updated == 0) {
                ResponseUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "conexion_no_encontrada");
                return;
            }
            JsonObjectBuilder body = Json.createObjectBuilder().add("ok", true);
            ResponseUtil.writeOk(response, body.build());
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_actualizar_conexion");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer sessionUserId = getSessionUserId(request);
        Integer sessionRoleId = getSessionRoleId(request);
        boolean admin = isAdmin(sessionRoleId);

        Integer idConexion = parseInt(request.getParameter("id_conexion"));
        if (idConexion == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "id_conexion_requerido");
            return;
        }
        if (!admin && !isOwnerConexion(idConexion.intValue(), sessionUserId)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        String sql = "DELETE FROM conexiones_diagrama WHERE id_conexion = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idConexion.intValue());
            int deleted = ps.executeUpdate();
            if (deleted == 0) {
                ResponseUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "conexion_no_encontrada");
                return;
            }
            JsonObjectBuilder body = Json.createObjectBuilder().add("ok", true);
            ResponseUtil.writeOk(response, body.build());
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_eliminar_conexion");
        }
    }

    private JsonObjectBuilder buildConexion(ResultSet rs) throws Exception {
        JsonObjectBuilder conexion = Json.createObjectBuilder();
        conexion.add("id_conexion", rs.getInt("id_conexion"));
        conexion.add("id_diagrama", rs.getInt("id_diagrama"));
        conexion.add("id_elemento_origen", rs.getInt("id_elemento_origen"));
        conexion.add("id_elemento_destino", rs.getInt("id_elemento_destino"));
        conexion.add("tipo_conexion", rs.getString("tipo_conexion"));
        JsonUtil.add(conexion, "etiqueta", rs.getString("etiqueta"));
        JsonUtil.add(conexion, "puntos_json", rs.getString("puntos_json"));
        JsonUtil.add(conexion, "estilo_json", rs.getString("estilo_json"));
        Timestamp creado = rs.getTimestamp("fecha_creacion");
        Timestamp actualizado = rs.getTimestamp("fecha_actualizacion");
        JsonUtil.add(conexion, "fecha_creacion", creado == null ? null : creado.toString());
        JsonUtil.add(conexion, "fecha_actualizacion", actualizado == null ? null : actualizado.toString());
        return conexion;
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

    private boolean isOwnerConexion(int idConexion, Integer sessionUserId) {
        if (sessionUserId == null) {
            return false;
        }
        String sql = "SELECT d.id_usuario FROM diagramas_uml d "
                + "INNER JOIN conexiones_diagrama c ON c.id_diagrama = d.id_diagrama "
                + "WHERE c.id_conexion = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idConexion);
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

    private String normalizeTipoConexion(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            return null;
        }
        String normalized = tipo.trim().toUpperCase();
        if ("ASOCIACION".equals(normalized)
                || "INCLUSION".equals(normalized)
                || "EXTENSION".equals(normalized)
                || "GENERALIZACION".equals(normalized)
                || "DEPENDENCIA".equals(normalized)
                || "ENLACE_NOTA".equals(normalized)) {
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
