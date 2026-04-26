public class ItemInstance {
    private final double x;
    private final double y;
    private boolean collected;
    private final ItemDefinition definition;

    public ItemInstance(ItemDefinition definition) {
        this.definition = definition;
        this.collected = false;
        this.x = MapDimensions.ROOM_CENTER_X;
        this.y = MapDimensions.ROOM_CENTER_Y;
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
