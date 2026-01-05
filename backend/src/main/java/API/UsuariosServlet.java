package API;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
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

@WebServlet(name = "UsuariosServlet", urlPatterns = {"/api/usuarios"})
public class UsuariosServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id_usuario_sesion = getSessionUserId(request);
        Integer id_rol_sesion = getSessionRoleId(request);
        Integer id_usuario = parseInt(request.getParameter("id_usuario"));

        boolean es_admin = isAdmin(id_rol_sesion);
        if (id_usuario != null) {
            if (!es_admin && !id_usuario.equals(id_usuario_sesion)) {
                ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
                return;
            }
            String sql = "SELECT u.id_usuario, u.nombre_usuario, u.correo, u.id_rol, r.nombre_rol, "
                    + "u.fecha_creacion, u.fecha_actualizacion "
                    + "FROM usuarios u INNER JOIN roles r ON r.id_rol = u.id_rol "
                    + "WHERE u.id_usuario = ?";
            try (Connection con = DB.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id_usuario.intValue());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        JsonObjectBuilder usuario = buildUsuario(rs);
                        JsonObjectBuilder body = Json.createObjectBuilder()
                                .add("ok", true)
                                .add("usuario", usuario);
                        ResponseUtil.writeOk(response, body.build());
                        return;
                    }
                }
                ResponseUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "usuario_no_encontrado");
            } catch (Exception ex) {
                ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_usuarios");
            }
            return;
        }

        if (!es_admin) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        String sql = "SELECT u.id_usuario, u.nombre_usuario, u.correo, u.id_rol, r.nombre_rol, "
                + "u.fecha_creacion, u.fecha_actualizacion "
                + "FROM usuarios u INNER JOIN roles r ON r.id_rol = u.id_rol "
                + "ORDER BY u.id_usuario";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            JsonArrayBuilder usuarios = Json.createArrayBuilder();
            while (rs.next()) {
                usuarios.add(buildUsuario(rs));
            }
            JsonObjectBuilder body = Json.createObjectBuilder()
                    .add("ok", true)
                    .add("usuarios", usuarios);
            ResponseUtil.writeOk(response, body.build());
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_usuarios");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id_rol_sesion = getSessionRoleId(request);
        if (!isAdmin(id_rol_sesion)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        JsonObject payload = JsonUtil.readJsonObject(request);
        String nombre_usuario = JsonUtil.getString(payload, "nombre_usuario");
        String correo = JsonUtil.getString(payload, "correo");
        String contrasena = JsonUtil.getString(payload, "contrasena");
        Integer id_rol = JsonUtil.getInt(payload, "id_rol");

        if (nombre_usuario == null || contrasena == null || id_rol == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "datos_incompletos");
            return;
        }

        String sql = "INSERT INTO usuarios (nombre_usuario, correo, contrasena, id_rol) VALUES (?,?,?,?)";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre_usuario);
            if (correo == null || correo.trim().isEmpty()) {
                ps.setNull(2, java.sql.Types.VARCHAR);
            } else {
                ps.setString(2, correo);
            }
            ps.setString(3, contrasena);
            ps.setInt(4, id_rol.intValue());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                JsonObjectBuilder body = Json.createObjectBuilder().add("ok", true);
                if (keys.next()) {
                    body.add("id_usuario", keys.getInt(1));
                }
                ResponseUtil.writeOk(response, body.build());
            }
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_crear_usuario");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        JsonObject payload = JsonUtil.readJsonObject(request);
        Integer id_usuario = JsonUtil.getInt(payload, "id_usuario");
        if (id_usuario == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "id_usuario_requerido");
            return;
        }

        Integer id_usuario_sesion = getSessionUserId(request);
        Integer id_rol_sesion = getSessionRoleId(request);
        boolean es_admin = isAdmin(id_rol_sesion);
        if (!es_admin && !id_usuario.equals(id_usuario_sesion)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        String nombre_usuario = JsonUtil.getString(payload, "nombre_usuario");
        String correo = JsonUtil.getString(payload, "correo");
        String contrasena = JsonUtil.getString(payload, "contrasena");
        Integer id_rol = JsonUtil.getInt(payload, "id_rol");

        if (nombre_usuario == null || contrasena == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "datos_incompletos");
            return;
        }
        if (!es_admin) {
            id_rol = id_rol_sesion;
        }
        if (id_rol == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "id_rol_requerido");
            return;
        }

        String sql = "UPDATE usuarios SET nombre_usuario = ?, correo = ?, contrasena = ?, id_rol = ? "
                + "WHERE id_usuario = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre_usuario);
            if (correo == null || correo.trim().isEmpty()) {
                ps.setNull(2, java.sql.Types.VARCHAR);
            } else {
                ps.setString(2, correo);
            }
            ps.setString(3, contrasena);
            ps.setInt(4, id_rol.intValue());
            ps.setInt(5, id_usuario.intValue());
            int updated = ps.executeUpdate();
            if (updated == 0) {
                ResponseUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "usuario_no_encontrado");
                return;
            }
            JsonObjectBuilder body = Json.createObjectBuilder().add("ok", true);
            ResponseUtil.writeOk(response, body.build());
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_actualizar_usuario");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id_rol_sesion = getSessionRoleId(request);
        if (!isAdmin(id_rol_sesion)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        Integer id_usuario = parseInt(request.getParameter("id_usuario"));
        if (id_usuario == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "id_usuario_requerido");
            return;
        }

        String sql = "DELETE FROM usuarios WHERE id_usuario = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id_usuario.intValue());
            int deleted = ps.executeUpdate();
            if (deleted == 0) {
                ResponseUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "usuario_no_encontrado");
                return;
            }
            JsonObjectBuilder body = Json.createObjectBuilder().add("ok", true);
            ResponseUtil.writeOk(response, body.build());
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_eliminar_usuario");
        }
    }

    private JsonObjectBuilder buildUsuario(ResultSet rs) throws Exception {
        JsonObjectBuilder usuario = Json.createObjectBuilder();
        usuario.add("id_usuario", rs.getInt("id_usuario"));
        usuario.add("nombre_usuario", rs.getString("nombre_usuario"));
        JsonUtil.add(usuario, "correo", rs.getString("correo"));
        usuario.add("id_rol", rs.getInt("id_rol"));
        usuario.add("nombre_rol", rs.getString("nombre_rol"));
        Timestamp creado = rs.getTimestamp("fecha_creacion");
        Timestamp actualizado = rs.getTimestamp("fecha_actualizacion");
        JsonUtil.add(usuario, "fecha_creacion", creado == null ? null : creado.toString());
        JsonUtil.add(usuario, "fecha_actualizacion", actualizado == null ? null : actualizado.toString());
        return usuario;
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
