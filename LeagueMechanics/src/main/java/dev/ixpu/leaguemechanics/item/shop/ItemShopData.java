package dev.ixpu.leaguemechanics.item.shop;

import org.bukkit.inventory.ItemRarity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemShopData {
    private static ItemShopData instance;
    private final Map<String, Integer> itemPrices = new HashMap<>();
    private final Map<String, Integer> itemLimits = new HashMap<>();
    private final Map<String, Integer> itemOrders = new HashMap<>();
    private final Map<String, String> itemModels = new HashMap<>();
    private final Map<String, ItemRarity> itemRarities = new HashMap<>();
    private final Map<String, String> itemGroups = new HashMap<>();
    private final Map<String, String> itemCategories = new HashMap<>();
    private final Map<String, List<String>> itemRequiredItems = new HashMap<>();
    private final Map<String, String> shortDescriptions = new HashMap<>();

    private ItemShopData() {
        loadDefaultItems();
        loadDefaultModels();
        loadDefaultClassModels();
        loadDefaultRequiredItems();
        loadDefaultShortDescriptions();
    }

    public static ItemShopData getInstance() {
        if (instance == null) {
            instance = new ItemShopData();
        }
        return instance;
    }

    private void loadDefaultItems() {

        // --------------------------------- STARTER ITEMS ---------------------------------

        addItem("cull", 18, 1, 0, ItemRarity.COMMON, null, "main");
        addItem("dark-seal", 15, 1, 1, ItemRarity.COMMON, null, "main");
        addItem("dorans-blade", 18, 1, 2, ItemRarity.COMMON, "dorans", "main");
        addItem("dorans-bow", 17, 1, 3, ItemRarity.COMMON, "dorans", "main");
        addItem("dorans-helm", 18, 1, 4, ItemRarity.COMMON, "dorans", "main");
        addItem("dorans-ring", 17, 1, 5, ItemRarity.COMMON, "dorans", "main");
        addItem("dorans-shield", 18, 1, 6, ItemRarity.COMMON, "dorans", "main");
        addItem("boots", 14, 1, 7, ItemRarity.UNCOMMON, "boots", "main");

        // --------------------------------- BASIC ITEMS ---------------------------------

        addItem("ruby-crystal", 17, 3, 8, ItemRarity.UNCOMMON, null, "main");
        addItem("dagger", 13, 3, 11, ItemRarity.UNCOMMON, null, "main");
        addItem("amplifying-tome", 17, 3, 0, ItemRarity.UNCOMMON, null, "mage");
        addItem("blasting-wand", 24, 3, 1, ItemRarity.UNCOMMON, null, "mage");
        addItem("needlessly-large-rod", 28, 3, 2, ItemRarity.UNCOMMON, null, "mage");
        addItem("long-sword", 15, 3, 0, ItemRarity.UNCOMMON, null, "fighter");
        addItem("pickaxe", 24, 3, 1, ItemRarity.UNCOMMON, null, "fighter");
        addItem("b.f-sword", 29, 3, 2, ItemRarity.UNCOMMON, null, "fighter");
        addItem("cloth-armor", 14, 3, 0, ItemRarity.UNCOMMON, null, "tank");
        addItem("rejuvenation-bead", 14, 3, 9, ItemRarity.UNCOMMON, null, "support");
        addItem("faeri-charm", 11, 3, 10, ItemRarity.UNCOMMON, null, "support");
        addItem("glowing-mote", 13, 3, 0, ItemRarity.UNCOMMON, null, "support");

        // --------------------------------- EPIC ITEMS ---------------------------------

        addItem("null-magic-mantle", 17, 3, 1, ItemRarity.UNCOMMON, null, "tank");
        addItem("chain-vest", 29, 3, 2, ItemRarity.RARE, null, "tank");
        addItem("negatron-cloak", 29, 3, 3, ItemRarity.RARE, null, "tank");
        addItem("blightting-jewel", 33, 1, 3, ItemRarity.RARE, null, "mage");
        addItem("flendish-codex", 26, 3, 4, ItemRarity.RARE, null, "mage");
        addItem("lost-chapter", 30, 3, 5, ItemRarity.RARE, null, "mage");
        addItem("fated-ashes", 30, 1, 3, ItemRarity.RARE, null, "mage");
        addItem("hextech-alternator", 30, 1, 7, ItemRarity.RARE, null, "mage");
        addItem("oblivion-orb", 28, 3, 8, ItemRarity.RARE, null, "mage");
        addItem("caulfields-warhammer", 27, 3, 3, ItemRarity.RARE, null, "fighter");
        addItem("hexdrinker", 35, 3, 4, ItemRarity.RARE, null, "fighter");
        addItem("serrated-dirk", 25, 1, 5, ItemRarity.RARE, null, "fighter");
        addItem("phage", 30, 3, 6, ItemRarity.RARE, null, "fighter");
        addItem("executioners-calling", 25, 1, 7, ItemRarity.RARE, null, "fighter");
        addItem("crystalline-bracer", 24, 3, 4, ItemRarity.RARE, null, "tank");
        addItem("giants-belt", 30, 3, 5, ItemRarity.RARE, null, "tank");
        addItem("bramble-vest", 26, 1, 7, ItemRarity.RARE, null, "tank");
        addItem("cloak-of-agility", 20, 3, 0, ItemRarity.UNCOMMON, null, "marksman");
        addItem("noonquiver", 35, 3, 1, ItemRarity.RARE, null, "marksman");
        addItem("rectrix", 25, 3, 2, ItemRarity.RARE, null, "marksman");
        addItem("recurve-bow", 30, 3, 3, ItemRarity.RARE, null, "marksman");
        addItem("scouts-slingshot", 25, 1, 4, ItemRarity.RARE, null, "marksman");
        addItem("last-whisper", 35, 1, 5, ItemRarity.RARE, null, "marksman");
        addItem("hearthbound-axe", 30, 3, 6, ItemRarity.RARE, null, "marksman");
        addItem("zeal", 30, 3, 7, ItemRarity.RARE, null, "marksman");
        addItem("berserkers-greaves", 30, 1, 12, ItemRarity.RARE, "boots", "main");
        addItem("mercurys-treads", 33, 1, 13, ItemRarity.RARE, "boots", "main");
        addItem("plated-steelcaps", 35, 1, 14, ItemRarity.RARE, "boots", "main");
        addItem("sorcerers-shoes", 33, 1, 15, ItemRarity.RARE, "boots", "main");
        addItem("spectres-cowl", 30, 3, 8, ItemRarity.RARE, null, "tank");
        addItem("winged-moonplate", 28, 3, 9, ItemRarity.RARE, null, "tank");
        addItem("wardens-mail", 29, 1, 10, ItemRarity.RARE, null, "tank");
        addItem("bamis-cinder", 28, 1, 11, ItemRarity.RARE, null, "tank");
        addItem("verdant-barrier", 34, 1, 9, ItemRarity.RARE, null, "mage");
        addItem("aether-wisp", 27, 3, 10, ItemRarity.RARE, null, "mage");
        addItem("vampiric-scepter", 24, 3, 8, ItemRarity.RARE, null, "fighter");
        addItem("tunneler", 32, 3, 9, ItemRarity.RARE, null, "fighter");
        addItem("the-brutalizer", 31, 3, 10, ItemRarity.RARE, null, "fighter");
        addItem("steel-sigil", 27, 3, 11, ItemRarity.RARE, null, "fighter");
        addItem("bandleglass-mirror", 25, 3, 11, ItemRarity.RARE, null, "support");
        addItem("kindlegem", 25, 3, 6, ItemRarity.RARE, null, "support");
    }

    private void loadDefaultRequiredItems() {
        addRequiredItem("chain-vest", "cloth-armor");
        addRequiredItem("negatron-cloak", "null-magic-mantle");
        addRequiredItem("blightting-jewel", "amplifying-tome");
        addRequiredItem("bramble-vest", "cloth-armor", "cloth-armor");
        addRequiredItem("caulfields-warhammer", "long-sword", "long-sword", "glowing-mote");
        addRequiredItem("crystalline-bracer", "ruby-crystal", "rejuvenation-bead");
        addRequiredItem("executioners-calling", "long-sword");
        addRequiredItem("fated-ashes", "amplifying-tome");
        addRequiredItem("flendish-codex", "amplifying-tome", "glowing-mote");
        addRequiredItem("giants-belt", "ruby-crystal");
        addRequiredItem("hearthbound-axe", "long-sword", "long-sword", "dagger");
        addRequiredItem("hexdrinker", "long-sword", "long-sword", "null-magic-mantle");
        addRequiredItem("hextech-alternator", "amplifying-tome", "amplifying-tome");
        addRequiredItem("kindlegem", "ruby-crystal", "glowing-mote");
        addRequiredItem("last-whisper", "long-sword", "long-sword");
        addRequiredItem("lost-chapter", "amplifying-tome", "glowing-mote");
        addRequiredItem("noonquiver", "long-sword", "cloak-of-agility");
        addRequiredItem("oblivion-orb", "amplifying-tome");
        addRequiredItem("phage", "ruby-crystal", "long-sword");
        addRequiredItem("rectrix", "long-sword");
        addRequiredItem("recurve-bow", "dagger");
        addRequiredItem("scouts-slingshot", "dagger", "dagger");
        addRequiredItem("serrated-dirk", "long-sword", "long-sword");
        addRequiredItem("spectres-cowl", "ruby-crystal", "null-magic-mantle", "rejuvenation-bead");
        addRequiredItem("vampiric-scepter", "long-sword");
        addRequiredItem("tunneler", "long-sword", "ruby-crystal");
        addRequiredItem("zeal", "cloak-of-agility", "dagger");
        addRequiredItem("winged-moonplate", "ruby-crystal");
        addRequiredItem("wardens-mail", "cloth-armor", "cloth-armor");
        addRequiredItem("verdant-barrier", "amplifying-tome", "amplifying-tome", "null-magic-mantle");
        addRequiredItem("the-brutalizer", "pickaxe", "glowing-mote");
        addRequiredItem("steel-sigil", "cloth-armor", "cloth-armor", "long-sword");
        addRequiredItem("aether-wisp", "amplifying-tome");
        addRequiredItem("bamis-cinder", "ruby-crystal", "glowing-mote");
        addRequiredItem("bandleglass-mirror", "faeri-charm", "amplifying-tome", "glowing-mote");
        addRequiredItem("berserkers-greaves", "boots", "dagger", "dagger");
        addRequiredItem("mercurys-treads", "boots", "null-magic-mantle");
        addRequiredItem("plated-steelcaps", "boots", "cloth-armor");
        addRequiredItem("sorcerers-shoes", "boots");
    }

    private void loadDefaultShortDescriptions() {
        setShortDesc("executioners-calling", "§2Anti Heal");
        setShortDesc("oblivion-orb", "§2Anti Heal");
        setShortDesc("bramble-vest", "§6Anti Attacker");
        setShortDesc("wardens-mail", "§6Anti Attacker");
        setShortDesc("null-magic-mantle", "§9Anti Magic");
        setShortDesc("negatron-cloak", "§9Anti Magic");
        setShortDesc("serrated-dirk", "§eAnti Armor");
        setShortDesc("last-whisper", "§eAnti Armor");
        setShortDesc("the-brutalizer", "§eAnti Armor");
    }

    public void setShortDesc(String itemId, String description) {
        if (itemId == null || description == null) return;
        shortDescriptions.put(itemId, description);
    }
    public String getShortDesc(String itemId) {
        if (itemId == null) return null;
        return shortDescriptions.get(itemId);
    }

    private void loadDefaultModels() {
        itemModels.put("cull", "minecraft:netherite_hoe");
        itemModels.put("dorans-blade", "minecraft:copper_sword");
        itemModels.put("dorans-bow", "minecraft:bow");
        itemModels.put("dorans-ring", "minecraft:music_disc_11");
        itemModels.put("dorans-helm", "minecraft:iron_helmet");
        itemModels.put("dorans-shield", "minecraft:shield");
        itemModels.put("dark-seal", "minecraft:popped_chorus_fruit");
        itemModels.put("boots", "minecraft:leather_boots");
        itemModels.put("amplifying-tome", "minecraft:book");
        itemModels.put("blasting-wand", "minecraft:golden_spear");
        itemModels.put("needlessly-large-rod", "minecraft:breeze_rod");
        itemModels.put("long-sword", "minecraft:iron_sword");
        itemModels.put("pickaxe", "minecraft:iron_pickaxe");
        itemModels.put("dagger", "minecraft:wooden_sword");
        itemModels.put("b.f-sword", "minecraft:diamond_sword");
        itemModels.put("cloth-armor", "minecraft:leather_chestplate");
        itemModels.put("null-magic-mantle", "minecraft:copper_nautilus_armor");
        itemModels.put("chain-vest", "minecraft:chainmail_chestplate");
        itemModels.put("negatron-cloak", "minecraft:iron_nautilus_armor");
        itemModels.put("rejuvenation-bead", "minecraft:green_bundle");
        itemModels.put("faeri-charm", "minecraft:orange_bundle");
        itemModels.put("ruby-crystal", "minecraft:red_dye");
        itemModels.put("blightting-jewel", "minecraft:amethyst_shard");
        itemModels.put("bramble-vest", "minecraft:iron_chestplate");
        itemModels.put("caulfields-warhammer", "minecraft:mace");
        itemModels.put("crystalline-bracer", "minecraft:copper_chestplate");
        itemModels.put("executioners-calling", "minecraft:trident");
        itemModels.put("fated-ashes", "minecraft:nether_star");
        itemModels.put("flendish-codex", "minecraft:writable_book");
        itemModels.put("giants-belt", "minecraft:lead");
        itemModels.put("hearthbound-axe", "minecraft:diamond_axe");
        itemModels.put("hexdrinker", "minecraft:copper_spear");
        itemModels.put("hextech-alternator", "minecraft:heavy_core");
        itemModels.put("kindlegem", "minecraft:redstone");
        itemModels.put("last-whisper", "minecraft:crossbow");
        itemModels.put("lost-chapter", "minecraft:paper");
        itemModels.put("cloak-of-agility", "minecraft:elytra");
        itemModels.put("noonquiver", "minecraft:diamond_spear");
        itemModels.put("oblivion-orb", "minecraft:slime_ball");
        itemModels.put("phage", "minecraft:wooden_axe");
        itemModels.put("rectrix", "minecraft:nether_wart");
        itemModels.put("recurve-bow", "minecraft:bow");
        itemModels.put("scouts-slingshot", "minecraft:fishing_rod");
        itemModels.put("serrated-dirk", "minecraft:wooden_spear");
        itemModels.put("spectres-cowl", "minecraft:waxed_oxidized_copper_golem_statue");
        itemModels.put("vampiric-scepter", "minecraft:iron_hoe");
        itemModels.put("tunneler", "minecraft:diamond_shovel");
        itemModels.put("zeal", "minecraft:golden_sword");
        itemModels.put("winged-moonplate", "minecraft:iron_leggings");
        itemModels.put("wardens-mail", "minecraft:golden_chestplate");
        itemModels.put("verdant-barrier", "minecraft:golden_nautilus_armor");
        itemModels.put("the-brutalizer", "minecraft:stone_axe");
        itemModels.put("steel-sigil", "minecraft:music_disc_chirp");
        itemModels.put("aether-wisp", "minecraft:ghast_tear");
        itemModels.put("bamis-cinder", "minecraft:fire_charge");
        itemModels.put("bandleglass-mirror", "minecraft:end_crystal");
        itemModels.put("berserkers-greaves", "minecraft:chainmail_boots");
        itemModels.put("mercurys-treads", "minecraft:diamond_boots");
        itemModels.put("plated-steelcaps", "minecraft:iron_boots");
        itemModels.put("sorcerers-shoes", "minecraft:netherite_boots");
        itemModels.put("glowing-mote", "minecraft:glow_ink_sac");
    }

    private final Map<String, String> classModels = new HashMap<>();

    private void loadDefaultClassModels() {
        classModels.put("fighter", "minecraft:netherite_sword");
        classModels.put("support", "minecraft:fishing_rod");
        classModels.put("assassin", "minecraft:wooden_sword");
        classModels.put("mage", "minecraft:enchanted_book");
        classModels.put("tank", "minecraft:iron_chestplate");
        classModels.put("marksman", "minecraft:bow");
    }

    public String getClassModel(String classId) {
        return classModels.get(classId);
    }

    private void addItem(String itemId, int price, int limit, int order, ItemRarity rarity, String group, String category) {
        itemPrices.put(itemId, price);
        itemLimits.put(itemId, limit);
        itemOrders.put(itemId, order);
        itemRarities.put(itemId, rarity);
        if (group != null) {
            itemGroups.put(itemId, group);
        }
        if (category != null) {
            itemCategories.put(itemId, category);
        }
    }

    private void addRequiredItem(String itemId, String... requiredIds) {
        itemRequiredItems.put(itemId, List.of(requiredIds));
    }

    public int getPrice(String itemId) {
        return itemPrices.getOrDefault(itemId, 0);
    }

    public int getLimit(String itemId) {
        return itemLimits.getOrDefault(itemId, 1);
    }

    public int getOrder(String itemId) {
        return itemOrders.getOrDefault(itemId, 0);
    }

    public boolean hasItem(String itemId) {
        return itemPrices.containsKey(itemId);
    }

    public String getModel(String itemId) {
        return itemModels.get(itemId);
    }

    public boolean hasCustomModel(String itemId) {
        return itemModels.containsKey(itemId);
    }

    public ItemRarity getRarity(String itemId) {
        return itemRarities.getOrDefault(itemId, ItemRarity.COMMON);
    }

    public String getGroup(String itemId) {
        return itemGroups.get(itemId);
    }

    public String getCategory(String itemId) {
        return itemCategories.get(itemId);
    }

    public List<String> getRequiredItems(String itemId) {
        return itemRequiredItems.getOrDefault(itemId, List.of());
    }
}