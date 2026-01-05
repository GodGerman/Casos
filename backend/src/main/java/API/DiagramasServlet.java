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
 * Servlet CRUD de diagramas UML con control de acceso por usuario.
 *
 * Permite listar, crear, actualizar y eliminar diagramas. Las operaciones se
 * restringen por sesion: el usuario solo ve/modifica sus recursos, salvo
 * administradores (id_rol = 1).
 *
 */
@WebServlet(name = "DiagramasServlet", urlPatterns = {"/api/diagramas"})
public class DiagramasServlet extends HttpServlet {

    /**
     * Obtiene un diagrama por id o lista diagramas del usuario.
     * No retorna valor; responde 400/403/404/500 segun validaciones.
     *
     * Flujo:
     *
     * - Si viene id_diagrama, lee un registro y valida propiedad/rol.
     * - Si no viene, lista diagramas del usuario o de todos si es admin.
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

        // Rama 1: lectura puntual cuando se recibe id_diagrama.
        Integer id_diagrama = parseInt(request.getParameter("id_diagrama"));
        if (id_diagrama != null) {
            // Caso lectura puntual por id.
            String sql = "SELECT id_diagrama, id_usuario, nombre, descripcion, estado, ancho_lienzo, alto_lienzo, "
                    + "configuracion_json, fecha_creacion, fecha_actualizacion "
                    + "FROM diagramas_uml WHERE id_diagrama = ?";
            try (Connection con = DB.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id_diagrama.intValue());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int id_usuario_propietario = rs.getInt("id_usuario");
                        if (!es_admin && (id_usuario_sesion == null || id_usuario_propietario != id_usuario_sesion.intValue())) {
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

        // Rama 2: listado; por defecto se limita al usuario de sesion si no es admin.
        Integer id_usuario = parseInt(request.getParameter("id_usuario"));
        if (!es_admin) {
            id_usuario = id_usuario_sesion;
        }
        if (id_usuario == null && !es_admin) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        // Construccion dinamica del SQL para filtrar por usuario si aplica.
        String sql = "SELECT id_diagrama, id_usuario, nombre, descripcion, estado, ancho_lienzo, alto_lienzo, "
                + "configuracion_json, fecha_creacion, fecha_actualizacion "
                + "FROM diagramas_uml ";
        if (id_usuario != null) {
            sql += "WHERE id_usuario = ? ";
        }
        sql += "ORDER BY id_diagrama";

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (id_usuario != null) {
                ps.setInt(1, id_usuario.intValue());
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

    /**
     * Crea un nuevo diagrama asociado al usuario en sesion (o al indicado si admin).
     * No retorna valor; responde 400/403/500 segun validaciones.
     *
     * Se lee payload JSON, valida campos obligatorios, asigna
     * valores por defecto (estado, dimensiones), inserta en BD y retorna el
     * id generado.
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
        Integer id_usuario = JsonUtil.getInt(payload, "id_usuario");
        String nombre = JsonUtil.getString(payload, "nombre");
        String descripcion = JsonUtil.getString(payload, "descripcion");
        String estado = normalizeEstado(JsonUtil.getString(payload, "estado"));
        Integer ancho_lienzo = JsonUtil.getInt(payload, "ancho_lienzo");
        Integer alto_lienzo = JsonUtil.getInt(payload, "alto_lienzo");
        String configuracion_json = JsonUtil.getString(payload, "configuracion_json");

        // Si no es admin, fuerza el id_usuario al de la sesion.
        if (!es_admin) {
            id_usuario = id_usuario_sesion;
        } else if (id_usuario == null) {
            id_usuario = id_usuario_sesion;
        }
        if (id_usuario == null || nombre == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "datos_incompletos");
            return;
        }
        if (estado == null) {
            // Estado por defecto si no viene en payload.
            estado = "ACTIVO";
        }
        if (ancho_lienzo == null) {
            ancho_lienzo = 1280;
        }
        if (alto_lienzo == null) {
            alto_lienzo = 720;
        }

        // Inserta registro y retorna la clave generada.
        String sql = "INSERT INTO diagramas_uml (id_usuario, nombre, descripcion, estado, ancho_lienzo, alto_lienzo, "
                + "configuracion_json) VALUES (?,?,?,?,?,?,?)";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, id_usuario.intValue());
            ps.setString(2, nombre);
            if (descripcion == null || descripcion.trim().isEmpty()) {
                ps.setNull(3, Types.LONGVARCHAR);
            } else {
                ps.setString(3, descripcion);
            }
            ps.setString(4, estado);
            ps.setInt(5, ancho_lienzo.intValue());
            ps.setInt(6, alto_lienzo.intValue());
            if (configuracion_json == null || configuracion_json.trim().isEmpty()) {
                ps.setNull(7, Types.LONGVARCHAR);
            } else {
                ps.setString(7, configuracion_json);
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

    /**
     * Actualiza un diagrama existente si el usuario es propietario o admin.
     * No retorna valor; responde 400/403/404/500 segun validaciones.
     *
     * Se validan campos, se verifica la propiedad, se ejecuta UPDATE y
     * reporta si no se encontro el registro.
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
        Integer id_diagrama = JsonUtil.getInt(payload, "id_diagrama");
        String nombre = JsonUtil.getString(payload, "nombre");
        String descripcion = JsonUtil.getString(payload, "descripcion");
        String estado = normalizeEstado(JsonUtil.getString(payload, "estado"));
        Integer ancho_lienzo = JsonUtil.getInt(payload, "ancho_lienzo");
        Integer alto_lienzo = JsonUtil.getInt(payload, "alto_lienzo");
        String configuracion_json = JsonUtil.getString(payload, "configuracion_json");

        if (id_diagrama == null || nombre == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "datos_incompletos");
            return;
        }
        if (estado == null) {
            estado = "ACTIVO";
        }
        if (ancho_lienzo == null || alto_lienzo == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "dimensiones_requeridas");
            return;
        }

        // Control de acceso: solo propietario o admin.
        if (!es_admin && !isOwnerDiagram(id_diagrama.intValue(), id_usuario_sesion)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        // Actualiza campos editables de diagrama.
        String sql = "UPDATE diagramas_uml SET nombre = ?, descripcion = ?, estado = ?, ancho_lienzo = ?, alto_lienzo = ?, "
                + "configuracion_json = ? WHERE id_diagrama = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            if (descripcion == null || descripcion.trim().isEmpty()) {
                ps.setNull(2, Types.LONGVARCHAR);
            } else {
                ps.setString(2, descripcion);
            }
            ps.setString(3, estado);
            ps.setInt(4, ancho_lienzo.intValue());
            ps.setInt(5, alto_lienzo.intValue());
            if (configuracion_json == null || configuracion_json.trim().isEmpty()) {
                ps.setNull(6, Types.LONGVARCHAR);
            } else {
                ps.setString(6, configuracion_json);
            }
            ps.setInt(7, id_diagrama.intValue());
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

    /**
     * Elimina un diagrama si el usuario es propietario o admin.
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

        Integer id_diagrama = parseInt(request.getParameter("id_diagrama"));
        if (id_diagrama == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "id_diagrama_requerido");
            return;
        }
        // Control de acceso: solo propietario o admin.
        if (!es_admin && !isOwnerDiagram(id_diagrama.intValue(), id_usuario_sesion)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        String sql = "DELETE FROM diagramas_uml WHERE id_diagrama = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id_diagrama.intValue());
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

    /**
     * Construye el JSON de un diagrama desde el ResultSet.
     *
     * Se extrae columnas y normaliza nulls/fechas a string.
     *
     *
     * @param rs ResultSet posicionado en el registro.
     * @return builder con campos del diagrama.
     * @throws Exception si ocurre error al leer columnas.
     */
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

    /**
     * Verifica si el diagrama pertenece al usuario de la sesion.
     *
     * Se consulta el id_usuario propietario y compara con sesion.
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
     * Normaliza el estado recibido a los valores soportados.
     *
     * Se recorta el texto, se convierte a mayusculas y se valida contra el catalogo permitido.
     *
     *
     * @param estado texto de entrada.
     * @return estado en mayusculas o null si no es valido.
     */
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
