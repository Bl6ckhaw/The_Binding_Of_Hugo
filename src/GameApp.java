import java.util.HashSet;
import java.util.Set;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class GameApp extends Application {
    // Class attributes for global access
    private Player player;
    private RoomRenderer roomRenderer;
    private GameMap gameMap; // Instance of GameMap to access rooms
    private ProjectileManager projectileManager; // Instance to manage projectiles
    private EnemyManager enemyManager; // Instance to manage enemies
    private UIManager uiManager; // Instance to manage UI
    
    // Set to manage multiple key presses
    private Set<KeyCode> pressedKeys = new HashSet<>();
    
    // Cooldown for shooting
    private long lastShotTime = 0;
    private final long SHOT_COOLDOWN = 200_000_000; // 200ms in nanoseconds

    
    @Override
    public void start(Stage primaryStage){

        // Create a canvas for game rendering
        Canvas gameCanvas = new Canvas();
        // Create UI overlay canvas
        Canvas uiCanvas = new Canvas();

        // Renderer for rooms
        this.roomRenderer = new RoomRenderer(gameCanvas);

        // Initialize UIManager (for health bar, etc.)
        this.uiManager = new UIManager(gameCanvas.getWidth(), gameCanvas.getHeight());

        // Initialize player position (center of room)
        this.player = new Player(5 * 32 + 16, 5 * 32 + 16, 6, 1, 1.0);

        // Manages all projectiles in the game
        this.projectileManager = new ProjectileManager();

        // Manages all rooms and navigation
        this.gameMap = new GameMap(projectileManager);

        // Manages all enemies in the current room
        this.enemyManager = new EnemyManager();

        // Main game loop (AnimationTimer)
        AnimationTimer gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Handle continuous movement
                handleContinuousInput();

                // Handle shooting with cooldown
                handleShooting(now);

                // Update all projectiles
                projectileManager.updateAll();

                // Update all enemies (AI, movement, etc.)
                enemyManager.updateAll(player);
                enemyManager.checkProjectileCollisions(projectileManager);
                enemyManager.removeDeadEnemies();

                // Isaac-like: synchronize Room's enemy list with EnemyManager (for door logic)
                Room currentRoom = gameMap.getCurrentRoom();
                currentRoom.getEnemiesInternal().clear();
                currentRoom.getEnemiesInternal().addAll(enemyManager.getEnemies());
                // If all enemies are dead and room not yet marked as completed, open doors and mark as clear
                if (!currentRoom.isCompleted() && currentRoom.getEnemies().isEmpty()) {
                    currentRoom.setDoorsClosed(false);
                    currentRoom.setCompleted(true);
                    currentRoom.generateReward(); // Generate a reward for the room
                }

                // Check if player is hit by projectiles
                projectileManager.checkPlayerCollisions(player);

                // Remove projectiles that hit walls
                projectileManager.removeOutOfBounds(currentRoom);
                
                Reward reward = currentRoom.getRewards();
                if (reward != null) {
                    // Interaction with rewards
                    double dx = player.getX() - reward.getX();
                    double dy = player.getY() - reward.getY();
                    double distance = Math.hypot(dx, dy);

                    if (distance < 15) { // If player is close enough to the reward
                        System.err.println("[DEBUG] Player collected reward: " + reward.getType());
                        switch (reward.getType()) {
                            case HEALTH:
                                player.heal();
                                break;
                            case DAMAGE:
                                player.increaseDamage();
                                break;
                            case SPEED:
                                player.increaseSpeed();
                                break;
                            case TEARS_SIZE:
                                player.increaseTearsSize();
                                break;
                        }
                        currentRoom.setRewards(null); // Remove the reward (it will no longer be displayed or collectible)
                        
                        // show player's stats in console
                        System.err.println("[DEBUG] Player stats - Health: " + player.getHealth() +
                                            ", Damage: " + player.getDamage() +
                                            ", Speed: " + player.getSpeed() +
                                            ", Tears Size: " + player.getTearsSize());
                    }
                    
                }
                

                // Render everything (room, projectiles, enemies, UI)
                roomRenderer.renderRoom(currentRoom, player.getX(), player.getY());
                if (reward != null) {
                    roomRenderer.renderRewards(reward);
                }
                projectileManager.render(roomRenderer.getGraphicsContext());
                enemyManager.renderAll(roomRenderer.getGraphicsContext());
                uiManager.render(player);
            }
        };
        gameLoop.start();

        // Create the main layout and add both canvases
        StackPane root = new StackPane(gameCanvas, uiManager.getCanvas());
        Scene scene = new Scene(root);
        primaryStage.setTitle("The Binding of Hugo");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Handle key events for player movement
        scene.setOnKeyPressed(event -> pressedKeys.add(event.getCode()));
        scene.setOnKeyReleased(event -> pressedKeys.remove(event.getCode()));

    }
    
    // Handles continuous movement input (WASD/ZQSD)
    private void handleContinuousInput() {
        int moveSpeed = 1;

        // Move up
        if (pressedKeys.contains(KeyCode.Z)) {
            double newY = player.getY() - moveSpeed;
            if (CollisionSystem.canPlayerMoveTo(player.getX(), newY, gameMap.getCurrentRoom())) {
                player.move(0, -moveSpeed);
                navigation();
            }
        }
        // Move down
        if (pressedKeys.contains(KeyCode.S)) {
            double newY = player.getY() + moveSpeed;
            if (CollisionSystem.canPlayerMoveTo(player.getX(), newY, gameMap.getCurrentRoom())) {
                player.move(0, moveSpeed);
                navigation();
            }
        }
        // Move left
        if (pressedKeys.contains(KeyCode.Q)) {
            double newX = player.getX() - moveSpeed;
            if (CollisionSystem.canPlayerMoveTo(newX, player.getY(), gameMap.getCurrentRoom())) {
                player.move(-moveSpeed, 0);
                navigation();
            }
        }
        // Move right
        if (pressedKeys.contains(KeyCode.D)) {
            double newX = player.getX() + moveSpeed;
            if (CollisionSystem.canPlayerMoveTo(newX, player.getY(), gameMap.getCurrentRoom())) {
                player.move(moveSpeed, 0);
                navigation();
            }
        }
    }
    
    // Handles shooting input and cooldown
    private void handleShooting(long now) {
        // Check cooldown
        if (now - lastShotTime < SHOT_COOLDOWN) {
            return;
        }

        // Shoot in the direction of the arrow key pressed
        if (pressedKeys.contains(KeyCode.UP)) {
            createProjectile(Direction.NORTH);
            lastShotTime = now;
        } else if (pressedKeys.contains(KeyCode.DOWN)) {
            createProjectile(Direction.SOUTH);
            lastShotTime = now;
        } else if (pressedKeys.contains(KeyCode.LEFT)) {
            createProjectile(Direction.WEST);
            lastShotTime = now;
        } else if (pressedKeys.contains(KeyCode.RIGHT)) {
            createProjectile(Direction.EAST);
            lastShotTime = now;
        }
    }

    // Handles room navigation and Isaac-like door logic
    private void navigation() {
        Room currentRoom = gameMap.getCurrentRoom();
        // Isaac-like: block navigation if doors are closed
        if (currentRoom.areDoorsClosed()) {
            return;
        }

        // Check if player is near a door
        Direction nearDoor = gameMap.isPlayerNearDoor(player.getX(), player.getY());

        if (nearDoor != null) {
            // Player is near a door
            if (gameMap.getNextRoom() == null) {
                // First time near this door, set next room
                gameMap.setNextRoom(nearDoor);
            }
        } else {
            // Player is not near any door, reset nextRoom
            if (gameMap.getNextRoom() != null) {
                gameMap.resetNextRoom();
            }
        }

        // Check if player has exited the room
        if (gameMap.getNextRoom() != null && gameMap.hasPlayerExitedRoom(player.getX(), player.getY())) {
            // Determine actual exit direction based on player position
            Direction actualExitDirection = getActualExitDirection(player.getX(), player.getY());

            // Clear projectiles when switching rooms
            projectileManager.clearProjectiles();

            // Switch to the next room and reposition player
            gameMap.switchToNextRoom();

            // Load the new room's enemies into the EnemyManager
            Room newRoom = gameMap.getCurrentRoom();
            enemyManager.setEnemies(new java.util.ArrayList<>(newRoom.getEnemies()));

            // Isaac-like: close doors if room is not clear (except start/boss)
            if (!newRoom.isCompleted() && newRoom.getType() == RoomType.NORMAL) {
                newRoom.setDoorsClosed(true);
            } else {
                newRoom.setDoorsClosed(false);
            }

            // Reposition player based on entry direction
            int[] spawnPos = gameMap.getPlayerSpawnPosition(actualExitDirection);
            player.setPosition(spawnPos[0], spawnPos[1]);
        }
    }
    
    // Determines the actual exit direction based on player position
    private Direction getActualExitDirection(double playerX, double playerY) {
        final int TILE_SIZE = 32;
        final int ROOM_SIZE = 11;

        if (playerY < TILE_SIZE) return Direction.NORTH;
        if (playerY > (ROOM_SIZE - 1) * TILE_SIZE) return Direction.SOUTH;
        if (playerX > (ROOM_SIZE - 1) * TILE_SIZE) return Direction.EAST;
        if (playerX < TILE_SIZE) return Direction.WEST;

        return null; // Should not happen
    }

    // Creates a new projectile in the given direction
    private void createProjectile(Direction shootDirection) {
        Projectile projectile = new Projectile(
            player.getX(),
            player.getY(),
            player.getDamage(),
            2, // projectile speed
            (int) player.getTearsSize(), // projectile size
            shootDirection,
            ProjectileOwner.PLAYER,   // Shot by player
            ProjectileTarget.ENEMY    // Targets enemies
        );
        projectileManager.addProjectile(projectile);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
