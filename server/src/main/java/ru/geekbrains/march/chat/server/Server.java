package ru.geekbrains.march.chat.server;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final Logger log = LogManager.getLogger();
    private int port;
    private List<ClientHandler> clients;
    private AuthenticationProvider authenticationProvider;
    private ExecutorService executorService;

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    /** Считаю, что тут можно использовать cachedThreadPool , т.к. он адаптируется под нагрузку,
     * кроме того количество соединений у нас ограничено количеством пользователей в БД(//todo unauthTimeoutDisconect().
     * При желании, конечно, сервак с такой реализацией и без доп мер защиты можно положить. В остальном использование
     * CachedThreadPool считаю оптимальным.
     * */

    public Server(int port) {
        this.port = port;
        this.clients = new ArrayList<>();
        this.authenticationProvider = new sqlAuthProvider();
        executorService = Executors.newCachedThreadPool();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
//            System.out.println("Сервер запущен на порту " + port);
            log.info("Server started at port "+port);
            while (true) {
                System.out.println("Ждем нового клиента..");
                Socket socket = serverSocket.accept();
//                System.out.println("Клиент подключился");
                log.info("New client connected");
                executorService.execute(()-> {
                    try {
                        new ClientHandler(this, socket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            authenticationProvider.disconnect();
            executorService.shutdown();
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        log.info("Client: "+clientHandler.getUsername()+" enter to chat");
        broadcastMessage("Клиент " + clientHandler.getUsername() + " вошел в чат");
        broadcastClientsList();

    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        log.info("Client: "+clientHandler.getUsername()+" logout");
        broadcastMessage("Клиент " + clientHandler.getUsername() + " вышел из чата");
        broadcastClientsList();

    }

    public synchronized void broadcastMessage(String message)  {
        for (ClientHandler clientHandler : clients) {
            clientHandler.sendMessage(message);
        }
    }

    public synchronized void sendPrivateMessage(ClientHandler sender, String receiverUsername, String message) {
        for (ClientHandler c : clients) {
            if (c.getUsername().equals(receiverUsername)) {
                c.sendMessage("От: " + sender.getUsername() + " Сообщение: " + message);
                sender.sendMessage("Пользователю: " + receiverUsername + " Сообщение: " + message);
                return;
            }
        }
        sender.sendMessage("Невозможно отправить сообщение пользователю: " + receiverUsername + ". Такого пользователя нет в сети.");
    }

    public synchronized boolean isUserOnline(String username) {
        for (ClientHandler clientHandler : clients) {
            if (clientHandler.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void broadcastClientsList() {
        StringBuilder stringBuilder = new StringBuilder("/clients_list ");
        for (ClientHandler c : clients) {
            stringBuilder.append(c.getUsername()).append(" ");
        }
        stringBuilder.setLength(stringBuilder.length() - 1);
        String clientsList = stringBuilder.toString();
        for (ClientHandler clientHandler : clients) {
            clientHandler.sendMessage(clientsList);
        }
    }
}
