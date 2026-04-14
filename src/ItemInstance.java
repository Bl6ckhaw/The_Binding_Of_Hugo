public class ItemInstance {

    private static final double TILE_SIZE = 32; // Assuming each tile is 32x32 pixels
    private static final double ROOM_CENTER_X = 5 * TILE_SIZE + TILE_SIZE / 2; // Center of the room in X
    private static final double ROOM_CENTER_Y = 5 * TILE_SIZE + TILE_SIZE / 2; // Center of the room in Y
    private final double x;
    private final double y;
    private boolean collected;
    private final ItemDefinition definition;

    public ItemInstance(ItemDefinition definition) {
        this.definition = definition;
        this.collected = false;
        this.x = ROOM_CENTER_X;
        this.y = ROOM_CENTER_Y;
    }


    public boolean isCollected() {
        return collected;
    }

    public void collect() {
        this.collected = true;
    }

    public ItemDefinition getDefinition() {
        return definition;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }


}
