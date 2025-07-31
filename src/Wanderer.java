import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Enemy that moves randomly in one direction and changes direction periodically or on collision.
 * Deals contact damage to the player if close enough.
 */
public class Wanderer extends Enemy {
    private Direction currentDirection;
    private long lastDirectionChange = 0;
    private static final long DIRECTION_CHANGE_INTERVAL = 1_000_000_000; // 1 second in nanoseconds
    private GameMap gameMap; // Reference to the game map for collision checks
    private static final double SIZE = 32 / 1.5; // Visual size of the enemy
    private static final double CONTACT_RANGE = 10.0; // Distance to deal damage to player
    private static final long ATTACK_COOLDOWN = 1_000_000_000; // 1 second in nanoseconds
    private long lastAttackTime = 0;

    public Wanderer(double x, double y, int health, int damage, double speed, GameMap gameMap) {
        super(x, y, health, damage, speed);
        this.gameMap = gameMap;
        chooseRandomDirection();
    }

    @Override
    public void update(Player player) {
        if (isAlive) {
            long currentTime = System.nanoTime();
            // Change direction periodically
            if (currentTime - lastDirectionChange >= DIRECTION_CHANGE_INTERVAL) {
                chooseRandomDirection();
                lastDirectionChange = currentTime;
            }

            // Move in the current direction
            double newX = this.x;
            double newY = this.y;

            switch(currentDirection) {
                case NORTH -> newY = this.y - speed;
                case SOUTH -> newY = this.y + speed;
                case EAST -> newX = this.x + speed;
                case WEST -> newX = this.x - speed;
            }

            // Check collisions before moving
            if (CollisionSystem.canPlayerMoveTo(newX, newY, gameMap.getCurrentRoom())) {
                this.x = newX;
                this.y = newY;
            } else {
                // If collision with a wall, change direction immediately
                chooseRandomDirection();
                lastDirectionChange = currentTime;
            }

            // Deal contact damage to the player if close enough and cooldown has passed
            double distance = Math.hypot(player.getX() - this.x, player.getY() - this.y);
            if (distance <= CONTACT_RANGE) {
                if (currentTime - lastAttackTime >= ATTACK_COOLDOWN) {
                    player.takeDamage(damage);
                    lastAttackTime = currentTime;
                }
            }
        }
    }

    @Override
    public void render(GraphicsContext gc, double tileSize, double offsetX, double offsetY) {
        if (isAlive) {
            Color wandererColor = Color.PINK;
            gc.setFill(wandererColor);
            double dx = offsetX + (x / (11 * 32)) * (tileSize * 11);
            double dy = offsetY + (y / (11 * 32)) * (tileSize * 11);
            double size = tileSize * 0.8; // Example: 80% of a tile
            gc.fillOval(dx - size / 2, dy - size / 2, size, size); // Centered drawing
        }
    }

    // Chooses a new random direction for the wanderer
    private void chooseRandomDirection() {
        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        currentDirection = directions[(int)(Math.random() * 4)];
    }
}
