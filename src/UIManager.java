import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Handles rendering of the user interface (UI), such as the health bar.
 */
public class UIManager {
    private Canvas uiCanvas;
    private GraphicsContext gc;

    // UI constants
    private static final int HEART_SIZE = 20;
    private static final int UI_MARGIN = 10;

    public UIManager(double width, double height) {
        this.uiCanvas = new Canvas(width, height);
        this.gc = uiCanvas.getGraphicsContext2D();

        // Set font style for UI text
        gc.setFont(Font.font("Arial", 14));
    }

    /**
     * Renders the UI for the player (currently only the health bar).
     */
    public void render(Player player) {
        // Clear previous UI
        gc.clearRect(0, 0, uiCanvas.getWidth(), uiCanvas.getHeight());

        // Draw the health bar
        drawHealthBar(player);
    }

    // Draws the player's health bar as a row of hearts
    private void drawHealthBar(Player player) {
        int currentHealth = player.getHealth();
        int maxHealth = player.getMaxHealth();

        // Position of the health bar
        int startX = UI_MARGIN;
        int startY = UI_MARGIN;

        // Draw the hearts (red for current health, gray for missing)
        for (int i = 0; i < maxHealth; i++) {
            int heartX = startX + i * (HEART_SIZE + UI_MARGIN);
            if (i < currentHealth) {
                gc.setFill(Color.RED); // Heart color for current health
            } else {
                gc.setFill(Color.GRAY); // Heart color for missing health
            }
            gc.fillOval(heartX, startY, HEART_SIZE, HEART_SIZE);
        }
    }

    // Returns the UI canvas for display
    public Canvas getCanvas() {
        return uiCanvas;
    }
}