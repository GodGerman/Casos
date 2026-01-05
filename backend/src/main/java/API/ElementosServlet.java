package API;

import java.io.IOException;
import java.math.BigDecimal;
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

@WebServlet(name = "ElementosServlet", urlPatterns = {"/api/elementos"})
public class ElementosServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id_usuario_sesion = getSessionUserId(request);
        Integer id_rol_sesion = getSessionRoleId(request);
        boolean es_admin = isAdmin(id_rol_sesion);

        Integer id_elemento = parseInt(request.getParameter("id_elemento"));
        if (id_elemento != null) {
            String sql = "SELECT id_elemento, id_diagrama, id_elemento_padre, tipo_elemento, etiqueta, pos_x, pos_y, "
                    + "ancho, alto, rotacion_grados, orden_z, estilo_json, metadatos_json, fecha_creacion, fecha_actualizacion "
                    + "FROM elementos_diagrama WHERE id_elemento = ?";
            try (Connection con = DB.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id_elemento.intValue());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int id_diagrama = rs.getInt("id_diagrama");
                        if (!es_admin && !isOwnerDiagram(id_diagrama, id_usuario_sesion)) {
                            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
                            return;
                        }
                        JsonObjectBuilder body = Json.createObjectBuilder()
                                .add("ok", true)
                                .add("elemento", buildElemento(rs));
                        ResponseUtil.writeOk(response, body.build());
                        return;
                    }
                }
                ResponseUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "elemento_no_encontrado");
            } catch (Exception ex) {
                ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_elementos");
            }
            return;
        }

        Integer id_diagrama = parseInt(request.getParameter("id_diagrama"));
        if (id_diagrama == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "id_diagrama_requerido");
            return;
        }
        if (!es_admin && !isOwnerDiagram(id_diagrama.intValue(), id_usuario_sesion)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        String sql = "SELECT id_elemento, id_diagrama, id_elemento_padre, tipo_elemento, etiqueta, pos_x, pos_y, "
                + "ancho, alto, rotacion_grados, orden_z, estilo_json, metadatos_json, fecha_creacion, fecha_actualizacion "
                + "FROM elementos_diagrama WHERE id_diagrama = ? ORDER BY id_elemento";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id_diagrama.intValue());
            try (ResultSet rs = ps.executeQuery()) {
                JsonArrayBuilder elementos = Json.createArrayBuilder();
                while (rs.next()) {
                    elementos.add(buildElemento(rs));
                }
                JsonObjectBuilder body = Json.createObjectBuilder()
                        .add("ok", true)
                        .add("elementos", elementos);
                ResponseUtil.writeOk(response, body.build());
            }
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_elementos");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id_usuario_sesion = getSessionUserId(request);
        Integer id_rol_sesion = getSessionRoleId(request);
        boolean es_admin = isAdmin(id_rol_sesion);

        JsonObject payload = JsonUtil.readJsonObject(request);
        Integer id_diagrama = JsonUtil.getInt(payload, "id_diagrama");
        Integer id_elemento_padre = JsonUtil.getInt(payload, "id_elemento_padre");
        String tipo_elemento = normalizeTipoElemento(JsonUtil.getString(payload, "tipo_elemento"));
        String etiqueta = JsonUtil.getString(payload, "etiqueta");
        Integer pos_x = JsonUtil.getInt(payload, "pos_x");
        Integer pos_y = JsonUtil.getInt(payload, "pos_y");
        Integer ancho = JsonUtil.getInt(payload, "ancho");
        Integer alto = JsonUtil.getInt(payload, "alto");
        BigDecimal rotacion_grados = JsonUtil.getDecimal(payload, "rotacion_grados");
        Integer orden_z = JsonUtil.getInt(payload, "orden_z");
        String estilo_json = JsonUtil.getString(payload, "estilo_json");
        String metadatos_json = JsonUtil.getString(payload, "metadatos_json");

        if (id_diagrama == null || tipo_elemento == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "datos_incompletos");
            return;
        }
        if (!es_admin && !isOwnerDiagram(id_diagrama.intValue(), id_usuario_sesion)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        if (pos_x == null) {
            pos_x = 0;
        }
        if (pos_y == null) {
            pos_y = 0;
        }
        if (ancho == null) {
            ancho = 120;
        }
        if (alto == null) {
            alto = 60;
        }
        if (rotacion_grados == null) {
            rotacion_grados = new BigDecimal("0.00");
        }
        if (orden_z == null) {
            orden_z = 0;
        }

        String sql = "INSERT INTO elementos_diagrama (id_diagrama, id_elemento_padre, tipo_elemento, etiqueta, pos_x, pos_y, "
                + "ancho, alto, rotacion_grados, orden_z, estilo_json, metadatos_json) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, id_diagrama.intValue());
            if (id_elemento_padre == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, id_elemento_padre.intValue());
            }
            ps.setString(3, tipo_elemento);
            if (etiqueta == null || etiqueta.trim().isEmpty()) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, etiqueta);
            }
            ps.setInt(5, pos_x.intValue());
            ps.setInt(6, pos_y.intValue());
            ps.setInt(7, ancho.intValue());
            ps.setInt(8, alto.intValue());
            ps.setBigDecimal(9, rotacion_grados);
            ps.setInt(10, orden_z.intValue());
            if (estilo_json == null || estilo_json.trim().isEmpty()) {
                ps.setNull(11, Types.LONGVARCHAR);
            } else {
                ps.setString(11, estilo_json);
            }
            if (metadatos_json == null || metadatos_json.trim().isEmpty()) {
                ps.setNull(12, Types.LONGVARCHAR);
            } else {
                ps.setString(12, metadatos_json);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                JsonObjectBuilder body = Json.createObjectBuilder().add("ok", true);
                if (keys.next()) {
                    body.add("id_elemento", keys.getInt(1));
                }
                ResponseUtil.writeOk(response, body.build());
            }
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_crear_elemento");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id_usuario_sesion = getSessionUserId(request);
        Integer id_rol_sesion = getSessionRoleId(request);
        boolean es_admin = isAdmin(id_rol_sesion);

        JsonObject payload = JsonUtil.readJsonObject(request);
        Integer id_elemento = JsonUtil.getInt(payload, "id_elemento");
        Integer id_diagrama = JsonUtil.getInt(payload, "id_diagrama");
        Integer id_elemento_padre = JsonUtil.getInt(payload, "id_elemento_padre");
        String tipo_elemento = normalizeTipoElemento(JsonUtil.getString(payload, "tipo_elemento"));
        String etiqueta = JsonUtil.getString(payload, "etiqueta");
        Integer pos_x = JsonUtil.getInt(payload, "pos_x");
        Integer pos_y = JsonUtil.getInt(payload, "pos_y");
        Integer ancho = JsonUtil.getInt(payload, "ancho");
        Integer alto = JsonUtil.getInt(payload, "alto");
        BigDecimal rotacion_grados = JsonUtil.getDecimal(payload, "rotacion_grados");
        Integer orden_z = JsonUtil.getInt(payload, "orden_z");
        String estilo_json = JsonUtil.getString(payload, "estilo_json");
        String metadatos_json = JsonUtil.getString(payload, "metadatos_json");

        if (id_elemento == null || id_diagrama == null || tipo_elemento == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "datos_incompletos");
            return;
        }
        if (!es_admin && !isOwnerDiagram(id_diagrama.intValue(), id_usuario_sesion)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        if (pos_x == null) {
            pos_x = 0;
        }
        if (pos_y == null) {
            pos_y = 0;
        }
        if (ancho == null) {
            ancho = 120;
        }
        if (alto == null) {
            alto = 60;
        }
        if (rotacion_grados == null) {
            rotacion_grados = new BigDecimal("0.00");
        }
        if (orden_z == null) {
            orden_z = 0;
        }

        String sql = "UPDATE elementos_diagrama SET id_diagrama = ?, id_elemento_padre = ?, tipo_elemento = ?, etiqueta = ?, "
                + "pos_x = ?, pos_y = ?, ancho = ?, alto = ?, rotacion_grados = ?, orden_z = ?, estilo_json = ?, metadatos_json = ? "
                + "WHERE id_elemento = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id_diagrama.intValue());
            if (id_elemento_padre == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, id_elemento_padre.intValue());
            }
            ps.setString(3, tipo_elemento);
            if (etiqueta == null || etiqueta.trim().isEmpty()) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, etiqueta);
            }
            ps.setInt(5, pos_x.intValue());
            ps.setInt(6, pos_y.intValue());
            ps.setInt(7, ancho.intValue());
            ps.setInt(8, alto.intValue());
            ps.setBigDecimal(9, rotacion_grados);
            ps.setInt(10, orden_z.intValue());
            if (estilo_json == null || estilo_json.trim().isEmpty()) {
                ps.setNull(11, Types.LONGVARCHAR);
            } else {
                ps.setString(11, estilo_json);
            }
            if (metadatos_json == null || metadatos_json.trim().isEmpty()) {
                ps.setNull(12, Types.LONGVARCHAR);
            } else {
                ps.setString(12, metadatos_json);
            }
            ps.setInt(13, id_elemento.intValue());
            int updated = ps.executeUpdate();
            if (updated == 0) {
                ResponseUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "elemento_no_encontrado");
                return;
            }
            JsonObjectBuilder body = Json.createObjectBuilder().add("ok", true);
            ResponseUtil.writeOk(response, body.build());
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_actualizar_elemento");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
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

        String sql = "DELETE FROM elementos_diagrama WHERE id_elemento = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id_elemento.intValue());
            int deleted = ps.executeUpdate();
            if (deleted == 0) {
                ResponseUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "elemento_no_encontrado");
                return;
            }
            JsonObjectBuilder body = Json.createObjectBuilder().add("ok", true);
            ResponseUtil.writeOk(response, body.build());
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_eliminar_elemento");
        }
    }

    private JsonObjectBuilder buildElemento(ResultSet rs) throws Exception {
        JsonObjectBuilder elemento = Json.createObjectBuilder();
        elemento.add("id_elemento", rs.getInt("id_elemento"));
        elemento.add("id_diagrama", rs.getInt("id_diagrama"));
        int padre = rs.getInt("id_elemento_padre");
        if (rs.wasNull()) {
            JsonUtil.add(elemento, "id_elemento_padre", (Integer) null);
        } else {
            elemento.add("id_elemento_padre", padre);
        }
        elemento.add("tipo_elemento", rs.getString("tipo_elemento"));
        JsonUtil.add(elemento, "etiqueta", rs.getString("etiqueta"));
        elemento.add("pos_x", rs.getInt("pos_x"));
        elemento.add("pos_y", rs.getInt("pos_y"));
        elemento.add("ancho", rs.getInt("ancho"));
        elemento.add("alto", rs.getInt("alto"));
        elemento.add("rotacion_grados", rs.getBigDecimal("rotacion_grados"));
        elemento.add("orden_z", rs.getInt("orden_z"));
        JsonUtil.add(elemento, "estilo_json", rs.getString("estilo_json"));
        JsonUtil.add(elemento, "metadatos_json", rs.getString("metadatos_json"));
        Timestamp creado = rs.getTimestamp("fecha_creacion");
        Timestamp actualizado = rs.getTimestamp("fecha_actualizacion");
        JsonUtil.add(elemento, "fecha_creacion", creado == null ? null : creado.toString());
        JsonUtil.add(elemento, "fecha_actualizacion", actualizado == null ? null : actualizado.toString());
        return elemento;
    }

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

    private String normalizeTipoElemento(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            return null;
        }
        String normalized = tipo.trim().toUpperCase();
        if ("ACTOR".equals(normalized)
                || "CASO_DE_USO".equals(normalized)
                || "LIMITE_SISTEMA".equals(normalized)
                || "PAQUETE".equals(normalized)
                || "NOTA".equals(normalized)
                || "TEXTO".equals(normalized)
                || "IMAGEN".equals(normalized)) {
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
