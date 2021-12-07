package de.tcrass.minos.view;

import de.tcrass.minos.model.factory.GameMetrics;

public class Coords {
    private float x;
    private float y;

    public Coords(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getCol() {
        return Math.round(x);
    }

    public int getRow() {
        return Math.round(y);
    }

    @Override
    public  String toString() {
        return String.format("Coords(col=%s, row=%s, x=%s, y=%s)", getCol(), getRow(), x, y);
    }
    
    public static Coords toMazeCoords(GameMetrics gameMetrics, float x, float y) {
        float col = (x - (float) gameMetrics.getWallThickness())
                / (float) gameMetrics.getCellSize();
        float row = (y - (float) gameMetrics.getWallThickness())
                / (float) gameMetrics.getCellSize();
        return new Coords(col, row);
    }
    
    public static Coords toScreenCoords(GameMetrics gameMetrics, float col, float row) {
        float x = (float)gameMetrics.getWallThickness() + col * (float)gameMetrics.getCellSize();
        float y = (float)gameMetrics.getWallThickness() + row * (float)gameMetrics.getCellSize();
        return new Coords(x, y);
    }
    
    public boolean isInSameCellAs(Coords other) {
        return this.getCol() == other.getCol() && this.getRow() == other.getRow();
    }

}
