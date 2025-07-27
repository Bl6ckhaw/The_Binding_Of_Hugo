

public class Player {
    private double x;         // Player's x position in pixels
    private double y;         // Player's y position in pixels
    private int health;       // Current health points
    private int maxHealth;    // Maximum health points
    private int damage;       // Damage dealt by the player
    private double speed;     // Movement speed
    private boolean isAlive;  // True if the player is alive

    public Player(double x, double y, int maxHealth, int damage, double speed) {
        this.x = x;
        this.y = y;
        this.maxHealth = maxHealth;
        this.health = maxHealth; // Start with full health
        this.damage = damage;
        this.speed = speed;
        this.isAlive = true; // Player is alive when created
    }

    // Applies damage to the player and updates alive state
    public void takeDamage(int dmg) {
        if (isAlive) {
            int nextHealth = health - dmg;
            if (nextHealth < 0) {
                health = 0; // Ensure health doesn't go below zero
                isAlive = false; // Player is dead
            } else {
                health = nextHealth;
            }
        } else {
            System.out.println("Player is already dead and cannot take more damage.");
            // Quit application (for the moment)
            System.exit(0);
        }
    }

    // Heals the player, but does not exceed max health
    public void heal(int amount) {
        int nextHealth = health + amount;
        if (nextHealth > maxHealth) {
            health = maxHealth;
        } else {
            health = nextHealth;
        }
    }

    // Sets the player's position in pixels
    public void setPosition(int newX, int newY) {
        this.x = newX;
        this.y = newY;
    }

    // Moves the player by the given delta values
    public void move(double deltaX, double deltaY) {
        this.x += deltaX;
        this.y += deltaY;
    }

    // Getters
    public double getX() {
        return x;
    }
    public double getY() {
        return y;
    }
    public int getHealth() {
        return health;
    }
    public int getMaxHealth() {
        return maxHealth;
    }
    public int getDamage() {
        return damage;
    }
    public double getSpeed() {
        return speed;
    }
    public boolean isAlive() {
        return isAlive;
    }
}