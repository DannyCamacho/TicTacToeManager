package com.tictactoe.tictactoemanager;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Game {
    private final String gameName;
    private final boolean vsAI;
    private char startingToken;
    private char currentToken;
    private char [] boardState;
    private final Map<String, Character> userTokens;

    public Game(String gameName, boolean vsAI) {
        this.gameName = gameName;
        this.vsAI = vsAI;
        startingToken = 'O';
        currentToken = 'O';
        boardState = new char[9];
        userTokens = new HashMap<>();
    }

    public String getGameName() {
        return gameName;
    }

    public boolean getVsAI() {
        return vsAI;
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

    public String [] getUserTokens() {
        String [] users = new String[userTokens.size() * 2];

        AtomicInteger i = new AtomicInteger();
        userTokens.forEach((key, value) -> {
            users[i.intValue()] = key;
            i.incrementAndGet();
            users[i.intValue()] = String.valueOf(value);
            i.incrementAndGet();
        });

        return users;
    }

    public void addPlayer(String userName) {
            if (!userTokens.containsValue('O'))
                userTokens.put(userName, 'O');
            else if (!userTokens.containsValue('X'))
                userTokens.put(userName, 'X');
            else
                userTokens.put(userName, 'S');
    }

    public void removePlayer(String userName) {
        userTokens.remove(userName);
    }

    public void changeStartingToken() {
        startingToken = startingToken == 'X' ? 'O' : 'X';
    }

    public void changeCurrentToStarting() {
        currentToken = startingToken;
    }

    public void clearBoard() {
        for (int i = 0; i < 9; ++i) {
            boardState[i] = '\0';
        }
    }
}