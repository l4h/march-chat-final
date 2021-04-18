package ru.geekbrains.march.chat.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    Button enter,logout;

    @FXML
    TextField msgField, loginField;

    @FXML
    PasswordField passwordField;

    @FXML
    TextArea msgArea;

    @FXML
    HBox loginPanel, msgPanel;

    @FXML
    ListView<String> clientsList;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;
    private History history;

    public void setUsername(String username) {
        this.username = username;
        boolean usernameIsNull = username == null;
        loginField.setVisible(usernameIsNull);
        enter.setVisible(usernameIsNull);
        passwordField.setVisible(usernameIsNull);
        logout.setVisible(!usernameIsNull);

        //loginPanel.setManaged(usernameIsNull);
        msgPanel.setVisible(!usernameIsNull);
        msgPanel.setManaged(!usernameIsNull);
        clientsList.setVisible(!usernameIsNull);
        clientsList.setManaged(!usernameIsNull);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setUsername(null);
    }

    public void login() {
        if (loginField.getText().isEmpty()) {
            showErrorAlert("Имя пользователя не может быть пустым");
            return;
        }

        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF("/login " + loginField.getText() + " " + passwordField.getText());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void connect() {
        try {
            socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            Thread t = new Thread(() -> {
                try {
                    // Цикл авторизации
                    doAuth();
                    history = new HistoryFileStorage(username);
                    msgArea.appendText(history.load());
                    // Цикл общения
                    doChat();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    disconnect();
                }
            });
            t.start();
        } catch (IOException e) {
            showErrorAlert("Невозможно подключиться к серверу");
        }
    }

    private void doChat() throws IOException {
        while (!socket.isClosed()) {
            String msg = in.readUTF();
            if (msg.startsWith("/")) {
                if (msg.startsWith("/clients_list ")) {
                    // /clients_list Bob Max Jack
                    String[] tokens = msg.split("\\s");
                    Platform.runLater(() -> {
                        clientsList.getItems().clear();
                        for (int i = 1; i < tokens.length; i++) {
                            clientsList.getItems().add(tokens[i]);
                        }
                    });
                }
                if (msg.startsWith("/q!")) break;
                continue;
            }
            msgArea.appendText(msg + "\n");
            //как только мы добавили сообщение в область чата сразу же закинем это сообщение в историю сообщений
            history.addMessage(msg + "\n");
        }
    }

    private void doAuth() throws IOException {
        while (true) {
            String msg = in.readUTF();
            if (msg.startsWith("/login_ok ")) {
                setUsername(msg.split("\\s")[1]);
                break;
            }
            if (msg.startsWith("/login_failed ")) {
                String cause = msg.split("\\s", 2)[1];
                msgArea.appendText(cause + "\n");
            }
        }
    }

    public void sendMsg() {
        String msg = msgField.getText();
        try {
            out.writeUTF(msg);
            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            showErrorAlert("Невозможно отправить сообщение");
        }
    }
    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            showErrorAlert("Невозможно отправить сообщение");
        }
    }

    public void disconnect() {
        sendMsg("/q!");
        setUsername(null);
        msgArea.clear();
        try {
            if (socket != null) {
                socket.close();
                history.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.setTitle("March Chat FX");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
