package dev.ixpu.leaguemechanics.item;

import java.util.HashMap;
import java.util.Map;

public class ItemStatsData {
    private static ItemStatsData instance;
    private final Map<String, ItemStatsRegistry> items = new HashMap<>();

    private ItemStatsData() {
        loadDefaultItems();
    }

    public static ItemStatsData getInstance() {
        if (instance == null) {
            instance = new ItemStatsData();
        }
        return instance;
    }

    private void loadDefaultItems() {

        // STARTER ITEMS

        addItem("dorans-blade", "Doran's Blade", 8.0, 0.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("dorans-bow", "Doran's Bow", 0.0, 0.0, 8.0, 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("dorans-helm", "Doran's Helm", 10.0, 0.0, 0.0, 8.0, 0.0, 0.0, 8.0, 8.0, 0.0, 0.0, 0.0, 0.0);
        addItem("dorans-ring", "Doran's Ring", 9.0, 0.0, 0.0, 18.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, true, "dorans-ring");
        addItem("dorans-shield", "Doran's Shield", 11.0, 0.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, true, "dorans-shield");
        addItem("dark-seal", "Dark Seal", 5.0, 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, true, "dark-seal");


        // BASIC ITEMS
        addItem("amplifying-tome", "Amplifying Tome", 0.0, 0.0, 0.0, 20.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("blasting-wand", "Blasting Wand", 0.0, 0.0, 0.0, 45.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("needlessly-large-rod", "Needlessly Large Rod", 0.0, 0.0, 0.0, 45.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("cull", "Cull", 0.0, 0.0, 7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, true, "cull");
        addItem("long-sword", "Long Sword", 0.0, 0.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("pickaxe", "Pickaxe", 0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("b.f-sword", "B.F. Sword", 0.0, 0.0, 45.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("cloth-armor", "Cloth Armor", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("chain-vest", "Chain Vest", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 55.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("null-magic-mantle", "Null-Magic Mantle", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 30.0, 0.0, 0.0, 0.0, 0.0);
        addItem("negatron-cloak", "Negatron Cloak", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 85.0, 0.0, 0.0, 0.0, 0.0);
        addItem("ruby-crystal", "Ruby Crystal", 10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("rejuvenation-bead", "Rejuvenation Bead", 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("faeri-charm", "Faeri Charm", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.5, 0.0);
        addItem("dagger", "Dagger", 0.0, 0.0, 0.0, 0.0, 0.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("aether-wisp", "Aether Wisp", 0.0, 0.0, 0.0, 30.0, 0.0 ,0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0);
        addItem("zeal", "Zeal", 0.0, 0.0, 0.0, 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 15.0, 0.0, 4.0);
        addItem("vampire-scepter", "Vampire Scepter", 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 7.0, 0.0, 0.0, 0.0);
        addItem("tunneler", "Tunneler", 25.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("cloak-of-agility", "Cloak of Agility", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 15.0, 0.0, 0.0);

        // BOOTS
        addItem("boots", "Boots", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 7.0);
        addItem("berserkers-greaves", "Berserker's Greaves", 0.0, 0.0, 0.0, 0.0, 0.0, 30.0, 0.0, 0.0, 0.0, 0.0, 0.0, 12.0);
        addItem("mercurys-treads", "Mercury's Treads", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 20.0, 0.0, 0.0, 0.0, 12.0,
                false, null, 0.0, 0.0, 0.0, 0.0, 0.0, 30.0);
        addItem("plated-steelcaps", "Plated Steelcaps", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0, 12.0);
        addItem("sorcerers-shoes", "Sorcerer's Shoes", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 12.0,
                false, null, 0.0, 0.0, 12.0, 0.0);


        // EPIC ITEMS

        addItem("blightting-jewel", "Blightting Jewel", 0.0, 0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                false, null, 0.0, 0.0, 0.0, 13.0);
        addItem("flendish-codex", "Flendish Codex", 0.0, 0.0, 0.0, 35.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                false, null, 0.0, 0.0, 0.0, 0.0, 5.0);
        addItem("lost-chapter", "Lost Chapter", 0.0, 0.0, 0.0, 40.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                false, null, 0.0, 0.0, 0.0, 0.0, 5.0);
        addItem("fated-ashes", "Fated Ashes", 0.0, 0.0, 0.0, 30.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "fated-ashes");
        addItem("hextech-alternator", "Hextech Alternator", 0.0, 0.0, 0.0, 45.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "hextech-alternator");
        addItem("oblivion-orb", "Oblivion Orb", 0.0, 0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "oblivion-orb");
        addItem("executioners-calling", "Executioner's Calling", 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "executioners-calling");
        addItem("caulfields-warhammer", "Caulfield's Warhammer", 0.0, 0.0, 20.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                false, null, 0.0, 0.0, 0.0, 0.0, 5.0);
        addItem("hexdrinker", "Hexdrinker", 0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0,
                true, "hexdrinker");
        addItem("serrated-dirk", "Serrated Dirk", 0.0, 0.0, 20.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                false, null, 10.0, 0.0, 0.0, 0.0);
        addItem("phage", "Phage", 20.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "phage");
        addItem("crystalline-bracer", "Crystalline Bracer", 20.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("giants-belt", "Giant's Belt", 35.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("kindlegem", "Kindlegem", 30.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                false, null, 0.0, 0.0, 0.0, 0.0, 5.0);
        addItem("bramble-vest", "Bramble Vest", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 30.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "bramble-vest");
        addItem("noonquiver", "Noonquiver", 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 20.0, 0.0, 0.0);
        addItem("rectrix", "Rectrix", 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0);
        addItem("recurve-bow", "Recurve Bow", 0.0, 0.0, 0.0, 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "recurve-bow");
        addItem("scouts-slingshot", "Scouts' Slingshot", 0.0, 0.0, 0.0, 0.0, 0.0, 20.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, true, "scouts-slingshot");
        addItem("last-whisper", "Last Whisper", 0.0, 0.0, 20.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                false, null, 0.0, 18.0, 0.0, 0.0);
        addItem("hearthbound-axe", "Hearthbound Axe", 0.0, 0.0, 20.0, 0.0, 0.0, 20.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("spectres-cowl", "Spectre's Cowl", 20.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 35.0, 0.0, 0.0, 0.0, 0.0);
        addItem("winged-moonplate", "Winged Moonplate", 20.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0);
        addItem("wardens-mail", "Warden's Mail", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 40.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "wardens-mail");
        addItem("bamis-cinder", "Bami's Cinder", 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "bamis-cinder", 0.0, 0.0, 0.0, 0.0, 2.0);
        addItem("verdant-barrier", "Verdant Barrier", 0.0, 0.0, 0.0, 40.0, 0.0, 0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0,
                true, "verdant-barrier");
        addItem("aether-wisp", "Aether Wisp", 0.0, 0.0, 0.0, 30.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0);
        addItem("bandleglass-mirror", "Bandleglass Mirror", 0.0, 0.0, 0.0, 20.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0,
                false, null, 0.0, 0.0, 0.0, 0.0, 5.0);
        addItem("vampiric-scepter", "Vampiric Scepter", 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 7.0, 0.0, 0.0, 0.0);
        addItem("the-brutalizer", "The Brutalizer", 0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                false, null, 5.0, 0.0, 0.0, 0.0, 5.0);
        addItem("steel-sigil", "Steel Sigil", 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 30.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("zeal", "Zeal", 0.0, 0.0, 0.0, 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 15.0, 0.0, 4.0);

        // COOLDOWN HASTE ITEMS
        addItem("glowing-mote", "Glowing Mote", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                false, null, 0.0, 0.0, 0.0, 0.0, 2.0);
    }

    private void addItem(String id, String name, double hp, double hr, double ad, double ap, double td,
                         double as, double ar, double mr, double ls, double cc, double sr, double ms) {
        addItem(id, name, hp, hr, ad, ap, td, as, ar, mr, ls, cc, sr, ms, false, null);
    } 

    private void addItem(String id, String name, double hp, double hr, double ad, double ap, double td,
                         double as, double ar, double mr, double ls, double cc, double sr, double ms,
                         boolean hasPassive, String passiveId) {
        addItem(id, name, hp, hr, ad, ap, td, as, ar, mr, ls, cc, sr, ms, hasPassive, passiveId,
                0.0, 0.0, 0.0, 0.0);
    }

    private void addItem(String id, String name, double hp, double hr, double ad, double ap, double td,
                         double as, double ar, double mr, double ls, double cc, double sr, double ms,
                         boolean hasPassive, String passiveId,
                         double apenFlat, double apenPercent, double mpenFlat, double mpenPercent) {
        addItem(id, name, hp, hr, ad, ap, td, as, ar, mr, ls, cc, sr, ms, hasPassive, passiveId,
                apenFlat, apenPercent, mpenFlat, mpenPercent, 0.0);
    }

    private void addItem(String id, String name, double hp, double hr, double ad, double ap, double td,
                         double as, double ar, double mr, double ls, double cc, double sr, double ms,
                         boolean hasPassive, String passiveId,
                         double apenFlat, double apenPercent, double mpenFlat, double mpenPercent,
                         double ch) {
        items.put(id, new ItemStatsRegistry(id, name, hp, hr, ad, ap, td, as, ar, mr, ls, cc, sr, ms,
                hasPassive, passiveId, apenFlat, apenPercent, mpenFlat, mpenPercent, ch));
    }

    private void addItem(String id, String name, double hp, double hr, double ad, double ap, double td,
                         double as, double ar, double mr, double ls, double cc, double sr, double ms,
                         boolean hasPassive, String passiveId,
                         double apenFlat, double apenPercent, double mpenFlat, double mpenPercent,
                         double ch, double tn) {
        items.put(id, new ItemStatsRegistry(id, name, hp, hr, ad, ap, td, as, ar, mr, ls, cc, sr, ms,
                hasPassive, passiveId, apenFlat, apenPercent, mpenFlat, mpenPercent, ch, tn));
    }

    public ItemStatsRegistry getItem(String id) {
        return items.get(id);
    }
}