package API;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public final class AuthService {
    private AuthService() {
    }

    public static final class AuthResult {
        public final int idUsuario;
        public final int idRol;
        public final String nombreUsuario;
        public final String nombreRol;

        public AuthResult(int idUsuario, int idRol, String nombreUsuario, String nombreRol) {
            this.idUsuario = idUsuario;
            this.idRol = idRol;
            this.nombreUsuario = nombreUsuario;
            this.nombreRol = nombreRol;
        }
    }

    public static AuthResult authenticate(String nombreUsuario, String contrasena) throws SQLException {
        String sql = "SELECT u.id_usuario, u.nombre_usuario, u.id_rol, r.nombre_rol "
                + "FROM usuarios u "
                + "INNER JOIN roles r ON r.id_rol = u.id_rol "
                + "WHERE u.nombre_usuario = ? AND u.contrasena = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario);
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

    public static void applySession(HttpServletRequest request, AuthResult result) {
        HttpSession session = request.getSession(true);
        session.setAttribute("id_usuario", result.idUsuario);
        session.setAttribute("id_rol", result.idRol);
        session.setAttribute("nombre_usuario", result.nombreUsuario);
        session.setAttribute("nombre_rol", result.nombreRol);
    }
}
