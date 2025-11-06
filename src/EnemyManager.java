import java.util.ArrayList;
import java.util.List;
import javafx.scene.canvas.GraphicsContext;

/**
 * Manages the list of enemies in the current room.
 * Handles updating, rendering, and collision detection for all enemies.
 */
public class EnemyManager {
    private List<Enemy> enemies;

    public EnemyManager() {
        enemies = new ArrayList<>();
    }

    // Adds an enemy to the current list
    public void addEnemy(Enemy enemy) {
        enemies.add(enemy);
    }

    // Updates all enemies (AI, movement, etc.)
    public void updateAll(Player player, ProjectileManager projectileManager, GameMap gameMap) {
        for (Enemy enemy : enemies) {
            enemy.update(player, projectileManager, gameMap);
        }
    }

    // Renders all alive enemies
    public void renderAll(GraphicsContext gc, double tileSize, double offsetX, double offsetY) {
        for (Enemy enemy : enemies) {
            if (enemy.isAlive()) {
                enemy.render(gc, tileSize, offsetX, offsetY);
            }
        }
    }

    // Removes all dead enemies from the list
    public void removeDeadEnemies() {
        enemies.removeIf(enemy -> !enemy.isAlive());
    }

    // Checks for collisions between projectiles and enemies
    public void checkProjectileCollisions(ProjectileManager projectileManager){
        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) continue;
            for (Projectile projectile : projectileManager.getProjectiles()) {
                // Only projectiles targeting enemies can hit them
                if (projectile.getTarget() != ProjectileTarget.ENEMY) continue;
                double distance = Math.hypot(projectile.getX() - enemy.getX(), 
                                            projectile.getY() - enemy.getY());
                // Collision if distance < sum of radii (with tolerance)
                double collisionDistance = ((projectile.getSize()/2) + (21.33/2)) * 1.2;
                if (distance < collisionDistance) {
                    enemy.takeDamage(projectile.getDamage());
                    projectileManager.markForRemoval(projectile);
                    break; // A projectile can only hit one enemy
                }
            }
        }
    }

    // Replaces the current enemy list with a new one (used when entering a new room)
    public void setEnemies(List<Enemy> enemies) {
        this.enemies = enemies;
    }

    // Returns the current list of enemies (for synchronization with Room)
    public List<Enemy> getEnemies() {
        return enemies;
    }
}
