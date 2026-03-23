public class Trap {
    private double x, y;
    private boolean isVisible = true;

    // CONSTANTS
    private static final double TILE_SIZE = 32; // Assuming each tile is 32x32 pixels
    private static final double ROOM_CENTER_X = 5 * TILE_SIZE + TILE_SIZE / 2; // Center of the room in X
    private static final double ROOM_CENTER_Y = 5 * TILE_SIZE + TILE_SIZE / 2; // Center of the room in Y

    public Trap() {
        this.isVisible = false;
    }

    // Getters
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public boolean isVisible() {
        return isVisible;
    }
    
    // Sets the position of the trap door in the middle of the room
    public void setPosition() {
        this.x = ROOM_CENTER_X;
        this.y = ROOM_CENTER_Y;
    }
    
    public void activate() {
        isVisible = true;
    }
}
