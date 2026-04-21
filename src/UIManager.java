import java.util.List;
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
    public void render(Player player, int currentLevel, List<ItemDefinition> collectedItems, int keyCount) {
        // Clear previous UI
        gc.clearRect(0, 0, uiCanvas.getWidth(), uiCanvas.getHeight());

        // Draw the health bar
        drawHealthBar(player);

        // Draw player stats
        drawPlayerStat(player);

        drawLevel(currentLevel);

        drawKeyCount(keyCount);

        drawCollectedItems(collectedItems);
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

    // Show all player stats in the UI
    public void drawPlayerStat(Player player) {
        gc.setFill(Color.BLUE);
        gc.fillText("Damage: " + player.getDamage(), 10, 40);
        gc.setFill(Color.GREEN);
        gc.fillText("Speed: " + player.getSpeed(), 10, 60);
        gc.setFill(Color.PURPLE);
        gc.fillText("Tears Size: " + player.getTearsSize(), 10, 80);
    }

    private void drawLevel(int currentLevel) {
        gc.setFill(Color.WHITE);
        gc.fillText("Level: " + currentLevel, 10, 100);
    }

    private void drawKeyCount(int keyCount) {
        gc.setFill(Color.GOLD);
        gc.fillText("Keys: " + keyCount, 10, 120);
    }

    private void drawCollectedItems(List<ItemDefinition> collectedItems) {
        double panelX = uiCanvas.getWidth() - 260;
        double startY = 40;

        gc.setFill(Color.color(0, 0, 0, 0.45));
        gc.fillRoundRect(panelX - 14, 12, 250, 260, 12, 12);

        gc.setFill(Color.WHITE);
        gc.fillText("Items", panelX, startY);

        if (collectedItems == null || collectedItems.isEmpty()) {
            gc.setFill(Color.LIGHTGRAY);
            gc.fillText("Aucun item ramasse", panelX, startY + 22);
            return;
        }

        int maxVisible = 10;
        int startIndex = Math.max(0, collectedItems.size() - maxVisible);
        for (int i = startIndex; i < collectedItems.size(); i++) {
            ItemDefinition item = collectedItems.get(i);
            double y = startY + 22 + (i - startIndex) * 20;
            gc.setFill(colorForRarity(item.getRarity()));
            gc.fillText("- " + item.getName(), panelX, y);
        }
    }

    private Color colorForRarity(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> Color.LIGHTGREEN;
            case RARE -> Color.DEEPSKYBLUE;
            case EPIC -> Color.GOLD;
            case LEGENDARY -> Color.CRIMSON;
        };
    }


    // Returns the UI canvas for display
    public Canvas getCanvas() {
        return uiCanvas;
    }
}