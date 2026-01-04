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

@WebServlet(name = "ArchivosServlet", urlPatterns = {"/api/archivos"})
@MultipartConfig
public class ArchivosServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer sessionUserId = getSessionUserId(request);
        Integer sessionRoleId = getSessionRoleId(request);
        boolean admin = isAdmin(sessionRoleId);

        Integer idArchivo = parseInt(request.getParameter("id_archivo"));
        if (idArchivo != null) {
            String sql = "SELECT id_archivo, id_usuario, tipo_media, titulo, descripcion, tamano_bytes, duracion_segundos, "
                    + "ancho, alto, ruta_archivo, fecha_creacion, fecha_actualizacion "
                    + "FROM archivos_multimedia WHERE id_archivo = ?";
            try (Connection con = DbUtil.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idArchivo.intValue());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int ownerId = rs.getInt("id_usuario");
                        if (!admin && (sessionUserId == null || ownerId != sessionUserId.intValue())) {
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

        Integer idUsuario = parseInt(request.getParameter("id_usuario"));
        if (!admin) {
            idUsuario = sessionUserId;
        }
        if (idUsuario == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
            return;
        }

        String sql = "SELECT id_archivo, id_usuario, tipo_media, titulo, descripcion, tamano_bytes, duracion_segundos, "
                + "ancho, alto, ruta_archivo, fecha_creacion, fecha_actualizacion "
                + "FROM archivos_multimedia WHERE id_usuario = ? ORDER BY id_archivo";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario.intValue());
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

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer sessionUserId = getSessionUserId(request);
        Integer sessionRoleId = getSessionRoleId(request);
        boolean admin = isAdmin(sessionRoleId);

        Integer idUsuario = parseInt(request.getParameter("id_usuario"));
        if (!admin) {
            idUsuario = sessionUserId;
        }
        String tipoMedia = normalizeTipoMedia(request.getParameter("tipo_media"));
        String titulo = request.getParameter("titulo");
        String descripcion = request.getParameter("descripcion");
        Integer ancho = parseInt(request.getParameter("ancho"));
        Integer alto = parseInt(request.getParameter("alto"));
        String duracion = request.getParameter("duracion_segundos");
        Double duracionSegundos = parseDouble(duracion);

        Part archivoPart = request.getPart("archivo");
        if (idUsuario == null || tipoMedia == null || archivoPart == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "datos_incompletos");
            return;
        }

        String originalName = getFileName(archivoPart);
        String extension = getExtension(originalName);
        if (!extensionValida(tipoMedia, extension)) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "extension_invalida");
            return;
        }

        String uploadsPath = getServletContext().getRealPath("/uploads");
        File uploadsDir = new File(uploadsPath);
        if (!uploadsDir.exists() && !uploadsDir.mkdirs()) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_directorio");
            return;
        }

        String nombreArchivo = UUID.randomUUID().toString() + "." + extension;
        File destino = new File(uploadsDir, nombreArchivo);
        try (InputStream in = archivoPart.getInputStream()) {
            Files.copy(in, destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        String rutaRelativa = "uploads/" + nombreArchivo;
        long tamanoBytes = archivoPart.getSize();

        String sql = "INSERT INTO archivos_multimedia (id_usuario, tipo_media, titulo, descripcion, tamano_bytes, "
                + "duracion_segundos, ancho, alto, ruta_archivo) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idUsuario.intValue());
            ps.setString(2, tipoMedia);
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
            ps.setLong(5, tamanoBytes);
            if (duracionSegundos == null) {
                ps.setNull(6, Types.DECIMAL);
            } else {
                ps.setDouble(6, duracionSegundos.doubleValue());
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
            ps.setString(9, rutaRelativa);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                JsonObjectBuilder body = Json.createObjectBuilder()
                        .add("ok", true)
                        .add("ruta_archivo", rutaRelativa);
                if (keys.next()) {
                    body.add("id_archivo", keys.getInt(1));
                }
                ResponseUtil.writeOk(response, body.build());
            }
        } catch (Exception ex) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error_subir_archivo");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer sessionUserId = getSessionUserId(request);
        Integer sessionRoleId = getSessionRoleId(request);
        boolean admin = isAdmin(sessionRoleId);

        Integer idArchivo = parseInt(request.getParameter("id_archivo"));
        if (idArchivo == null) {
            ResponseUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "id_archivo_requerido");
            return;
        }

        String sqlSelect = "SELECT id_usuario, ruta_archivo FROM archivos_multimedia WHERE id_archivo = ?";
        String sqlDelete = "DELETE FROM archivos_multimedia WHERE id_archivo = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement psSelect = con.prepareStatement(sqlSelect)) {
            psSelect.setInt(1, idArchivo.intValue());
            try (ResultSet rs = psSelect.executeQuery()) {
                if (!rs.next()) {
                    ResponseUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "archivo_no_encontrado");
                    return;
                }
                int ownerId = rs.getInt("id_usuario");
                String ruta = rs.getString("ruta_archivo");
                if (!admin && (sessionUserId == null || ownerId != sessionUserId.intValue())) {
                    ResponseUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "acceso_denegado");
                    return;
                }

                try (PreparedStatement psDelete = con.prepareStatement(sqlDelete)) {
                    psDelete.setInt(1, idArchivo.intValue());
                    psDelete.executeUpdate();
                }

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

    private boolean extensionValida(String tipoMedia, String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        if ("AUDIO".equals(tipoMedia)) {
            return "mp3".equals(extension);
        }
        if ("VIDEO".equals(tipoMedia)) {
            return "mp4".equals(extension);
        }
        if ("IMAGEN".equals(tipoMedia)) {
            return "jpg".equals(extension) || "jpeg".equals(extension);
        }
        return false;
    }

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
