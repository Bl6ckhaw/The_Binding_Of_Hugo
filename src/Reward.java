public class Reward {
    private RewardType type;
    private double x, y;

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
        this.x = MapDimensions.ROOM_CENTER_X;
        this.y = MapDimensions.ROOM_CENTER_Y;
    }
}
