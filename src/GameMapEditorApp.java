import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class GameMapEditorApp extends Application {
    public static final int HEIGHT = 600;
    public static final int WIDTH = 800;
    
    private Canvas canvas;
    private GraphicsContext gc;
    private Room currentRoom;
    private Stage primaryStage;
    private ComboBox<String> toolSelector;
    private static final int TILE_SIZE = 32;
    private static final int ROOM_SIZE = 11;
    
    // Current selected tool
    private String selectedTool = "Wall";

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        // Initialize the room editor
        initializeEditor();
        
        // Create the main layout
        BorderPane root = new BorderPane();
        
        // Create canvas for room editing
        canvas = new Canvas(ROOM_SIZE * TILE_SIZE, ROOM_SIZE * TILE_SIZE);
        gc = canvas.getGraphicsContext2D();
        
        // Create toolbar
        VBox toolbar = createToolbar();
        
        // Create bottom controls
        HBox bottomControls = createBottomControls();
        
        // Layout setup
        root.setCenter(canvas);
        root.setLeft(toolbar);
        root.setBottom(bottomControls);
        root.setPadding(new Insets(10));
        
        // Set up mouse events for canvas
        setupCanvasEvents();
        
        // Initial render
        renderRoom();
        
        // Create scene and setup stage
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        primaryStage.setTitle("The Binding of Hugo - Map Editor");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    private void initializeEditor() {
        // Create a default empty room for editing
        currentRoom = new Room(RoomType.NORMAL, 0, 0);
        // Add some default doors
        currentRoom.addDoor(Direction.NORTH);
        currentRoom.addDoor(Direction.SOUTH);
    }
    
    private VBox createToolbar() {
        VBox toolbar = new VBox(10);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.TOP_LEFT);
        
        Label toolLabel = new Label("Tools:");
        toolLabel.setStyle("-fx-font-weight: bold;");
        
        // Tool selector
        toolSelector = new ComboBox<>();
        toolSelector.getItems().addAll("Wall", "Remove Wall");
        toolSelector.setValue("Wall");
        toolSelector.setOnAction(e -> selectedTool = toolSelector.getValue());
        
        // Clear button
        Button clearButton = new Button("Clear Room");
        clearButton.setOnAction(e -> {
            currentRoom = new Room(RoomType.NORMAL, 0, 0);
            // Re-add default doors
            currentRoom.addDoor(Direction.NORTH);
            currentRoom.addDoor(Direction.SOUTH);
            renderRoom();
        });
        
        toolbar.getChildren().addAll(toolLabel, toolSelector, clearButton);
        
        return toolbar;
    }
    
    private HBox createBottomControls() {
        HBox controls = new HBox(10);
        controls.setPadding(new Insets(10));
        controls.setAlignment(Pos.CENTER);
        
        Button saveButton = new Button("Save Room");
        saveButton.setOnAction(e -> saveRoom());
        
        Button loadButton = new Button("Load Room");
        loadButton.setOnAction(e -> loadRoom());
        
        Button testButton = new Button("Test Room");
        testButton.setOnAction(e -> testRoom());
        
        controls.getChildren().addAll(saveButton, loadButton, testButton);
        
        return controls;
    }
    
    private void setupCanvasEvents() {
        canvas.setOnMouseClicked(this::handleCanvasClick);
        canvas.setOnMouseDragged(this::handleCanvasClick);
    }
    
    private void handleCanvasClick(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();
        
        // Convert pixel coordinates to grid coordinates
        int gridX = (int) (x / TILE_SIZE);
        int gridY = (int) (y / TILE_SIZE);
        
        // Apply the selected tool
        switch (selectedTool) {
            case "Wall":
                addWall(gridX, gridY);
                break;
            case "Remove Wall":
                removeWall(gridX, gridY);
                break;
        }
        
        renderRoom();
    }
    
    private void addWall(int gridX, int gridY) {
        // Don't add walls on the border or door positions
        if (gridX <= 0 || gridX >= ROOM_SIZE - 1 || 
            gridY <= 0 || gridY >= ROOM_SIZE - 1 || 
            (gridX == 5 && (gridY == 1 || gridY == ROOM_SIZE - 2)) || 
            (gridY == 5 && (gridX == 1 || gridX == ROOM_SIZE - 2))) {
            return;
        }
        
        // Check if wall already exists
        boolean wallExists = currentRoom.getWalls().stream()
            .anyMatch(wall -> wall.blocksPosition(gridX, gridY));
        
        if (!wallExists) {
            currentRoom.getWalls().add(new Wall(gridX, gridY, 1, 1));
        }
    }
    
    private void removeWall(int gridX, int gridY) {
        currentRoom.getWalls().removeIf(wall -> wall.blocksPosition(gridX, gridY));
    }
    
    private void renderRoom() {
        // Clear canvas
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        // Render floor
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        // Render border walls
        gc.setFill(Color.YELLOW);
        gc.fillRect(0, 0, canvas.getWidth(), TILE_SIZE); // Top
        gc.fillRect(0, canvas.getHeight() - TILE_SIZE, canvas.getWidth(), TILE_SIZE); // Bottom
        gc.fillRect(0, 0, TILE_SIZE, canvas.getHeight()); // Left
        gc.fillRect(canvas.getWidth() - TILE_SIZE, 0, TILE_SIZE, canvas.getHeight()); // Right
        
        // Render walls
        gc.setFill(Color.GRAY);
        for (Wall wall : currentRoom.getWalls()) {
            gc.fillRect(wall.getX() * TILE_SIZE, wall.getY() * TILE_SIZE, 
                       wall.getWidth() * TILE_SIZE, wall.getHeight() * TILE_SIZE);
        }
        
        // Draw grid lines
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(1);
        for (int i = 0; i <= ROOM_SIZE; i++) {
            gc.strokeLine(i * TILE_SIZE, 0, i * TILE_SIZE, canvas.getHeight());
            gc.strokeLine(0, i * TILE_SIZE, canvas.getWidth(), i * TILE_SIZE);
        }
    }
    
    private void saveRoom() {
        TextInputDialog dialog = new TextInputDialog("room");
        dialog.setTitle("Save Room");
        dialog.setHeaderText("Enter a name for the room (no extension)");
        dialog.setContentText("Name:");
        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent()) return;
        String name = result.get().trim();
        if (name.isEmpty()) return;

        try {
            Path out = Paths.get("saved_rooms", name + ".txt");
            MapIO.saveRoom(currentRoom, out);
            System.out.println("Room saved to " + out.toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to save room: " + e.getMessage());
        }
    }

    private void loadRoom() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Room");
        File initialDir = Paths.get("saved_rooms").toFile();
        if (initialDir.exists()) chooser.setInitialDirectory(initialDir);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Room files", "*.txt", "*.room"));

        File file = chooser.showOpenDialog(primaryStage);
        if (file == null) return;

        try {
            Room loaded = MapIO.loadRoom(file.toPath());
            currentRoom = loaded;
            System.out.println("Room loaded from " + file.toPath().toAbsolutePath());
            renderRoom();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load room: " + e.getMessage());
        }
    }
    
    private void testRoom() {
        System.out.println("Test room functionality - to be implemented");
    }

    public static void main(String[] args) {
        launch(args);
    }
}