package de.tcrass.minos.model.factory;

import de.tcrass.minos.model.Game;
import de.tcrass.minos.model.GameState;
import de.tcrass.minos.model.Maze;

public class GameFactory {

    private static final String TAG = GameFactory.class.getSimpleName();
    
    public static Game createGame(GameMetrics gameMetrics) {
        
        Game game = new Game(gameMetrics);
        Maze maze = MazeFactory.createMaze(gameMetrics);
        game.setMaze(maze);
        game.setState(GameState.READY);
        return game;
    }
}
