package API;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utilidad de conexion JDBC para el backend.
 *
 * Centraliza credenciales/URL, carga el driver y ofrece helpers simples para
 * ejecutar consultas. Se usa como wrapper ligero (no es pool), por lo que cada
 * servlet suele abrir/cerrar conexiones por request.
 *
 */
public class DB implements Serializable {
    /**
     * Obtiene una configuracion priorizando variable de entorno, luego propiedad JVM
     * y finalmente un valor por defecto.
     *
     * @param env_key variable de entorno a consultar.
     * @param prop_key propiedad del sistema (System.getProperty).
     * @param fallback valor por defecto si no hay configuracion.
     * @return valor normalizado (trim) o el fallback si no existe.
     *
     * Se consulta primero System.getenv, luego System.getProperty,
     * y solo si ambos estan vacios retorna el fallback.
     *
     */
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

    // Carga el driver JDBC al inicializar la clase para fallar temprano si falta
    // en el classpath; evita errores tardios en el primer request.
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

    /**
     * Abre una conexion JDBC usando el driver y URL especificados.
     * No retorna valor; deja la conexion lista en esta instancia.
     *
     * Se carga el driver (Class.forName) y crea una conexion con
     * DriverManager usando las credenciales centralizadas.
     *
     *
     * @param db_driver clase del driver JDBC.
     * @param db_url URL JDBC completa.
     * @throws IOException si el driver no existe en el classpath.
     * @throws SQLException si la conexion no se pudo abrir.
     */
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

    /**
     * Abre una conexion usando los valores configurados en esta clase.
     * No retorna valor; deja la conexion lista en esta instancia.
     *
     * Se delega a {@link #setConnection(String, String)} usando
     * DRIVER/URL definidos como constantes.
     *
     *
     * @throws IOException si el driver no existe en el classpath.
     * @throws SQLException si la conexion no se pudo abrir.
     */
    public void setConnection() throws IOException, SQLException {
        setConnection(DRIVER, URL);
    }

    /**
     * Cierra la conexion y los Statements abiertos por esta instancia.
     * No retorna valor; limpia referencias internas.
     *
     * Se cierra Connection, luego Statements y
     * limpia referencias para evitar reutilizacion accidental.
     *
     *
     * @throws SQLException si ocurre un error al cerrar recursos JDBC.
     */
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

    /**
     * Ejecuta una sentencia SQL de escritura (INSERT/UPDATE/DELETE).
     *
     * Se crea un Statement temporal, ejecuta la sentencia y lo
     * cierra en un finally para liberar recursos.
     *
     *
     * @param sql sentencia a ejecutar.
     * @return numero de filas afectadas.
     * @throws SQLException si no hay conexion activa o la sentencia falla.
     */
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

    /**
     * Ejecuta una sentencia SQL de lectura y retorna el ResultSet.
     *
     * Se crea un Statement y ejecuta la consulta. El Statement se
     * mantiene vivo porque el ResultSet depende de el; se recomienda cerrar
     * mediante {@link #closeConnection()} cuando ya no se use.
     *
     *
     * @param sql consulta a ejecutar.
     * @return ResultSet con los datos; puede ser null si falla antes de ejecutar.
     * @throws SQLException si no hay conexion activa o la consulta falla.
     */
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

    /**
     * Abre una conexion nueva con las credenciales configuradas.
     *
     * Se usa DriverManager con URL/USER/PASS estaticos.
     *
     *
     * @return conexion JDBC lista para usar.
     * @throws SQLException si no se puede abrir la conexion.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    /**
     * @return URL JDBC actualmente configurada en esta instancia.
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return nombre de driver JDBC configurado en esta instancia.
     */
    public String getDriver() {
        return driver;
    }
}
