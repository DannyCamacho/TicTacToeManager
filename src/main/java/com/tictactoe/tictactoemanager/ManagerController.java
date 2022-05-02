package com.tictactoe.tictactoemanager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

public class ManagerController {
    @FXML
    public Button startButton;
    @FXML
    public ListView<String> clientListView, gameListView;
    @FXML
    public TextArea ta;

    public void update(String message) {
        ta.appendText(message);
    }

    public void onStartButtonClicked() {
        TicTacToeManager manager = new TicTacToeManager("localhost", 8000, this);
        manager.execute();
        startButton.setVisible(false);
    }

    public void addClient(String user) {
        clientListView.getItems().add(user);
    }

    public void removeClient(String user) {
        clientListView.getItems().remove(user);
    }

    public void addGame(String game) {
        gameListView.getItems().add(game);
    }

    public void removeGame(String game) {
        gameListView.getItems().remove(game);
    }
}