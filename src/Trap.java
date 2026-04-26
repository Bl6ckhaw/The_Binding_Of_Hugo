public class Trap {
    private double x, y;
    private boolean isVisible = true;

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
        this.x = MapDimensions.ROOM_CENTER_X;
        this.y = MapDimensions.ROOM_CENTER_Y;
    }
    
    public void activate() {
        isVisible = true;
    }
}
