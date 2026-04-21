import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


public class GameMap {
    private static final int GRID_SIZE = 8; // Number of rooms per row/column (3x3 grid)
    private static final int MIN_ROOMS = 5;
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
        playerX = startRoom.getX();
        playerY = startRoom.getY();
        currentRoom = startRoom; // Set current room to start room
        nextRoom = null; // No next room at start
    }

    // Initializes all rooms and generates enemies for normal rooms
    private void initializeRooms() {
        clearLayoutDebugFile();
        Random rng = new Random();
        int targetRooms = 10 + rng.nextInt(5) + GameApp.currentLevel; // 10..14

        int startX;
        int startY;
        int randomStart = rng.nextInt(4);
        switch (randomStart) {
            case 0 -> {
                startX = 3;
                startY = 3;
            }
            case 1 -> {
                startX = 4;
                startY = 3;
            }
            case 2 -> {
                startX = 3;
                startY = 4;
            }
            default -> {
                startX = 4;
                startY = 4;
            }
        }

        startRoom = new Room(RoomType.START, startX, startY);
        grid[startX][startY] = startRoom;

        int[][] distanceFromStart = new int[GRID_SIZE][GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                distanceFromStart[i][j] = -1;
            }
        }
        distanceFromStart[startX][startY] = 0;

        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startX, startY});
        List<int[]> endRooms = new ArrayList<>();
        int roomsCount = 1;

        while (!queue.isEmpty() && roomsCount < targetRooms) {
            int[] current = queue.poll();
            int cx = current[0];
            int cy = current[1];
            int currentDist = distanceFromStart[cx][cy];
            boolean isStartRoom = (cx == startX && cy == startY);

            List<int[]> neighbors = new ArrayList<>();
            neighbors.add(new int[]{cx, cy - 1});
            neighbors.add(new int[]{cx, cy + 1});
            neighbors.add(new int[]{cx + 1, cy});
            neighbors.add(new int[]{cx - 1, cy});
            Collections.shuffle(neighbors, rng);

            int createdFromCurrent = 0;

            for (int[] n : neighbors) {
                if (roomsCount >= targetRooms) break;

                int nx = n[0];
                int ny = n[1];

                boolean forceFirstExpansion = isStartRoom && roomsCount == 1 && createdFromCurrent == 0;
                if (!forceFirstExpansion && rng.nextDouble() < 0.5) continue;
                if (!isInsideGrid(nx, ny)) continue;
                if (grid[nx][ny] != null) continue;
                if (countOccupiedNeighbors(nx, ny) >= 2) continue;

                Room normalRoom = new Room(RoomType.NORMAL, nx, ny);
                loadRandomLayoutInto(normalRoom, rng);
                grid[nx][ny] = normalRoom;
                distanceFromStart[nx][ny] = currentDist + 1;
                queue.add(new int[]{nx, ny});

                roomsCount++;
                createdFromCurrent++;
            }

            if (createdFromCurrent == 0 && !(cx == startX && cy == startY)) {
                endRooms.add(new int[]{cx, cy});
            }
        }

        if (roomsCount < MIN_ROOMS) {
            boolean createdRoom = true;
            while (roomsCount < MIN_ROOMS && createdRoom) {
                createdRoom = false;

                List<int[]> occupiedRooms = new ArrayList<>();
                for (int i = 0; i < GRID_SIZE; i++) {
                    for (int j = 0; j < GRID_SIZE; j++) {
                        if (grid[i][j] != null) {
                            occupiedRooms.add(new int[]{i, j});
                        }
                    }
                }
                Collections.shuffle(occupiedRooms, rng);

                for (int[] current : occupiedRooms) {
                    int cx = current[0];
                    int cy = current[1];

                    List<int[]> neighbors = new ArrayList<>();
                    neighbors.add(new int[]{cx, cy - 1});
                    neighbors.add(new int[]{cx, cy + 1});
                    neighbors.add(new int[]{cx + 1, cy});
                    neighbors.add(new int[]{cx - 1, cy});
                    Collections.shuffle(neighbors, rng);

                    for (int[] n : neighbors) {
                        int nx = n[0];
                        int ny = n[1];

                        if (!isInsideGrid(nx, ny)) continue;
                        if (grid[nx][ny] != null) continue;
                        if (countOccupiedNeighbors(nx, ny) >= 3) continue;

                        Room normalRoom = new Room(RoomType.NORMAL, nx, ny);
                        loadRandomLayoutInto(normalRoom, rng);
                        grid[nx][ny] = normalRoom;

                        int parentDist = distanceFromStart[cx][cy];
                        if (parentDist < 0) parentDist = 0;
                        distanceFromStart[nx][ny] = parentDist + 1;

                        roomsCount++;
                        createdRoom = true;
                        break;
                    }

                    if (roomsCount >= MIN_ROOMS) break;
                }
            }
        }

        List<int[]> bossCandidates = new ArrayList<>();
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (grid[i][j] == null) continue;
                if (i == startX && j == startY) continue;
                if (countOccupiedNeighbors(i, j) == 1) {
                    bossCandidates.add(new int[]{i, j});
                }
            }
        }

        if (bossCandidates.isEmpty()) {
            bossCandidates.addAll(endRooms);
        }

        if (bossCandidates.isEmpty()) {
            for (int i = 0; i < GRID_SIZE; i++) {
                for (int j = 0; j < GRID_SIZE; j++) {
                    if (grid[i][j] != null && !(i == startX && j == startY)) {
                        bossCandidates.add(new int[]{i, j});
                    }
                }
            }
        }

        int minBossDistance = Math.max(4, targetRooms / 3);
        int[] bossPos = null;
        int bestDistance = -1;

        for (int[] pos : bossCandidates) {
            int x = pos[0];
            int y = pos[1];
            int dist = distanceFromStart[x][y];
            if (dist >= minBossDistance && dist > bestDistance) {
                bestDistance = dist;
                bossPos = pos;
            }
        }

        if (bossPos == null) {
            for (int[] pos : bossCandidates) {
                int x = pos[0];
                int y = pos[1];
                int dist = distanceFromStart[x][y];
                if (dist > bestDistance) {
                    bestDistance = dist;
                    bossPos = pos;
                }
            }
        }

        if (bossPos != null) {
            int bx = bossPos[0];
            int by = bossPos[1];
            bossRoom = new Room(RoomType.BOSS, bx, by);
            grid[bx][by] = bossRoom;
            endRooms.removeIf(pos -> pos[0] == bx && pos[1] == by);
        }
        
        int[] itemPos = null;
        if (!endRooms.isEmpty()) {
            itemPos = endRooms.get(rng.nextInt(endRooms.size()));
        } else {
            List<int[]> normalRooms = new ArrayList<>();
            for (int i = 0; i < GRID_SIZE; i++) {
                for (int j = 0; j < GRID_SIZE; j++) {
                    if (grid[i][j] != null && grid[i][j].getType() == RoomType.NORMAL) {
                        normalRooms.add(new int[]{i, j});
                    }
                }
            }
            if (!normalRooms.isEmpty()) {
                itemPos = normalRooms.get(rng.nextInt(normalRooms.size()));
            }
        }

        if (itemPos != null) {
            int ix = itemPos[0];
            int iy = itemPos[1];
            Room previousRoom = grid[ix][iy];
            Room itemRoom = new Room(RoomType.ITEM, ix, iy);
            if (!loadFixedLayoutInto(itemRoom, Paths.get("saved_rooms", "item_room.txt")) && previousRoom != null) {
                // Fallback: keep previous geometry if item_room.txt is unavailable.
                itemRoom.copyLayoutFrom(previousRoom);
            }
            grid[ix][iy] = itemRoom;
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

        // Lock doors to ITEM room
        lockItemRoomDoorsAfterGeneration();

        dumpLayoutToFile("after_connect", targetRooms, roomsCount);
    }

    // Locks all doors that lead to the ITEM room and unlocks them from the ITEM room's neighbors
    private void lockItemRoomDoorsAfterGeneration() {
        Room itemRoom = null;
        int itemRoomX = -1, itemRoomY = -1;

        // Find the ITEM room
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (grid[i][j] != null && grid[i][j].getType() == RoomType.ITEM) {
                    itemRoom = grid[i][j];
                    itemRoomX = i;
                    itemRoomY = j;
                    break;
                }
            }
            if (itemRoom != null) break;
        }

        if (itemRoom == null) return; // No item room found

        // Lock doors from neighbors to the ITEM room
        Room[] neighbors = new Room[4];
        Direction[] itemRoomDirs = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };
        Direction[] neighborDirs = { Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST };
        int[][] neighborCoords = {
            { itemRoomX, itemRoomY - 1 },      // NORTH
            { itemRoomX, itemRoomY + 1 },      // SOUTH
            { itemRoomX + 1, itemRoomY },      // EAST
            { itemRoomX - 1, itemRoomY }       // WEST
        };

        for (int i = 0; i < 4; i++) {
            int nx = neighborCoords[i][0];
            int ny = neighborCoords[i][1];

            if (isInsideGrid(nx, ny)) {
                neighbors[i] = grid[nx][ny];
                if (neighbors[i] != null) {
                    // Lock the correct side of the connection in both rooms
                    neighbors[i].lockDoor(neighborDirs[i]);
                    itemRoom.lockDoor(itemRoomDirs[i]);
                }
            }
        }
    }

    // Helper: returns the opposite direction
    private Direction getOppositeDirection(Direction dir) {
        return switch (dir) {
            case NORTH -> Direction.SOUTH;
            case SOUTH -> Direction.NORTH;
            case EAST -> Direction.WEST;
            case WEST -> Direction.EAST;
            default -> null;
        };
    }

    private void dumpLayoutToFile(String phase, int targetRooms, int roomsCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("phase=").append(phase).append("\n");
        sb.append("targetRooms=").append(targetRooms)
          .append(", actualRooms=").append(roomsCount)
          .append("\n");

        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                Room room = grid[x][y];
                char marker = '.';
                if (room != null) {
                    if (room.getType() == RoomType.START) {
                        marker = 'S';
                    } else if (room.getType() == RoomType.BOSS) {
                        marker = 'B';
                    } else if (room.getType() == RoomType.ITEM) {
                        marker = 'I';
                    } else {
                        marker = 'N';
                    }
                }
                sb.append(marker);
            }
            sb.append("\n");
        }
        sb.append("---\n");

        try {
            Path outDir = Paths.get("saved_rooms");
            Files.createDirectories(outDir);
            Path outFile = outDir.resolve("layout_debug.txt");
            Files.writeString(
                    outFile,
                    sb.toString(),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
            );
        } catch (IOException ex) {
            System.err.println("Could not write layout_debug.txt: " + ex.getMessage());
        }
    }

    private void clearLayoutDebugFile() {
        try {
            Path outDir = Paths.get("saved_rooms");
            Files.createDirectories(outDir);
            Path outFile = outDir.resolve("layout_debug.txt");
            Files.deleteIfExists(outFile);
        } catch (IOException ex) {
            System.err.println("Could not clear layout_debug.txt: " + ex.getMessage());
        }
    }

    private boolean isInsideGrid(int x, int y) {
        return x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE;
    }

    private int countOccupiedNeighbors(int x, int y) {
        int count = 0;

        if (isInsideGrid(x, y - 1) && grid[x][y - 1] != null) count++;
        if (isInsideGrid(x, y + 1) && grid[x][y + 1] != null) count++;
        if (isInsideGrid(x + 1, y) && grid[x + 1][y] != null) count++;
        if (isInsideGrid(x - 1, y) && grid[x - 1][y] != null) count++;

        return count;
    }

    private void loadRandomLayoutInto(Room room, Random rng) {
        try {
            Path dir = Paths.get("saved_rooms");
            if (Files.exists(dir) && Files.isDirectory(dir)) {
                List<Path> files = Files.list(dir)
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().toLowerCase().endsWith(".txt"))
                    .filter(p -> !p.getFileName().toString().equalsIgnoreCase("item_room.txt"))
                        .collect(Collectors.toList());
                if (!files.isEmpty()) {
                    Path chosen = files.get(rng.nextInt(files.size()));
                    try {
                        Room loaded = MapIO.loadRoom(chosen);
                        room.copyLayoutFrom(loaded);
                    } catch (IOException ex) {
                        System.err.println("Failed to load prefab " + chosen + " : " + ex.getMessage());
                    }
                }
            }
        } catch (IOException ex) {
            System.err.println("Could not list saved_rooms: " + ex.getMessage());
        }
    }

    private boolean loadFixedLayoutInto(Room room, Path layoutPath) {
        if (room == null || layoutPath == null) return false;
        if (!Files.exists(layoutPath) || !Files.isRegularFile(layoutPath)) {
            System.err.println("Missing fixed layout file: " + layoutPath);
            return false;
        }
        try {
            Room loaded = MapIO.loadRoom(layoutPath);
            room.copyLayoutFrom(loaded);
            return true;
        } catch (IOException ex) {
            System.err.println("Failed to load fixed layout " + layoutPath + " : " + ex.getMessage());
            return false;
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

    // Unlocks a door in the current room if the player has the required key
    public boolean unlockDoorWithKey(Direction dir, Player player) {
        Room room = getCurrentRoom();
        if (room == null || player == null) return false;

        // Check if the door in this direction is locked
        if (!room.isDoorDirectionLocked(dir)) {
            return true; // Door is not locked
        }

        if (player.useKey()) {
            // Unlock the door and consume one key
            room.unlockDoor(dir);

            Room adjacentRoom = getAdjacentRoom(room, dir);
            if (adjacentRoom != null) {
                Direction oppositeDir = getOppositeDirection(dir);
                if (oppositeDir != null) {
                    adjacentRoom.unlockDoor(oppositeDir);
                }
            }

            return true; // Successfully unlocked
        }

        return false; // Player doesn't have the required key
    }

    // Checks if a door in a direction is locked
    public boolean isDoorLocked(Direction dir) {
        Room room = getCurrentRoom();
        if (room == null) return false;
        return room.isDoorDirectionLocked(dir);
    }

    // Returns the neighboring room in the given direction from the provided room
    private Room getAdjacentRoom(Room room, Direction dir) {
        int x = room.getX();
        int y = room.getY();

        return switch (dir) {
            case NORTH -> isInsideGrid(x, y - 1) ? grid[x][y - 1] : null;
            case SOUTH -> isInsideGrid(x, y + 1) ? grid[x][y + 1] : null;
            case EAST -> isInsideGrid(x + 1, y) ? grid[x + 1][y] : null;
            case WEST -> isInsideGrid(x - 1, y) ? grid[x - 1][y] : null;
            default -> null;
        };
    }

}
