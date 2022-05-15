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
    private final Set<String> userNames;
    private final Set<String> lobby;
    private final Set<String> gameNames;
    private final Map<String, Game> games;

    public TicTacToeManager(String hostname, int port, ManagerController controller) {
        this.hostname = hostname;
        this.port = port;
        this.controller = controller;
        userNames = new HashSet<>();
        lobby = new HashSet<>();
        gameNames = new HashSet<>();
        games = new HashMap<>();
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
        Game game = games.get(result.gameName());
        game.setBoardState(result.board());
        game.setCurrentToken(result.playerToken());
        if (!Objects.equals(result.result(), "N")) {
            game.updateGameHistory(result.result().charAt(0));
        }
        for (String user : game.getUserTokens().keySet()) {
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

    void addUserName(ServerConnection message) throws IOException {
        userNames.add(message.userName());
        lobby.add(message.userName());
        Platform.runLater(() -> controller.addClient(message.userName()));
        print("\nPlayer (" + message.userName() + ") has connected");
        sendChatMessage((new ChatMessage("ConnectL", "", message.userName(), message.userName() + " has joined the lobby.\n")));
    }

    void removeUserName(ServerConnection message) throws IOException {
        userNames.remove(message.userName());
        lobby.remove(message.userName());
        Platform.runLater(() -> controller.removeClient(message.userName()));
        print("\nPlayer (" + message.userName() + ") has disconnected");
        sendChatMessage((new ChatMessage("ConnectL", "", message.userName(), message.userName() + " has left the lobby.\n")));
    }

    void addGame(ConnectToGame message) throws IOException {
        for (String game : games.keySet()) {
            if (Objects.equals(game, message.gameName())) {
                addPlayerToGame(message);
                return;
            }
        }
        games.put(message.gameName(), new Game(message.gameName(), message.VsAI()));
        gameNames.add(message.gameName());
        Platform.runLater(() -> controller.addGame(message.gameName()));
        print("\nGame created: " + message.gameName());
        addPlayerToGame(message);
    }

    void removeGame(String gameName) {
        games.remove(gameName);
        gameNames.remove(gameName);
        Platform.runLater(() -> controller.removeGame(gameName));
        print("\nGame removed: " + gameName);
    }

    void addPlayerToGame(ConnectToGame message) throws IOException {
        output.writeObject(new ConnectToGame(message.gameName(), message.userName(), true, message.VsAI()));
        output.flush();
        Game game = games.get(message.gameName());
        game.addPlayer(message.userName());
        lobby.remove(message.userName());
        print("\nPlayer (" + message.userName() + ") has joined game: " + message.gameName());
        output.writeObject(new UpdateGame(message.gameName(), message.userName(), game.getUserToken(message.userName()), game.getBoardState(), "Initialize"));
        output.flush();
        output.writeObject(new ChatMessage("Board", message.gameName(), message.userName(), "Connected to Game " + message.gameName() + ".\n"));
        output.flush();
        sendChatMessage((new ChatMessage("ConnectB", message.gameName(), message.userName(), message.userName() + " has joined the game.\n")));
        output.writeObject(new UpdateGameHistory(game.getGameName(), message.userName(), game.getXodWins(), game.getGameHistory()));
        output.flush();
    }

    void removePlayerFromGame(ConnectToGame message) throws IOException {
        Game game = games.get(message.gameName());
        game.removePlayer(message.userName());
        lobby.add(message.userName());
        print("\nPlayer (" + message.userName() + ") has left game: " + message.gameName());
        sendChatMessage((new ChatMessage("ConnectB", message.gameName(), message.userName(), message.userName() + " has left the game.\n")));
    }

    void getGameList(GameListRequest message) throws IOException {
        String[] gameList = new String[gameNames.size()];
        int i = 0;
        for (String game : gameNames) {
            gameList[i++] = game;
        }
        output.writeObject(new GameListResult(message.userName(), gameList));
        output.flush();
        print("\nPlayer (" + message.userName() + ") game list updated");
        output.writeObject(new ChatMessage("Lobby", "", message.userName(), "Game List has been updated.\n"));
        output.flush();
    }

    void receiveUpdateGameMessage(UpdateGame message) throws IOException {
        Game game = games.get(message.gameName());
        if (Objects.equals(message.result(), "End")) {
            game.clearBoard();
            game.changeStartingToken();
            game.changeCurrentToStarting();

            for (String user : game.getUserTokens().keySet()) {
                output.writeObject(new UpdateGame(game.getGameName(), user, game.getCurrentToken(), game.getBoardState(), "End"));
                output.flush();
            }
        } else if (Objects.equals(message.result(), "New")) {
            for (String user : game.getUserTokens().keySet()) {
                output.writeObject(new UpdateGame(game.getGameName(), user, game.getCurrentToken(), game.getBoardState(), "N"));
                output.flush();
                if (game.getVsAI() && game.getCurrentToken() == 'X') {
                    output.writeObject(new MinimaxMoveSend(game.getGameName(), 'X', game.getBoardState()));
                    output.flush();
                }
            }
        }
    }

    void sendChatMessage(ChatMessage message) throws IOException {
        Game game = games.get(message.gameName());

        if (Objects.equals(message.messageType(), "ConnectL")) {
            for (String user : lobby) {
                if (!Objects.equals(user, message.userName())) {
                    output.writeObject(new ChatMessage("Lobby", "Lobby", user, message.message()));
                    output.flush();
                }
            }
        } else if (Objects.equals(message.messageType(), "ConnectB")) {
            for (String user : game.getUserTokens().keySet()) {
                if (!Objects.equals(user, message.userName())) {
                    output.writeObject(new ChatMessage("Board", game.getGameName(), user, message.message()));
                    output.flush();
                }
            }
        } else if (Objects.equals(message.messageType(), "Lobby")) {
            for (String user : lobby) {
                output.writeObject(new ChatMessage("Lobby", "Lobby", user, message.message()));
                output.flush();
            }
        } else if (Objects.equals(message.messageType(), "Board")) {
            {
                for (String user : game.getUserTokens().keySet()) {
                    output.writeObject(new ChatMessage("Board", game.getGameName(), user, message.message()));
                    output.flush();
                }
            }
        }
    }
}