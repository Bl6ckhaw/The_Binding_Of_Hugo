import javafx.scene.canvas.GraphicsContext;

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

    // Update enemy logic (AI, movement, etc.)
    public abstract void update(Player player);

    // Render the enemy on the canvas
    public abstract void render(GraphicsContext gc);

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
}
