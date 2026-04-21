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
                  room.getType() == RoomType.ITEM ? Color.BEIGE :
                  Color.WHITE;
        gc.setFill(floorColor);
        gc.fillRect(offsetX, offsetY, tileSize * ROOM_SIZE, tileSize * ROOM_SIZE);

        renderWalls(room, tileSize, offsetX, offsetY);
        
        // Doors
        for (Direction dir : room.getDirections()) {
            Color doorColor;
            if (room.areDoorsClosed()) {
                doorColor = Color.RED;
            } else if (room.isDoorDirectionLocked(dir)) {
                doorColor = Color.BLUE;
            } else {
                doorColor = room.areDoorsClosed() ? Color.RED : Color.GREEN;
            }

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

    public void renderWalls(Room room, double tileSize, double offsetX, double offsetY){

        // Walls around the room
        Color wallColor = Color.YELLOW;
        gc.setFill(wallColor);
        gc.fillRect(offsetX, offsetY, tileSize * ROOM_SIZE, tileSize); // Haut
        gc.fillRect(offsetX, offsetY + tileSize * (ROOM_SIZE - 1), tileSize * ROOM_SIZE, tileSize); // Bas
        gc.fillRect(offsetX, offsetY, tileSize, tileSize * ROOM_SIZE); // Gauche
        gc.fillRect(offsetX + tileSize * (ROOM_SIZE - 1), offsetY, tileSize, tileSize * ROOM_SIZE); // Droite

        // rocks
        gc.setFill(Color.GRAY);
        for (Wall wall : room.getWalls()) {
            double wx = offsetX + wall.getX() * tileSize;
            double wy = offsetY + wall.getY() * tileSize;
            double wW = wall.getWidth() * tileSize;
            double wH = wall.getHeight() * tileSize;
            gc.fillRect(wx, wy, wW, wH);
        }
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
            case KEY -> gc.setFill(Color.GOLD);
        }
        gc.fillRect(rewardX - tileSize/4, rewardY - tileSize/4, tileSize/2, tileSize/2);
    }

    public void renderItem(ItemInstance item) {
        if (item == null || item.isCollected()) return;

        double width = canvas.getWidth();
        double height = canvas.getHeight();
        double tileSize = Math.min(width, height) / ROOM_SIZE;
        double offsetX = (width - tileSize * ROOM_SIZE) / 2;
        double offsetY = (height - tileSize * ROOM_SIZE) / 2;

        double itemX = offsetX + (item.getX() / (ROOM_SIZE * 32)) * (tileSize * ROOM_SIZE);
        double itemY = offsetY + (item.getY() / (ROOM_SIZE * 32)) * (tileSize * ROOM_SIZE);

        switch (item.getDefinition().getRarity()) {
            case COMMON -> gc.setFill(Color.LIGHTGREEN);
            case RARE -> gc.setFill(Color.DEEPSKYBLUE);
            case EPIC -> gc.setFill(Color.GOLD);
            case LEGENDARY -> gc.setFill(Color.CRIMSON);
        }
        gc.fillOval(itemX - tileSize / 3, itemY - tileSize / 3, 2 * tileSize / 3, 2 * tileSize / 3);
    }

    // Renders the trap door for boss rooms
    public void renderTrap(Trap trap) {
        if (trap == null || !trap.isVisible()) return;
        
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        final int ROOM_SIZE = 11;
        double tileSize = Math.min(width, height) / ROOM_SIZE;
        double offsetX = (width - tileSize * ROOM_SIZE) / 2;
        double offsetY = (height - tileSize * ROOM_SIZE) / 2;

        // Convertir les coordonnées du trap en pixels écran
        double trapX = offsetX + (trap.getX() / (ROOM_SIZE * TILE_SIZE)) * (tileSize * ROOM_SIZE);
        double trapY = offsetY + (trap.getY() / (ROOM_SIZE * TILE_SIZE)) * (tileSize * ROOM_SIZE);

        double outerRadius = tileSize * 0.45;
        double innerRadius = tileSize * 0.25;

        gc.setFill(Color.MEDIUMPURPLE);
        gc.fillOval(trapX - outerRadius, trapY - outerRadius, outerRadius * 2, outerRadius * 2);

        gc.setFill(Color.DARKVIOLET);
        gc.fillOval(trapX - innerRadius, trapY - innerRadius, innerRadius * 2, innerRadius * 2);

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeOval(trapX - outerRadius, trapY - outerRadius, outerRadius * 2, outerRadius * 2);
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
