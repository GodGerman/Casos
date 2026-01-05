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

/**
 * Servlet CRUD de elementos UML (actores, casos, notas, etc.) por diagrama.
 *
 * Controla acceso por sesion y valida propiedad del diagrama al que pertenece
 * el elemento. Expone operaciones de lectura puntual/listado y alta/baja/cambio.
 *
 */
@WebServlet(name = "ElementosServlet", urlPatterns = {"/api/elementos"})
public class ElementosServlet extends HttpServlet {

    /**
     * Obtiene un elemento por id o lista los elementos de un diagrama.
     * No retorna valor; responde 400/403/404/500 segun validaciones.
     *
     * Flujo:
     *
     * - Si viene id_elemento, consulta el registro y valida acceso.
     * - Si no viene, requiere id_diagrama y lista todos sus elementos.
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

        // Rama 1: lectura puntual por id_elemento.
        Integer id_elemento = parseInt(request.getParameter("id_elemento"));
        if (id_elemento != null) {
            // Lectura puntual por id_elemento.
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

    /**
     * Crea un nuevo elemento en un diagrama existente.
     * No retorna valor; responde 400/403/500 segun validaciones.
     *
     * Se validan campos, se fuerza propietario si no es admin,
     * aplica valores por defecto (posicion, tamanio) e inserta en BD.
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

        // Valores por defecto para renderizado inicial.
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

        // Inserta elemento y devuelve id generado.
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

    /**
     * Actualiza un elemento existente del diagrama.
     * No retorna valor; responde 400/403/404/500 segun validaciones.
     *
     * Se validan campos obligatorios, se verifica la propiedad del
     * diagrama y ejecuta UPDATE sobre los campos editables.
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

        // Asegura valores numericos validos para mantener consistencia en UI.
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

        // Actualiza campos editables del elemento.
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

    /**
     * Elimina un elemento si el usuario es propietario del diagrama.
     * No retorna valor; responde 400/403/404/500 segun validaciones.
     *
     * Se valida el id_elemento, se verifica la propiedad via join y
     * ejecuta DELETE.
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

    /**
     * Construye el JSON de respuesta para un elemento.
     *
     * Se extrae columnas, maneja nulls con JsonUtil y formatea
     * timestamps como string ISO.
     *
     *
     * @param rs ResultSet posicionado en un registro valido.
     * @return builder con campos del elemento.
     * @throws Exception si falla la lectura desde el ResultSet.
     */
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
     * Verifica si un elemento pertenece a un diagrama del usuario autenticado.
     *
     * Se hace join elemento->diagrama y compara propietario.
     *
     *
     * @param id_elemento id del elemento.
     * @param id_usuario_sesion id del usuario autenticado.
     * @return true si es propietario; false en caso contrario.
     */
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

    /**
     * Normaliza el tipo de elemento a los valores soportados.
     *
     * Se recorta el texto, se convierte a mayusculas y se compara contra el catalogo permitido.
     *
     *
     * @param tipo texto de entrada.
     * @return tipo en mayusculas o null si no es valido.
     */
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
