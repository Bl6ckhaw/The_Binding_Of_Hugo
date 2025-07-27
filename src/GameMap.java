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
        for (int i = 0; i < GRID_SIZE; i++){
            for (int j = 0; j < GRID_SIZE; j++){
                if (i == 0 && j == 0){
                    grid[i][j] = new Room(RoomType.START, i, j);
                    startRoom = grid[i][j];
                } else if (i == GRID_SIZE - 1 && j == GRID_SIZE - 1) {
                    grid[i][j] = new Room(RoomType.BOSS, i, j);
                    bossRoom = grid[i][j];
                } else {
                    grid[i][j] = new Room(RoomType.NORMAL, i, j);
                    // Generate a random number of enemies for each normal room
                    int min = 2, max = 5;
                    int nbEnemies = min + (int)(Math.random() * (max - min + 1));
                    for (int n = 0; n < nbEnemies; n++) {
                        Enemy e = grid[i][j].generateRandomEnemy(projectileManager, this);
                        grid[i][j].addEnemy(e);
                    }
                }
                connectRooms(i, j);
            }
        }
    }

    // Adds doors between adjacent rooms
    private void connectRooms(int i, int j) {
        Room room = grid[i][j];
        // i = X (horizontal), j = Y (vertical)
        if (j > 0) room.addDoor(Direction.NORTH); // Connect to room above (j-1)
        if (j < GRID_SIZE - 1) room.addDoor(Direction.SOUTH); // Connect to room below (j+1)
        if (i > 0) room.addDoor(Direction.WEST); // Connect to room to the left (i-1)
        if (i < GRID_SIZE - 1) room.addDoor(Direction.EAST); // Connect to room to the right (i+1)
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
                // Player comes from the north, spawn near the south door
                spawnX = DOOR_POSITION * TILE_SIZE + TILE_SIZE / 2;
                spawnY = (ROOM_SIZE - 2) * TILE_SIZE; // Just before the south wall
            }
            case SOUTH -> {
                // Player comes from the south, spawn near the north door
                spawnX = DOOR_POSITION * TILE_SIZE + TILE_SIZE / 2;
                spawnY = TILE_SIZE * 2; // Just after the north wall
            }
            case EAST -> {
                // Player comes from the east, spawn near the west door
                spawnX = TILE_SIZE * 2; // Just after the west wall
                spawnY = DOOR_POSITION * TILE_SIZE + TILE_SIZE / 2;
            }
            case WEST -> {
                // Player comes from the west, spawn near the east door
                spawnX = (ROOM_SIZE - 2) * TILE_SIZE; // Just before the east wall
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
