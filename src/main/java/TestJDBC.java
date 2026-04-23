import java.sql.*;

public class TestJDBC {
    public static void main(String[] args) {

        String url = "jdbc:mysql://localhost:3306/mindcare";
        String user = "root";
        String pwd = "";

        try {
            Connection conn = DriverManager.getConnection(url, user, pwd);
            System.out.println("Connexion réussie !");

            Statement ste = conn.createStatement();

            ResultSet rs = ste.executeQuery("SELECT * FROM user");

            while (rs.next()) {
                System.out.println(rs.getString("id"));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}