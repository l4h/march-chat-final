package ru.geekbrains.march.chat.client;

public interface History {

    void addMessage(String msg);
    String load();
    void close();
}
