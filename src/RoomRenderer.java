import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Handles rendering of a room, its walls, doors, and the player.
 */
public class RoomRenderer {
    private Canvas canvas;
    private GraphicsContext gc;
    private static final int TILE_SIZE = 32;
    private static final int ROOM_SIZE = 11; // Number of tiles per room (11x11 grid)
    private static final int DOOR_POSITION = 5; // Door is in the middle of the wall (11/2 = 5)
    
    public RoomRenderer(Canvas canvas){
        this.canvas = canvas;
        this.gc = canvas.getGraphicsContext2D();
        this.canvas.setWidth(ROOM_SIZE * TILE_SIZE);
        this.canvas.setHeight(ROOM_SIZE * TILE_SIZE);
    }

    /**
     * Renders the room, including floor, walls, doors, and the player.
     */
    public void renderRoom(Room room, double playerX, double playerY) {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Draw the floor
        Color floorColor = room.getType() == RoomType.START ? Color.LIGHTBLUE : 
                          room.getType() == RoomType.BOSS ? Color.DARKBLUE : 
                          Color.WHITE;
        gc.setFill(floorColor);
        gc.fillRect(1, 1, canvas.getWidth() - 1, canvas.getHeight() - 1);

        // Draw the walls
        Color wallColor = Color.YELLOW;
        gc.setFill(wallColor);
        gc.fillRect(0, 0, canvas.getWidth(), TILE_SIZE); // Top wall
        gc.fillRect(0, canvas.getHeight() - TILE_SIZE, canvas.getWidth(), TILE_SIZE); // Bottom wall
        gc.fillRect(0, 0, TILE_SIZE, canvas.getHeight()); // Left wall
        gc.fillRect(canvas.getWidth() - TILE_SIZE, 0, TILE_SIZE, canvas.getHeight()); // Right wall

        // Draw the doors (red if closed, green if open)
        Color doorColor = room.areDoorsClosed() ? Color.RED : Color.GREEN;
        for (Direction dir : room.getDirections()) {
            switch (dir) {
                case NORTH -> {
                    gc.setFill(doorColor);
                    gc.fillRect(DOOR_POSITION * TILE_SIZE, 0, TILE_SIZE, TILE_SIZE);
                }
                case SOUTH -> {
                    gc.setFill(doorColor);
                    gc.fillRect(DOOR_POSITION * TILE_SIZE, (ROOM_SIZE-1) * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
                case EAST -> {
                    gc.setFill(doorColor);
                    gc.fillRect((ROOM_SIZE-1) * TILE_SIZE, DOOR_POSITION * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
                case WEST -> {
                    gc.setFill(doorColor);
                    gc.fillRect(0, DOOR_POSITION * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }
        renderPlayer(playerX, playerY);
    }

    // Draws the player at the given coordinates
    public void renderPlayer(double playerX, double playerY){
        Color playerColor = Color.BLACK;
        gc.setFill(playerColor);
        int playerSize = TILE_SIZE/2;
        gc.fillOval(playerX - playerSize/2, playerY - playerSize/2, playerSize, playerSize);
    }

    // Returns the GraphicsContext for additional drawing (e.g. projectiles, enemies)
    public GraphicsContext getGraphicsContext() {
        return gc;
    }

    public int getHeight(){
        return ROOM_SIZE * TILE_SIZE;
    }
    public int getWidth(){
        return ROOM_SIZE * TILE_SIZE;
    }
}
