package ru.geekbrains.march.chat.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private static Logger logCl = LogManager.getLogger();
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;

    public String getUsername() {
        return username;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        doAuth(server);
        receiveMessage(server);
        disconnect();

    }

    private void doAuth(Server server) throws IOException {
        while (true) { // Цикл авторизации
            String msg = in.readUTF();
            if (msg.startsWith("/login ")) {
                // /login Bob 100xyz
                String[] tokens = msg.split("\\s+");
                if (tokens.length != 3) {
                    sendMessage("/login_failed Введите имя пользователя и пароль");
                    continue;
                }
                String login = tokens[1];
                String password = tokens[2];

                String userNickname = server.getAuthenticationProvider().getNicknameByLoginAndPassword(login, password);
                if (userNickname == null) {
                    sendMessage("/login_failed Введен некорретный логин/пароль");
                    logCl.warn(">>Неудачная попытка аутентификации");
                    continue;
                }
                if (server.isUserOnline(userNickname)) {
                    sendMessage("/login_failed Учетная запись уже используется");
                    logCl.warn(">>Попытка повторной аутентификации{nickname: "+userNickname+"}");
                    continue;
                }
                username = userNickname;
                sendMessage("/login_ok " + username);
                server.subscribe(this);
                break;
            }
        }
    }

    private void receiveMessage(Server server) throws IOException {
        while (true) { // Цикл общения с клиентом
            String msg = in.readUTF();
            if (msg.startsWith("/")) {
                executeCommand(msg);
                continue;
            }
            server.broadcastMessage(username + ": " + msg);
        }
    }

    private void executeCommand(String cmd) {
        // /w Bob Hello, Bob!!!
        if (cmd.startsWith("/w ")) {
            String[] tokens = cmd.split("\\s+", 3);
            if (tokens.length != 3) {
                sendMessage("Server: Введена некорректная команда");
                return;
            }
            server.sendPrivateMessage(this, tokens[1], tokens[2]);
            return;
        }
        if (cmd.startsWith("/q!")){
            disconnect();
            return;
        }

        // /change_nick myNewNickname
        if (cmd.startsWith("/change_nick ")) {
            String[] tokens = cmd.split("\\s+");
            if (tokens.length != 2) {
                sendMessage("Server: Введена некорректная команда");
                return;
            }
            String newNickname = tokens[1];
            if (server.getAuthenticationProvider().isNickFree(newNickname)) {
                server.getAuthenticationProvider().changeNickname(username, newNickname);
                logCl.info(username +" переименовался в " + newNickname);
                username = newNickname;
                sendMessage("Server: Вы изменили никнейм на " + newNickname);
                server.broadcastClientsList();
            } else {
                sendMessage("Server: Такой никнейм уже занят");
                return;
            }
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        sendMessage("/q!");
        server.unsubscribe(this);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
