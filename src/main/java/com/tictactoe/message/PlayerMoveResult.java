package com.tictactoe.message;

import java.io.Serializable;

public record PlayerMoveResult(String gameName, char playerToken, String result, char [] board) implements Serializable {}