package API;

public final class DbConfig {
    private DbConfig() {
    }

    private static String getValue(String envKey, String propKey, String fallback) {
        String value = System.getenv(envKey);
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(propKey);
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
    public static final String PASS = getValue("DB_PASS", "db.pass", "1234");
}
