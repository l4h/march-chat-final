package ru.geekbrains.march.chat.client;

import javafx.beans.binding.StringBinding;

import java.io.*;

public class HistoryFileStorage implements History{
    private File history;
    private FileWriter out;
    private FileReader in;


    public HistoryFileStorage(String username){
        history = new File(username);
        try {
            if(!history.exists()){
                history.createNewFile();
            }
            out = new FileWriter(history,true);
            in = new FileReader(history);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void addMessage(String msg) {
        try {
            out.append(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String load() {
        StringBuilder history = new StringBuilder();
        int a;
        try {
            do {
                a = in.read();
                history.append((char)a);
            } while (a!=-1);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return history.substring(0,history.length()-1);
    }

    public void close(){
        try {
            out.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
