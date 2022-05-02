package com.tictactoe.tictactoemanager;

import com.tictactoe.message.*;
import javafx.application.Platform;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

public class TicTacToeManager {
    private final String hostname;
    private final int port;
    private ObjectOutputStream output;
    private final ManagerController controller;
    private final Set<String> userNames = new HashSet<>();
    private final Set<String> gameNames = new HashSet<>();
    private final Set<Game> games = new HashSet<>();

    public TicTacToeManager(String hostname, int port, ManagerController controller) {
        this.hostname = hostname;
        this.port = port;
        this.controller = controller;
    }

    public void execute() {
        try {
            Socket socket = new Socket(hostname, port);
            print("Connected to the Tic-Tac-Toe Server");
            output = new ObjectOutputStream(socket.getOutputStream());
            output.writeObject(new ServerConnection("Manager", "Game Manager", true));
            output.flush();
            new ReadThread(socket, this).start();
        } catch (UnknownHostException ex) {
            print("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            print("I/O Error: " + ex.getMessage());
        }
    }

    void print(String message) {
        Platform.runLater(() -> controller.update(message));
    }

    void addUserName(String userName) {
        userNames.add(userName);
        Platform.runLater(() -> controller.addClient(userName));
    }

    void removeUserName(String userName) {
        userNames.remove(userName);
        Platform.runLater(() -> controller.removeClient(userName));
    }

    void addGame(String gameName) {
        for (Game game : games)
            if (Objects.equals(game.getGameName(), gameName))
                return;
        games.add(new Game(gameName));
        gameNames.add(gameName);
        Platform.runLater(() -> controller.addGame(gameName));
        print("\nGame created: " + gameName);
    }

    void removeGame(String gameName) {
        games.removeIf(game -> Objects.equals(game.getGameName(), gameName));
        gameNames.remove(gameName);
        Platform.runLater(() -> controller.removeGame(gameName));
        print("\nGame removed: " + gameName);
    }

    void addPlayerToGame(String gameName, String userName) throws IOException {
        for (Game game : games) {
            if (Objects.equals(game.getGameName(), gameName)) {
                game.addPlayer(userName);
                output.writeObject(new UpdateGame(game.getGameName(), game.getUserTokens(), game.getStartingToken(), game.getCurrentToken(), game.getBoardState(), "N"));
                output.flush();
            }
        }
        print("\nPlayer (" + userName + ") has joined game: " + gameName);
    }

    void removePlayerFromGame(String gameName, String userName) {
        for (Game game : games) {
            if (Objects.equals(game.getGameName(), gameName)) {
                game.removePlayer(userName);
            }
        }
        print("\nPlayer (" + userName + ") has left game: " + gameName);
    }

    void getGameList(String userName) throws IOException {
        output.writeObject(new GameListResult(userName, gameNames));
        output.flush();
        print("\nPlayer (" + userName + ") game list updated");
    }

    void updateGame(PlayerMoveResult result) throws IOException {
        for (Game game : games) {
            if (Objects.equals(game.getGameName(), result.gameName())) {
                game.setBoardState(result.board());
                game.setCurrentToken(result.playerToken());
                output.writeObject(new UpdateGame(game.getGameName(), game.getUserTokens(), game.getStartingToken(), game.getCurrentToken(), game.getBoardState(), result.result()));
                output.flush();
            }
        }
    }

    void newGame(UpdateGame message) throws IOException {
        for (Game game : games) {
            if (Objects.equals(game.getGameName(), message.gameName())) {
                if (Objects.equals(message.result(), "New Game")) {
                    game.clearBoard();
                    game.changeStartingToken();
                    game.changeCurrentToken();
                    output.writeObject(new UpdateGame(game.getGameName(), game.getUserTokens(), game.getStartingToken(), game.getCurrentToken(), game.getBoardState(), "New Game"));
                    output.flush();
                }
            }
        }
    }
}