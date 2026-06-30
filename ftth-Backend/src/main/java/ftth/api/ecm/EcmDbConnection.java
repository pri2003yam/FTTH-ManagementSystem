package ftth.api.ecm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Component
public class EcmDbConnection {

    private final String url;
    private final String username;
    private final String password;

    public EcmDbConnection(
            @Value("${ecm.datasource.url:jdbc:postgresql://localhost:5444/enterprisedb}") String url,
            @Value("${ecm.datasource.username:ecm}") String username,
            @Value("${ecm.datasource.password:ECM}") String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}
