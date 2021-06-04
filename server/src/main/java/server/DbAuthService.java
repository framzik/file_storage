package server;

import java.sql.*;

public class DbAuthService implements AuthService {

  private Connection connection;
  private Statement statement;

  public DbAuthService() {
    try {
      connect();
    } catch (ClassNotFoundException | SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public String getNicknameByLoginAndPassword(String login, String password) {
    String nickname = null;
    try {
      PreparedStatement psFindLogin = connection
          .prepareStatement("SELECT nickname FROM users where login = ? AND password =?; ");
      psFindLogin.setString(1, login);
      psFindLogin.setString(2, password);
      nickname = psFindLogin.executeQuery().getString("nickname");
    } catch (SQLException throwable) {
      throwable.printStackTrace();
    }
    return nickname;
  }

  @Override
  public boolean registration(String login, String password, String nickname) {
    boolean ok = false;
    try {
      PreparedStatement psInsert = connection
          .prepareStatement("INSERT INTO users (login,password,nickname) values (?,?,?);");
      psInsert.setString(1, login);
      psInsert.setString(2, password);
      psInsert.setString(3, nickname);
      if (psInsert.executeUpdate() != 0) {
        ok = true;
      }
    } catch (SQLException throwable) {
      throwable.printStackTrace();
    }
    return ok;
  }


  private void connect() throws ClassNotFoundException, SQLException {
    Class.forName("org.sqlite.JDBC");
    connection = DriverManager.getConnection("jdbc:sqlite:file_storage_users.db");
    statement = connection.createStatement();
  }

  @Override
  public void disconnect() {
    try {
      statement.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    try {
      connection.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
