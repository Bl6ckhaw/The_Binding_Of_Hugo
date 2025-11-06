public class CollisionSystem {
    private static final int TILE_SIZE = 32; // Size of each tile in pixels
    private static final int PLAYER_SIZE = 16; // Size of the player in pixels (TILE_SIZE/2)
    private static final int ENEMY_SIZE = 16; // Size of enemies in pixels
    private static final int PROJECTILE_SIZE = 4; // Size of projectiles in pixels
    private static final int ROOM_SIZE = 11; // Number of tiles per room (11x11 grid)
    private static final int DOOR_POSITION = 5; // Door is in the middle of the wall (11/2 = 5)

    /**
     * Checks if can move
     */
    public static boolean canPlayerMoveTo(double playerX, double playerY, Room room) {
        // Vérifier collision avec les murs intérieurs
        if (room.isAreaBlocked(playerX, playerY, PLAYER_SIZE, PLAYER_SIZE)) {
            return false;
        }

        // Vérifier les limites de la salle et les portes (logique existante)
        double playerLeft = playerX - PLAYER_SIZE / 2;
        double playerRight = playerX + PLAYER_SIZE / 2;
        double playerTop = playerY - PLAYER_SIZE / 2;
        double playerBottom = playerY + PLAYER_SIZE / 2;

        if (playerLeft < TILE_SIZE || playerRight > (ROOM_SIZE - 1) * TILE_SIZE ||
            playerTop < TILE_SIZE || playerBottom > (ROOM_SIZE - 1) * TILE_SIZE) {

            // Allow movement through doors if present and player is at the door position
            if (room.hasDoor(Direction.NORTH) && playerBottom > TILE_SIZE && playerTop < TILE_SIZE &&
                playerX >= DOOR_POSITION * TILE_SIZE && playerX <= (DOOR_POSITION + 1) * TILE_SIZE) {
                return true;
            } else if (room.hasDoor(Direction.SOUTH) && playerTop < (ROOM_SIZE - 1) * TILE_SIZE && playerBottom > (ROOM_SIZE - 1) * TILE_SIZE &&
                playerX >= DOOR_POSITION * TILE_SIZE && playerX <= (DOOR_POSITION + 1) * TILE_SIZE) {
                return true;
            } else if (room.hasDoor(Direction.EAST) && playerLeft < (ROOM_SIZE - 1) * TILE_SIZE && playerRight > (ROOM_SIZE - 1) * TILE_SIZE &&
                playerY >= DOOR_POSITION * TILE_SIZE && playerY <= (DOOR_POSITION + 1) * TILE_SIZE) {
                return true;
            } else if (room.hasDoor(Direction.WEST) && playerRight > TILE_SIZE && playerLeft < TILE_SIZE &&
                playerY >= DOOR_POSITION * TILE_SIZE && playerY <= (DOOR_POSITION + 1) * TILE_SIZE) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if an enemy can move to the given position.
     */
    public static boolean canEnemyMoveTo(double enemyX, double enemyY, Room room) {
        // Vérifier collision avec les murs intérieurs
        if (room.isAreaBlocked(enemyX, enemyY, ENEMY_SIZE, ENEMY_SIZE)) {
            return false;
        }

        // Vérifier les limites de la salle (les ennemis ne peuvent pas sortir)
        double enemyLeft = enemyX - ENEMY_SIZE / 2;
        double enemyRight = enemyX + ENEMY_SIZE / 2;
        double enemyTop = enemyY - ENEMY_SIZE / 2;
        double enemyBottom = enemyY + ENEMY_SIZE / 2;

        if (enemyLeft < TILE_SIZE || enemyRight > (ROOM_SIZE - 1) * TILE_SIZE ||
            enemyTop < TILE_SIZE || enemyBottom > (ROOM_SIZE - 1) * TILE_SIZE) {
            return false;
        }

        return true;
    }

    /**
     * Checks if a projectile can move to the given position.
     */
    public static boolean canProjectileMoveTo(double projectileX, double projectileY, Room room) {
        // Vérifier collision avec les murs intérieurs
        if (room.isPositionBlocked(projectileX, projectileY)) {
            return false;
        }

        // Vérifier les limites de la salle
        if (projectileX <= TILE_SIZE || projectileX >= (ROOM_SIZE - 1) * TILE_SIZE ||
            projectileY <= TILE_SIZE || projectileY >= (ROOM_SIZE - 1) * TILE_SIZE) {
            return false;
        }

        return true;
    }

    /**
     * Checks if two entities collide (generic collision detection).
     * Used for player-enemy, projectile-enemy, etc.
     */
    public static boolean entitiesCollide(double x1, double y1, double size1, 
                                        double x2, double y2, double size2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance < (size1 + size2) / 2;
    }

    /**
     * Checks if a projectile hits an enemy.
     */
    public static boolean projectileHitsEnemy(double projectileX, double projectileY, 
                                            double enemyX, double enemyY) {
        return entitiesCollide(projectileX, projectileY, PROJECTILE_SIZE, 
                             enemyX, enemyY, ENEMY_SIZE);
    }

    /**
     * Checks if the player collides with an enemy.
     */
    public static boolean playerCollidesWithEnemy(double playerX, double playerY, 
                                                double enemyX, double enemyY) {
        return entitiesCollide(playerX, playerY, PLAYER_SIZE, 
                             enemyX, enemyY, ENEMY_SIZE);
    }

    /**
     * Checks if a projectile hits the player.
     */
    public static boolean projectileHitsPlayer(double projectileX, double projectileY, 
                                             double playerX, double playerY) {
        return entitiesCollide(projectileX, projectileY, PROJECTILE_SIZE, 
                             playerX, playerY, PLAYER_SIZE);
    }

    /**
     * Checks if an entity is within a certain range of another entity.
     */
    public static boolean isInRange(double x1, double y1, double x2, double y2, double range) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance <= range;
    }

    /**
     * Gets the distance between two entities.
     */
    public static double getDistance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Checks if a position is within the room bounds (excluding walls).
     */
    public static boolean isInRoomBounds(double x, double y) {
        return x >= TILE_SIZE && x <= (ROOM_SIZE - 1) * TILE_SIZE &&
               y >= TILE_SIZE && y <= (ROOM_SIZE - 1) * TILE_SIZE;
    }

    /**
     * Returns a safe spawn position within the room (avoids walls and center).
     */
    public static double[] getSafeSpawnPosition(Room room) {
        return getSafeSpawnPosition(room, false);
    }
    
    /**
     * Returns a safe spawn position within the room.
     * @param room La salle
     * @param avoidCenter Si true, évite le centre de la salle (pour les ennemis)
     */
    public static double[] getSafeSpawnPosition(Room room, boolean avoidCenter) {
        int attempts = 0;
        while (attempts < 100) {
            double x, y;
            
            if (avoidCenter) {
                // Pour les ennemis : spawn dans les coins/bords
                if (Math.random() < 0.5) {
                    // Spawn près des bords
                    x = TILE_SIZE * (2 + Math.random() * 2); // X entre 2-4
                    y = TILE_SIZE * (2 + Math.random() * 7); // Y entre 2-9
                } else {
                    // Spawn dans les coins
                    x = TILE_SIZE * (7 + Math.random() * 2); // X entre 7-9
                    y = TILE_SIZE * (2 + Math.random() * 7); // Y entre 2-9
                }
            } else {
                // Pour les récompenses/autres : spawn n'importe où (sauf murs)
                x = TILE_SIZE * 2 + Math.random() * (ROOM_SIZE - 4) * TILE_SIZE;
                y = TILE_SIZE * 2 + Math.random() * (ROOM_SIZE - 4) * TILE_SIZE;
            }
            
            // Vérifier que la position n'est pas dans un mur
            if (!room.isPositionBlocked(x, y)) {
                // Vérifier distance du centre pour les ennemis
                if (avoidCenter) {
                    double centerX = ROOM_SIZE * TILE_SIZE / 2;
                    double centerY = ROOM_SIZE * TILE_SIZE / 2;
                    double distance = getDistance(x, y, centerX, centerY);
                    if (distance > TILE_SIZE * 2) { // Au moins 2 tuiles du centre
                        return new double[]{x, y};
                    }
                } else {
                    return new double[]{x, y};
                }
            }
            attempts++;
        }
        
        // Fallback: position sûre par défaut
        if (avoidCenter) {
            return new double[]{TILE_SIZE * 3, TILE_SIZE * 3}; // Coin
        } else {
            return new double[]{ROOM_SIZE * TILE_SIZE / 2, ROOM_SIZE * TILE_SIZE / 2}; // Centre
        }
    }
}