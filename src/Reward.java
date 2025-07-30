public class Reward {
    private RewardType type;
    private double x, y;

    // CONSTANTS
    private static final double TILE_SIZE = 32; // Assuming each tile is 32x32 pixels
    private static final double ROOM_CENTER_X = 5 * TILE_SIZE + TILE_SIZE / 2; // Center of the room in X
    private static final double ROOM_CENTER_Y = 5 * TILE_SIZE + TILE_SIZE / 2; // Center of the room in Y

    public Reward(RewardType type) {
        this.type = type;
        
    }

    // Getters
    public RewardType getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
    // Sets the position of the reward in the middle of the room
        public void setPosition() {
        this.x = ROOM_CENTER_X;
        this.y = ROOM_CENTER_Y;
    }
}
