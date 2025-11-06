import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Enemy that stays in place and shoots at the player when aligned horizontally or vertically.
 */
public class StaticShooter extends Enemy {
    private static final long SHOOT_COOLDOWN = 2_000_000_000; // 2 seconds in nanoseconds
    private long lastShotTime = 0;
    private ProjectileManager projectileManager;
    private int health;
    private int damage;
    private double x, y;
    private static final double SIZE = 32 / 1.5;

    public StaticShooter(double x, double y, int health, int damage, ProjectileManager projectileManager) {
        super(x, y, health, damage, 0);
        this.x = x;
        this.y = y;
        this.health = health;
        this.damage = damage;
        this.projectileManager = projectileManager;
    }

    @Override
    public void update(Player player, ProjectileManager projectileManager, GameMap gameMap) {
        if (isAlive){
            // Shoot if perfectly aligned horizontally or vertically (with 10px tolerance)
            double deltaX = Math.abs(this.x - player.getX());
            double deltaY = Math.abs(this.y - player.getY());

            if (deltaX < 10 || deltaY < 10) {
                shootAtPlayer(player); 
            }
        }
    }

    // Shoots a projectile at the player if cooldown has passed
    private void shootAtPlayer(Player player) {
        long currentTime = System.nanoTime();
        if (currentTime - lastShotTime >= SHOOT_COOLDOWN) {
            Direction direction;
            
            // Calculate which axis is closer to the player
            double deltaX = Math.abs(player.getX() - this.x);
            double deltaY = Math.abs(player.getY() - this.y);
            
            // Shoot along the axis where the player is closer
            if (deltaX < deltaY) {
                // Closer horizontally → shoot vertically
                direction = player.getY() > this.y ? Direction.SOUTH : Direction.NORTH;
            } else {
                // Closer vertically → shoot horizontally  
                direction = player.getX() > this.x ? Direction.EAST : Direction.WEST;
            }
            
            Projectile projectile = new Projectile(this.x, this.y, damage, 2, 10, direction, 
                                                  ProjectileOwner.ENEMY,    // Fired by enemy
                                                  ProjectileTarget.PLAYER); // Targets the player
            projectileManager.addProjectile(projectile);
            lastShotTime = currentTime;
        }
    }

    @Override
    public void render(GraphicsContext gc, double tileSize, double offsetX, double offsetY) {
        if (isAlive) {
            Color shooterColor = Color.RED;
            gc.setFill(shooterColor);
            double dx = offsetX + (x / (11 * 32)) * (tileSize * 11);
            double dy = offsetY + (y / (11 * 32)) * (tileSize * 11);
            double size = tileSize * 0.8; // Example: 80% of a tile
            gc.fillOval(dx - size / 2, dy - size / 2, size, size); // Centered
        }
    }
}
