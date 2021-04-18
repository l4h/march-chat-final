package ru.geekbrains.march.chat.server;

public interface AuthenticationProvider {
    String getNicknameByLoginAndPassword(String login, String password);
    void changeNickname(String oldNickname, String newNickname);
    boolean isNickFree(String nick);
    void disconnect();
}
