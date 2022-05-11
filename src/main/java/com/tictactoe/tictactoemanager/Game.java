package com.tictactoe.tictactoemanager;

import java.util.*;

public class Game {
    private final String gameName;
    private final boolean vsAI;
    private char startingToken;
    private char currentToken;
    private char [] boardState;
    private final Map<String, Character> userTokens;
    private final int [] xodWins;
    private final ArrayList<String> gameHistory;

    public Game(String gameName, boolean vsAI) {
        this.gameName = gameName;
        this.vsAI = vsAI;
        startingToken = 'O';
        currentToken = 'O';
        boardState = new char[9];
        userTokens = new HashMap<>();
        xodWins = new int[] { 0, 0, 0 };
        gameHistory = new ArrayList<>();
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

    public Map<String, Character> getUserTokens() {
        return userTokens;
    }

    public char getUserToken(String userName) {
        return userTokens.get(userName);
    }

    public String [] getXodWins() {
        String [] xod = new String[3];
        for (int i = 0; i < 3; ++i) {
            xod[i] = "" + xodWins[i];
        }
        return xod;
    }

    public String [] getGameHistory() {
        String [] history = new String[gameHistory.size()];
        for (int i = 0; i < gameHistory.size(); ++i) {
            history[i] = gameHistory.get(i);
        }
        return history;
    }

    public void addPlayer(String userName) {
            if (!userTokens.containsValue('O'))
                userTokens.put(userName, 'O');
            else if (!userTokens.containsValue('X') && !vsAI)
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
        boardState = new char[9];
    }

    public void updateGameHistory(char result) {
        String s;

        switch (result) {
            case 'X' -> { s = "\t\tPlayer X Win\n"; ++xodWins[0]; }
            case 'O' -> { s = "\t\tPlayer O Win\n"; ++xodWins[1]; }
            default -> { s = "\t\tDraw Game\n"; ++xodWins[2]; }
        }

        gameHistory.add((boardState[0] == 0 ? "  " : boardState[0]) + "  |  " +
                (boardState[1] == 0 ? "  " : boardState[1]) + "  |  " +
                (boardState[2] == 0 ? "  " : boardState[2]) + "\n" +
                (boardState[3] == 0 ? "  " : boardState[3]) + "  |  " +
                (boardState[4] == 0 ? "  " : boardState[4]) + "  |  " +
                (boardState[5] == 0 ? "  " : boardState[5]) + s +
                (boardState[6] == 0 ? "  " : boardState[6]) + "  |  " +
                (boardState[7] == 0 ? "  " : boardState[7]) + "  |  " +
                (boardState[8] == 0 ? "  " : boardState[8]));
    }
}