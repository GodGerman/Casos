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
        public final int id_usuario;
        public final int id_rol;
        public final String nombre_usuario;
        public final String nombre_rol;

        public AuthResult(int id_usuario, int id_rol, String nombre_usuario, String nombre_rol) {
            this.id_usuario = id_usuario;
            this.id_rol = id_rol;
            this.nombre_usuario = nombre_usuario;
            this.nombre_rol = nombre_rol;
        }
    }

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

    public static void applySession(HttpServletRequest request, AuthResult result) {
        HttpSession session = request.getSession(true);
        session.setAttribute("id_usuario", result.id_usuario);
        session.setAttribute("id_rol", result.id_rol);
        session.setAttribute("nombre_usuario", result.nombre_usuario);
        session.setAttribute("nombre_rol", result.nombre_rol);
    }
}
