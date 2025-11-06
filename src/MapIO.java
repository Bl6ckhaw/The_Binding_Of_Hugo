import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class MapIO {
    // Format simple:
    // type:NORMAL
    // doors:NORTH,SOUTH
    // wall:2,3,1,1
    // wall:4,5,1,2

    public static void saveRoom(Room room, Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("type:").append(room.getType().name()).append("\n");

        sb.append("doors:");
        boolean first = true;
        for (Direction d : room.getDirections()) {
            if (!first) sb.append(",");
            sb.append(d.name());
            first = false;
        }
        sb.append("\n");

        for (Wall w : room.getWalls()) {
            sb.append("wall:").append(w.getX()).append(",").append(w.getY()).append(",")
              .append(w.getWidth()).append(",").append(w.getHeight()).append("\n");
        }

        Files.createDirectories(path.getParent() == null ? Paths.get(".") : path.getParent());
        Files.write(path, sb.toString().getBytes());
    }

    public static Room loadRoom(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        RoomType type = RoomType.NORMAL;
        Room room = null;

        for (String line : lines) {
            if (line.startsWith("type:")) {
                type = RoomType.valueOf(line.substring(5).trim());
                room = new Room(type, 0, 0);
            } else if (line.startsWith("doors:")) {
                if (room == null) room = new Room(type, 0, 0);
                String rest = line.substring(6).trim();
                if (!rest.isEmpty()) {
                    String[] parts = rest.split(",");
                    for (String p : parts) {
                        room.addDoor(Direction.valueOf(p));
                    }
                }
            } else if (line.startsWith("wall:")) {
                if (room == null) room = new Room(type, 0, 0);
                String[] parts = line.substring(5).split(",");
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int w = Integer.parseInt(parts[2].trim());
                int h = Integer.parseInt(parts[3].trim());
                room.getWalls().add(new Wall(x, y, w, h));
            }
        }

        if (room == null) room = new Room(type, 0, 0);
        return room;
    }
}