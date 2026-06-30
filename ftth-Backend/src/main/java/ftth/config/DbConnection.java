package ftth.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DbConnection {

    private static final String URL = System.getenv("SPRING_DATASOURCE_URL") != null
            ? System.getenv("SPRING_DATASOURCE_URL")
            : "jdbc:mysql://localhost:3306/testdb";
    private static final String USER = System.getenv("SPRING_DATASOURCE_USERNAME") != null
            ? System.getenv("SPRING_DATASOURCE_USERNAME")
            : "root";
    private static final String PASSWORD = System.getenv("SPRING_DATASOURCE_PASSWORD") != null
            ? System.getenv("SPRING_DATASOURCE_PASSWORD")
            : "Aaha@6598";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                "MySQL JDBC driver not found. Add mysql-connector-j to runtime classpath (example: -cp \"out;lib/*\").",
                e
            );
        }
    }

    private DbConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
