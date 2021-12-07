package de.tcrass.minos.controller;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.tcrass.minos.R;
import de.tcrass.minos.model.Cell;
import de.tcrass.minos.model.Direction;
import de.tcrass.minos.model.Game;
import de.tcrass.minos.model.GameState;
import de.tcrass.minos.view.Coords;
import de.tcrass.minos.view.GameView;
import de.tcrass.minos.view.render.GameRenderer;
import de.tcrass.minos.event.GameStateChangedEvent;

public class GameController {

    private static final String TAG = GameController.class.getSimpleName();

    private Context context;
    private Game game;
    private GameView gameView;

    private Vibrator vibrator;

    private boolean isDragging = false;
    private Coords screenDragStartPos;
    private Coords playerDragStartPos;
    private Coords lastPlayerDragPos;
    private int dragPointerId;

    private MediaPlayer startingPlayer;
    private MediaPlayer finishedPlayer;
    
    public GameController(Context context, Game game, GameView gameView) {
        this.context = context;
        this.game = game;
        this.gameView = gameView;

        this.vibrator = (Vibrator) context
                .getSystemService(Context.VIBRATOR_SERVICE);
        
        startingPlayer = MediaPlayer.create(context, R.raw.starting);
        finishedPlayer = MediaPlayer.create(context, R.raw.finished);
        
    }

    /* --- Player drag'n'drop -------------------------------------- */

    public boolean handleDrag(View view, MotionEvent ev) {

        if (game.getState() == GameState.PLAYING) {
            final int action = ev.getAction();

            switch (action) {
            case MotionEvent.ACTION_DOWN: {
                int pointerIndex = ev.getPointerId(action);
                float x = ev.getX(pointerIndex);
                float y = ev.getY(pointerIndex);
                if (playerContainsPoint(x, y)) {
                    isDragging = true;
                    dragPointerId = ev.getPointerId(0);
                    screenDragStartPos = new Coords(x, y);
                    playerDragStartPos = createBracketedMazeCoords(game
                            .getPlayer().getX(), game.getPlayer().getY());
                    lastPlayerDragPos = playerDragStartPos;
                    Log.i(TAG,
                            String.format(
                                    "Start dragging at screenDragStart=%s, playerDragStart=%s",
                                    screenDragStartPos, playerDragStartPos));
                    performInitDragFeedback();
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (isDragging) {
                    int pointerIndex = ev.findPointerIndex(dragPointerId);
                    float x = ev.getX(pointerIndex);
                    float y = ev.getY(pointerIndex);
                    Coords playerDelta = Coords.toMazeCoords(game.getMetrics(),
                            x - screenDragStartPos.getX(), y
                                    - screenDragStartPos.getY());
                    Coords playerDragTargetPos = createBracketedMazeCoords(
                            playerDragStartPos.getX() + playerDelta.getX(),
                            playerDragStartPos.getY() + playerDelta.getY());
                    Log.i(TAG,
                            String.format(
                                    "Dragging to x=%s, y=%s, playerDragPos=%s, playerDelta=%s",
                                    x, y, playerDragTargetPos, playerDelta));

                    List<Cell> route = findRoute(lastPlayerDragPos,
                            playerDragTargetPos);
                    Coords newPlayerDragPos = lastPlayerDragPos;
                    if (route.size() > 0) {
                        Cell dest = route.get(route.size() - 1);
                        newPlayerDragPos = createBracketedMazeCoords(game
                                .getMaze().getCol(dest),
                                game.getMaze().getRow(dest));
                    }
                    Cell playerCell = game.getMaze().getCell(
                            newPlayerDragPos.getCol(),
                            newPlayerDragPos.getRow());
                    Set<Direction> availableDirections = game.getMaze()
                            .getConnectedNeighbours(playerCell).keySet();
                    newPlayerDragPos = createConstrainedMazeCoords(
                            newPlayerDragPos, playerDragTargetPos,
                            availableDirections);
                    game.getPlayer().setX(newPlayerDragPos.getX());
                    game.getPlayer().setY(newPlayerDragPos.getY());

                    if (hasPlayerReachedDestination()) {
                        game.getPlayer().moveTo(game.getMetrics().getDestinationPosition());
                        setGameState(GameState.FINISHED);
                    } else {
                        // Perform "hit a wall" feedback?
                    }

                    gameView.invalidate();
                    lastPlayerDragPos = newPlayerDragPos;
                }

                break;
            }
            case MotionEvent.ACTION_UP: {
                isDragging = false;
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                isDragging = false;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = ev.getActionIndex();
                final int pointerId = ev.getPointerId(pointerIndex);

                if (pointerId == dragPointerId) {
                    isDragging = false;
                }
                break;
            }
            }

        }

        return true;
    }

    private boolean hasPlayerReachedDestination() {
        return game.getMetrics().cellContains(game.getMetrics().getDestinationPosition(), game.getPlayer().getX(), game.getPlayer().getY());
    }

    private List<Cell> findRoute(Coords start, Coords dest) {
        Cell currentCell = game.getMaze().getCell(start.getCol(),
                start.getRow());
        Cell destCell = game.getMaze().getCell(dest.getCol(), dest.getRow());
        List<Cell> route = new ArrayList<>();
        int dCols = dest.getCol() - start.getCol();
        int dRows = dest.getRow() - start.getRow();
        Log.i(TAG, String.format("findRoute: dCols=%s, dRows=%s", dCols, dRows));

        List<Direction> queryDirections = getQueryDirections(dCols, dRows);
        Map<Direction, Cell> candidates = game.getMaze()
                .getConnectedNeighbours(currentCell, queryDirections);

        while (currentCell != destCell && candidates.size() > 0) {
            currentCell = null;
            for (Direction queryDirection : queryDirections) {
                Log.i(TAG, String.format("  queryDirection=%s", queryDirection));
                if (candidates.containsKey(queryDirection)) {
                    currentCell = candidates.get(queryDirection);
                    route.add(currentCell);
                    dCols = dest.getCol() - game.getMaze().getCol(currentCell);
                    dRows = dest.getRow() - game.getMaze().getRow(currentCell);
                    queryDirections = getQueryDirections(dCols, dRows);
                    candidates = game.getMaze().getConnectedNeighbours(
                            currentCell, queryDirections);
                    break;
                }
            }
        }
        return route;
    }

    private List<Direction> getQueryDirections(int dCols, int dRows) {
        List<Direction> queryDirections = new ArrayList<>();
        if (Math.abs(dCols) > Math.abs(dRows)) {
            queryDirections.add(dCols > 0 ? Direction.EAST : Direction.WEST);
            if (dRows > 0) {
                queryDirections.add(Direction.SOUTH);
            } else if (dRows < 0) {
                queryDirections.add(Direction.NORTH);
            }
        } else if (dRows != 0) {
            queryDirections.add(dRows > 0 ? Direction.SOUTH : Direction.NORTH);
            if (dCols > 0) {
                queryDirections.add(Direction.EAST);
            } else if (dCols < 0) {
                queryDirections.add(Direction.WEST);
            }
        }
        return queryDirections;
    }

    /* --- Coords utils -------------------------------------------- */

    private boolean playerContainsPoint(float x, float y) {
        Coords mazeCoords = Coords.toMazeCoords(game.getMetrics(), x, y);
        float dx = game.getPlayer().getX() - mazeCoords.getX() + 0.5f;
        float dy = game.getPlayer().getY() - mazeCoords.getY() + 0.5f;
        return Math.sqrt(dx * dx + dy * dy) <= GameRenderer.REL_PLAYER_SIZE;
    }

    private Coords createBracketedMazeCoords(float col, float row) {
        if (col < 0) {
            col = 0f;
        } else if (col > game.getMaze().getCols() - 1) {
            col = game.getMaze().getCols() - 1;
        }
        if (row < 0) {
            row = 0f;
        } else if (row > game.getMaze().getRows() - 1) {
            row = game.getMaze().getRows() - 1;
        }
        return new Coords(col, row);
    }

    private Coords createConstrainedMazeCoords(Coords cellCoords,
            Coords pullCoords, Set<Direction> availableDirections) {

        float pullCol = pullCoords.getX() - cellCoords.getCol();
        float pullRow = pullCoords.getY() - cellCoords.getRow();
        float dCol = pullCol;
        float dRow = pullRow;
        if (!availableDirections.contains(pullCol > 0 ? Direction.EAST
                : Direction.WEST)) {
            dCol = 0;
        }
        if (!availableDirections.contains(pullRow > 0 ? Direction.SOUTH
                : Direction.NORTH)) {
            dRow = 0;
        }
        if (Math.abs(dCol) > Math.abs(dRow)) {
            dRow = 0;
        } else {
            dCol = 0;
        }
        return createBracketedMazeCoords(cellCoords.getCol() + dCol,
                cellCoords.getRow() + dRow);
    }

    /* --- Game state handling ------------------------------------- */

    public void setGameState(GameState state) {

        switch (game.getState()) {
        case READY:
            switch (state) {
            case PLAYING:
                performGameStartedFeedback();
                break;
            }
            break;
        case PLAYING:
            switch (state) {
            case FINISHED:
                performGameFinishedFeedback();
                break;
            }
            break;
        }

        game.setState(state);
        gameView.invalidate();

        EventBus.getDefault().post(new GameStateChangedEvent(state));
    }

    /* --- Feedback handling --------------------------------------- */

    private void performInitDragFeedback() {
        vibrator.vibrate(100);
    }

    private void performWallHitFeedback() {
        vibrator.vibrate(20);
    }

    private void performGameStartedFeedback() {
        playAudio(startingPlayer);
        vibrator.vibrate(200);
    }

    private void performGameFinishedFeedback() {
        playAudio(finishedPlayer);
        vibrator.vibrate(200);
    }

    private void playAudio(MediaPlayer player) {
        if (player.isPlaying()) {
            player.stop();
            player.seekTo(0);
        }
        player.start();
    }
    
}
