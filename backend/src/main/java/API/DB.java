package API;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DB implements Serializable {
    private static String getValue(String env_key, String prop_key, String fallback) {
        String value = System.getenv(env_key);
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(prop_key);
        }
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    public static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    public static final String URL = getValue(
            "DB_URL",
            "db.url",
            "jdbc:mysql://localhost:3306/aplicacion?useSSL=false&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8"
    );
    public static final String USER = getValue("DB_USER", "db.user", "root");
    public static final String PASS = getValue("DB_PASS", "db.pass", "2005");

    static {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private String url;
    private String driver;
    private transient Connection conexion;
    private Statement stmt_query;
    private Statement stmt_update;
    private ResultSet result_set;

    public void setConnection(String db_driver, String db_url) throws IOException, SQLException {
        try {
            Class.forName(db_driver);
            conexion = DriverManager.getConnection(db_url, USER, PASS);
            this.url = db_url;
            this.driver = db_driver;
        } catch (ClassNotFoundException e) {
            throw new IOException(e.getMessage());
        }
    }

    public void setConnection() throws IOException, SQLException {
        setConnection(DRIVER, URL);
    }

    public void closeConnection() throws SQLException {
        if (conexion != null) {
            conexion.close();
        }
        url = driver = null;
        if (stmt_update != null) {
            stmt_update.close();
        }
        if (stmt_query != null) {
            stmt_query.close();
        }
        stmt_update = stmt_query = null;
        result_set = null;
    }

    public int executeUpdate(String sql) throws SQLException {
        if (conexion == null) {
            throw new SQLException("No ha configurado correctamente la conexion Source:Bean handledb");
        }

        stmt_update = null;
        int affected_rows = 0;

        try {
            stmt_update = conexion.createStatement();
            affected_rows = stmt_update.executeUpdate(sql);
        } finally {
            if (stmt_update != null) {
                stmt_update.close();
            }
        }
        return affected_rows;
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        if (conexion == null) {
            throw new SQLException("No ha configurado correctamente la conexion Source:Bean handledb");
        }

        stmt_query = null;
        result_set = null;

        stmt_query = conexion.createStatement();
        result_set = stmt_query.executeQuery(sql);
        return result_set;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public String getUrl() {
        return url;
    }

    public String getDriver() {
        return driver;
    }
}
