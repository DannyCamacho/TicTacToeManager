package com.tictactoe.tictactoemanager;

import java.util.*;

public class Game {
    private final String gameName;
    private char startingToken;
    private char currentToken;
    private char [] boardState;
    private final ArrayList<String> userNames;
    private final Map<String, Character> userTokens;

    public Game(String gameName) {
        this.gameName = gameName;
        startingToken = 'O';
        currentToken = 'O';
        boardState = new char[9];
        userNames = new ArrayList<>();
        userTokens = new HashMap<>();
    }

    public String getGameName() {
        return gameName;
    }

    public char getStartingToken() {
        return startingToken;
    }

    public char getCurrentToken() {
        return currentToken;
    }

    public void setCurrentToken(char currentToken) {
        this.currentToken = currentToken;
    }

    public char[] getBoardState() {
        return boardState;
    }

    public void setBoardState(char[] boardState) {
        this.boardState = boardState;
    }

    public ArrayList<String> getUserNames() {
        return userNames;
    }

    public Map<String, Character> getUserTokens() {
        return userTokens;
    }

    public void addPlayer(String userName) {
        if (!userNames.contains(userName)) {
            userNames.add(userName);
            if (!userTokens.containsValue('O'))
                userTokens.put(userName, 'O');
            else if (!userTokens.containsValue('X'))
                userTokens.put(userName, 'X');
            else
                userTokens.put(userName, 'S');
        }
    }

    public void removePlayer(String userName) {
        userNames.remove(userName);
        userTokens.remove(userName);
    }

    public void changeStartingToken() {
        startingToken = startingToken == 'X' ? 'O' : 'X';
    }

    public void changeCurrentToken() {
        currentToken = startingToken;
    }

    public void clearBoard() {
        for (int i = 0; i < 9; ++i) {
            boardState[i] = '\0';
        }
    }
}