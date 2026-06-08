package org.example;

import java.sql.Connection;
import java.sql.DriverManager;

public class TestConnection {

    public static void main(String[] args) throws Exception {

        String url = "jdbc:postgresql://localhost:5432/tpch";
        String user = "postgres";
        String pass = "postgres";

        try (Connection conn =
                     DriverManager.getConnection(url, user, pass)) {

            System.out.println("CONNECTED TO NODE1");
            System.out.println(conn.getMetaData().getDatabaseProductName());
        }
    }
}