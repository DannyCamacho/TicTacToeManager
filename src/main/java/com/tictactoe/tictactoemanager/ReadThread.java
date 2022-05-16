package com.tictactoe.tictactoemanager;

import com.tictactoe.message.*;
import java.io.*;
import java.net.*;

public class ReadThread extends Thread {
    private final TicTacToeManager manager;
    private ObjectInputStream input;

    public ReadThread(Socket socket, TicTacToeManager manager) {
        this.manager = manager;
        try {
            input = new ObjectInputStream(socket.getInputStream());
        } catch (IOException ex) {
            manager.print("\nError getting input stream: " + ex.getMessage() + "\n");
        }
    }

    public void run() {
        while (true) {
            try {
                Object message = input.readObject();
                if (message instanceof ServerConnection) {
                    if (((ServerConnection)message).connection()) {
                        manager.addUser((ServerConnection)message);
                    } else {
                        manager.removeUser((ServerConnection)message);
                    }
                } else if (message instanceof ConnectToGame) {
                    if (((ConnectToGame)message).connection()) {
                        manager.addGame((ConnectToGame)message);
                    } else {
                        manager.removePlayerFromGame((ConnectToGame)message);
                    }
                } else if (message instanceof ChatMessage) {
                    manager.sendChatMessage((ChatMessage)message);
                } else if (message instanceof GameListRequest) {
                    manager.getGameList((GameListRequest)message);
                } else if (message instanceof PlayerMoveResult) {
                    manager.updateGame((PlayerMoveResult)message);
                } else if (message instanceof UpdateGame) {
                    manager.receiveUpdateGameMessage((UpdateGame)message);
                }
            } catch (IOException | ClassNotFoundException ex) {
                manager.print("\nError reading from server: " + ex.getMessage()+ "\n");
                break;
            }
        }
    }
}