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
    private final Set<String> lobby = new HashSet<>();
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

    void updateGame(PlayerMoveResult result) throws IOException {
        for (Game game : games) {
            if (Objects.equals(game.getGameName(), result.gameName())) {
                game.setBoardState(result.board());
                game.setCurrentToken(result.playerToken());
                output.writeObject(new UpdateGame(game.getGameName(), game.getUserTokens(), game.getCurrentToken(), game.getBoardState(), result.result()));
                output.flush();
                if (game.getVsAI() && Objects.equals(result.result(), "N") && game.getCurrentToken() == 'X') {
                    output.writeObject(new MinimaxMoveSend(game.getGameName(), 'X', game.getBoardState()));
                    output.flush();
                }
            }
        }
    }

    void addUserName(ServerConnection message) throws IOException {
        userNames.add(message.userName());
        lobby.add(message.userName());
        Platform.runLater(() -> controller.addClient(message.userName()));
        print("\nPlayer (" + message.userName() + ") has connected");
        sendChatMessage((new ChatMessage("Connect", null, message.userName(), message.userName() + " has joined the lobby.\n")));
    }

    void removeUserName(ServerConnection message) throws IOException {
        userNames.remove(message.userName());
        lobby.remove(message.userName());
        Platform.runLater(() -> controller.removeClient(message.userName()));
        print("\nPlayer (" + message.userName() + ") has disconnected");
        sendChatMessage((new ChatMessage("Connect", null, message.userName(), message.userName() + " has left the lobby.\n")));
    }

    void addGame(ConnectToGame message) throws IOException {
        for (Game game : games)
            if (Objects.equals(game.getGameName(), message.gameName())) {
                addPlayerToGame(message);
                return;
            }
        games.add(new Game(message.gameName(), message.VsAI()));
        gameNames.add(message.gameName());
        Platform.runLater(() -> controller.addGame(message.gameName()));
        print("\nGame created: " + message.gameName());
        addPlayerToGame(message);
    }

    void removeGame(String gameName) {
        games.removeIf(game -> Objects.equals(game.getGameName(), gameName));
        gameNames.remove(gameName);
        Platform.runLater(() -> controller.removeGame(gameName));
        print("\nGame removed: " + gameName);
    }

    void addPlayerToGame(ConnectToGame message) throws IOException {
        output.writeObject(new ConnectToGame(message.gameName(), message.userName(), true, message.VsAI()));
        output.flush();
        for (Game game : games) {
            if (Objects.equals(game.getGameName(), message.gameName())) {
                game.addPlayer(message.userName());
                output.writeObject(new UpdateGame(message.gameName(), game.getUserTokens(), game.getCurrentToken(), game.getBoardState(), "Initialize"));
                output.flush();
            }
        }
        lobby.remove(message.userName());
        print("\nPlayer (" + message.userName() + ") has joined game: " + message.gameName());
        output.writeObject(new ChatMessage("Board", message.gameName(), message.userName(), "Connected to Game " + message.gameName() + "."));
        output.flush();
        sendChatMessage((new ChatMessage("Connect", message.gameName(), message.userName(), message.userName() + " has joined the game.\n")));
    }

    void removePlayerFromGame(ConnectToGame message) throws IOException {
        for (Game game : games) {
            if (Objects.equals(game.getGameName(), message.gameName())) {
                game.removePlayer(message.userName());
            }
        }
        lobby.add(message.userName());
        print("\nPlayer (" + message.userName() + ") has left game: " + message.gameName());
        sendChatMessage((new ChatMessage("Connect", message.gameName(), message.userName(), message.userName() + " has left the game.\n")));
    }

    void getGameList(GameListRequest message) throws IOException {
        String[] gameList = new String[gameNames.size()];
        int i = 0;
        for (String game : gameNames) gameList[i++] = game;
        output.writeObject(new GameListResult(message.userName(), gameList));
        output.flush();
        print("\nPlayer (" + message.userName() + ") game list updated");
        output.writeObject(new ChatMessage("Lobby", null, message.userName(), "Game List has been updated.\n"));
        output.flush();
    }

    void receiveUpdateGameMessage(UpdateGame message) throws IOException {
        for (Game game : games) {
            if (Objects.equals(game.getGameName(), message.gameName())) {
                if (Objects.equals(message.result(), "End")) {
                    System.out.println("clearing board for " + game.getGameName());
                    game.clearBoard();
                    game.changeStartingToken();
                    game.changeCurrentToStarting();
                    System.out.println(game.getBoardState());
                    output.writeObject(new UpdateGame(game.getGameName(), game.getUserTokens(), game.getCurrentToken(), game.getBoardState(), "End"));
                    output.flush();
                    if (game.getVsAI() && game.getCurrentToken() == 'X') {
                        output.writeObject(new MinimaxMoveSend(game.getGameName(), 'X', game.getBoardState()));
                        output.flush();
                    }
                } else if (Objects.equals(message.result(), "New")) {
                    output.writeObject(new UpdateGame(game.getGameName(), game.getUserTokens(), game.getCurrentToken(), game.getBoardState(), "New"));
                    output.flush();
                }
            }
        }
    }

    void sendChatMessage(ChatMessage message) throws IOException {
        if (Objects.equals(message.messageType(), "Connect")) {
            if (Objects.equals(message.gameName(), null)) {
                for (String user : lobby) {
                    if (!Objects.equals(user, message.userName())) {
                        output.writeObject(new ChatMessage("Lobby", null, user, message.message()));
                        output.flush();
                    }
                }
            } else {
                for (Game game : games) {
                    if (Objects.equals(game.getGameName(), message.gameName())) {
                        for (String user : game.getUsers().keySet()) {
                            if (!Objects.equals(user, message.userName())) {
                                output.writeObject(new ChatMessage("Board", game.getGameName(), user, message.message()));
                                output.flush();
                            }
                        }
                    }
                }
            }
        } else if (Objects.equals(message.messageType(), "Lobby")) {
            for (String user : lobby) {
                output.writeObject(new ChatMessage("Lobby", null, user, message.message()));
                output.flush();
            }
        } else if (Objects.equals(message.messageType(), "Board")) {
            {
                for (Game game : games) {
                    if (Objects.equals(game.getGameName(), message.gameName())) {
                        for (String user : game.getUsers().keySet()) {
                            output.writeObject(new ChatMessage("Board", game.getGameName(), user, message.message()));
                            output.flush();
                        }
                    }
                }
            }
        }
    }
}