import java.util.List;
import java.util.Random;

public final class ItemCatalog {
    private static final Random RNG = new Random();

    private static final List<ItemDefinition> ITEMS = List.of(
        new ItemDefinition(1, "Blood Vial", "+1 health", ItemRarity.COMMON, ItemStat.HEALTH, 1.0),
        new ItemDefinition(2, "Rusty Fang", "+1 damage", ItemRarity.RARE, ItemStat.DAMAGE, 1.0),
        new ItemDefinition(3, "Feather Charm", "+0.5 speed", ItemRarity.EPIC, ItemStat.SPEED, 0.5)
    );

    private ItemCatalog() {
    }

    public static ItemDefinition getRandomItemDefinition() {
        return ITEMS.get(RNG.nextInt(ITEMS.size()));
    }

    public static List<ItemDefinition> getAll() {
        return ITEMS;
    }
}
