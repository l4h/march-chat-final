package ru.geekbrains.march.chat.server;

import java.sql.*;

/**
 * CRUD
 * C - Create
 * R - Read
 * U - Update
 * D - Delete
 */


public class sqlAuthProvider implements AuthenticationProvider {
    private static Connection connection;
    private static Statement stmt;

    public sqlAuthProvider() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:Users.db");
            stmt = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Невозможно подключиться к базе данных");
        }
    }


    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        String nick = null;
        try {
            ResultSet resultSet = stmt.executeQuery(String.format("select nick from users where login='%s' AND password='%s'", login, password));
            if (resultSet.next()) {
                nick = resultSet.getString(1);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return nick;
    }

    @Override
    public synchronized void changeNickname(String oldNickname, String newNickname) {
        try {
            stmt.executeUpdate(String.format("update users set nick = '%s' where nick = '%s'", newNickname, oldNickname));
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Override
    public synchronized boolean isNickFree(String nick) {
        try {
            //Запрос выглядит  глупо, но нам важно каким будет результат. Если пустой то ник свободен, если нет - уже занят.
            ResultSet rs = stmt.executeQuery(String.format("select nick from users where nick='%s'",nick));
            if (rs.next()) {
                rs.getString("score");
                return false;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return true;
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }
}
