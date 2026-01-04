package API;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DbUtil {
    static {
        try {
            Class.forName(DbConfig.DRIVER);
        } catch (ClassNotFoundException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private DbUtil() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DbConfig.URL, DbConfig.USER, DbConfig.PASS);
    }
}
