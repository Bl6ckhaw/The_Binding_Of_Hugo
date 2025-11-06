import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
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
    private GameMap gameMap; // Reference to the game map for enemy generation
    private ProjectileManager projectileManager;

    private List<Wall> walls; // existing

    // marque si la room a été chargée depuis un prefab (layout)
    private boolean prefabLoaded = false;

    public boolean isPrefabLoaded() { return prefabLoaded; }


    public Room(RoomType type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.directions = EnumSet.noneOf(Direction.class);
        this.doorsClosed = false;
        this.isCompleted = false;
        this.enemies = new HashSet<>();
        this.walls = new ArrayList<>();

        
    }

    // Copie uniquement le layout (walls) depuis une Room source.
    public void copyLayoutFrom(Room src) {
        this.walls.clear();
        for (Wall w : src.getWalls()) {
            // copy primitives to avoid partager les mêmes objets
            this.walls.add(new Wall(w.getX(), w.getY(), w.getWidth(), w.getHeight()));
        }
        // Ne PAS copier les doors : on laisse connectRooms gérer l'ouverture en fonction des murs voisins.
    }

    // check if the position is blocked by a wall
    public boolean isPositionBlocked(double worldX, double worldY) {
        // Convertir les coordonnées monde en coordonnées grille
        int gridX = (int) (worldX / 32);
        int gridY = (int) (worldY / 32);
        
        // Vérifier si la position est dans un mur
        for (Wall wall : walls) {
            if (wall.blocksPosition(gridX, gridY)) {
                return true;
            }
        }
        return false;
    }
    
    // check if an area is blocked by a rock
    public boolean isAreaBlocked(double centerX, double centerY, double width, double height) {
        // Vérifier les 4 coins de la bounding box
        double halfWidth = width / 2;
        double halfHeight = height / 2;
        
        return isPositionBlocked(centerX - halfWidth, centerY - halfHeight) ||
               isPositionBlocked(centerX + halfWidth, centerY - halfHeight) ||
               isPositionBlocked(centerX - halfWidth, centerY + halfHeight) ||
               isPositionBlocked(centerX + halfWidth, centerY + halfHeight);
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

    // Simplified setReferences: ne tente plus de charger des fichiers.
    public void setReferences(GameMap gameMap, ProjectileManager projectileManager) {
        this.gameMap = gameMap;
        this.projectileManager = projectileManager;

        if (this.type == RoomType.NORMAL) {
            // Si aucun mur n'a été fourni (ni prefabs copiés), créer une bordure par défaut
            if (this.walls.isEmpty()) {
                this.walls.clear();
                this.walls.add(new Wall(0, 0, ROOM_SIZE, 1)); // top
                this.walls.add(new Wall(0, ROOM_SIZE - 1, ROOM_SIZE, 1)); // bottom
                this.walls.add(new Wall(0, 0, 1, ROOM_SIZE)); // left
                this.walls.add(new Wall(ROOM_SIZE - 1, 0, 1, ROOM_SIZE)); // right
            }
            // Ensuite générer ennemis (ne dépend plus du chargement de layout)
            generateRandomEnemies(projectileManager, gameMap);
        } else {
            if (this.type == RoomType.BOSS) {
                spawnBoss();
            } else if (this.type == RoomType.START) {
                // no enemies by default
            }
        }
    }

    // helper: position de la porte au centre
    private static final int DOOR_POS = ROOM_SIZE / 2;

    // Vérifie si une tile (tx,ty) est bloquée par un mur
    private boolean isTileBlocked(int tx, int ty) {
        for (Wall w : walls) {
            if (w.blocksPosition(tx, ty)) return true;
        }
        return false;
    }

    // Vérifie si la tile correspond à une porte
    private boolean isDoorTile(int tx, int ty) {
        for (Direction d : directions) {
            switch (d) {
                case NORTH -> { if (tx == DOOR_POS && ty == 0) return true; }
                case SOUTH -> { if (tx == DOOR_POS && ty == ROOM_SIZE - 1) return true; }
                case EAST  -> { if (tx == ROOM_SIZE - 1 && ty == DOOR_POS) return true; }
                case WEST  -> { if (tx == 0 && ty == DOOR_POS) return true; }
            }
        }
        return false;
    }

    // Vérifie qu'une tile est valide pour spawn (pas mur, pas porte, pas bordure, pas sur un autre ennemi)
    private boolean isTileValidForSpawn(int tx, int ty) {
        // éviter bordure
        if (tx <= 0 || tx >= ROOM_SIZE - 1 || ty <= 0 || ty >= ROOM_SIZE - 1) return false;
        if (isTileBlocked(tx, ty)) return false;
        if (isDoorTile(tx, ty)) return false;
        // éviter collision avec autres ennemis (par tile)
        for (Enemy e : enemies) {
            int etx = (int) (e.getX() / TILE_SIZE);
            int ety = (int) (e.getY() / TILE_SIZE);
            if (etx == tx && ety == ty) return false;
        }
        return true;
    }

    // MODIFIÉ : génère un ennemi à une tile libre (essais limités)
    public Enemy generateRandomEnemy(ProjectileManager projectileManager, GameMap gameMap) {
        int attempts = 50;
        while (attempts-- > 0) {
            int minTile = 3;
            int maxTile = ROOM_SIZE - 4; // évite trop près des bords
            int tx = minTile + (int) (Math.random() * (maxTile - minTile + 1));
            int ty = minTile + (int) (Math.random() * (maxTile - minTile + 1));

            // si la tile est valide, créer l'ennemi au centre de la tile
            if (isTileValidForSpawn(tx, ty)) {
                double randomX = tx * TILE_SIZE + TILE_SIZE / 2.0;
                double randomY = ty * TILE_SIZE + TILE_SIZE / 2.0;

                int numRandom = (int) (Math.random() * 3);
                switch (numRandom) {
                    case 0:
                        return new Wanderer(randomX, randomY, 3, 1, 1.0, gameMap);
                    case 1:
                        if (projectileManager != null) {
                            return new StaticShooter(randomX, randomY, 3, 1, projectileManager);
                        } else {
                            return new Wanderer(randomX, randomY, 3, 1, 1.0, gameMap);
                        }
                    case 2:
                        return new Follower(randomX, randomY, 3, 1, 0.5, gameMap);
                }
            }
        }
        // si aucun emplacement trouvé, fallback : retourne null (ou un Wanderer au centre si tu préfères)
        return null;
    }

    // MODIFIÉ : génère plusieurs ennemis en utilisant la fonction ci‑dessus
    private void generateRandomEnemies(ProjectileManager projectileManager, GameMap gameMap) {
        int numEnemies = 2 + (int)(Math.random() * 4); // 2 à 5 ennemis

        for (int i = 0; i < numEnemies; i++) {
            Enemy enemy = generateRandomEnemy(projectileManager, gameMap);
            if (enemy != null) {
                enemies.add(enemy);
            } else {
                // si on n'a pas trouvé de place, on arrête les essais pour éviter boucle longue
                break;
            }
        }
    }

    // Spawn du boss au centre
    public void spawnBoss() {
        double centerX = 5.5 * 32;
        double centerY = 5.5 * 32;
        enemies.add(new BossEnemy(centerX, centerY));
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

    // WALLS
    public List<Wall> getWalls() {
        return walls;
    }

    
    
}
