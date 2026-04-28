public final class MapDimensions {
    private MapDimensions() {
        // Utility class
    }

    public static final int GRID_SIZE = 8;
    public static final int TILE_SIZE = 32;
    public static final int ROOM_SIZE = 15; // Must be an odd number to have a central tile
    public static final int DOOR_POSITION = ROOM_SIZE / 2;

    public static final int ROOM_PIXEL_SIZE = ROOM_SIZE * TILE_SIZE;
    public static final double ROOM_CENTER_X = ROOM_PIXEL_SIZE / 2.0;
    public static final double ROOM_CENTER_Y = ROOM_PIXEL_SIZE / 2.0;

    public static final int PLAYER_SIZE = TILE_SIZE / 2;
    public static final int ENEMY_SIZE = (int) Math.round(TILE_SIZE * 0.75);
    public static final int BOSS_SIZE = TILE_SIZE;
    public static final int PROJECTILE_HITBOX_SIZE = 4;
    public static final int DEFAULT_TEAR_SIZE = 8;

    public static final double PLAYER_RENDER_SCALE = 0.5;  // 50% of tile
    public static final double ENEMY_RENDER_SCALE = 0.8;   // 80% of tile
    public static final double BOSS_RENDER_SCALE = 1.2;    // 120% of tile
    public static final double PROJECTILE_RENDER_SCALE = DEFAULT_TEAR_SIZE / (double) TILE_SIZE;  // Dynamic from tear size
}
