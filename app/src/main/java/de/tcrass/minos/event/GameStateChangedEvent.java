package de.tcrass.minos.event;

import de.tcrass.minos.model.GameState;

public class GameStateChangedEvent {

    private GameState gameState;

    public GameStateChangedEvent(GameState gameState) {
        this.gameState = gameState;
    }

    public GameState getGameState() {
        return gameState;
    }

}
