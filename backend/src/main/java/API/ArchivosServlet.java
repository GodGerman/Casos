package API;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.UUID;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

/**
 * Servlet para administrar archivos multimedia: listado, carga y borrado.
 *
 * Gestiona tanto el almacenamiento fisico en /uploads como los metadatos
 * en la tabla archivos_multimedia. Controla acceso por sesion/rol.
 *
 */
@WebServlet(name = "ArchivosServlet", urlPatterns = {"/api/archivos"})
@MultipartConfig
public class ArchivosServlet extends HttpServlet {

    /**
     * Obtiene un archivo por id o lista archivos del usuario.
     * No retorna valor; responde 400/403/404/500 segun validaciones.
     *
     * Flujo:
     *
     * - Si viene id_archivo, valida propiedad y devuelve metadatos.
     * - Si no viene, lista archivos del usuario (o del indicado si admin).
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

        // Rama 1: lectura puntual por id_archivo.
        Integer id_archivo = parseInt(request.getParameter("id_archivo"));
        if (id_archivo != null) {
            // Lectura puntual: valida que el usuario sea propietario o admin.
            String sql = "SELECT id_archivo, id_usuario, tipo_media, titulo, descripcion, tamano_bytes, duracion_segundos, "
                    + "ancho, alto, ruta_archivo, fecha_creacion, fecha_actualizacion "
                    + "FROM archivos_multimedia WHERE id_archivo = ?";
            try (Connection con = DB.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id_archivo.intValue());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int id_usuario_propietario = rs.getInt("id_usuario");
                        if (!es_admin && (id_usuario_sesion == null || id_usuario_propietario != id_usuario_sesion.intValue())) {
                            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
                            return;
                        }
                        JsonObjectBuilder body = Json.createObjectBuilder()
                                .add("ok", true)
                                .add("archivo", buildArchivo(rs, request));
                        ResponseUtil.writeOk(response, body.build());
                        return;
                    }
                }
                ResponseUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "archivo_no_encontrado");
            } catch (Exception ex) {
                ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_archivos");
            }
            return;
        }

        // Rama 2: listado por usuario; admin puede filtrar por id_usuario.
        Integer id_usuario = parseInt(request.getParameter("id_usuario"));
        if (!es_admin) {
            id_usuario = id_usuario_sesion;
        } else if (id_usuario == null) {
            id_usuario = id_usuario_sesion;
        }
        if (id_usuario == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        // Lista archivos del usuario solicitado.
        String sql = "SELECT id_archivo, id_usuario, tipo_media, titulo, descripcion, tamano_bytes, duracion_segundos, "
                + "ancho, alto, ruta_archivo, fecha_creacion, fecha_actualizacion "
                + "FROM archivos_multimedia WHERE id_usuario = ? ORDER BY id_archivo";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id_usuario.intValue());
            try (ResultSet rs = ps.executeQuery()) {
                JsonArrayBuilder archivos = Json.createArrayBuilder();
                while (rs.next()) {
                    archivos.add(buildArchivo(rs, request));
                }
                JsonObjectBuilder body = Json.createObjectBuilder()
                        .add("ok", true)
                        .add("archivos", archivos);
                ResponseUtil.writeOk(response, body.build());
            }
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_archivos");
        }
    }

    /**
     * Sube un archivo multipart y registra metadatos en BD.
     * No retorna valor; responde 400/403/500 segun validaciones.
     *
     * Flujo:
     *
     * - Lee parametros y archivo multipart.
     * - Valida extension por tipo de media.
     * - Guarda el archivo en /uploads con nombre unico.
     * - Inserta metadatos y ruta relativa en BD.
     *
     *
     * @param request request HTTP actual (multipart/form-data).
     * @param response response HTTP actual.
     * @throws ServletException si el contenedor falla.
     * @throws IOException si falla la lectura o escritura del archivo.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id_usuario_sesion = getSessionUserId(request);
        Integer id_rol_sesion = getSessionRoleId(request);
        boolean es_admin = isAdmin(id_rol_sesion);

        Integer id_usuario = parseInt(request.getParameter("id_usuario"));
        if (!es_admin) {
            id_usuario = id_usuario_sesion;
        } else if (id_usuario == null) {
            id_usuario = id_usuario_sesion;
        }
        String tipo_media = normalizeTipoMedia(request.getParameter("tipo_media"));
        String titulo = request.getParameter("titulo");
        String descripcion = request.getParameter("descripcion");
        Integer ancho = parseInt(request.getParameter("ancho"));
        Integer alto = parseInt(request.getParameter("alto"));
        String duracion = request.getParameter("duracion_segundos");
        Double duracion_segundos = parseDouble(duracion);

        Part archivoPart = request.getPart("archivo");
        if (id_usuario == null || tipo_media == null || archivoPart == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "datos_incompletos");
            return;
        }

        // Valida extension segun tipo multimedia.
        String originalName = getFileName(archivoPart);
        String extension = getExtension(originalName);
        if (!extensionValida(tipo_media, extension)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "extension_invalida");
            return;
        }

        // Prepara carpeta de uploads dentro del contexto del servlet.
        String uploadsPath = getServletContext().getRealPath("/uploads");
        File uploadsDir = new File(uploadsPath);
        if (!uploadsDir.exists() && !uploadsDir.mkdirs()) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_directorio");
            return;
        }

        // Genera nombre unico para evitar colisiones.
        String nombreArchivo = UUID.randomUUID().toString() + "." + extension;
        File destino = new File(uploadsDir, nombreArchivo);
        try (InputStream in = archivoPart.getInputStream()) {
            Files.copy(in, destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        String ruta_archivo = "uploads/" + nombreArchivo;
        long tamano_bytes = archivoPart.getSize();

        // Inserta metadatos y ruta relativa en BD.
        String sql = "INSERT INTO archivos_multimedia (id_usuario, tipo_media, titulo, descripcion, tamano_bytes, "
                + "duracion_segundos, ancho, alto, ruta_archivo) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, id_usuario.intValue());
            ps.setString(2, tipo_media);
            if (titulo == null || titulo.trim().isEmpty()) {
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setString(3, titulo);
            }
            if (descripcion == null || descripcion.trim().isEmpty()) {
                ps.setNull(4, Types.LONGVARCHAR);
            } else {
                ps.setString(4, descripcion);
            }
            ps.setLong(5, tamano_bytes);
            if (duracion_segundos == null) {
                ps.setNull(6, Types.DECIMAL);
            } else {
                ps.setDouble(6, duracion_segundos.doubleValue());
            }
            if (ancho == null) {
                ps.setNull(7, Types.INTEGER);
            } else {
                ps.setInt(7, ancho.intValue());
            }
            if (alto == null) {
                ps.setNull(8, Types.INTEGER);
            } else {
                ps.setInt(8, alto.intValue());
            }
            ps.setString(9, ruta_archivo);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                JsonObjectBuilder body = Json.createObjectBuilder()
                        .add("ok", true)
                        .add("ruta_archivo", ruta_archivo);
                if (keys.next()) {
                    body.add("id_archivo", keys.getInt(1));
                }
                ResponseUtil.writeOk(response, body.build());
            }
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_subir_archivo");
        }
    }

    /**
     * Elimina un archivo multimedia (registro y archivo fisico).
     * No retorna valor; responde 400/403/404/500 segun validaciones.
     *
     * Se valida la propiedad, se elimina el registro de BD y luego
     * borra el archivo fisico si existe en el filesystem.
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

        Integer id_archivo = parseInt(request.getParameter("id_archivo"));
        if (id_archivo == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "id_archivo_requerido");
            return;
        }

        // Obtiene propietario y ruta antes de borrar.
        String sqlSelect = "SELECT id_usuario, ruta_archivo FROM archivos_multimedia WHERE id_archivo = ?";
        String sqlDelete = "DELETE FROM archivos_multimedia WHERE id_archivo = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement psSelect = con.prepareStatement(sqlSelect)) {
            psSelect.setInt(1, id_archivo.intValue());
            try (ResultSet rs = psSelect.executeQuery()) {
                if (!rs.next()) {
                    ResponseUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "archivo_no_encontrado");
                    return;
                }
                int id_usuario_propietario = rs.getInt("id_usuario");
                String ruta = rs.getString("ruta_archivo");
                if (!es_admin && (id_usuario_sesion == null || id_usuario_propietario != id_usuario_sesion.intValue())) {
                    ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
                    return;
                }

                // Borra registro primero para mantener consistencia con BD.
                try (PreparedStatement psDelete = con.prepareStatement(sqlDelete)) {
                    psDelete.setInt(1, id_archivo.intValue());
                    psDelete.executeUpdate();
                }

                // Borra archivo fisico si existe.
                if (ruta != null) {
                    File file = new File(getServletContext().getRealPath("/"), ruta);
                    if (file.exists()) {
                        file.delete();
                    }
                }
                JsonObjectBuilder body = Json.createObjectBuilder().add("ok", true);
                ResponseUtil.writeOk(response, body.build());
            }
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_eliminar_archivo");
        }
    }

    /**
     * Convierte una fila de archivo en JSON agregando URL publica.
     *
     * Se construye metadatos y deriva url_publica desde contextPath.
     *
     *
     * @param rs ResultSet posicionado.
     * @param request request actual para construir URL.
     * @return builder con metadatos del archivo.
     * @throws Exception si falla lectura de datos.
     */
    private JsonObjectBuilder buildArchivo(ResultSet rs, HttpServletRequest request) throws Exception {
        JsonObjectBuilder archivo = Json.createObjectBuilder();
        archivo.add("id_archivo", rs.getInt("id_archivo"));
        archivo.add("id_usuario", rs.getInt("id_usuario"));
        archivo.add("tipo_media", rs.getString("tipo_media"));
        JsonUtil.add(archivo, "titulo", rs.getString("titulo"));
        JsonUtil.add(archivo, "descripcion", rs.getString("descripcion"));
        archivo.add("tamano_bytes", rs.getLong("tamano_bytes"));
        if (rs.getObject("duracion_segundos") == null) {
            JsonUtil.add(archivo, "duracion_segundos", (String) null);
        } else {
            archivo.add("duracion_segundos", rs.getBigDecimal("duracion_segundos"));
        }
        int ancho = rs.getInt("ancho");
        if (rs.wasNull()) {
            JsonUtil.add(archivo, "ancho", (Integer) null);
        } else {
            archivo.add("ancho", ancho);
        }
        int alto = rs.getInt("alto");
        if (rs.wasNull()) {
            JsonUtil.add(archivo, "alto", (Integer) null);
        } else {
            archivo.add("alto", alto);
        }
        String ruta = rs.getString("ruta_archivo");
        JsonUtil.add(archivo, "ruta_archivo", ruta);
        if (ruta != null) {
            String urlPublica = request.getContextPath() + "/" + ruta;
            archivo.add("url_publica", urlPublica);
        } else {
            JsonUtil.add(archivo, "url_publica", (String) null);
        }
        Timestamp creado = rs.getTimestamp("fecha_creacion");
        Timestamp actualizado = rs.getTimestamp("fecha_actualizacion");
        JsonUtil.add(archivo, "fecha_creacion", creado == null ? null : creado.toString());
        JsonUtil.add(archivo, "fecha_actualizacion", actualizado == null ? null : actualizado.toString());
        return archivo;
    }

    /**
     * Extrae el nombre del archivo desde el header Content-Disposition.
     *
     * Se parsea el header multipart y extrae el valor de filename=.
     *
     *
     * @param part Part del multipart.
     * @return nombre de archivo o null si no esta presente.
     */
    private String getFileName(Part part) {
        String header = part.getHeader("content-disposition");
        if (header == null) {
            return null;
        }
        for (String item : header.split(";")) {
            String trimmed = item.trim();
            if (trimmed.startsWith("filename")) {
                String name = trimmed.substring(trimmed.indexOf('=') + 1).trim().replace("\"", "");
                return name;
            }
        }
        return null;
    }

    /**
     * Obtiene la extension de un filename.
     *
     * Se busca el ultimo punto y devuelve el sufijo en minusculas.
     *
     *
     * @param filename nombre del archivo.
     * @return extension en minusculas o cadena vacia si no hay.
     */
    private String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int idx = filename.lastIndexOf('.');
        if (idx == -1 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1).toLowerCase();
    }

    /**
     * Valida la extension en funcion del tipo de media.
     *
     * Se compara extension contra un whitelist segun tipo_media.
     *
     *
     * @param tipo_media tipo normalizado (AUDIO, VIDEO, IMAGEN).
     * @param extension extension en minusculas.
     * @return true si la extension es valida.
     */
    private boolean extensionValida(String tipo_media, String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        if ("AUDIO".equals(tipo_media)) {
            return "mp3".equals(extension);
        }
        if ("VIDEO".equals(tipo_media)) {
            return "mp4".equals(extension);
        }
        if ("IMAGEN".equals(tipo_media)) {
            return "jpg".equals(extension) || "jpeg".equals(extension);
        }
        return false;
    }

    /**
     * Normaliza el tipo de media a valores soportados.
     *
     * Se recorta el texto, se convierte a mayusculas y se valida contra el catalogo permitido.
     *
     *
     * @param tipo texto recibido.
     * @return tipo en mayusculas o null si no es valido.
     */
    private String normalizeTipoMedia(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            return null;
        }
        String normalized = tipo.trim().toUpperCase();
        if ("IMAGEN".equals(normalized) || "AUDIO".equals(normalized) || "VIDEO".equals(normalized)) {
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
     * Parsea un decimal desde query string.
     *
     * Se recorta el texto y se parsea con manejo de NumberFormatException.
     *
     *
     * @param value texto recibido.
     * @return Double o null si no es valido.
     */
    private Double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.valueOf(value.trim());
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
