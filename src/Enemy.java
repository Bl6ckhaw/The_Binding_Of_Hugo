import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Abstract base class for all enemies.
 * Handles position, health, damage, speed, and alive state.
 */
public abstract class Enemy {
    protected double x, y;         // Enemy position
    protected int health;          // Enemy health points
    protected int damage;          // Damage dealt to the player
    protected double speed;        // Movement speed
    protected boolean isAlive;     // True if enemy is alive

    public Enemy(double x, double y, int health, int damage, double speed) {
        this.x = x;
        this.y = y;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
        this.isAlive = true;
    }

    // Abstract method to update enemy state - signature corrigée pour correspondre aux classes filles
    public abstract void update(Player player, ProjectileManager projectileManager, GameMap gameMap);

    public int getCollisionSize() {
        return MapDimensions.ENEMY_SIZE;
    }

    protected double getRenderSize(double tileSize) {
        return tileSize * MapDimensions.ENEMY_RENDER_SCALE;
    }

    // Render the enemy on the canvas at screen coordinates
    public void render(GraphicsContext gc, double screenX, double screenY, double tileSize) {
        double size = getRenderSize(tileSize);
        gc.setFill(Color.DARKRED);
        gc.fillOval(screenX - size/2, screenY - size/2, size, size);
    }

    // Apply damage to the enemy and update alive state
    public void takeDamage(int damage) {
        this.health -= damage;
        if (this.health <= 0) {
            this.isAlive = false;
        }
    }

    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public int getDamage() { return damage; }
    public boolean isAlive() { return isAlive; }
    public int getHealth() { return health; } // Ajouté pour BossEnemy
}