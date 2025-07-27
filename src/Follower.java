import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Enemy that follows the player and attacks in close range.
 */
public class Follower extends Enemy {
    private static final double SIZE = 32 / 1.5; // Visual size of the enemy
    private static final double ATTACK_RANGE = 25.0; // Distance to attack the player
    private static final long ATTACK_COOLDOWN = 1_000_000_000; // 1 second in nanoseconds
    private long lastAttackTime = 0;
    private GameMap gameMap; // Reference to the game map for collision checks

    public Follower(double x, double y, int health, int damage, double speed, GameMap gameMap) {
        super(x, y, health, damage, speed);
        this.gameMap = gameMap;
    }

    @Override
    public void update(Player player) {
        if (isAlive) {
            // Calculate direction to the player
            double deltaX = player.getX() - this.x;
            double deltaY = player.getY() - this.y;
            double distance = Math.hypot(deltaX, deltaY);

            // Move towards the player if not too close
            if (distance > ATTACK_RANGE) {
                double angle = Math.atan2(deltaY, deltaX);
                double newX = this.x + Math.cos(angle) * speed;
                double newY = this.y + Math.sin(angle) * speed;

                // Check collisions before moving
                if (CollisionSystem.canPlayerMoveTo(newX, newY, gameMap.getCurrentRoom())) {
                    this.x = newX;
                    this.y = newY;
                }
            }

            // Attack if close enough and cooldown has passed
            if (distance <= ATTACK_RANGE) {
                long currentTime = System.nanoTime();
                if (currentTime - lastAttackTime >= ATTACK_COOLDOWN) {
                    player.takeDamage(damage);
                    lastAttackTime = currentTime;
                }
            }
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        if (isAlive) {
            Color followerColor = Color.PURPLE;
            gc.setFill(followerColor);
            gc.fillOval(x - SIZE / 2, y - SIZE / 2, SIZE, SIZE); // Centered drawing
        }
    }

}
