public class Projectile {
    private double x;                  // X position in pixels
    private double y;                  // Y position in pixels
    private int damage;                // Damage dealt by the projectile
    private int speed;                 // Speed of the projectile
    private int size;                  // Visual size (diameter) of the projectile
    private Direction direction;       // Direction of movement
    private ProjectileOwner owner;     // Who fired the projectile (PLAYER or ENEMY)
    private ProjectileTarget target;   // Who is targeted (PLAYER or ENEMY)

    public Projectile(double x, double y, int damage, int speed, int size, Direction direction, 
                     ProjectileOwner owner, ProjectileTarget target){
        this.x = x;
        this.y = y;
        this.damage = damage;
        this.speed = speed;
        this.size = size;
        this.direction = direction;
        this.owner = owner;
        this.target = target;
    }

    // Updates the projectile's position based on its direction and speed
    public void update() {
        double dx = 0, dy = 0;
        switch(direction) {
            case NORTH: dy = -1; break;
            case SOUTH: dy = 1; break;
            case WEST:  dx = -1; break;
            case EAST:  dx = 1; break;
            case NORTH_EAST: dx = 1; dy = -1; break;
            case NORTH_WEST: dx = -1; dy = -1; break;
            case SOUTH_EAST: dx = 1; dy = 1; break;
            case SOUTH_WEST: dx = -1; dy = 1; break;
        }
        // Normalize the speed based on the direction
        if (direction == Direction.NORTH_EAST || direction == Direction.NORTH_WEST ||
            direction == Direction.SOUTH_EAST || direction == Direction.SOUTH_WEST) {
            dx /= Math.sqrt(2);
            dy /= Math.sqrt(2);
        }
        x += dx * speed;
        y += dy * speed;
    }

    // Returns true if the projectile is outside the room boundaries
    public boolean isOutofBounds(int roomWidth, int roomHeight) {
        return x < 0 || x > roomWidth || y < 0 || y > roomHeight;
    }

    // Getters
    public double getX() {
        return x;
    }
    public double getY() {
        return y;
    }
    public int getDamage() {
        return damage;
    }
    public int getSpeed() {
        return speed;
    }
    public int getSize() {
        return size;
    }
    public Direction getDirection() {
        return direction;
    }
    public ProjectileOwner getOwner() {
        return owner;
    }
    public ProjectileTarget getTarget() {
        return target;
    }

    // Compatibility method for legacy code: returns true if fired by the player
    public boolean isPlayerProjectile() {
        return owner == ProjectileOwner.PLAYER;
    }
}
