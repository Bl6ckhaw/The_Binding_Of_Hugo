import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

public class GameApp extends Application {
    // Class attributes for global access
    private Player player;
    private RoomRenderer roomRenderer;
    private GameMap gameMap; // Instance of GameMap to access rooms
    private ProjectileManager projectileManager; // Instance to manage projectiles
    private EnemyManager enemyManager; // Instance to manage enemies
    private UIManager uiManager; // Instance to manage UI
    private StackPane gameRoot;
    private MediaView transitionView;
    private MediaPlayer transitionPlayer;
    private PauseTransition transitionMinTimer;
    private static final int MIN_TRANSITION_MS = 2000;
    private boolean transitionVideoEnded = false;
    private boolean transitionMinTimeElapsed = false;
    private final List<ItemDefinition> collectedItems = new ArrayList<>();

    
    // Set to manage multiple key presses
    private final Set<KeyCode> pressedKeys = new HashSet<>();
    
    // Cooldown for shooting
    private long lastShotTime = 0;
    private final long SHOT_COOLDOWN = 200_000_000; // 200ms in nanoseconds

    // Screen dimensions for fullscreen
    private final double screenWidth = Screen.getPrimary().getBounds().getWidth();
    private final double screenHeight = Screen.getPrimary().getBounds().getHeight();

    // current level
    public static int currentLevel = 0;
    private boolean transitionInProgress = false;

    @Override
    public void start(Stage primaryStage) {
        showMenu(primaryStage);
    }

    private void showMenu(Stage primaryStage) {
        StackPane menuRoot = new StackPane();
        menuRoot.setStyle("-fx-background-color: #222;");
        Label title = new Label("The Binding of Hugo");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 36px;");
        Button startBtn = new Button("Jouer");
        startBtn.setStyle("-fx-font-size: 20px;");
        Button editMapBtn = new Button("Edit Map"); 
        editMapBtn.setStyle("-fx-font-size: 20px;"); 
        Button exitBtn = new Button("Quit");
        exitBtn.setStyle("-fx-font-size: 20px;");
        VBox vbox = new VBox(30, title, startBtn, editMapBtn, exitBtn);
        vbox.setAlignment(Pos.CENTER);
        menuRoot.getChildren().add(vbox);

        Scene menuScene = new Scene(menuRoot, 800, 600);
        primaryStage.setScene(menuScene);
        primaryStage.setFullScreen(true);
        primaryStage.show();

        startBtn.setOnAction(e -> startGame(primaryStage));
        // open the map editor in a new window
        editMapBtn.setOnAction(e -> {
            try {
                GameMapEditorApp editor = new GameMapEditorApp();
                Stage editorStage = new Stage();
                editor.start(editorStage);
            } catch (Exception ex) {
                System.err.println("Failed to open map editor: " + ex.getMessage());
            }
        });

        exitBtn.setOnAction(e -> primaryStage.close());
    
    }

    public void startGame(Stage primaryStage){
        currentLevel = 0;
        transitionInProgress = false;
        collectedItems.clear();

        // Create a canvas for game rendering
        Canvas gameCanvas = new Canvas(screenWidth, screenHeight);

        // Renderer for rooms
        this.roomRenderer = new RoomRenderer(gameCanvas);

        // Initialize UIManager (for health bar, etc.)
        this.uiManager = new UIManager(screenWidth, screenHeight);

        // Initialize player position (center of room)
        this.player = new Player(MapDimensions.ROOM_CENTER_X, MapDimensions.ROOM_CENTER_Y, 6, 1, 1.0);

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
                if (!player.isAlive()) {
                    this.stop(); // stop the game loop before changing scene
                    javafx.application.Platform.runLater(() -> showMenu(primaryStage));
                    return;
                }
                if (transitionInProgress) {
                    return;
                }
                // Handle continuous movement
                handleContinuousInput();

                // Handle shooting with cooldown
                handleShooting(now);

                // Update all projectiles
                projectileManager.updateAll();

                // Update all enemies (AI, movement, etc.)
                enemyManager.updateAll(player, projectileManager, gameMap);
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
                    
                    // Si c'est une salle BOSS, activer le trap au lieu de générer une récompense
                    if (currentRoom.getType() == RoomType.BOSS) {
                        currentRoom.generateReward(currentRoom.getType()); 
                        if (currentRoom.getTrap() != null) {
                            currentRoom.getTrap().activate(); 
                        }
                    } else if (currentRoom.getType() == RoomType.NORMAL) {
                        currentRoom.generateReward(currentRoom.getType()); 
                    }
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
                            case HEALTH -> {
                                if (player.getHealth() < player.getMaxHealth()) {
                                    player.heal();
                                    currentRoom.setRewards(null);
                                }
                            }
                            case DAMAGE -> {
                                player.increaseDamage();
                                currentRoom.setRewards(null);
                            }
                            case SPEED -> {
                                player.increaseSpeed();
                                currentRoom.setRewards(null);
                            }
                            case TEARS_SIZE -> {
                                player.increaseTearsSize();
                                currentRoom.setRewards(null);
                            }
                            case KEY -> {
                                player.addKey();
                                currentRoom.setRewards(null);
                            }
                        }
                         // Remove the reward (it will no longer be displayed or collectible)
                        
                        // show player's stats in console
                        System.err.println("[DEBUG] Player stats - Health: " + player.getHealth() +
                                            ", Damage: " + player.getDamage() +
                                            ", Speed: " + player.getSpeed() +
                                            ", Tears Size: " + player.getTearsSize());
                    }
                    
                }

                ItemInstance item = currentRoom.getItemInstance();
                if (item != null && !item.isCollected()) {
                    double dx = player.getX() - item.getX();
                    double dy = player.getY() - item.getY();
                    double distance = Math.hypot(dx, dy);

                    if (distance < 15) {
                        System.err.println("[DEBUG] Player collected item: " + item.getDefinition().getName());
                        applyItemEffect(item.getDefinition());
                        collectedItems.add(item.getDefinition());
                        item.collect();
                    }
                }

                // Check trap interaction (boss room)
                Trap trap = currentRoom.getTrap();
                if (trap != null && trap.isVisible()) {
                    double dx = player.getX() - trap.getX();
                    double dy = player.getY() - trap.getY();
                    double distance = Math.hypot(dx, dy);

                    if (distance < 20 && !transitionInProgress) {
                        advanceToNextLevel();
                        return;
                    }
                }

                double width = gameCanvas.getWidth();
                double height = gameCanvas.getHeight();
                double tileSize = Math.min(width, height) / MapDimensions.ROOM_SIZE;
                double offsetX = (width - tileSize * MapDimensions.ROOM_SIZE) / 2;
                double offsetY = (height - tileSize * MapDimensions.ROOM_SIZE) / 2;

                               

                // Render everything (room, projectiles, enemies, UI)
                roomRenderer.renderRoom(currentRoom, player.getX(), player.getY());

                if (reward != null) {
                    roomRenderer.renderRewards(reward);
                }
                if (item != null) {
                    roomRenderer.renderItem(item);
                }
                projectileManager.render(roomRenderer.getGraphicsContext(), tileSize, offsetX, offsetY);
                enemyManager.renderAll(roomRenderer.getGraphicsContext(), tileSize, offsetX, offsetY);

                // Draw trap above projectiles/enemies to keep it visible
                if (currentRoom.getTrap() != null && currentRoom.getTrap().isVisible()) {
                    roomRenderer.renderTrap(currentRoom.getTrap());
                }

                uiManager.render(player, currentLevel, collectedItems, player.getKeyCount());
            }
        };
        gameLoop.start();

        // Create the main layout and add both canvases
        gameRoot = new StackPane(gameCanvas, uiManager.getCanvas());
        transitionView = new MediaView();
        transitionView.setVisible(false);
        transitionView.setMouseTransparent(true);
        transitionView.setManaged(false);
        transitionView.setFitWidth(screenWidth);
        transitionView.setFitHeight(screenHeight);
        transitionView.setPreserveRatio(false);
        gameRoot.getChildren().add(transitionView);

        Scene scene = new Scene(gameRoot);
        primaryStage.setTitle("The Binding of Hugo");
        primaryStage.setScene(scene);
        primaryStage.setFullScreen(true); // Fullscreen mode
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
            // Check if the door is locked
            if (gameMap.isDoorLocked(nearDoor)) {
                // Try to unlock with a key
                if (!gameMap.unlockDoorWithKey(nearDoor, player)) {
                    // Door is locked and player doesn't have the key - can't proceed
                    gameMap.resetNextRoom();
                    return; // Exit early, don't set next room
                }
            }

            // Player is near a door (and not blocked by a lock)
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
            if (!newRoom.isCompleted() && (newRoom.getType() == RoomType.NORMAL || newRoom.getType() == RoomType.BOSS)) {
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
        final int TILE_SIZE = MapDimensions.TILE_SIZE;
        final int ROOM_SIZE = MapDimensions.ROOM_SIZE;

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

    private void applyItemEffect(ItemDefinition definition) {
        switch (definition.getStat()) {
            case HEALTH -> {
                int bonusHealth = (int) Math.round(definition.getAmount());
                player.setMaxHealth(bonusHealth);
            }
            case DAMAGE -> {
                int damageIncreases = (int) Math.round(definition.getAmount());
                for (int i = 0; i < damageIncreases; i++) {
                    player.increaseDamage();
                }
            }
            case SPEED -> {
                int speedSteps = (int) Math.round(definition.getAmount() / 0.5);
                for (int i = 0; i < speedSteps; i++) {
                    player.increaseSpeed();
                }
            }
            case TEARS_SIZE -> {
                int tearsSteps = (int) Math.round(definition.getAmount() / 0.5);
                for (int i = 0; i < tearsSteps; i++) {
                    player.increaseTearsSize();
                }
            }
        }

        System.err.println("[DEBUG] Player stats - Health: " + player.getHealth() +
                           ", Damage: " + player.getDamage() +
                           ", Speed: " + player.getSpeed() +
                           ", Tears Size: " + player.getTearsSize());
    }

    private void advanceToNextLevel() {
        transitionInProgress = true;
        pressedKeys.clear();
        transitionVideoEnded = false;
        transitionMinTimeElapsed = false;

        if (transitionMinTimer != null) {
            transitionMinTimer.stop();
        }
        transitionMinTimer = new PauseTransition(Duration.millis(MIN_TRANSITION_MS));
        transitionMinTimer.setOnFinished(event -> {
            transitionMinTimeElapsed = true;
            tryCompleteLevelTransition();
        });
        transitionMinTimer.playFromStart();

        String transitionPath = resolveTransitionMediaUri();
        if (transitionPath == null) {
            System.err.println("[DEBUG] Transition video not found. Skipping video transition.");
            transitionVideoEnded = true;
            tryCompleteLevelTransition();
            return;
        }

        System.err.println("[DEBUG] Transition media URI: " + transitionPath);

        Media media = new Media(transitionPath);
        transitionPlayer = new MediaPlayer(media);
        transitionPlayer.setAutoPlay(false);
        transitionPlayer.setOnEndOfMedia(() -> {
            transitionVideoEnded = true;
            tryCompleteLevelTransition();
        });
        transitionPlayer.setOnError(() -> {
            Throwable transitionError = transitionPlayer.getError();
            String message = transitionError != null ? transitionError.getMessage() : "unknown error";
            System.err.println("[DEBUG] Transition video error: " + message);
            transitionVideoEnded = true;
            tryCompleteLevelTransition();
        });
        media.setOnError(() -> {
            Throwable mediaError = media.getError();
            String message = mediaError != null ? mediaError.getMessage() : "unknown error";
            System.err.println("[DEBUG] Media loading error: " + message);
            transitionVideoEnded = true;
            tryCompleteLevelTransition();
        });
        transitionPlayer.setOnReady(() -> {
            transitionView.toFront();
            transitionView.setTranslateY(0);
            transitionView.setVisible(true);
            transitionPlayer.play();
        });

        transitionView.setMediaPlayer(transitionPlayer);
    }

    private void tryCompleteLevelTransition() {
        if (transitionVideoEnded && transitionMinTimeElapsed) {
            completeLevelTransition();
        }
    }

    private String resolveTransitionMediaUri() {
        Path localPath = Paths.get("cutScene", "transition.mp4").toAbsolutePath().normalize();
        if (Files.exists(localPath) && Files.isRegularFile(localPath)) {
            return localPath.toUri().toString();
        }

        URL classpathResource = getClass().getResource("/cutScene/transition.mp4");
        if (classpathResource != null) {
            return classpathResource.toExternalForm();
        }

        return null;
    }

    private void completeLevelTransition() {
        if (!transitionInProgress) {
            return;
        }

        if (transitionMinTimer != null) {
            transitionMinTimer.stop();
            transitionMinTimer = null;
        }

        if (transitionPlayer != null) {
            transitionPlayer.stop();
            transitionPlayer.dispose();
            transitionPlayer = null;
        }
        if (transitionView != null) {
            transitionView.setTranslateY(0);
            transitionView.setMediaPlayer(null);
            transitionView.setVisible(false);
        }

        currentLevel++;
        System.err.println("[DEBUG] Next level: " + currentLevel);

        projectileManager.clearProjectiles();
        gameMap = new GameMap(projectileManager);

        Room startRoom = gameMap.getCurrentRoom();
        enemyManager.setEnemies(new java.util.ArrayList<>(startRoom.getEnemies()));
        startRoom.setDoorsClosed(false);

        player.setPosition((int) MapDimensions.ROOM_CENTER_X, (int) MapDimensions.ROOM_CENTER_Y);
        transitionVideoEnded = false;
        transitionMinTimeElapsed = false;
        transitionInProgress = false;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
