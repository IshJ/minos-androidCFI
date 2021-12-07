package de.tcrass.minos.model.factory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Random;

import de.tcrass.minos.model.Cell;
import de.tcrass.minos.model.Direction;
import de.tcrass.minos.model.Maze;

public class MazeFactory {

    private static Random random = new Random();

    public static Maze createMaze(GameMetrics gameMetrics) {
        Maze maze = new Maze(gameMetrics.getCols(), gameMetrics.getRows());

        List<Cell> unvisitedCells = new ArrayList<>(maze.getCells());
        Deque<Cell> stack = new ArrayDeque<>();

        // Randomly pick a cell and call it the "current cell"
        Cell currentCell = unvisitedCells.get(random.nextInt(unvisitedCells
                .size()));

        // Make the initial cell the current cell and mark it as visited
        currentCell.setVisited(true);
        unvisitedCells.remove(currentCell);

        // While there are unvisited cells...
        while (unvisitedCells.size() > 0) {
            Map<Direction, Cell> unvisitedNeighbours = getUnvisitedNeighbours(
                    maze, currentCell);
            // If the current cell has any neighbours which have not been
            // visited
            if (unvisitedNeighbours.size() > 0) {
                // Choose randomly one of the unvisited neighbours
                List<Direction> directions = new ArrayList<>(
                        unvisitedNeighbours.keySet());
                Direction direction = directions.get(random.nextInt(directions
                        .size()));
                Cell nextCell = maze.getNeighbour(currentCell, direction);
                // Push the current cell to the stack
                stack.push(currentCell);
                // Remove the wall between the current cell and the chosen cell
                maze.tearDownWall(currentCell, direction);
                // Make the chosen cell the current cell and mark it as visited
                currentCell = nextCell;
                currentCell.setVisited(true);
                unvisitedCells.remove(currentCell);
            }
            // Else if stack is not empty
            else if (stack.size() > 0) {
                // Pop a cell from the stack and make it the current cell
                currentCell = stack.pop();
            }
            // Else
            else {
                // Pick a random unvisited cell, make it the current cell and
                // mark it as visited
                currentCell = unvisitedCells.get(random.nextInt(unvisitedCells
                        .size()));
                currentCell.setVisited(true);
                unvisitedCells.remove(currentCell);
            }
        }

        return maze;
    }

    private static Map<Direction, Cell> getUnvisitedNeighbours(Maze maze,
            Cell cell) {
        Map<Direction, Cell> unvisitedNeighbours = maze.getNeighbours(cell);
        List<Direction> directions = new ArrayList<>(unvisitedNeighbours.keySet());
        for (Direction direction : directions) {
            if (unvisitedNeighbours.get(direction).isVisited()) {
                unvisitedNeighbours.remove(direction);
            }
        }
        return unvisitedNeighbours;
    }

}
