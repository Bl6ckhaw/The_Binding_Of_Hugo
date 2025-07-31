import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Manages all projectiles in the game.
 * Handles updating, rendering, collision checks, and removal of projectiles.
 */
public class ProjectileManager {
    private List<Projectile> projectiles;
    private static final int PLAYER_SIZE = 32; // Used for collision with player
    private Color projectileColor; // Color of the projectiles

    public ProjectileManager() {
        this.projectiles = new ArrayList<>();
        this.projectileColor = Color.BLACK;
    }

    // Adds a new projectile to the list
    public void addProjectile(Projectile projectile) {
        projectiles.add(projectile);
    }

    // Updates all projectiles' positions
    public void updateAll(){
        for (Projectile p : projectiles) {
            p.update();
        }
    }

    // Removes projectiles that are out of bounds or hit a wall
    public void removeOutOfBounds(Room currentRoom){
        Iterator<Projectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            Projectile p = iterator.next();
            // Use CollisionSystem to check for wall collisions
            if (!CollisionSystem.canPlayerMoveTo(p.getX(), p.getY(), currentRoom)) {
                iterator.remove();
            }
        }
    }

    // Renders all projectiles
    public void render(GraphicsContext gc, double tileSize, double offsetX, double offsetY) {
        for (Projectile p : projectiles) {
            double x = offsetX + (p.getX() / (11 * 32)) * (tileSize * 11);
            double y = offsetY + (p.getY() / (11 * 32)) * (tileSize * 11);
            double size = (p.getSize() / 32.0) * tileSize; // taille relative à une tuile
            gc.setFill(p.getOwner() == ProjectileOwner.PLAYER ? Color.BLACK : Color.ORANGE);
            gc.fillOval(x - size/2, y - size/2, size, size);
        }
    }

    // Marks a projectile for removal (removes it from the list)
    public void markForRemoval(Projectile p) {
        projectiles.remove(p);
    }

    // Checks for collisions between projectiles and the player
    public void checkPlayerCollisions(Player player) {
        Iterator<Projectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            Projectile p = iterator.next();
            if (p.getTarget() == ProjectileTarget.PLAYER) {
                double distance = Math.hypot(p.getX() - player.getX(), p.getY() - player.getY());
                // 20% more tolerant collision radius
                if (distance < (p.getSize() / 2 + PLAYER_SIZE / 2) * 1.2) {
                    player.takeDamage(p.getDamage());
                    iterator.remove(); // Remove the projectile after collision
                }
            }
        }
    }

    // Returns a copy of the current list of projectiles (prevents external modification)
    public List<Projectile> getProjectiles() {
        return new ArrayList<>(projectiles); // return a copy to avoid external modification
    }

    // Clears all projectiles (used when entering a new room)
    public void clearProjectiles() {
        projectiles.clear();
    }


}
