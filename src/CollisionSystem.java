public class CollisionSystem {
    private static final int TILE_SIZE = 32; // Size of each tile in pixels
    private static final int PLAYER_SIZE = 16; // Size of the player in pixels (TILE_SIZE/2)
    private static final int ROOM_SIZE = 11; // Number of tiles per room (11x11 grid)
    private static final int DOOR_POSITION = 5; // Door is in the middle of the wall (11/2 = 5)

    /**
     * Checks if the player can move to the given position in the specified room.
     * Returns false if the player would collide with a wall, except at door positions.
     */
    public static boolean canPlayerMoveTo(double playerX, double playerY, Room room) {
        // Compute the player's bounding box
        double playerLeft = playerX - PLAYER_SIZE / 2;
        double playerRight = playerX + PLAYER_SIZE / 2;
        double playerTop = playerY - PLAYER_SIZE / 2;
        double playerBottom = playerY + PLAYER_SIZE / 2;

        // Check collision with room boundaries (walls)
        if (playerLeft < TILE_SIZE || playerRight > (ROOM_SIZE - 1) * TILE_SIZE ||
            playerTop < TILE_SIZE || playerBottom > (ROOM_SIZE - 1) * TILE_SIZE) {

            // Allow movement through doors if present and player is at the door position
            if (room.hasDoor(Direction.NORTH) && playerBottom > TILE_SIZE && playerTop < TILE_SIZE &&
                playerX >= DOOR_POSITION * TILE_SIZE && playerX <= (DOOR_POSITION + 1) * TILE_SIZE) {
                return true; // Can move through the north door
            } else if (room.hasDoor(Direction.SOUTH) && playerTop < (ROOM_SIZE - 1) * TILE_SIZE && playerBottom > (ROOM_SIZE - 1) * TILE_SIZE &&
                playerX >= DOOR_POSITION * TILE_SIZE && playerX <= (DOOR_POSITION + 1) * TILE_SIZE) {
                return true; // Can move through the south door
            } else if (room.hasDoor(Direction.EAST) && playerLeft < (ROOM_SIZE - 1) * TILE_SIZE && playerRight > (ROOM_SIZE - 1) * TILE_SIZE &&
                playerY >= DOOR_POSITION * TILE_SIZE && playerY <= (DOOR_POSITION + 1) * TILE_SIZE) {
                return true; // Can move through the east door
            } else if (room.hasDoor(Direction.WEST) && playerRight > TILE_SIZE && playerLeft < TILE_SIZE &&
                playerY >= DOOR_POSITION * TILE_SIZE && playerY <= (DOOR_POSITION + 1) * TILE_SIZE) {
                return true; // Can move through the west door
            } else {
                return false; // Collision with a wall, no door
            }
        }
        return true; // No collision, can move 
    }
}