import java.util.ArrayDeque;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Enemy that follows the player and attacks in close range.
 */
public class Follower extends Enemy {
    private static final double ATTACK_RANGE = 25.0; // Distance to attack the player
    private static final long ATTACK_COOLDOWN = 1_000_000_000; // 1 second in nanoseconds
    private long lastAttackTime = 0;
    private final GameMap gameMap; // Reference to the game map for collision checks
    private List<Point2D> currentPath; // Current path to the player
    private int nextWaypointIndex; // Index of the next waypoint in the path
    private long lastPathUpdateTime = 0; // Last time the path was updated
    private static final long PATH_UPDATE_INTERVAL = 250_000_000; // Update path every 0.5 seconds
    private static final double WAYPOINT_REACH_THRESHOLD = 10.0; // Distance to consider a waypoint reached

    public Follower(double x, double y, int health, int damage, double speed, GameMap gameMap) {
        super(x, y, health, damage, speed);
        this.gameMap = gameMap;
        this.currentPath = Collections.emptyList();
        this.nextWaypointIndex = 0;
    }

    public Point2D getTilePosition() {
        int tileX = (int) (x / MapDimensions.TILE_SIZE);
        int tileY = (int) (y / MapDimensions.TILE_SIZE);
        return new Point2D(tileX, tileY);
    }

    @Override
    public void update(Player player, ProjectileManager projectileManager, GameMap gameMap) {
        if (isAlive) {
            // Calculate distance to the player
            double deltaX = player.getX() - this.x;
            double deltaY = player.getY() - this.y;
            double distance = Math.hypot(deltaX, deltaY);

            long now = System.nanoTime();
            if (now - lastPathUpdateTime >= PATH_UPDATE_INTERVAL || currentPath.isEmpty()) {
                Point2D myTile = this.getTilePosition();
                Point2D targetTile = player.getTilePosition();
                
                // Vérification : les deux tiles doivent être walkable
                if (gameMap.isWalkable((int)myTile.getX(), (int)myTile.getY()) && 
                    gameMap.isWalkable((int)targetTile.getX(), (int)targetTile.getY())) {
                    currentPath = bfsPath(gameMap, myTile, targetTile);
                    nextWaypointIndex = 1;  // Sauter la tuile de départ
                    lastPathUpdateTime = now;
                }
            }

            // Suivi du chemin via waypoints
            this.movementTowardsPlayer();

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
        
    // BFS returning path as list of tiles from start -> goal (inclusive).
    public static List<Point2D> bfsPath(GameMap map, Point2D start, Point2D goal) {
        int w = map.getWidth();
        int h = map.getHeight();
        boolean[][] visited = new boolean[w][h];
        Point2D[][] parent = new Point2D[w][h];
        int sx = (int)start.getX();
        int sy = (int)start.getY(); 

        ArrayDeque<Point2D> q = new ArrayDeque<>();
        q.add(start);
        visited[sx][sy] = true;

        // 4-directional moves
        int[] dx = {1,-1,0,0};
        int[] dy = {0,0,1,-1};

        while (!q.isEmpty()) {
            Point2D cur = q.poll();
            if (cur.equals(goal)) break;

            int cx = (int)cur.getX();
            int cy = (int)cur.getY();

            for (int i = 0; i < dx.length; i++) {
                int nx = cx + dx[i];
                int ny = cy + dy[i];
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                if (visited[nx][ny]) continue;
                if (!map.isWalkable(nx, ny)) continue;
                visited[nx][ny] = true;
                parent[nx][ny] = cur;
                q.add(new Point2D(nx, ny));
            }
        }

        int gx = (int)goal.getX();
        int gy = (int)goal.getY();
        

        // Reconstruct path
        if (!visited[gx][gy]) return Collections.emptyList(); // no path

        LinkedList<Point2D> path = new LinkedList<>();
        Point2D cur = goal;
        int cx = (int)cur.getX();
        int cy = (int)cur.getY();
        while (cur != null) {
            
            path.addFirst(cur);
            cur = parent[cx][cy];
            if (cur != null){
                cx = (int)cur.getX();
                cy = (int)cur.getY();
            }
        }
        return path;
    }
    
    // movement towards next waypoint in path — force axis-aligned (H/V) moves
    public void movementTowardsPlayer() {
        if (!currentPath.isEmpty() && nextWaypointIndex < currentPath.size()) {
            Point2D targetWaypoint = currentPath.get(nextWaypointIndex);

            // Convertir tile → pixel (centre de la tile)
            double targetX = targetWaypoint.getX() * MapDimensions.TILE_SIZE + MapDimensions.TILE_SIZE / 2.0;
            double targetY = targetWaypoint.getY() * MapDimensions.TILE_SIZE + MapDimensions.TILE_SIZE / 2.0;

            double dx = targetX - this.x;
            double dy = targetY - this.y;
            double dist = Math.hypot(dx, dy);

            if (dist > 0) {
                double absDx = Math.abs(dx);
                double absDy = Math.abs(dy);
                Room room = gameMap.getCurrentRoom();

                // Choose major axis first to get rectilinear movement
                if (absDx >= absDy) {
                    // Try horizontal move
                    double step = Math.min(speed, absDx);
                    double dir = Math.signum(dx);
                    double nx = this.x + dir * step;
                    double ny = this.y;
                    if (CollisionSystem.canEnemyMoveTo(nx, ny, room)) {
                        this.x = nx;
                    } else {
                        // fallback to vertical
                        if (absDy > 0) {
                            double vstep = Math.min(speed, absDy);
                            double vdir = Math.signum(dy);
                            double vx = this.x;
                            double vy = this.y + vdir * vstep;
                            if (CollisionSystem.canEnemyMoveTo(vx, vy, room)) {
                                this.y = vy;
                            }
                        }
                    }
                } else {
                    // Try vertical move first
                    double vstep = Math.min(speed, absDy);
                    double vdir = Math.signum(dy);
                    double vx = this.x;
                    double vy = this.y + vdir * vstep;
                    if (CollisionSystem.canEnemyMoveTo(vx, vy, room)) {
                        this.y = vy;
                    } else {
                        // fallback to horizontal
                        if (absDx > 0) {
                            double step = Math.min(speed, absDx);
                            double dir = Math.signum(dx);
                            double nx = this.x + dir * step;
                            double ny = this.y;
                            if (CollisionSystem.canEnemyMoveTo(nx, ny, room)) {
                                this.x = nx;
                            }
                        }
                    }
                }
            }

            // Advance to next waypoint if close enough. Snap to center only when reachable in one step.
            if (dist <= WAYPOINT_REACH_THRESHOLD) {
                nextWaypointIndex++;
                if (dist <= speed + 0.5) {
                    Room room = gameMap.getCurrentRoom();
                    if (!room.isPositionBlocked(targetX, targetY)) {
                        this.x = targetX;
                        this.y = targetY;
                    }
                }
            }
        }
    }

    @Override
    public void render(GraphicsContext gc, double screenX, double screenY, double tileSize) {
        if (isAlive) {
            double size = tileSize * MapDimensions.ENEMY_RENDER_SCALE;
            Color followerColor = Color.PURPLE;
            gc.setFill(followerColor);
            gc.fillOval(screenX - size / 2, screenY - size / 2, size, size);
        }
    }

}
