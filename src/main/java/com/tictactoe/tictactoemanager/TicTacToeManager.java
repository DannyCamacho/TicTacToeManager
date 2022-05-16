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
    private final ManagerController controller;
    private final Set<String> lobby;
    private final Set<Game> games;
    private ObjectOutputStream output;

    public TicTacToeManager(String hostname, int port, ManagerController controller) {
        this.hostname = hostname;
        this.port = port;
        this.controller = controller;
        lobby = new HashSet<>();
        games = new HashSet<>();
    }

    public void execute() {
        try {
            Socket socket = new Socket(hostname, port);
            print("Connected to the Tic-Tac-Toe Server");
            output = new ObjectOutputStream(socket.getOutputStream());
            output.writeObject(new ServerConnection("Manager", "Manager", true));
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

    void updateGame(PlayerMoveResult result) throws IOException {
        for (Game game : games) {
            if (Objects.equals(game.getGameName(), result.gameName())) {
                game.setBoardState(result.board());
                game.setCurrentToken(result.playerToken());
                if (!Objects.equals(result.result(), "N")) {
                    game.updateGameHistory(result.result().charAt(0));
                }
                for (String user : game.getUsers()) {
                    output.writeObject(new UpdateGame(game.getGameName(), user, game.getCurrentToken(), game.getBoardState(), result.result()));
                    output.flush();
                    if (!Objects.equals(result.result(), "N")) {
                        output.writeObject(new UpdateGameHistory(game.getGameName(), user, game.getXodWins(), game.getGameHistory()));
                        output.flush();
                    }
                }
                if (game.getVsAI() && Objects.equals(result.result(), "N") && game.getCurrentToken() == 'X') {
                    output.writeObject(new MinimaxMoveSend(game.getGameName(), 'X', game.getBoardState()));
                    output.flush();
                }
            }
        }
    }

    void addUser(ServerConnection message) throws IOException {
        sendChatMessage((new ChatMessage("Lobby", "", "", message.userName() + " has joined the lobby.\n")));
        lobby.add(message.userName());
        Platform.runLater(() -> controller.addUser(message.userName()));
        print("\nPlayer (" + message.userName() + ") has connected");
    }

    void removeUser(ServerConnection message) throws IOException {
        lobby.remove(message.userName());
        sendChatMessage((new ChatMessage("Lobby", "", "", message.userName() + " has left the lobby.\n")));
        Platform.runLater(() -> controller.removeUser(message.userName()));
        print("\nPlayer (" + message.userName() + ") has disconnected");
    }

    void addGame(ConnectToGame message) throws IOException {
        for (Game game : games) {
            if (Objects.equals(game.getGameName(), message.gameName())) {
                addPlayerToGame(message);
                return;
            }
        }
        games.add(new Game(message.gameName(), message.VsAI()));
        Platform.runLater(() -> controller.addGame(message.gameName()));
        print("\nGame created: " + message.gameName());
        addPlayerToGame(message);
    }

    void removeGame(String gameName) {
        for (Game game : games) {
            if (Objects.equals(game.getGameName(), gameName)) {
                games.remove(game);
                Platform.runLater(() -> controller.removeGame(gameName));
                print("\nGame removed: " + gameName);
                return;
            }
        }
    }

    void addPlayerToGame(ConnectToGame message) throws IOException {
        lobby.remove(message.userName());
        sendChatMessage((new ChatMessage("Lobby", "", "", message.userName() + " has left the lobby.\n")));
        output.writeObject(new ConnectToGame(message.gameName(), message.userName(), true, message.VsAI()));
        output.flush();
        for (Game game : games) {
            if (Objects.equals(game.getGameName(), message.gameName())) {
                sendChatMessage((new ChatMessage("Board", message.gameName(), "", message.userName() + " has joined the game.\n")));
                game.addPlayer(message.userName());
                print("\nPlayer (" + message.userName() + ") has joined game: " + message.gameName());
                output.writeObject(new UpdateGame(message.gameName(), message.userName(), game.getUserToken(message.userName()), game.getBoardState(), "Initialize"));
                output.flush();
                output.writeObject(new UpdateGameHistory(game.getGameName(), message.userName(), game.getXodWins(), game.getGameHistory()));
                output.flush();
            }
        }
    }

    void removePlayerFromGame(ConnectToGame message) throws IOException {
        for (Game game : games) {
            if (Objects.equals(game.getGameName(), message.gameName())) {
                game.removePlayer(message.userName());
                sendChatMessage((new ChatMessage("Board", message.gameName(), "", message.userName() + " has left the game.\n")));
                sendChatMessage((new ChatMessage("Lobby", "", "", message.userName() + " has joined the lobby.\n")));
                lobby.add(message.userName());
                print("\nPlayer (" + message.userName() + ") has left game: " + message.gameName());
            }
        }
    }

    void getGameList(GameListRequest message) throws IOException {
        String[] gameList = new String[games.size()];
        int i = 0;
        for (Game game : games) {
            gameList[i++] = game.getGameName();
        }
        output.writeObject(new GameListResult(message.userName(), gameList));
        output.flush();
        print("\nPlayer (" + message.userName() + ") game list updated");
        output.writeObject(new ChatMessage("Lobby", "", message.userName(), "Game List has been updated.\n"));
        output.flush();
    }

    void receiveUpdateGameMessage(UpdateGame message) throws IOException {
        for (Game game : games) {
            if (Objects.equals(game.getGameName(), message.gameName())) {
                if (Objects.equals(message.result(), "End")) {
                    game.clearBoard();
                    game.changeStartingToken();
                    game.setCurrentToStarting();
                    for (String user : game.getUsers()) {
                        output.writeObject(new UpdateGame(game.getGameName(), user, game.getCurrentToken(), game.getBoardState(), "End"));
                        output.flush();
                    }
                } else if (Objects.equals(message.result(), "New")) {
                        output.writeObject(new UpdateGame(game.getGameName(), message.userName(), game.getCurrentToken(), game.getBoardState(), "N"));
                        output.flush();
                    if (game.getVsAI() && game.getCurrentToken() == 'X') {
                        output.writeObject(new MinimaxMoveSend(game.getGameName(), 'X', game.getBoardState()));
                        output.flush();
                    }
                }
            }
        }
    }

    void sendChatMessage(ChatMessage message) throws IOException {
        if (Objects.equals(message.messageType(), "Lobby")) {
            for (String user : lobby) {
                output.writeObject(new ChatMessage("Lobby", "", user, message.message()));
                output.flush();
            }
        } else if (Objects.equals(message.messageType(), "Board")) {
            for (Game game : games) {
                if (Objects.equals(game.getGameName(), message.gameName())) {
                    for (String user : game.getUsers()) {
                        output.writeObject(new ChatMessage("Board", game.getGameName(), user, message.message()));
                        output.flush();
                    }
                }
            }
        }
    }
}