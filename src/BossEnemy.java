import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class BossEnemy extends Enemy {
    private static final boolean TEST_PASSIVE_BOSS = false;
    private int attackCooldown = 0;
    private static final int MAX_COOLDOWN = 180; // 3 secondes à 60 FPS



    public BossEnemy(double x, double y) {
        super(x, y, 50, 5, 0); // Beaucoup de vie, gros dégâts, vitesse nulle (statique)
    }

    
    @Override
    public void update(Player player, ProjectileManager projectileManager, GameMap gameMap) {
        if (TEST_PASSIVE_BOSS) {
            return;
        }

        if (attackCooldown > 0) {
            attackCooldown--;
        } else {
            shootPattern(projectileManager, gameMap);
            attackCooldown = MAX_COOLDOWN;
        }
    }

    private void shootPattern(ProjectileManager projectileManager, GameMap gameMap) {
    int pattern = (int) (Math.random() * 3); 
        switch (pattern) {
            case 0, 1:
                // Cross attack (+)
                projectileManager.addProjectile(new Projectile(getX(), getY(), 2, 1, 20, Direction.NORTH, ProjectileOwner.ENEMY, ProjectileTarget.PLAYER));
                projectileManager.addProjectile(new Projectile(getX(), getY(), 2, 1, 20, Direction.SOUTH, ProjectileOwner.ENEMY, ProjectileTarget.PLAYER));
                projectileManager.addProjectile(new Projectile(getX(), getY(), 2, 1, 20, Direction.EAST, ProjectileOwner.ENEMY, ProjectileTarget.PLAYER));
                projectileManager.addProjectile(new Projectile(getX(), getY(), 2, 1, 20, Direction.WEST, ProjectileOwner.ENEMY, ProjectileTarget.PLAYER));
                break;
            case 2:
                // Diagonal attack (x)
                projectileManager.addProjectile(new Projectile(getX(), getY(), 2, 1, 20, Direction.NORTH_EAST, ProjectileOwner.ENEMY, ProjectileTarget.PLAYER));
                projectileManager.addProjectile(new Projectile(getX(), getY(), 2, 1, 20, Direction.NORTH_WEST, ProjectileOwner.ENEMY, ProjectileTarget.PLAYER));
                projectileManager.addProjectile(new Projectile(getX(), getY(), 2, 1, 20, Direction.SOUTH_EAST, ProjectileOwner.ENEMY, ProjectileTarget.PLAYER));
                projectileManager.addProjectile(new Projectile(getX(), getY(), 2, 1, 20, Direction.SOUTH_WEST, ProjectileOwner.ENEMY, ProjectileTarget.PLAYER));
                break;
        }
}

    @Override
    public void render(GraphicsContext gc, double tileSize, double offsetX, double offsetY) {
        double x = offsetX + (getX() / MapDimensions.ROOM_PIXEL_SIZE) * (tileSize * MapDimensions.ROOM_SIZE);
        double y = offsetY + (getY() / MapDimensions.ROOM_PIXEL_SIZE) * (tileSize * MapDimensions.ROOM_SIZE);
        double size = tileSize * 1.2; // plus gros que les autres ennemis
        gc.setFill(Color.DARKVIOLET);
        gc.fillOval(x - size/2, y - size/2, size, size);
        // Barre de vie du boss
        gc.setFill(Color.RED);
        double lifeRatio = Math.max(0, getHealth() / 50.0);
        gc.fillRect(x - size/2, y - size/2 - 10, size * lifeRatio, 8);
        gc.setStroke(Color.BLACK);
        gc.strokeRect(x - size/2, y - size/2 - 10, size, 8);
    }

    
}
