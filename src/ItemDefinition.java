public class ItemDefinition {

    private final int id;
    private final String name;
    private final String description;
    private final ItemRarity rarity;
    private final ItemStat stat;
    private final double amount;

    public ItemDefinition(int id, String name, String description, ItemRarity rarity, ItemStat stat, double amount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rarity = rarity;
        this.stat = stat;
        this.amount = amount;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ItemRarity getRarity() {
        return rarity;
    }

    public ItemStat getStat() {
        return stat;
    }

    public double getAmount() {
        return amount;
    }
    

}
