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
    }

    /**
     * Renders the room, including floor, walls, doors, and the player.
     */
    public void renderRoom(Room room, double playerX, double playerY) {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // change background color
        gc.setFill(Color.DARKGRAY); 
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        double width = canvas.getWidth();
        double height = canvas.getHeight();

        // Tile size to keep the room square and fill the screen
        double tileSize = Math.min(width, height) / ROOM_SIZE;

        // Offset to center the room
        double offsetX = (width - tileSize * ROOM_SIZE) / 2;
        double offsetY = (height - tileSize * ROOM_SIZE) / 2;

        // Floor (centered square)
        Color floorColor = room.getType() == RoomType.START ? Color.LIGHTBLUE : 
                          room.getType() == RoomType.BOSS ? Color.DARKBLUE : 
                          Color.WHITE;
        gc.setFill(floorColor);
        gc.fillRect(offsetX, offsetY, tileSize * ROOM_SIZE, tileSize * ROOM_SIZE);

        // Walls
        Color wallColor = Color.YELLOW;
        gc.setFill(wallColor);
        gc.fillRect(offsetX, offsetY, tileSize * ROOM_SIZE, tileSize); // Haut
        gc.fillRect(offsetX, offsetY + tileSize * (ROOM_SIZE - 1), tileSize * ROOM_SIZE, tileSize); // Bas
        gc.fillRect(offsetX, offsetY, tileSize, tileSize * ROOM_SIZE); // Gauche
        gc.fillRect(offsetX + tileSize * (ROOM_SIZE - 1), offsetY, tileSize, tileSize * ROOM_SIZE); // Droite

        // Doors
        Color doorColor = room.areDoorsClosed() ? Color.RED : Color.GREEN;
        for (Direction dir : room.getDirections()) {
            switch (dir) {
                case NORTH -> {
                    gc.setFill(doorColor);
                    gc.fillRect(offsetX + DOOR_POSITION * tileSize, offsetY, tileSize, tileSize);
                }
                case SOUTH -> {
                    gc.setFill(doorColor);
                    gc.fillRect(offsetX + DOOR_POSITION * tileSize, offsetY + (ROOM_SIZE - 1) * tileSize, tileSize, tileSize);
                }
                case EAST -> {
                    gc.setFill(doorColor);
                    gc.fillRect(offsetX + (ROOM_SIZE - 1) * tileSize, offsetY + DOOR_POSITION * tileSize, tileSize, tileSize);
                }
                case WEST -> {
                    gc.setFill(doorColor);
                    gc.fillRect(offsetX, offsetY + DOOR_POSITION * tileSize, tileSize, tileSize);
                }
            }
        }
        // Adapt player position to room scale
        renderPlayer(offsetX + (playerX / (ROOM_SIZE * 32)) * (tileSize * ROOM_SIZE),
                     offsetY + (playerY / (ROOM_SIZE * 32)) * (tileSize * ROOM_SIZE),
                     tileSize, tileSize);
    }

    // Draws the player at the given coordinates
    public void renderPlayer(double playerX, double playerY, double tileWidth, double tileHeight){
        Color playerColor = Color.BLACK;
        gc.setFill(playerColor);
        double playerSize = Math.min(tileWidth, tileHeight) / 2;
        gc.fillOval(playerX - playerSize/2, playerY - playerSize/2, playerSize, playerSize);
    }

    // Renders rewards (e.g. hearts, coins) at the specified coordinates
    public void renderRewards(Reward rewards) {
        if (rewards == null) return;
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        int ROOM_SIZE = 11;
        double tileSize = Math.min(width, height) / ROOM_SIZE;
        double offsetX = (width - tileSize * ROOM_SIZE) / 2;
        double offsetY = (height - tileSize * ROOM_SIZE) / 2;

        // Position reward en pixels salle
        double rewardX = offsetX + (rewards.getX() / (ROOM_SIZE * 32)) * (tileSize * ROOM_SIZE);
        double rewardY = offsetY + (rewards.getY() / (ROOM_SIZE * 32)) * (tileSize * ROOM_SIZE);

        switch (rewards.getType()) {
            case HEALTH -> gc.setFill(Color.RED);
            case DAMAGE -> gc.setFill(Color.BLUE);
            case SPEED -> gc.setFill(Color.GREEN);
            case TEARS_SIZE -> gc.setFill(Color.PURPLE);
        }
        gc.fillRect(rewardX - tileSize/4, rewardY - tileSize/4, tileSize/2, tileSize/2);
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
