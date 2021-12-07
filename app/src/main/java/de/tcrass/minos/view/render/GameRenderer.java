package de.tcrass.minos.view.render;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

import de.tcrass.minos.R;
import de.tcrass.minos.model.Cell;
import de.tcrass.minos.model.Game;
import de.tcrass.minos.model.GameState;

public class GameRenderer {

    private static final String TAG = GameRenderer.class.getSimpleName();

    public static final float REL_DESTINATION_SIZE = 0.7f;
    public static final float REL_PLAYER_SIZE = 0.9f;

    private Context context;
    private Game game;

    private Paint wallPaint;
    private Paint destinationPaint;
    private Paint overlayPaint;
    
    public GameRenderer(Context context, Game game) {
        this.context = context;
        this.game = game;
        
        wallPaint = new Paint();
        wallPaint.setStyle(Paint.Style.FILL);
        wallPaint.setColor(context.getResources().getColor(
                R.color.maze_wall));

        destinationPaint = new Paint();
        destinationPaint.setStyle(Paint.Style.STROKE);
        destinationPaint.setStrokeWidth(2 * game.getMetrics()
                .getWallThickness());
        destinationPaint.setColor(context.getResources().getColor(
                R.color.destination));
        
        overlayPaint = new Paint();
        overlayPaint.setStyle(Paint.Style.FILL);
        overlayPaint.setColor(context.getResources().getColor(R.color.overlay));
        
    }

    public void render(Canvas canvas) {
        Log.d(TAG, "render");

        renderWalls(canvas);
        renderDestination(canvas);
        renderPlayer(canvas);
    }

    private void renderWalls(Canvas canvas) {
        int wallThickness = game.getMetrics().getWallThickness();
        int cellSize = game.getMetrics().getCellSize();

        RenderUtils.drawWalls(canvas, 15, 0, 0, canvas.getWidth() - 1,
                canvas.getHeight() - 1, wallThickness, wallPaint);

        for (int row = 0; row < game.getMaze().getRows(); row++) {
            for (int col = 0; col < game.getMaze().getCols(); col++) {
                int left = wallThickness + cellSize * col;
                int top = wallThickness + cellSize * row;
                Cell cell = game.getMaze().getCell(col, row);
                RenderUtils.drawWalls(canvas, cell.getWalls(), left, top,
                        cellSize, cellSize, wallThickness, wallPaint);
            }
        }
    }

    private void renderDestination(Canvas canvas) {
        int cx = Math.round(game.getMetrics().getWallThickness()
                + (float) game.getMetrics().getCellSize()
                * (game.getMetrics().getDestinationPosition().getCol() + 0.5f));
        int cy = Math.round(game.getMetrics().getWallThickness()
                + (float) game.getMetrics().getCellSize()
                * (game.getMetrics().getDestinationPosition().getRow() + 0.5f));
        int r = Math.round(REL_DESTINATION_SIZE
                * (game.getMetrics().getCellSize() / 2f - game.getMetrics()
                        .getWallThickness()));

        canvas.drawLine(cx - r, cy - r, cx + r, cy + r, destinationPaint);
        canvas.drawLine(cx - r, cy + r, cx + r, cy - r, destinationPaint);
    }

    private void renderPlayer(Canvas canvas) {
        Paint playerPaint = new Paint();
        playerPaint.setStyle(Paint.Style.FILL);
        playerPaint.setColor(context.getResources().getColor(
                game.getState() == GameState.FINISHED ? R.color.player_finished
                        : R.color.player));

        int cx = Math.round(game.getMetrics().getWallThickness()
                + (float) game.getMetrics().getCellSize()
                * (game.getPlayer().getX() + 0.5f));
        int cy = Math.round(game.getMetrics().getWallThickness()
                + (float) game.getMetrics().getCellSize()
                * (game.getPlayer().getY() + 0.5f));
        int r = Math.round(REL_PLAYER_SIZE
                * (game.getMetrics().getCellSize() / 2f - game.getMetrics()
                        .getWallThickness()));

        canvas.drawCircle(cx, cy, r, playerPaint);
    }

    public void renderOverlay(Canvas canvas) {
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), overlayPaint);
    }

}
