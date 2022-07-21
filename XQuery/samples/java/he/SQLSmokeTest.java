package he;

// This is just a smoke test that the SQL environment is setup correctly. It doesn't actually
// test any Saxon functionality.

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class SQLSmokeTest {

    public static void main(String[] args) {
        try {
            Object driver = Class.forName("org.postgresql.Driver").getConstructor().newInstance();
            System.out.println("Loaded driver: " + driver);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }

        String serverUrl = "jdbc:postgresql://localhost/sample";

        Properties props = new Properties();
        props.setProperty("user","testeditor");
        props.setProperty("password","sekrit");

        Connection conn = null;

        try {
            conn = DriverManager.getConnection(serverUrl, props);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT NAME,COLOR FROM FRUITS WHERE COLOR = 'Yellow';");
            rs.next();
            while (rs.getRow() != 0) {
                System.out.println("A " + rs.getString(1) + " is " + rs.getString(2));
                rs.next();
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                System.err.println("Failure during shutdown");
                System.err.println(ex.getMessage());
            }
        }
    }
}
