import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a single room in the game map.
 * Stores its type, doors, enemies, and completion/door state.
 */
public class Room {
    private RoomType type;                // Room type (START, NORMAL, BOSS)
    private Set<Direction> directions;    // Directions where doors exist
    private int x;                        // Room's grid X coordinate
    private int y;                        // Room's grid Y coordinate
    private boolean doorsClosed;          // True if doors are currently closed
    private boolean isCompleted;          // True if room has been cleared of enemies
    private Set<Enemy> enemies;           // Set of enemies in this room
    private static final int ROOM_SIZE = 11; // Number of tiles per room (11x11)
    private static final int TILE_SIZE = 32; // Size of a tile in pixels
    private Reward reward; // Set of rewards available in this room

    public Room(RoomType type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.directions = EnumSet.noneOf(Direction.class);
        this.doorsClosed = false;
        this.isCompleted = false;
        this.enemies = new HashSet<>();
    }

    // GETTERS

    public RoomType getType() {
        return type;
    }

    public Set<Direction> getDirections() {
        return Collections.unmodifiableSet(directions);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    // Returns an unmodifiable set of enemies (for external read-only access)
    public Set<Enemy> getEnemies() {
        return Collections.unmodifiableSet(enemies);
    }

    // Internal modifiable access for enemy generation
    Set<Enemy> getEnemiesInternal() {
        return enemies;
    }

    // Adds an enemy to this room
    public void addEnemy(Enemy enemy) {
        enemies.add(enemy);
    }

    public Reward getRewards() {
        return reward;
    }
    public void setRewards(Reward reward) {
        this.reward = reward;
    }



    // DOORS

    // Returns true if the room has a door in the given direction
    public boolean hasDoor(Direction dir) {
        return directions.contains(dir);
    }

    // Adds a door in the given direction
    public void addDoor(Direction dir) {
        directions.add(dir);
    }

    // Removes a door in the given direction
    public void removeDoor(Direction dir) {
        directions.remove(dir);
    }

    // Returns true if doors are closed (player is locked in)
    public boolean areDoorsClosed() {
        return doorsClosed;
    }

    // Sets the door state (closed/open)
    public void setDoorsClosed(boolean doorsClosed) {
        this.doorsClosed = doorsClosed;
    }

    // ENEMIES

    /**
     * Generates a random enemy for this room, spawning in the central area (avoids walls and doors).
     */
    public Enemy generateRandomEnemy(ProjectileManager projectileManager, GameMap gameMap) {
        int minTile = 3;
        int maxTileX = ROOM_SIZE - 4; // 7 if ROOM_SIZE=11
        int maxTileY = ROOM_SIZE - 4;
        double randomX = (minTile + Math.random() * (maxTileX - minTile + 1)) * TILE_SIZE + TILE_SIZE / 2;
        double randomY = (minTile + Math.random() * (maxTileY - minTile + 1)) * TILE_SIZE + TILE_SIZE / 2;

        int numRandom = (int) (Math.random() * 3);
        switch (numRandom) {
            case 0:
                return new Wanderer(randomX, randomY, 3, 1, 1.0, gameMap);
            case 1:
                return new StaticShooter(randomX, randomY, 3, 1, projectileManager);
            case 2:
                return new Follower(randomX, randomY, 3, 1, 0.5, gameMap);
            default:
                return new Wanderer(randomX, randomY, 3, 1, 1.0, gameMap);
        }
    }

    // COMPLETION

    // Sets the room as completed (all enemies defeated)
    public void setCompleted(boolean completed){
        this.isCompleted = completed;
        
    }

    // REWARDS

    // Generate a random reward for this room
        public Reward generateReward() {
        int randomNum = (int) (Math.random() * 100);
        if (randomNum < 65) {
            reward = new Reward(RewardType.HEALTH);
        } else if (randomNum < 85) {
            reward = new Reward(RewardType.SPEED);
        } else if (randomNum < 99) {
            reward = new Reward(RewardType.TEARS_SIZE);
        } else {
            reward = new Reward(RewardType.DAMAGE);
        }
        reward.setPosition(); // Set position for rendering 
        return reward;
    }
}
