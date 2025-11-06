import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


public class GameMap {
    private static final int GRID_SIZE = 3; // Number of rooms per row/column (3x3 grid)
    private static final int TILE_SIZE = 32; // Size of each tile in pixels
    private static final int ROOM_SIZE = 11; // Number of tiles per room (11x11 grid)
    private static final int DOOR_POSITION = 5; // Door is in the middle of the wall (11/2 = 5)
    private Room[][] grid;
    private int playerX;
    private int playerY;
    private Room startRoom;
    private Room bossRoom;
    private Room currentRoom;
    private Room nextRoom;
    private ProjectileManager projectileManager;

    public GameMap(ProjectileManager projectileManager) {
        this.projectileManager = projectileManager;
        grid = new Room[GRID_SIZE][GRID_SIZE];
        initializeRooms();
        playerX = 0; // Starting position in the grid
        playerY = 0; // Starting position in the grid
        currentRoom = startRoom; // Set current room to start room
        nextRoom = null; // No next room at start
    }

    // Initializes all rooms and generates enemies for normal rooms
    private void initializeRooms() {
        // Create all rooms first
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (i == 0 && j == 0) {
                    grid[i][j] = new Room(RoomType.START, i, j);
                    startRoom = grid[i][j];
                } else if (i == GRID_SIZE - 1 && j == GRID_SIZE - 1) {
                    grid[i][j] = new Room(RoomType.BOSS, i, j);
                    bossRoom = grid[i][j];
                } else {
                    grid[i][j] = new Room(RoomType.NORMAL, i, j);

                    // Try to load a random saved layout and copy it INTO this room
                    try {
                        Path dir = Paths.get("saved_rooms");
                        if (Files.exists(dir) && Files.isDirectory(dir)) {
                            List<Path> files = Files.list(dir)
                                    .filter(Files::isRegularFile)
                                    .filter(p -> p.toString().toLowerCase().endsWith(".txt"))
                                    .collect(Collectors.toList());
                            if (!files.isEmpty()) {
                                Path chosen = files.get(new Random().nextInt(files.size()));
                                try {
                                    Room loaded = MapIO.loadRoom(chosen);
                                    // copy layout (walls) into the freshly created room
                                    grid[i][j].copyLayoutFrom(loaded);
                                } catch (IOException ex) {
                                    System.err.println("Failed to load prefab " + chosen + " : " + ex.getMessage());
                                }
                            }
                        }
                    } catch (IOException ex) {
                        System.err.println("Could not list saved_rooms: " + ex.getMessage());
                    }
                }
            }
        }

        // THEN set references only on non-null rooms
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (grid[i][j] != null) {
                    grid[i][j].setReferences(this, projectileManager);
                }
            }
        }

        // Connect rooms (doors) after all rooms exist and have references
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                connectRooms(i, j);
            }
        }
    }

    private void connectRooms(int i, int j) {
        Room room = grid[i][j];
        if (room == null) return;

        // helper - returns true if the tile (tx,ty) in room r is blocked by a wall
        java.util.function.BiPredicate<Room, int[]> isBlocked = (r, coords) -> {
            if (r == null) return true; // no room => treat as blocked
            int tx = coords[0], ty = coords[1];
            for (Wall w : r.getWalls()) {
                if (w.blocksPosition(tx, ty)) return true;
            }
            return false;
        };

        // NORTH neighbor (same i, j-1)
        if (j > 0) {
            Room neigh = grid[i][j - 1];
            if (neigh != null) {
                int[] doorTileRoom = new int[]{DOOR_POSITION, 0};
                int[] doorTileNeigh = new int[]{DOOR_POSITION, ROOM_SIZE - 1};
                if (!isBlocked.test(room, doorTileRoom) && !isBlocked.test(neigh, doorTileNeigh)) {
                    if (!room.hasDoor(Direction.NORTH)) room.addDoor(Direction.NORTH);
                    if (!neigh.hasDoor(Direction.SOUTH)) neigh.addDoor(Direction.SOUTH);
                }
            }
        }

        // SOUTH neighbor (same i, j+1)
        if (j < GRID_SIZE - 1) {
            Room neigh = grid[i][j + 1];
            if (neigh != null) {
                int[] doorTileRoom = new int[]{DOOR_POSITION, ROOM_SIZE - 1};
                int[] doorTileNeigh = new int[]{DOOR_POSITION, 0};
                if (!isBlocked.test(room, doorTileRoom) && !isBlocked.test(neigh, doorTileNeigh)) {
                    if (!room.hasDoor(Direction.SOUTH)) room.addDoor(Direction.SOUTH);
                    if (!neigh.hasDoor(Direction.NORTH)) neigh.addDoor(Direction.NORTH);
                }
            }
        }

        // EAST neighbor (i+1, same j)
        if (i < GRID_SIZE - 1) {
            Room neigh = grid[i + 1][j];
            if (neigh != null) {
                int[] doorTileRoom = new int[]{ROOM_SIZE - 1, DOOR_POSITION};
                int[] doorTileNeigh = new int[]{0, DOOR_POSITION};
                if (!isBlocked.test(room, doorTileRoom) && !isBlocked.test(neigh, doorTileNeigh)) {
                    if (!room.hasDoor(Direction.EAST)) room.addDoor(Direction.EAST);
                    if (!neigh.hasDoor(Direction.WEST)) neigh.addDoor(Direction.WEST);
                }
            }
        }

        // WEST neighbor (i-1, same j)
        if (i > 0) {
            Room neigh = grid[i - 1][j];
            if (neigh != null) {
                int[] doorTileRoom = new int[]{0, DOOR_POSITION};
                int[] doorTileNeigh = new int[]{ROOM_SIZE - 1, DOOR_POSITION};
                if (!isBlocked.test(room, doorTileRoom) && !isBlocked.test(neigh, doorTileNeigh)) {
                    if (!room.hasDoor(Direction.WEST)) room.addDoor(Direction.WEST);
                    if (!neigh.hasDoor(Direction.EAST)) neigh.addDoor(Direction.EAST);
                }
            }
        }
    }

    // Checks if the player can move in the given direction (is there a door?)
    public boolean canMoveTo(Direction dir){
        return getCurrentRoom().hasDoor(dir);
    }

    // Moves the player in the given direction if possible
    public void movePlayer(Direction dir){
        if (canMoveTo(dir)) {
            switch (dir) {
                case NORTH -> playerY--;
                case SOUTH -> playerY++;
                case EAST -> playerX++;
                case WEST -> playerX--;
            }
        } else {
            throw new IllegalArgumentException("Cannot move in that direction");
        }
    }

    // Checks if the player is near a door and returns the direction, or null if not near any door
    public Direction isPlayerNearDoor(double playerPixelX, double playerPixelY) {
        currentRoom = getCurrentRoom();

        // Check each direction that has a door
        for (Direction dir : currentRoom.getDirections()) {
            switch (dir) {
                case NORTH -> {
                    if (playerPixelY <= TILE_SIZE * 2 &&
                        playerPixelX >= DOOR_POSITION * TILE_SIZE && 
                        playerPixelX <= (DOOR_POSITION + 1) * TILE_SIZE) {
                        return Direction.NORTH;
                    }
                }
                case SOUTH -> {
                    if (playerPixelY >= (ROOM_SIZE - 2) * TILE_SIZE &&
                        playerPixelX >= DOOR_POSITION * TILE_SIZE && 
                        playerPixelX <= (DOOR_POSITION + 1) * TILE_SIZE) {
                        return Direction.SOUTH;
                    }
                }
                case EAST -> {
                    if (playerPixelX >= (ROOM_SIZE - 2) * TILE_SIZE &&
                        playerPixelY >= DOOR_POSITION * TILE_SIZE && 
                        playerPixelY <= (DOOR_POSITION + 1) * TILE_SIZE) {
                        return Direction.EAST;
                    }
                }
                case WEST -> {
                    if (playerPixelX <= TILE_SIZE * 2 &&
                        playerPixelY >= DOOR_POSITION * TILE_SIZE && 
                        playerPixelY <= (DOOR_POSITION + 1) * TILE_SIZE) {
                        return Direction.WEST;
                    }
                }
            }
        }
        return null; // No door nearby
    }

    // Sets the next room based on the direction the player is moving
    public void setNextRoom(Direction direction) {
        int newX = playerX;
        int newY = playerY;
        switch (direction) {
            case NORTH -> newY--;
            case SOUTH -> newY++;
            case EAST -> newX++;
            case WEST -> newX--;
        }
        
        // Check if the new coordinates are valid
        if (newX >= 0 && newX < GRID_SIZE && newY >= 0 && newY < GRID_SIZE) {
            nextRoom = grid[newX][newY];
        } else {
            nextRoom = null; // No valid room
        }
    }

    // Resets the next room (used after switching rooms)
    public void resetNextRoom() {
        nextRoom = null;
    }

    // Returns true if the player has exited the current room boundaries
    public boolean hasPlayerExitedRoom(double playerPixelX, double playerPixelY) {
        return playerPixelX < TILE_SIZE || 
               playerPixelX > (ROOM_SIZE - 1) * TILE_SIZE || 
               playerPixelY < TILE_SIZE || 
               playerPixelY > (ROOM_SIZE - 1) * TILE_SIZE;
    }

    // Switches to the next room and updates player coordinates
    public void switchToNextRoom() {
        if (nextRoom != null) {
            currentRoom = nextRoom;
            playerX = currentRoom.getX();
            playerY = currentRoom.getY();
            nextRoom = null; // Reset next room after switching
        } else {
            throw new IllegalStateException("No next room set");
        }
    }

    /**
     * Calculates the spawn position of the player in the new room.
     * @param fromDirection the direction from which the player is coming
     * @return an array [x, y] with the pixel coordinates
     */
    public int[] getPlayerSpawnPosition(Direction fromDirection) {
        int spawnX, spawnY;
        
        switch (fromDirection) {
            case NORTH -> {
                // Player comes from the north, spawn près de la porte sud (plus proche)
                spawnX = DOOR_POSITION * TILE_SIZE + TILE_SIZE / 2;
                spawnY = (ROOM_SIZE - 1) * TILE_SIZE; // Plus proche : -3 au lieu de -2
            }
            case SOUTH -> {
                // Player comes from the south, spawn près de la porte nord (plus proche)
                spawnX = DOOR_POSITION * TILE_SIZE + TILE_SIZE / 2;
                spawnY = TILE_SIZE ; // Plus proche : 3 au lieu de 2
            }
            case EAST -> {
                // Player comes from the east, spawn près de la porte ouest (plus proche)
                spawnX = TILE_SIZE ; // Plus proche : 3 au lieu de 2
                spawnY = DOOR_POSITION * TILE_SIZE + TILE_SIZE / 2;
            }
            case WEST -> {
                // Player comes from the west, spawn près de la porte est (plus proche)
                spawnX = (ROOM_SIZE - 1) * TILE_SIZE; // Plus proche : -3 au lieu de -2
                spawnY = DOOR_POSITION * TILE_SIZE + TILE_SIZE / 2;
            }
            default -> {
                // Default position (center of the room)
                spawnX = 5 * TILE_SIZE + TILE_SIZE / 2;
                spawnY = 5 * TILE_SIZE + TILE_SIZE / 2;
            }
        }
        
        return new int[]{spawnX, spawnY};
    }

    // Returns the current room object
    public Room getCurrentRoom(){
        return grid[playerX][playerY];
    }

    // Returns the next room object (if set)
    public Room getNextRoom(){
        return nextRoom;
    }

    // Returns the height of a room in pixels
    public int getHeight() {
        return ROOM_SIZE * TILE_SIZE;
    }

    // Returns the width of a room in pixels
    public int getWidth() {
        return ROOM_SIZE * TILE_SIZE;
    }

}
