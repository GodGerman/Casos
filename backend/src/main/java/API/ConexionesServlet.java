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

/**
 * Servlet CRUD de conexiones entre elementos de un diagrama UML.
 *
 * Controla acceso por sesion y propiedad del diagrama para proteger
 * relaciones entre elementos. Expone listado, alta, cambio y baja.
 *
 */
@WebServlet(name = "ConexionesServlet", urlPatterns = {"/api/conexiones"})
public class ConexionesServlet extends HttpServlet {

    /**
     * Obtiene una conexion por id o lista conexiones de un diagrama.
     * No retorna valor; responde 400/403/404/500 segun validaciones.
     *
     * Flujo:
     *
     * - Si viene id_conexion, consulta un registro y valida acceso.
     * - Si no viene, requiere id_diagrama y lista todas las conexiones.
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

        // Rama 1: lectura puntual por id_conexion.
        Integer id_conexion = parseInt(request.getParameter("id_conexion"));
        if (id_conexion != null) {
            // Lectura puntual por id_conexion.
            String sql = "SELECT id_conexion, id_diagrama, id_elemento_origen, id_elemento_destino, tipo_conexion, "
                    + "etiqueta, puntos_json, estilo_json, fecha_creacion, fecha_actualizacion "
                    + "FROM conexiones_diagrama WHERE id_conexion = ?";
            try (Connection con = DB.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id_conexion.intValue());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int id_diagrama = rs.getInt("id_diagrama");
                        if (!es_admin && !isOwnerDiagram(id_diagrama, id_usuario_sesion)) {
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

        // Rama 2: listado por diagrama.
        Integer id_diagrama = parseInt(request.getParameter("id_diagrama"));
        if (id_diagrama == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "id_diagrama_requerido");
            return;
        }
        if (!es_admin && !isOwnerDiagram(id_diagrama.intValue(), id_usuario_sesion)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        String sql = "SELECT id_conexion, id_diagrama, id_elemento_origen, id_elemento_destino, tipo_conexion, "
                + "etiqueta, puntos_json, estilo_json, fecha_creacion, fecha_actualizacion "
                + "FROM conexiones_diagrama WHERE id_diagrama = ? ORDER BY id_conexion";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id_diagrama.intValue());
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

    /**
     * Crea una nueva conexion entre dos elementos.
     * No retorna valor; responde 400/403/500 segun validaciones.
     *
     * Se validan ids y el tipo, se verifica la propiedad del diagrama,
     * inserta en BD y devuelve la clave generada.
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
        Integer id_elemento_origen = JsonUtil.getInt(payload, "id_elemento_origen");
        Integer id_elemento_destino = JsonUtil.getInt(payload, "id_elemento_destino");
        String tipo_conexion = normalizeTipoConexion(JsonUtil.getString(payload, "tipo_conexion"));
        String etiqueta = JsonUtil.getString(payload, "etiqueta");
        String puntos_json = JsonUtil.getString(payload, "puntos_json");
        String estilo_json = JsonUtil.getString(payload, "estilo_json");

        if (id_diagrama == null || id_elemento_origen == null || id_elemento_destino == null || tipo_conexion == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "datos_incompletos");
            return;
        }
        if (!es_admin && !isOwnerDiagram(id_diagrama.intValue(), id_usuario_sesion)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        // Inserta conexion y retorna id generado.
        String sql = "INSERT INTO conexiones_diagrama (id_diagrama, id_elemento_origen, id_elemento_destino, "
                + "tipo_conexion, etiqueta, puntos_json, estilo_json) VALUES (?,?,?,?,?,?,?)";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, id_diagrama.intValue());
            ps.setInt(2, id_elemento_origen.intValue());
            ps.setInt(3, id_elemento_destino.intValue());
            ps.setString(4, tipo_conexion);
            if (etiqueta == null || etiqueta.trim().isEmpty()) {
                ps.setNull(5, Types.VARCHAR);
            } else {
                ps.setString(5, etiqueta);
            }
            if (puntos_json == null || puntos_json.trim().isEmpty()) {
                ps.setNull(6, Types.LONGVARCHAR);
            } else {
                ps.setString(6, puntos_json);
            }
            if (estilo_json == null || estilo_json.trim().isEmpty()) {
                ps.setNull(7, Types.LONGVARCHAR);
            } else {
                ps.setString(7, estilo_json);
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

    /**
     * Actualiza una conexion existente si el usuario es propietario o admin.
     * No retorna valor; responde 400/403/404/500 segun validaciones.
     *
     * Se validan campos, se verifica la propiedad y se ejecuta UPDATE.
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
        Integer id_usuario_sesion = getSessionUserId(request);
        Integer id_rol_sesion = getSessionRoleId(request);
        boolean es_admin = isAdmin(id_rol_sesion);

        JsonObject payload = JsonUtil.readJsonObject(request);
        Integer id_conexion = JsonUtil.getInt(payload, "id_conexion");
        Integer id_diagrama = JsonUtil.getInt(payload, "id_diagrama");
        Integer id_elemento_origen = JsonUtil.getInt(payload, "id_elemento_origen");
        Integer id_elemento_destino = JsonUtil.getInt(payload, "id_elemento_destino");
        String tipo_conexion = normalizeTipoConexion(JsonUtil.getString(payload, "tipo_conexion"));
        String etiqueta = JsonUtil.getString(payload, "etiqueta");
        String puntos_json = JsonUtil.getString(payload, "puntos_json");
        String estilo_json = JsonUtil.getString(payload, "estilo_json");

        if (id_conexion == null || id_diagrama == null || id_elemento_origen == null || id_elemento_destino == null || tipo_conexion == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "datos_incompletos");
            return;
        }
        if (!es_admin && !isOwnerDiagram(id_diagrama.intValue(), id_usuario_sesion)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        // Actualiza campos editables de la conexion.
        String sql = "UPDATE conexiones_diagrama SET id_diagrama = ?, id_elemento_origen = ?, id_elemento_destino = ?, "
                + "tipo_conexion = ?, etiqueta = ?, puntos_json = ?, estilo_json = ? WHERE id_conexion = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id_diagrama.intValue());
            ps.setInt(2, id_elemento_origen.intValue());
            ps.setInt(3, id_elemento_destino.intValue());
            ps.setString(4, tipo_conexion);
            if (etiqueta == null || etiqueta.trim().isEmpty()) {
                ps.setNull(5, Types.VARCHAR);
            } else {
                ps.setString(5, etiqueta);
            }
            if (puntos_json == null || puntos_json.trim().isEmpty()) {
                ps.setNull(6, Types.LONGVARCHAR);
            } else {
                ps.setString(6, puntos_json);
            }
            if (estilo_json == null || estilo_json.trim().isEmpty()) {
                ps.setNull(7, Types.LONGVARCHAR);
            } else {
                ps.setString(7, estilo_json);
            }
            ps.setInt(8, id_conexion.intValue());
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

    /**
     * Elimina una conexion si el usuario es propietario o admin.
     * No retorna valor; responde 400/403/404/500 segun validaciones.
     *
     * Se valida el id, se verifica la propiedad y se ejecuta DELETE.
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

        Integer id_conexion = parseInt(request.getParameter("id_conexion"));
        if (id_conexion == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "id_conexion_requerido");
            return;
        }
        if (!es_admin && !isOwnerConexion(id_conexion.intValue(), id_usuario_sesion)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        String sql = "DELETE FROM conexiones_diagrama WHERE id_conexion = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id_conexion.intValue());
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

    /**
     * Construye el JSON de una conexion.
     *
     * Se extrae columnas, maneja nulls y formatea timestamps.
     *
     *
     * @param rs ResultSet posicionado en un registro valido.
     * @return builder con campos de la conexion.
     * @throws Exception si falla la lectura del ResultSet.
     */
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

    /**
     * Verifica si el diagrama pertenece al usuario de la sesion.
     *
     * Se consulta el propietario del diagrama y compara con sesion.
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
     * Verifica si una conexion pertenece a un diagrama del usuario autenticado.
     *
     * Se join conexion->diagrama y compara propietario.
     *
     *
     * @param id_conexion id de la conexion.
     * @param id_usuario_sesion id del usuario autenticado.
     * @return true si es propietario; false en caso contrario.
     */
    private boolean isOwnerConexion(int id_conexion, Integer id_usuario_sesion) {
        if (id_usuario_sesion == null) {
            return false;
        }
        String sql = "SELECT d.id_usuario FROM diagramas_uml d "
                + "INNER JOIN conexiones_diagrama c ON c.id_diagrama = d.id_diagrama "
                + "WHERE c.id_conexion = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id_conexion);
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
     * Normaliza el tipo de conexion a los valores soportados.
     *
     * Se recorta el texto, se convierte a mayusculas y se valida contra el catalogo permitido.
     *
     *
     * @param tipo texto de entrada.
     * @return tipo en mayusculas o null si no es valido.
     */
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
