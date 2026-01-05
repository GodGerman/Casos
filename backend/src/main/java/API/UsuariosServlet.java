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

/**
 * Servlet CRUD de usuarios con validacion de permisos (admin vs usuario propio).
 *
 * Un admin puede listar/crear/editar/eliminar usuarios. Un usuario normal
 * solo puede consultar y actualizar su propio registro.
 *
 */
@WebServlet(name = "UsuariosServlet", urlPatterns = {"/api/usuarios"})
public class UsuariosServlet extends HttpServlet {

    /**
     * Obtiene un usuario por id o lista todos (solo admin).
     * No retorna valor; responde 403/404/500 segun permisos o datos.
     *
     * Flujo:
     *
     * - Si viene id_usuario, valida que sea admin o el propio usuario.
     * - Si no viene, solo admin puede listar todos.
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
        Integer id_usuario = parseInt(request.getParameter("id_usuario"));

        boolean es_admin = isAdmin(id_rol_sesion);
        if (id_usuario != null) {
            // Un usuario solo puede ver su propio perfil si no es admin.
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

        // Listado completo de usuarios (solo admin).
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

    /**
     * Crea un usuario nuevo (solo admin).
     * No retorna valor; responde 400/403/500 segun validaciones.
     *
     * Se validan campos obligatorios, se inserta el usuario y
     * devuelve el id generado.
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

        // Inserta usuario y retorna id generado.
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

    /**
     * Actualiza datos de un usuario. Admin puede modificar rol;
     * usuario normal solo actualiza su propio registro.
     * No retorna valor; responde 400/403/404/500 segun validaciones.
     *
     * Se valida el id y se aplica la regla de permisos (admin vs propio)
     * y ejecuta UPDATE. Si no hay filas afectadas, reporta 404.
     *
     *
     * @param request request HTTP actual.
     * @param response response HTTP actual.
     * @throws ServletException si el contenedor falla.
     * @throws IOException si falla la escritura de respuesta.
     */
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
            // Usuario no admin conserva su rol actual.
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

    /**
     * Elimina un usuario por id (solo admin).
     * No retorna valor; responde 400/403/404/500 segun validaciones.
     *
     * Se valida el rol admin, se obtiene el id desde query y se ejecuta DELETE.
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

    /**
     * Construye el JSON de respuesta para un usuario.
     *
     * Se extrae columnas, maneja nulls con JsonUtil y formatea fechas.
     *
     *
     * @param rs ResultSet ya posicionado en un registro valido.
     * @return builder con los campos del usuario.
     * @throws Exception si falla la lectura desde el ResultSet.
     */
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
