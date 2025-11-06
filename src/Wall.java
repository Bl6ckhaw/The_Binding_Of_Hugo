public class Wall {
    private int x, y, width, height;

    public Wall(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public boolean blocksPosition(int tx, int ty) {
        return tx >= x && tx < x + width && ty >= y && ty < y + height;
    }

    @Override
    public String toString() {
        return x + "," + y + "," + width + "," + height;
    }
}
