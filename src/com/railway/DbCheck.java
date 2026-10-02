package com.railway;

import com.railway.repository.jdbc.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbCheck {
    public static void main(String[] args) {
        try (Connection c = DBConnection.getConnection();
             Statement s = c.createStatement()) {

            System.out.println("Connected to: " + c.getMetaData().getURL());

            for (String table : new String[] {"stations", "trains", "train_stops", "train_classes"}) {
                try (ResultSet rs = s.executeQuery("select count(*) from " + table)) {
                    rs.next();
                    System.out.println(table + ": " + rs.getInt(1) + " rows");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}