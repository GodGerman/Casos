package API;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Servicio de autenticacion basado en tabla de usuarios y roles.
 *
 * Encapsula la verificacion de credenciales contra la base de datos y el
 * llenado de la sesion HTTP con los datos minimos usados por el frontend
 * y por el filtro de autorizacion.
 *
 */
public final class AuthService {
    /**
     * Constructor privado para evitar instanciacion.
     */
    private AuthService() {
    }

    /**
     * Resultado de autenticacion exitoso con datos minimos del usuario y rol.
     *
     * Se usa para poblar la sesion y construir la respuesta del login.
     *
     */
    public static final class AuthResult {
        public final int id_usuario;
        public final int id_rol;
        public final String nombre_usuario;
        public final String nombre_rol;

        /**
         * @param id_usuario identificador del usuario autenticado.
         * @param id_rol identificador del rol del usuario.
         * @param nombre_usuario nombre de usuario (login).
         * @param nombre_rol nombre descriptivo del rol.
         *
         * Este objeto es inmutable para evitar cambios entre validacion y uso.
         *
         */
        public AuthResult(int id_usuario, int id_rol, String nombre_usuario, String nombre_rol) {
            this.id_usuario = id_usuario;
            this.id_rol = id_rol;
            this.nombre_usuario = nombre_usuario;
            this.nombre_rol = nombre_rol;
        }
    }

    /**
     * Valida credenciales contra la base de datos.
     *
     * Se ejecuta un SELECT con JOIN a roles usando
     * PreparedStatement para evitar inyeccion, compara usuario/contrasena y
     * construye {@link AuthResult} si existe coincidencia.
     *
     *
     * @param nombre_usuario usuario a validar.
     * @param contrasena contrasena en texto plano (segun esquema actual).
     * @return AuthResult si las credenciales son validas; null si no coinciden.
     * @throws SQLException si falla el acceso a datos.
     */
    public static AuthResult authenticate(String nombre_usuario, String contrasena) throws SQLException {
        String sql = "SELECT u.id_usuario, u.nombre_usuario, u.id_rol, r.nombre_rol "
                + "FROM usuarios u "
                + "INNER JOIN roles r ON r.id_rol = u.id_rol "
                + "WHERE u.nombre_usuario = ? AND u.contrasena = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre_usuario);
            ps.setString(2, contrasena);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new AuthResult(
                            rs.getInt("id_usuario"),
                            rs.getInt("id_rol"),
                            rs.getString("nombre_usuario"),
                            rs.getString("nombre_rol")
                    );
                }
            }
        }
        return null;
    }

    /**
     * Crea o reutiliza la sesion HTTP e inyecta los atributos del usuario.
     * No retorna valor; solo modifica la sesion HTTP.
     *
     * Se obtiene/crea HttpSession y setea atributos usados por
     * AuthFilter y servlets (id_usuario, id_rol, etc.).
     *
     *
     * @param request request HTTP actual.
     * @param result datos de autenticacion exitosa.
     */
    public static void applySession(HttpServletRequest request, AuthResult result) {
        HttpSession session = request.getSession(true);
        session.setAttribute("id_usuario", result.id_usuario);
        session.setAttribute("id_rol", result.id_rol);
        session.setAttribute("nombre_usuario", result.nombre_usuario);
        session.setAttribute("nombre_rol", result.nombre_rol);
    }
}
