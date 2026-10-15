package dev.ixpu.leaguemechanics.item.shop;

import org.bukkit.configuration.ConfigurationSection;
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
    private final Map<String, String> fallbackItemModels = new HashMap<>();
    private final Map<String, ItemRarity> itemRarities = new HashMap<>();
    private final Map<String, String> itemGroups = new HashMap<>();
    private final Map<String, String> itemCategories = new HashMap<>();
    private final Map<String, List<String>> itemRequiredItems = new HashMap<>();
    private final Map<String, String> shortDescriptions = new HashMap<>();

    private ItemShopData() {
        loadDefaultItems();
        loadDefaultModels();
        loadFallbackModels();
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
        addItem("dark-seal", 15, 1, 1, ItemRarity.COMMON, "glory", "main");
        addItem("dorans-blade", 18, 1, 2, ItemRarity.COMMON, "dorans", "main");
        addItem("dorans-bow", 17, 1, 3, ItemRarity.COMMON, "dorans", "main");
        addItem("dorans-helm", 18, 1, 4, ItemRarity.COMMON, "dorans", "main");
        addItem("dorans-ring", 17, 1, 5, ItemRarity.COMMON, "dorans", "main");
        addItem("dorans-shield", 18, 1, 6, ItemRarity.COMMON, "dorans", "main");
        addItem("boots", 14, 1, 7, ItemRarity.UNCOMMON, "boots", "main");

        // --------------------------------- BASIC ITEMS ---------------------------------

        addItem("ruby-crystal", 17, 3, 8, ItemRarity.UNCOMMON, null, "main");
        addItem("dagger", 13, 3, 11, ItemRarity.UNCOMMON, null, "assassin");
        addItem("amplifying-tome", 17, 3, 0, ItemRarity.UNCOMMON, null, "mage");
        addItem("blasting-wand", 24, 3, 1, ItemRarity.UNCOMMON, null, "mage");
        addItem("needlessly-large-rod", 28, 3, 2, ItemRarity.UNCOMMON, null, "mage");
        addItem("long-sword", 15, 3, 0, ItemRarity.UNCOMMON, null, "fighter");
        addItem("pickaxe", 24, 3, 1, ItemRarity.UNCOMMON, null, "fighter");
        addItem("b.f-sword", 29, 3, 2, ItemRarity.UNCOMMON, null, "fighter");
        addItem("cloth-armor", 14, 3, 0, ItemRarity.UNCOMMON, null, "tank");
        addItem("rejuvenation-bead", 14, 3, 9, ItemRarity.UNCOMMON, null, "support");
        addItem("faeri-charm", 11, 3, 10, ItemRarity.UNCOMMON, null, "support");
        addItem("glowing-mote", 13, 3, 0, ItemRarity.UNCOMMON, null, "assassin");

        // --------------------------------- EPIC ITEMS ---------------------------------

        addItem("null-magic-mantle", 17, 3, 1, ItemRarity.UNCOMMON, null, "tank");
        addItem("chain-vest", 29, 3, 2, ItemRarity.RARE, null, "tank");
        addItem("negatron-cloak", 29, 3, 3, ItemRarity.RARE, null, "tank");
        addItem("blightting-jewel", 33, 1, 3, ItemRarity.RARE, "blight", "mage");
        addItem("flendish-codex", 26, 3, 4, ItemRarity.RARE, null, "mage");
        addItem("lost-chapter", 30, 3, 5, ItemRarity.RARE, null, "mage");
        addItem("fated-ashes", 30, 1, 3, ItemRarity.RARE, null, "mage");
        addItem("hextech-alternator", 30, 1, 7, ItemRarity.RARE, null, "mage");
        addItem("oblivion-orb", 28, 3, 8, ItemRarity.RARE, null, "mage");
        addItem("caulfields-warhammer", 27, 3, 3, ItemRarity.RARE, null, "assassin");
        addItem("hexdrinker", 35, 3, 4, ItemRarity.RARE, "lifeline", "assassin");
        addItem("serrated-dirk", 25, 1, 5, ItemRarity.RARE, null, "assassin");
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
        addItem("last-whisper", 35, 1, 5, ItemRarity.RARE, "fatality", "marksman");
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
        addItem("verdant-barrier", 34, 1, 9, ItemRarity.RARE, "annul", "mage");
        addItem("aether-wisp", 27, 3, 10, ItemRarity.RARE, null, "mage");
        addItem("vampiric-scepter", 24, 3, 8, ItemRarity.RARE, null, "fighter");
        addItem("tunneler", 32, 3, 9, ItemRarity.RARE, null, "fighter");
        addItem("the-brutalizer", 31, 3, 10, ItemRarity.RARE, null, "fighter");
        addItem("steel-sigil", 27, 3, 11, ItemRarity.RARE, null, "fighter");
        addItem("bandleglass-mirror", 25, 3, 11, ItemRarity.RARE, null, "support");
        addItem("kindlegem", 25, 3, 6, ItemRarity.RARE, null, "support");
        addItem("forbidden-idol", 60, 3, 20, ItemRarity.RARE, null, "support");
        addItem("haunting-guise", 130, 3, 20, ItemRarity.RARE, null, "mage");
        addItem("sheen", 90, 1, 20, ItemRarity.RARE, "spellblade", "fighter");
        addItem("tiamat", 120, 1, 21, ItemRarity.RARE, "hydra", "fighter");

        // --------------------------------- LEGENDARY ITEMS ---------------------------------

        addItem("abyssal-mask", 265, 1, 23, ItemRarity.EPIC, null, "tank");
        addItem("ardent-censer", 220, 1, 21, ItemRarity.EPIC, null, "support");
        addItem("atmas-reckoning", 250, 1, 20, ItemRarity.EPIC, null, "marksman");
        addItem("bandlepipes", 230, 1, 24, ItemRarity.EPIC, null, "support");
        addItem("banshees-veil", 300, 1, 33, ItemRarity.EPIC, "annul", "mage");
        addItem("black-cleaver", 300, 1, 23, ItemRarity.EPIC, "fatality", "fighter");
        addItem("blackfire-torch", 280, 1, 27, ItemRarity.EPIC, null, "mage");
        addItem("blade-of-the-ruined-king", 320, 1, 32, ItemRarity.EPIC, null, "fighter");
        addItem("bloodletters-curse", 290, 1, 30, ItemRarity.EPIC, null, "mage");
        addItem("bloodthirster", 340, 1, 39, ItemRarity.EPIC, null, "fighter");
        addItem("chempunk-chainsword", 300, 1, 24, ItemRarity.EPIC, null, "fighter");
        addItem("cosmic-drive", 300, 1, 34, ItemRarity.EPIC, null, "mage");
        addItem("cryptbloom", 300, 1, 35, ItemRarity.EPIC, "blight", "mage");
        addItem("dead-mans-plate", 290, 1, 27, ItemRarity.EPIC, null, "tank");
        addItem("deaths-dance", 330, 1, 35, ItemRarity.EPIC, null, "fighter");
        addItem("eclipse", 290, 1, 24, ItemRarity.EPIC, null, "assassin");
        addItem("edge-of-night", 300, 1, 25, ItemRarity.EPIC, "annul", "assassin");
        addItem("endless-hunger", 310, 1, 27, ItemRarity.EPIC, null, "fighter");
        addItem("essence-reaver", 305, 1, 28, ItemRarity.EPIC, "spellblade", "assassin");
        addItem("experimental-hexplate", 300, 1, 25, ItemRarity.EPIC, null, "fighter");
        addItem("force-of-nature", 280, 1, 25, ItemRarity.EPIC, null, "tank");
        addItem("guardian-angel", 320, 1, 30, ItemRarity.EPIC, null, "tank");
        addItem("guinsoos-rageblade", 300, 1, 27, ItemRarity.EPIC, null, "marksman");
        addItem("heartsteel", 300, 1, 29, ItemRarity.EPIC, null, "tank");
        addItem("hexoptics-c44", 280, 1, 26, ItemRarity.EPIC, null, "marksman");
        addItem("horizon-focus", 270, 1, 24, ItemRarity.EPIC, null, "mage");
        addItem("hubris", 280, 1, 22, ItemRarity.EPIC, null, "assassin");
        addItem("hullbreaker", 300, 1, 26, ItemRarity.EPIC, null, "fighter");
        addItem("iceborn-gauntlet", 290, 1, 28, ItemRarity.EPIC, "spellblade", "tank");
        addItem("immortal-shieldbow", 300, 1, 28, ItemRarity.EPIC, "lifeline", "marksman");
        addItem("imperial-mandate", 240, 1, 26, ItemRarity.EPIC, null, "support");
        addItem("infinity-edge", 350, 1, 35, ItemRarity.EPIC, null, "marksman");
        addItem("jaksho-the-protean", 320, 1, 31, ItemRarity.EPIC, null, "tank");
        addItem("knights-vow", 230, 1, 25, ItemRarity.EPIC, null, "support");
        addItem("kraken-slayer", 265, 1, 21, ItemRarity.EPIC, null, "marksman");
        addItem("liandrys-torment", 300, 1, 36, ItemRarity.EPIC, null, "mage");
        addItem("lich-bane", 290, 1, 31, ItemRarity.EPIC, "spellblade", "mage");
        addItem("lord-dominiks-regards", 330, 1, 34, ItemRarity.EPIC, "fatality", "marksman");
        addItem("ludens-echo", 275, 1, 26, ItemRarity.EPIC, null, "mage");
        addItem("malignance", 270, 1, 25, ItemRarity.EPIC, null, "mage");
        addItem("maw-of-malmortius", 310, 1, 28, ItemRarity.EPIC, "lifeline", "fighter");
        addItem("mejais-soulstealer", 150, 1, 21, ItemRarity.EPIC, "glory", "mage");
        addItem("mercurial-scimitar", 320, 1, 33, ItemRarity.EPIC, null, "fighter");
        addItem("morellonomicon", 285, 1, 29, ItemRarity.EPIC, null, "mage");
        addItem("mortal-reminder", 300, 1, 29, ItemRarity.EPIC, "fatality", "marksman");
        addItem("nashors-tooth", 290, 1, 32, ItemRarity.EPIC, null, "mage");
        addItem("navori-flickerblade", 265, 1, 22, ItemRarity.EPIC, null, "marksman");
        addItem("overlords-bloodmail", 330, 1, 32, ItemRarity.EPIC, null, "tank");
        addItem("phantom-dancer", 265, 1, 23, ItemRarity.EPIC, null, "marksman");
        addItem("rabadons-deathcap", 350, 1, 39, ItemRarity.EPIC, null, "mage");
        addItem("rapid-firecannon", 265, 1, 24, ItemRarity.EPIC, null, "marksman");
        addItem("ravenous-hydra", 330, 1, 36, ItemRarity.EPIC, "hydra", "fighter");
        addItem("riftmaker", 310, 1, 37, ItemRarity.EPIC, null, "mage");
        addItem("runaans-hurricane", 265, 1, 25, ItemRarity.EPIC, null, "marksman");
        addItem("rylais-crystal-scepter", 260, 1, 23, ItemRarity.EPIC, null, "mage");
        addItem("serpents-fang", 250, 1, 21, ItemRarity.EPIC, null, "assassin");
        addItem("seryldas-grudge", 300, 1, 26, ItemRarity.EPIC, "fatality", "assassin");
        addItem("shadowflame", 320, 1, 38, ItemRarity.EPIC, null, "mage");
        addItem("spear-of-shojin", 310, 1, 29, ItemRarity.EPIC, null, "fighter");
        addItem("spirit-visage", 270, 1, 24, ItemRarity.EPIC, null, "tank");
        addItem("staff-of-flowing-water", 225, 1, 23, ItemRarity.EPIC, null, "support");
        addItem("statikk-shiv", 300, 1, 30, ItemRarity.EPIC, null, "marksman");
        addItem("steraks-gage", 320, 1, 34, ItemRarity.EPIC, "lifeline", "fighter");
        addItem("stormrazor", 320, 1, 33, ItemRarity.EPIC, null, "marksman");
        addItem("stormsurge", 280, 1, 28, ItemRarity.EPIC, null, "mage");
        addItem("stridebreaker", 310, 1, 30, ItemRarity.EPIC, "hydra", "fighter");
        addItem("sundered-sky", 310, 1, 31, ItemRarity.EPIC, null, "fighter");
        addItem("sunfire-aegis", 280, 1, 26, ItemRarity.EPIC, null, "tank");
        addItem("terminus", 300, 1, 31, ItemRarity.EPIC, "fatality", "marksman");
        addItem("the-collector", 300, 1, 27, ItemRarity.EPIC, null, "assassin");
        addItem("thornmail", 245, 1, 22, ItemRarity.EPIC, null, "tank");
        addItem("titanic-hydra", 330, 1, 37, ItemRarity.EPIC, "hydra", "fighter");
        addItem("trinity-force", 333, 1, 38, ItemRarity.EPIC, "spellblade", "fighter");
        addItem("unending-despair", 188, 1, 20, ItemRarity.EPIC, null, "tank");
        addItem("void-staff", 200, 1, 22, ItemRarity.EPIC, "blight", "mage");
        addItem("voltaic-cyclosword", 200, 1, 20, ItemRarity.EPIC, null, "assassin");
        addItem("warmogs-armor", 210, 1, 21, ItemRarity.EPIC, null, "tank");
        addItem("wits-end", 280, 1, 22, ItemRarity.EPIC, null, "fighter");
        addItem("youmuus-ghostblade", 280, 1, 23, ItemRarity.EPIC, null, "assassin");
        addItem("yun-tal-wildarrows", 300, 1, 32, ItemRarity.EPIC, null, "marksman");
        addItem("zekes-convergence", 220, 1, 22, ItemRarity.EPIC, null, "support");
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
        addRequiredItem("sheen", "glowing-mote");
        addRequiredItem("tiamat", "long-sword", "long-sword");
        addRequiredItem("abyssal-mask", "kindlegem", "negatron-cloak");
        addRequiredItem("ardent-censer", "aether-wisp", "forbidden-idol");
        addRequiredItem("bandlepipes", "kindlegem", "cloth-armor", "null-magic-mantle");
        addRequiredItem("banshees-veil", "needlessly-large-rod", "verdant-barrier");
        addRequiredItem("black-cleaver", "phage", "kindlegem", "pickaxe");
        addRequiredItem("blackfire-torch", "lost-chapter", "fated-ashes");
        addRequiredItem("blade-of-the-ruined-king", "vampiric-scepter", "recurve-bow", "pickaxe");
        addRequiredItem("bloodletters-curse", "haunting-guise", "flendish-codex");
        addRequiredItem("bloodthirster", "b.f-sword", "pickaxe", "vampiric-scepter");
        addRequiredItem("chempunk-chainsword", "executioners-calling", "giants-belt", "caulfields-warhammer");
        addRequiredItem("cosmic-drive", "kindlegem", "aether-wisp", "flendish-codex");
        addRequiredItem("cryptbloom", "blightting-jewel", "flendish-codex", "flendish-codex");
        addRequiredItem("dead-mans-plate", "winged-moonplate", "ruby-crystal", "chain-vest");
        addRequiredItem("deaths-dance", "steel-sigil", "caulfields-warhammer", "pickaxe");
        addRequiredItem("eclipse", "caulfields-warhammer", "pickaxe", "long-sword");
        addRequiredItem("edge-of-night", "serrated-dirk", "tunneler");
        addRequiredItem("endless-hunger", "caulfields-warhammer", "pickaxe", "long-sword");
        addRequiredItem("essence-reaver", "sheen", "caulfields-warhammer", "cloak-of-agility");
        addRequiredItem("experimental-hexplate", "tunneler", "phage", "dagger");
        addRequiredItem("force-of-nature", "negatron-cloak", "ruby-crystal", "winged-moonplate");
        addRequiredItem("guardian-angel", "steel-sigil", "b.f-sword");
        addRequiredItem("guinsoos-rageblade", "amplifying-tome", "recurve-bow", "pickaxe");
        addRequiredItem("heartsteel", "giants-belt", "giants-belt", "crystalline-bracer");
        addRequiredItem("hexoptics-c44", "pickaxe", "noonquiver", "long-sword");
        addRequiredItem("horizon-focus", "flendish-codex", "flendish-codex", "amplifying-tome");
        addRequiredItem("hubris", "serrated-dirk", "caulfields-warhammer");
        addRequiredItem("hullbreaker", "tunneler", "winged-moonplate", "pickaxe");
        addRequiredItem("iceborn-gauntlet", "sheen", "ruby-crystal", "chain-vest");
        addRequiredItem("immortal-shieldbow", "pickaxe", "noonquiver");
        addRequiredItem("imperial-mandate", "amplifying-tome", "amplifying-tome", "bandleglass-mirror");
        addRequiredItem("infinity-edge", "b.f-sword", "pickaxe", "cloak-of-agility");
        addRequiredItem("jaksho-the-protean", "giants-belt", "chain-vest", "negatron-cloak");
        addRequiredItem("knights-vow", "kindlegem", "chain-vest", "rejuvenation-bead");
        addRequiredItem("kraken-slayer", "rectrix", "hearthbound-axe", "recurve-bow");
        addRequiredItem("liandrys-torment", "haunting-guise", "fated-ashes");
        addRequiredItem("lich-bane", "sheen", "aether-wisp", "blasting-wand");
        addRequiredItem("lord-dominiks-regards", "last-whisper", "noonquiver");
        addRequiredItem("ludens-echo", "lost-chapter", "hextech-alternator");
        addRequiredItem("malignance", "lost-chapter", "blasting-wand");
        addRequiredItem("maw-of-malmortius", "hexdrinker", "caulfields-warhammer");
        addRequiredItem("mejais-soulstealer", "dark-seal");
        addRequiredItem("mercurial-scimitar", "pickaxe", "vampiric-scepter");
        addRequiredItem("morellonomicon", "oblivion-orb", "blasting-wand", "kindlegem");
        addRequiredItem("mortal-reminder", "executioners-calling", "last-whisper", "cloak-of-agility");
        addRequiredItem("nashors-tooth", "recurve-bow", "blasting-wand", "flendish-codex");
        addRequiredItem("navori-flickerblade", "dagger", "dagger", "zeal");
        addRequiredItem("overlords-bloodmail", "tunneler", "tunneler");
        addRequiredItem("phantom-dancer", "dagger", "dagger", "zeal");
        addRequiredItem("rabadons-deathcap", "needlessly-large-rod", "needlessly-large-rod");
        addRequiredItem("rapid-firecannon", "zeal", "scouts-slingshot");
        addRequiredItem("ravenous-hydra", "tiamat", "vampiric-scepter", "caulfields-warhammer");
        addRequiredItem("riftmaker", "haunting-guise", "flendish-codex");
        addRequiredItem("runaans-hurricane", "zeal", "scouts-slingshot");
        addRequiredItem("rylais-crystal-scepter", "blasting-wand", "giants-belt", "amplifying-tome");
        addRequiredItem("serpents-fang", "serrated-dirk", "pickaxe");
        addRequiredItem("seryldas-grudge", "caulfields-warhammer", "last-whisper");
        addRequiredItem("shadowflame", "hextech-alternator", "needlessly-large-rod");
        addRequiredItem("spear-of-shojin", "pickaxe", "tunneler", "ruby-crystal");
        addRequiredItem("spirit-visage", "spectres-cowl", "kindlegem");
        addRequiredItem("staff-of-flowing-water", "flendish-codex", "forbidden-idol");
        addRequiredItem("statikk-shiv", "scouts-slingshot", "pickaxe", "aether-wisp");
        addRequiredItem("steraks-gage", "pickaxe", "tunneler", "ruby-crystal");
        addRequiredItem("stormrazor", "b.f-sword", "cloak-of-agility", "scouts-slingshot");
        addRequiredItem("stormsurge", "hextech-alternator", "aether-wisp");
        addRequiredItem("stridebreaker", "tiamat", "phage", "dagger");
        addRequiredItem("sundered-sky", "tunneler", "caulfields-warhammer");
        addRequiredItem("sunfire-aegis", "bamis-cinder", "chain-vest", "ruby-crystal");
        addRequiredItem("terminus", "hearthbound-axe", "recurve-bow");
        addRequiredItem("the-collector", "pickaxe", "serrated-dirk", "cloak-of-agility");
        addRequiredItem("thornmail", "bramble-vest", "chain-vest", "ruby-crystal");
        addRequiredItem("titanic-hydra", "sheen", "phage", "hearthbound-axe");
        addRequiredItem("trinity-force", "sheen", "phage", "hearthbound-axe");
        addRequiredItem("unending-despair", "chain-vest", "kindlegem", "ruby-crystal");
        addRequiredItem("void-staff", "blightting-jewel", "blasting-wand");
        addRequiredItem("voltaic-cyclosword", "the-brutalizer", "long-sword", "long-sword");
        addRequiredItem("warmogs-armor", "giants-belt", "giants-belt", "crystalline-bracer");
        addRequiredItem("wits-end", "recurve-bow", "recurve-bow", "negatron-cloak");
        addRequiredItem("youmuus-ghostblade", "serrated-dirk", "rectrix", "long-sword");
        addRequiredItem("yun-tal-wildarrows", "b.f-sword", "scouts-slingshot", "long-sword");
        addRequiredItem("zekes-convergence", "kindlegem", "cloth-armor", "null-magic-mantle");
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
        itemModels.put("cull", "leaguemechanics:cull");
        itemModels.put("dorans-blade", "leaguemechanics:dorans-blade");
        itemModels.put("dorans-bow", "leaguemechanics:dorans-bow");
        itemModels.put("dorans-ring", "leaguemechanics:dorans-ring");
        itemModels.put("dorans-helm", "leaguemechanics:dorans-helm");
        itemModels.put("dorans-shield", "leaguemechanics:dorans-shield");
        itemModels.put("dark-seal", "leaguemechanics:dark-seal");
        itemModels.put("boots", "leaguemechanics:boots");
        itemModels.put("amplifying-tome", "leaguemechanics:amplifying-tome");
        itemModels.put("blasting-wand", "leaguemechanics:blasting-wand");
        itemModels.put("needlessly-large-rod", "leaguemechanics:needlessly-large-rod");
        itemModels.put("long-sword", "leaguemechanics:long-sword");
        itemModels.put("pickaxe", "leaguemechanics:pickaxe");
        itemModels.put("dagger", "leaguemechanics:dagger");
        itemModels.put("b.f-sword", "leaguemechanics:b.f-sword");
        itemModels.put("cloth-armor", "leaguemechanics:cloth-armor");
        itemModels.put("null-magic-mantle", "leaguemechanics:null-magic-mantle");
        itemModels.put("chain-vest", "leaguemechanics:chain-vest");
        itemModels.put("negatron-cloak", "leaguemechanics:negatron-cloak");
        itemModels.put("rejuvenation-bead", "leaguemechanics:rejuvenation-bead");
        itemModels.put("faeri-charm", "leaguemechanics:faeri-charm");
        itemModels.put("ruby-crystal", "leaguemechanics:ruby-crystal");
        itemModels.put("blightting-jewel", "leaguemechanics:blightting-jewel");
        itemModels.put("bramble-vest", "leaguemechanics:bramble-vest");
        itemModels.put("caulfields-warhammer", "leaguemechanics:caulfields-warhammer");
        itemModels.put("crystalline-bracer", "leaguemechanics:crystalline-bracer");
        itemModels.put("executioners-calling", "leaguemechanics:executioners-calling");
        itemModels.put("fated-ashes", "leaguemechanics:fated-ashes");
        itemModels.put("flendish-codex", "leaguemechanics:flendish-codex");
        itemModels.put("giants-belt", "leaguemechanics:giants-belt");
        itemModels.put("hearthbound-axe", "leaguemechanics:hearthbound-axe");
        itemModels.put("hexdrinker", "leaguemechanics:hexdrinker");
        itemModels.put("hextech-alternator", "leaguemechanics:hextech-alternator");
        itemModels.put("kindlegem", "leaguemechanics:kindlegem");
        itemModels.put("last-whisper", "leaguemechanics:last-whisper");
        itemModels.put("lost-chapter", "leaguemechanics:lost-chapter");
        itemModels.put("cloak-of-agility", "leaguemechanics:cloak-of-agility");
        itemModels.put("noonquiver", "leaguemechanics:noonquiver");
        itemModels.put("oblivion-orb", "leaguemechanics:oblivion-orb");
        itemModels.put("phage", "leaguemechanics:phage");
        itemModels.put("rectrix", "leaguemechanics:rectrix");
        itemModels.put("recurve-bow", "leaguemechanics:recurve-bow");
        itemModels.put("scouts-slingshot", "leaguemechanics:scouts-slingshot");
        itemModels.put("serrated-dirk", "leaguemechanics:serrated-dirk");
        itemModels.put("spectres-cowl", "leaguemechanics:spectres-cowl");
        itemModels.put("vampiric-scepter", "leaguemechanics:vampiric-scepter");
        itemModels.put("tunneler", "leaguemechanics:tunneler");
        itemModels.put("zeal", "leaguemechanics:zeal");
        itemModels.put("winged-moonplate", "leaguemechanics:winged-moonplate");
        itemModels.put("wardens-mail", "leaguemechanics:wardens-mail");
        itemModels.put("verdant-barrier", "leaguemechanics:verdant-barrier");
        itemModels.put("the-brutalizer", "leaguemechanics:the-brutalizer");
        itemModels.put("steel-sigil", "leaguemechanics:steel-sigil");
        itemModels.put("aether-wisp", "leaguemechanics:aether-wisp");
        itemModels.put("bamis-cinder", "leaguemechanics:bamis-cinder");
        itemModels.put("bandleglass-mirror", "leaguemechanics:bandleglass-mirror");
        itemModels.put("berserkers-greaves", "leaguemechanics:berserkers-greaves");
        itemModels.put("mercurys-treads", "leaguemechanics:mercurys-treads");
        itemModels.put("plated-steelcaps", "leaguemechanics:plated-steelcaps");
        itemModels.put("sorcerers-shoes", "leaguemechanics:sorcerers-shoes");
        itemModels.put("glowing-mote", "leaguemechanics:glowing-mote");
        itemModels.put("forbidden-idol", "leaguemechanics:forbidden-idol");
        itemModels.put("haunting-guise", "leaguemechanics:haunting-guise");
        itemModels.put("sheen", "leaguemechanics:sheen");
        itemModels.put("tiamat", "leaguemechanics:tiamat");
        itemModels.put("abyssal-mask", "leaguemechanics:abyssal-mask");
        itemModels.put("ardent-censer", "leaguemechanics:ardent-censer");
        itemModels.put("atmas-reckoning", "leaguemechanics:atmas-reckoning");
        itemModels.put("bandlepipes", "leaguemechanics:bandlepipes");
        itemModels.put("banshees-veil", "leaguemechanics:banshees-veil");
        itemModels.put("black-cleaver", "leaguemechanics:black-cleaver");
        itemModels.put("blackfire-torch", "leaguemechanics:blackfire-torch");
        itemModels.put("blade-of-the-ruined-king", "leaguemechanics:blade-of-the-ruined-king");
        itemModels.put("bloodletters-curse", "leaguemechanics:bloodletters-curse");
        itemModels.put("bloodthirster", "leaguemechanics:bloodthirster");
        itemModels.put("chempunk-chainsword", "leaguemechanics:chempunk-chainsword");
        itemModels.put("cosmic-drive", "leaguemechanics:cosmic-drive");
        itemModels.put("cryptbloom", "leaguemechanics:cryptbloom");
        itemModels.put("dead-mans-plate", "leaguemechanics:dead-mans-plate");
        itemModels.put("deaths-dance", "leaguemechanics:deaths-dance");
        itemModels.put("eclipse", "leaguemechanics:eclipse");
        itemModels.put("edge-of-night", "leaguemechanics:edge-of-night");
        itemModels.put("endless-hunger", "leaguemechanics:endless-hunger");
        itemModels.put("essence-reaver", "leaguemechanics:essence-reaver");
        itemModels.put("experimental-hexplate", "leaguemechanics:experimental-hexplate");
        itemModels.put("force-of-nature", "leaguemechanics:force-of-nature");
        itemModels.put("guardian-angel", "leaguemechanics:guardian-angel");
        itemModels.put("guinsoos-rageblade", "leaguemechanics:guinsoos-rageblade");
        itemModels.put("heartsteel", "leaguemechanics:heartsteel");
        itemModels.put("hexoptics-c44", "leaguemechanics:hexoptics-c44");
        itemModels.put("horizon-focus", "leaguemechanics:horizon-focus");
        itemModels.put("hubris", "leaguemechanics:hubris");
        itemModels.put("hullbreaker", "leaguemechanics:hullbreaker");
        itemModels.put("iceborn-gauntlet", "leaguemechanics:iceborn-gauntlet");
        itemModels.put("immortal-shieldbow", "leaguemechanics:immortal-shieldbow");
        itemModels.put("imperial-mandate", "leaguemechanics:imperial-mandate");
        itemModels.put("infinity-edge", "leaguemechanics:infinity-edge");
        itemModels.put("jaksho-the-protean", "leaguemechanics:jaksho-the-protean");
        itemModels.put("knights-vow", "leaguemechanics:knights-vow");
        itemModels.put("kraken-slayer", "leaguemechanics:kraken-slayer");
        itemModels.put("liandrys-torment", "leaguemechanics:liandrys-torment");
        itemModels.put("lich-bane", "leaguemechanics:lich-bane");
        itemModels.put("lord-dominiks-regards", "leaguemechanics:lord-dominiks-regards");
        itemModels.put("ludens-echo", "leaguemechanics:ludens-echo");
        itemModels.put("malignance", "leaguemechanics:malignance");
        itemModels.put("maw-of-malmortius", "leaguemechanics:maw-of-malmortius");
        itemModels.put("mejais-soulstealer", "leaguemechanics:mejais-soulstealer");
        itemModels.put("mercurial-scimitar", "leaguemechanics:mercurial-scimitar");
        itemModels.put("morellonomicon", "leaguemechanics:morellonomicon");
        itemModels.put("mortal-reminder", "leaguemechanics:mortal-reminder");
        itemModels.put("nashors-tooth", "leaguemechanics:nashors-tooth");
        itemModels.put("navori-flickerblade", "leaguemechanics:navori-flickerblade");
        itemModels.put("overlords-bloodmail", "leaguemechanics:overlords-bloodmail");
        itemModels.put("phantom-dancer", "leaguemechanics:phantom-dancer");
        itemModels.put("rabadons-deathcap", "leaguemechanics:rabadons-deathcap");
        itemModels.put("rapid-firecannon", "leaguemechanics:rapid-firecannon");
        itemModels.put("ravenous-hydra", "leaguemechanics:ravenous-hydra");
        itemModels.put("riftmaker", "leaguemechanics:riftmaker");
        itemModels.put("runaans-hurricane", "leaguemechanics:runaans-hurricane");
        itemModels.put("rylais-crystal-scepter", "leaguemechanics:rylais-crystal-scepter");
        itemModels.put("serpents-fang", "leaguemechanics:serpents-fang");
        itemModels.put("seryldas-grudge", "leaguemechanics:seryldas-grudge");
        itemModels.put("shadowflame", "leaguemechanics:shadowflame");
        itemModels.put("spear-of-shojin", "leaguemechanics:spear-of-shojin");
        itemModels.put("spirit-visage", "leaguemechanics:spirit-visage");
        itemModels.put("staff-of-flowing-water", "leaguemechanics:staff-of-flowing-water");
        itemModels.put("statikk-shiv", "leaguemechanics:statikk-shiv");
        itemModels.put("steraks-gage", "leaguemechanics:steraks-gage");
        itemModels.put("stormrazor", "leaguemechanics:stormrazor");
        itemModels.put("stormsurge", "leaguemechanics:stormsurge");
        itemModels.put("stridebreaker", "leaguemechanics:stridebreaker");
        itemModels.put("sundered-sky", "leaguemechanics:sundered-sky");
        itemModels.put("sunfire-aegis", "leaguemechanics:sunfire-aegis");
        itemModels.put("terminus", "leaguemechanics:terminus");
        itemModels.put("the-collector", "leaguemechanics:the-collector");
        itemModels.put("thornmail", "leaguemechanics:thornmail");
        itemModels.put("titanic-hydra", "leaguemechanics:titanic-hydra");
        itemModels.put("trinity-force", "leaguemechanics:trinity-force");
        itemModels.put("unending-despair", "leaguemechanics:unending-despair");
        itemModels.put("void-staff", "leaguemechanics:void-staff");
        itemModels.put("voltaic-cyclosword", "leaguemechanics:voltaic-cyclosword");
        itemModels.put("warmogs-armor", "leaguemechanics:warmogs-armor");
        itemModels.put("wits-end", "leaguemechanics:wits-end");
        itemModels.put("youmuus-ghostblade", "leaguemechanics:youmuus-ghostblade");
        itemModels.put("yun-tal-wildarrows", "leaguemechanics:yun-tal-wildarrows");
        itemModels.put("zekes-convergence", "leaguemechanics:zekes-convergence");
    }

    private void loadFallbackModels() {
        fallbackItemModels.put("aether-wisp", "minecraft:ghast_tear");
        fallbackItemModels.put("amplifying-tome", "minecraft:book");
        fallbackItemModels.put("b.f-sword", "minecraft:diamond_sword");
        fallbackItemModels.put("bamis-cinder", "minecraft:fire_charge");
        fallbackItemModels.put("bandleglass-mirror", "minecraft:end_crystal");
        fallbackItemModels.put("berserkers-greaves", "minecraft:chainmail_boots");
        fallbackItemModels.put("blasting-wand", "minecraft:golden_spear");
        fallbackItemModels.put("blightting-jewel", "minecraft:amethyst_shard");
        fallbackItemModels.put("boots", "minecraft:leather_boots");
        fallbackItemModels.put("bramble-vest", "minecraft:iron_chestplate");
        fallbackItemModels.put("caulfields-warhammer", "minecraft:mace");
        fallbackItemModels.put("chain-vest", "minecraft:chainmail_chestplate");
        fallbackItemModels.put("cloak-of-agility", "minecraft:elytra");
        fallbackItemModels.put("cloth-armor", "minecraft:leather_chestplate");
        fallbackItemModels.put("crystalline-bracer", "minecraft:copper_chestplate");
        fallbackItemModels.put("cull", "minecraft:netherite_hoe");
        fallbackItemModels.put("dagger", "minecraft:wooden_sword");
        fallbackItemModels.put("dark-seal", "minecraft:popped_chorus_fruit");
        fallbackItemModels.put("dorans-blade", "minecraft:copper_sword");
        fallbackItemModels.put("dorans-bow", "minecraft:bow");
        fallbackItemModels.put("dorans-helm", "minecraft:iron_helmet");
        fallbackItemModels.put("dorans-ring", "minecraft:music_disc_11");
        fallbackItemModels.put("dorans-shield", "minecraft:shield");
        fallbackItemModels.put("executioners-calling", "minecraft:trident");
        fallbackItemModels.put("faeri-charm", "minecraft:orange_bundle");
        fallbackItemModels.put("fated-ashes", "minecraft:nether_star");
        fallbackItemModels.put("flendish-codex", "minecraft:writable_book");
        fallbackItemModels.put("giants-belt", "minecraft:lead");
        fallbackItemModels.put("glowing-mote", "minecraft:glow_ink_sac");
        fallbackItemModels.put("hearthbound-axe", "minecraft:diamond_axe");
        fallbackItemModels.put("hexdrinker", "minecraft:copper_spear");
        fallbackItemModels.put("hextech-alternator", "minecraft:heavy_core");
        fallbackItemModels.put("kindlegem", "minecraft:redstone");
        fallbackItemModels.put("last-whisper", "minecraft:crossbow");
        fallbackItemModels.put("long-sword", "minecraft:iron_sword");
        fallbackItemModels.put("lost-chapter", "minecraft:paper");
        fallbackItemModels.put("mercurys-treads", "minecraft:diamond_boots");
        fallbackItemModels.put("needlessly-large-rod", "minecraft:breeze_rod");
        fallbackItemModels.put("negatron-cloak", "minecraft:iron_nautilus_armor");
        fallbackItemModels.put("noonquiver", "minecraft:diamond_spear");
        fallbackItemModels.put("null-magic-mantle", "minecraft:copper_nautilus_armor");
        fallbackItemModels.put("oblivion-orb", "minecraft:slime_ball");
        fallbackItemModels.put("phage", "minecraft:wooden_axe");
        fallbackItemModels.put("pickaxe", "minecraft:iron_pickaxe");
        fallbackItemModels.put("plated-steelcaps", "minecraft:iron_boots");
        fallbackItemModels.put("rectrix", "minecraft:nether_wart");
        fallbackItemModels.put("recurve-bow", "minecraft:bow");
        fallbackItemModels.put("rejuvenation-bead", "minecraft:green_bundle");
        fallbackItemModels.put("ruby-crystal", "minecraft:red_dye");
        fallbackItemModels.put("scouts-slingshot", "minecraft:fishing_rod");
        fallbackItemModels.put("serrated-dirk", "minecraft:wooden_spear");
        fallbackItemModels.put("sorcerers-shoes", "minecraft:netherite_boots");
        fallbackItemModels.put("spectres-cowl", "minecraft:waxed_oxidized_copper_golem_statue");
        fallbackItemModels.put("steel-sigil", "minecraft:music_disc_chirp");
        fallbackItemModels.put("the-brutalizer", "minecraft:stone_axe");
        fallbackItemModels.put("tunneler", "minecraft:diamond_shovel");
        fallbackItemModels.put("vampiric-scepter", "minecraft:iron_hoe");
        fallbackItemModels.put("verdant-barrier", "minecraft:golden_nautilus_armor");
        fallbackItemModels.put("wardens-mail", "minecraft:golden_chestplate");
        fallbackItemModels.put("winged-moonplate", "minecraft:iron_leggings");
        fallbackItemModels.put("zeal", "minecraft:golden_sword");
        fallbackItemModels.put("forbidden-idol", "minecraft:totem_of_undying");
        fallbackItemModels.put("haunting-guise", "minecraft:wither_skeleton_skull");
        fallbackItemModels.put("sheen", "minecraft:blaze_powder");
        fallbackItemModels.put("tiamat", "minecraft:golden_axe");
        fallbackItemModels.put("abyssal-mask", "minecraft:creeper_head");
        fallbackItemModels.put("ardent-censer", "minecraft:soul_lantern");
        fallbackItemModels.put("atmas-reckoning", "minecraft:diamond_chestplate");
        fallbackItemModels.put("bandlepipes", "minecraft:goat_horn");
        fallbackItemModels.put("banshees-veil", "minecraft:white_dye");
        fallbackItemModels.put("black-cleaver", "minecraft:netherite_axe");
        fallbackItemModels.put("blackfire-torch", "minecraft:soul_campfire");
        fallbackItemModels.put("blade-of-the-ruined-king", "minecraft:stone_sword");
        fallbackItemModels.put("bloodletters-curse", "minecraft:fermented_spider_eye");
        fallbackItemModels.put("bloodthirster", "minecraft:netherite_spear");
        fallbackItemModels.put("chempunk-chainsword", "minecraft:diamond_pickaxe");
        fallbackItemModels.put("cosmic-drive", "minecraft:ender_eye");
        fallbackItemModels.put("cryptbloom", "minecraft:wither_rose");
        fallbackItemModels.put("dead-mans-plate", "minecraft:netherite_chestplate");
        fallbackItemModels.put("deaths-dance", "minecraft:diamond_leggings");
        fallbackItemModels.put("eclipse", "minecraft:clock");
        fallbackItemModels.put("edge-of-night", "minecraft:netherite_scrap");
        fallbackItemModels.put("endless-hunger", "minecraft:rotten_flesh");
        fallbackItemModels.put("essence-reaver", "minecraft:echo_shard");
        fallbackItemModels.put("experimental-hexplate", "minecraft:netherite_helmet");
        fallbackItemModels.put("force-of-nature", "minecraft:vine");
        fallbackItemModels.put("guardian-angel", "minecraft:enchanted_golden_apple");
        fallbackItemModels.put("guinsoos-rageblade", "minecraft:blaze_rod");
        fallbackItemModels.put("heartsteel", "minecraft:heart_of_the_sea");
        fallbackItemModels.put("hexoptics-c44", "minecraft:spyglass");
        fallbackItemModels.put("horizon-focus", "minecraft:recovery_compass");
        fallbackItemModels.put("hubris", "minecraft:golden_helmet");
        fallbackItemModels.put("hullbreaker", "minecraft:nautilus_shell");
        fallbackItemModels.put("iceborn-gauntlet", "minecraft:blue_ice");
        fallbackItemModels.put("immortal-shieldbow", "minecraft:spectral_arrow");
        fallbackItemModels.put("imperial-mandate", "minecraft:bell");
        fallbackItemModels.put("infinity-edge", "minecraft:diamond");
        fallbackItemModels.put("jaksho-the-protean", "minecraft:conduit");
        fallbackItemModels.put("knights-vow", "minecraft:golden_leggings");
        fallbackItemModels.put("kraken-slayer", "minecraft:ink_sac");
        fallbackItemModels.put("liandrys-torment", "minecraft:magma_cream");
        fallbackItemModels.put("lich-bane", "minecraft:dragon_breath");
        fallbackItemModels.put("lord-dominiks-regards", "minecraft:tipped_arrow");
        fallbackItemModels.put("ludens-echo", "minecraft:firework_star");
        fallbackItemModels.put("malignance", "minecraft:ominous_bottle");
        fallbackItemModels.put("maw-of-malmortius", "minecraft:potion");
        fallbackItemModels.put("mejais-soulstealer", "minecraft:skeleton_skull");
        fallbackItemModels.put("mercurial-scimitar", "minecraft:stone_hoe");
        fallbackItemModels.put("morellonomicon", "minecraft:experience_bottle");
        fallbackItemModels.put("mortal-reminder", "minecraft:bone");
        fallbackItemModels.put("nashors-tooth", "minecraft:prismarine_crystals");
        fallbackItemModels.put("navori-flickerblade", "minecraft:feather");
        fallbackItemModels.put("overlords-bloodmail", "minecraft:iron_ingot");
        fallbackItemModels.put("phantom-dancer", "minecraft:phantom_membrane");
        fallbackItemModels.put("rabadons-deathcap", "minecraft:dragon_head");
        fallbackItemModels.put("rapid-firecannon", "minecraft:firework_rocket");
        fallbackItemModels.put("ravenous-hydra", "minecraft:turtle_helmet");
        fallbackItemModels.put("riftmaker", "minecraft:chorus_fruit");
        fallbackItemModels.put("runaans-hurricane", "minecraft:wind_charge");
        fallbackItemModels.put("rylais-crystal-scepter", "minecraft:packed_ice");
        fallbackItemModels.put("serpents-fang", "minecraft:spider_eye");
        fallbackItemModels.put("seryldas-grudge", "minecraft:chain");
        fallbackItemModels.put("shadowflame", "minecraft:lava_bucket");
        fallbackItemModels.put("spear-of-shojin", "minecraft:stone_spear");
        fallbackItemModels.put("spirit-visage", "minecraft:player_head");
        fallbackItemModels.put("staff-of-flowing-water", "minecraft:water_bucket");
        fallbackItemModels.put("statikk-shiv", "minecraft:lightning_rod");
        fallbackItemModels.put("steraks-gage", "minecraft:gold_ingot");
        fallbackItemModels.put("stormrazor", "minecraft:iron_spear");
        fallbackItemModels.put("stormsurge", "minecraft:sea_lantern");
        fallbackItemModels.put("stridebreaker", "minecraft:chainmail_leggings");
        fallbackItemModels.put("sundered-sky", "minecraft:diamond_horse_armor");
        fallbackItemModels.put("sunfire-aegis", "minecraft:campfire");
        fallbackItemModels.put("terminus", "minecraft:netherite_shovel");
        fallbackItemModels.put("the-collector", "minecraft:emerald");
        fallbackItemModels.put("thornmail", "minecraft:cactus");
        fallbackItemModels.put("titanic-hydra", "minecraft:turtle_scute");
        fallbackItemModels.put("trinity-force", "minecraft:beacon");
        fallbackItemModels.put("unending-despair", "minecraft:soul_torch");
        fallbackItemModels.put("void-staff", "minecraft:end_rod");
        fallbackItemModels.put("voltaic-cyclosword", "minecraft:diamond_hoe");
        fallbackItemModels.put("warmogs-armor", "minecraft:golden_apple");
        fallbackItemModels.put("wits-end", "minecraft:name_tag");
        fallbackItemModels.put("youmuus-ghostblade", "minecraft:iron_axe");
        fallbackItemModels.put("yun-tal-wildarrows", "minecraft:arrow");
        fallbackItemModels.put("zekes-convergence", "minecraft:glass_bottle");
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

    public void applyPrices(ConfigurationSection section) {
        if (section == null) return;
        for (Map.Entry<String, Integer> entry : itemPrices.entrySet()) {
            String key = entry.getKey().replace('.', '-');
            if (section.contains(key)) {
                entry.setValue(Math.max(0, section.getInt(key, entry.getValue())));
            }
        }
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

    public String getFallbackModel(String itemId) {
        return fallbackItemModels.get(itemId);
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