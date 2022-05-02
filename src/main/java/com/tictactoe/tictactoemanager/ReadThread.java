package com.tictactoe.tictactoemanager;

import com.tictactoe.message.*;
import java.io.*;
import java.net.*;
import java.util.Objects;

public class ReadThread extends Thread {
    private ObjectInputStream input;
    private final TicTacToeManager manager;

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
                    if (Objects.equals(((ServerConnection)message).connectType(), "Player")) {
                        if (((ServerConnection)message).connection()) {
                            manager.addUserName(((ServerConnection)message).userName());
                        } else {
                            manager.removeUserName(((ServerConnection)message).userName());
                        }
                    }
                } else if (message instanceof ConnectToGame) {
                    if (((ConnectToGame)message).connection()) {
                        manager.addGame(((ConnectToGame)message).gameName());
                        manager.addPlayerToGame(((ConnectToGame)message).gameName(), ((ConnectToGame)message).userName());
                    } else {
                        manager.removePlayerFromGame(((ConnectToGame)message).gameName(), ((ConnectToGame)message).userName());
                    }
                } else if (message instanceof GameListRequest) {
                    manager.getGameList(((GameListRequest)message).userName());
                } else if (message instanceof PlayerMoveResult) {
                    manager.updateGame((PlayerMoveResult)message);
                } else if (message instanceof UpdateGame) {
                    manager.newGame((UpdateGame)message);
                }
            } catch (IOException | ClassNotFoundException ex) {
                manager.print("\nError reading from server: " + ex.getMessage()+ "\n");
                break;
            }
        }
    }
}