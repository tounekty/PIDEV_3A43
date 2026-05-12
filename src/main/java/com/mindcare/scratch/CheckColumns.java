package com.mindcare.scratch;

import com.mindcare.db.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

public class CheckColumns {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM appointment LIMIT 1")) {
            
            ResultSetMetaData meta = rs.getMetaData();
            int count = meta.getColumnCount();
            System.out.println("Columns in 'appointment' table:");
            for (int i = 1; i <= count; i++) {
                System.out.println("- " + meta.getColumnName(i));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
