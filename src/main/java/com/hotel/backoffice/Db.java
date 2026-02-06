package com.hotel.backoffice;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Db {
    private Db() {}

    public static Connection getConnection() throws SQLException {
        String url = System.getenv("jdbc:mysql://localhost:3306/hotel");
        String user = System.getenv("root");
        String password = System.getenv("");
        return DriverManager.getConnection(url, user, password);
    }
}
