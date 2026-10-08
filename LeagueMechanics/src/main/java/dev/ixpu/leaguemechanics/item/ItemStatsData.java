package dev.ixpu.leaguemechanics.item;

import org.bukkit.configuration.ConfigurationSection;

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

        addItem("dorans-blade", "Doran's Blade", 20.0, 0.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("dorans-bow", "Doran's Bow", 0.0, 0.0, 8.0, 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("dorans-helm", "Doran's Helm", 38.0, 0.0, 0.0, 8.0, 0.0, 0.0, 8.0, 8.0, 0.0, 0.0, 0.0, 0.0);
        addItem("dorans-ring", "Doran's Ring", 23.0, 0.0, 0.0, 18.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, true, "dorans-ring");
        addItem("dorans-shield", "Doran's Shield", 27.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, true, "dorans-shield");
        addItem("dark-seal", "Dark Seal", 12.0, 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, true, "dark-seal");


        // BASIC ITEMS
        addItem("amplifying-tome", "Amplifying Tome", 0.0, 0.0, 0.0, 20.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("blasting-wand", "Blasting Wand", 0.0, 0.0, 0.0, 45.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("needlessly-large-rod", "Needlessly Large Rod", 0.0, 0.0, 0.0, 65.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("cull", "Cull", 0.0, 0.0, 7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, true, "cull");
        addItem("long-sword", "Long Sword", 0.0, 0.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("pickaxe", "Pickaxe", 0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("b.f-sword", "B.F. Sword", 0.0, 0.0, 40.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("cloth-armor", "Cloth Armor", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("chain-vest", "Chain Vest", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 75.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("null-magic-mantle", "Null-Magic Mantle", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 20.0, 0.0, 0.0, 0.0, 0.0);
        addItem("negatron-cloak", "Negatron Cloak", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 85.0, 0.0, 0.0, 0.0, 0.0);
        addItem("ruby-crystal", "Ruby Crystal", 38.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("rejuvenation-bead", "Rejuvenation Bead", 0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("faeri-charm", "Faeri Charm", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0);
        addItem("dagger", "Dagger", 0.0, 0.0, 0.0, 0.0, 0.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("zeal", "Zeal", 0.0, 0.0, 0.0, 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 15.0, 0.0, 4.0);
        addItem("tunneler", "Tunneler", 62.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("cloak-of-agility", "Cloak of Agility", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 15.0, 0.0, 0.0);
        addItem("glowing-mote", "Glowing Mote", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                false, null, 0.0, 0.0, 0.0, 0.0, 5.0);


        // BOOTS
        addItem("boots", "Boots", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 8.0);
        addItem("berserkers-greaves", "Berserker's Greaves", 0.0, 0.0, 0.0, 0.0, 0.0, 30.0, 0.0, 0.0, 0.0, 0.0, 0.0, 15.0);
        addItem("mercurys-treads", "Mercury's Treads", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 20.0, 0.0, 0.0, 0.0, 15.0,
                false, null, 0.0, 0.0, 0.0, 0.0, 0.0, 30.0);
        addItem("plated-steelcaps", "Plated Steelcaps", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0, 15.0);
        addItem("sorcerers-shoes", "Sorcerer's Shoes", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 15.0,
                false, null, 0.0, 0.0, 12.0, 0.0);


        // EPIC ITEMS

        addItem("blightting-jewel", "Blightting Jewel", 0.0, 0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                false, null, 0.0, 0.0, 0.0, 13.0);
        addItem("flendish-codex", "Flendish Codex", 0.0, 0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                false, null, 0.0, 0.0, 0.0, 0.0, 10.0);
        addItem("lost-chapter", "Lost Chapter", 0.0, 0.0, 0.0, 40.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                false, null, 0.0, 0.0, 0.0, 0.0, 10.0);
        addItem("fated-ashes", "Fated Ashes", 0.0, 0.0, 0.0, 20.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "fated-ashes");
        addItem("hextech-alternator", "Hextech Alternator", 0.0, 0.0, 0.0, 45.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "hextech-alternator");
        addItem("oblivion-orb", "Oblivion Orb", 0.0, 0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "oblivion-orb");
        addItem("executioners-calling", "Executioner's Calling", 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "executioners-calling");
        addItem("caulfields-warhammer", "Caulfield's Warhammer", 0.0, 0.0, 20.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                false, null, 0.0, 0.0, 0.0, 0.0, 10.0);
        addItem("hexdrinker", "Hexdrinker", 0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0,
                true, "hexdrinker");
        addItem("serrated-dirk", "Serrated Dirk", 0.0, 0.0, 20.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                false, null, 10.0, 0.0, 0.0, 0.0);
        addItem("phage", "Phage", 50.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "phage");
        addItem("crystalline-bracer", "Crystalline Bracer", 50.0, 20.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("giants-belt", "Giant's Belt", 88.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("kindlegem", "Kindlegem", 50.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                false, null, 0.0, 0.0, 0.0, 0.0, 10.0);
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
        addItem("spectres-cowl", "Spectre's Cowl", 50.0, 20.0, 0.0, 0.0, 0.0, 0.0, 0.0, 35.0, 0.0, 0.0, 0.0, 0.0);
        addItem("winged-moonplate", "Winged Moonplate", 50.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0);
        addItem("wardens-mail", "Warden's Mail", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 40.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "wardens-mail");
        addItem("bamis-cinder", "Bami's Cinder", 38.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "bamis-cinder", 0.0, 0.0, 0.0, 0.0, 5.0);
        addItem("verdant-barrier", "Verdant Barrier", 0.0, 0.0, 0.0, 40.0, 0.0, 0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0,
                true, "verdant-barrier");
        addItem("aether-wisp", "Aether Wisp", 0.0, 0.0, 0.0, 30.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0);
        addItem("bandleglass-mirror", "Bandleglass Mirror", 0.0, 0.0, 0.0, 20.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0,
                false, null, 0.0, 0.0, 0.0, 0.0, 10.0);
        addItem("vampiric-scepter", "Vampiric Scepter", 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 12.0, 0.0, 0.0, 0.0);
        addItem("the-brutalizer", "The Brutalizer", 0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                false, null, 5.0, 0.0, 0.0, 0.0, 10.0);
        addItem("steel-sigil", "Steel Sigil", 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 30.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("zeal", "Zeal", 0.0, 0.0, 0.0, 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 15.0, 0.0, 4.0);
        addItem("forbidden-idol", "Forbidden Idol", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, 0.0,
                false, null, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 8.0);
        addItem("haunting-guise", "Haunting Guise", 50.0, 0.0, 0.0, 30.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        addItem("sheen", "Sheen", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "sheen", 0.0, 0.0, 0.0, 0.0, 10.0);
        addItem("tiamat", "Tiamat", 0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "tiamat");

        // LEGENDARY ITEMS

        addItem("infinity-edge", "Infinity Edge", 0.0, 0.0, 75.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 25.0, 0.0, 0.0,
                true, "infinity-edge", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 30.0, 0.0);
        addItem("ardent-censer", "Ardent Censer", 0.0, 0.0, 0.0, 45.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, 4.0,
                true, "ardent-censer", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 10.0);
        addItem("abyssal-mask", "Abyssal Mask", 88.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 45.0, 0.0, 0.0, 0.0, 0.0,
                true, "abyssal-mask", 0.0, 0.0, 0.0, 0.0, 15.0);
        addItem("atmas-reckoning", "Atma's Reckoning", 175.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 20.0, 0.0, 0.0,
                true, "atmas-reckoning");
        addItem("bandlepipes", "Bandlepipes", 50.0, 0.0, 0.0, 0.0, 0.0, 0.0, 20.0, 20.0, 0.0, 0.0, 0.0, 0.0,
                true, "bandlepipes", 0.0, 0.0, 0.0, 0.0, 15.0);
        addItem("banshees-veil", "Banshee's Veil", 0.0, 0.0, 0.0, 105.0, 0.0, 0.0, 0.0, 40.0, 0.0, 0.0, 0.0, 0.0,
                true, "banshees-veil");
        addItem("black-cleaver", "Black Cleaver", 100.0, 0.0, 45.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "black-cleaver", 0.0, 0.0, 0.0, 0.0, 20.0);
        addItem("blackfire-torch", "Blackfire Torch", 0.0, 0.0, 0.0, 80.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "blackfire-torch", 0.0, 0.0, 0.0, 0.0, 20.0);
        addItem("blade-of-the-ruined-king", "Blade of the Ruined King", 0.0, 0.0, 40.0, 0.0, 0.0, 25.0, 0.0, 0.0, 15.0, 0.0, 0.0, 0.0,
                true, "blade-of-the-ruined-king");
        addItem("bloodletters-curse", "Bloodletter's Curse", 100.0, 0.0, 0.0, 65.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "bloodletters-curse", 0.0, 0.0, 0.0, 0.0, 15.0);
        addItem("bloodthirster", "Bloodthirster", 0.0, 0.0, 80.0, 0.0, 0.0, 0.0, 0.0, 0.0, 17.0, 0.0, 0.0, 0.0,
                true, "bloodthirster");
        addItem("chempunk-chainsword", "Chempunk Chainsword", 112.0, 0.0, 45.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "chempunk-chainsword", 0.0, 0.0, 0.0, 0.0, 15.0);
        addItem("cosmic-drive", "Cosmic Drive", 88.0, 0.0, 0.0, 70.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0,
                true, "cosmic-drive", 0.0, 0.0, 0.0, 0.0, 25.0);
        addItem("cryptbloom", "Cryptbloom", 0.0, 0.0, 0.0, 75.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "cryptbloom", 0.0, 0.0, 0.0, 30.0, 20.0);
        addItem("dead-mans-plate", "Dead Man's Plate", 88.0, 0.0, 0.0, 0.0, 0.0, 0.0, 55.0, 0.0, 0.0, 0.0, 0.0, 4.0,
                true, "dead-mans-plate");
        addItem("deaths-dance", "Death's Dance", 0.0, 0.0, 60.0, 0.0, 0.0, 0.0, 50.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "deaths-dance", 0.0, 0.0, 0.0, 0.0, 15.0);
        addItem("eclipse", "Eclipse", 0.0, 0.0, 60.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "eclipse", 0.0, 0.0, 0.0, 0.0, 15.0);
        addItem("edge-of-night", "Edge of Night", 64.0, 0.0, 50.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "edge-of-night", 15.0, 0.0, 0.0, 0.0);
        addItem("endless-hunger", "Endless Hunger", 0.0, 0.0, 65.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "endless-hunger", 0.0, 0.0, 0.0, 0.0, 0.0, 20.0);
        addItem("essence-reaver", "Essence Reaver", 0.0, 0.0, 50.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 25.0, 0.0, 0.0,
                true, "essence-reaver", 0.0, 0.0, 0.0, 0.0, 20.0);
        addItem("experimental-hexplate", "Experimental Hexplate", 88.0, 0.0, 40.0, 0.0, 0.0, 20.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "experimental-hexplate");
        addItem("force-of-nature", "Force of Nature", 100.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 55.0, 0.0, 0.0, 0.0, 4.0,
                true, "force-of-nature");
        addItem("guardian-angel", "Guardian Angel", 0.0, 0.0, 55.0, 0.0, 0.0, 0.0, 45.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "guardian-angel");
        addItem("guinsoos-rageblade", "Guinsoo's Rageblade", 0.0, 0.0, 30.0, 30.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "guinsoos-rageblade");
        addItem("heartsteel", "Heartsteel", 225.0, 20.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "heartsteel");
        addItem("hexoptics-c44", "Hexoptics C44", 0.0, 0.0, 55.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 25.0, 0.0, 0.0,
                true, "hexoptics-c44");
        addItem("horizon-focus", "Horizon Focus", 0.0, 0.0, 0.0, 75.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "horizon-focus", 0.0, 0.0, 0.0, 0.0, 25.0);
        addItem("hubris", "Hubris", 0.0, 0.0, 55.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "hubris", 18.0, 0.0, 0.0, 0.0, 10.0);
        addItem("hullbreaker", "Hullbreaker", 125.0, 0.0, 40.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0,
                true, "hullbreaker");
        addItem("iceborn-gauntlet", "Iceborn Gauntlet", 75.0, 0.0, 0.0, 0.0, 0.0, 0.0, 50.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "iceborn-gauntlet", 0.0, 0.0, 0.0, 0.0, 15.0);
        addItem("immortal-shieldbow", "Immortal Shieldbow", 0.0, 0.0, 55.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 25.0, 0.0, 0.0,
                true, "immortal-shieldbow");
        addItem("imperial-mandate", "Imperial Mandate", 0.0, 0.0, 0.0, 60.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, 0.0,
                true, "imperial-mandate", 0.0, 0.0, 0.0, 0.0, 15.0);
        addItem("jaksho-the-protean", "Jak'Sho, The Protean", 88.0, 0.0, 0.0, 0.0, 0.0, 0.0, 45.0, 45.0, 0.0, 0.0, 0.0, 0.0,
                true, "jaksho-the-protean");
        addItem("knights-vow", "Knight's Vow", 50.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "knights-vow", 0.0, 0.0, 0.0, 0.0, 10.0);
        addItem("kraken-slayer", "Kraken Slayer", 0.0, 0.0, 45.0, 0.0, 0.0, 40.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0,
                true, "kraken-slayer");
        addItem("liandrys-torment", "Liandry's Torment", 75.0, 0.0, 0.0, 60.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "liandrys-torment");
        addItem("lich-bane", "Lich Bane", 0.0, 0.0, 0.0, 100.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0,
                true, "lich-bane", 0.0, 0.0, 0.0, 0.0, 10.0);
        addItem("lord-dominiks-regards", "Lord Dominik's Regards", 0.0, 0.0, 35.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 25.0, 0.0, 0.0,
                true, "lord-dominiks-regards", 0.0, 35.0, 0.0, 0.0);
        addItem("ludens-echo", "Luden's Echo", 0.0, 0.0, 0.0, 100.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "ludens-echo", 0.0, 0.0, 0.0, 0.0, 10.0);
        addItem("malignance", "Malignance", 0.0, 0.0, 0.0, 90.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "malignance", 0.0, 0.0, 0.0, 0.0, 15.0);
        addItem("maw-of-malmortius", "Maw of Malmortius", 0.0, 0.0, 60.0, 0.0, 0.0, 0.0, 0.0, 40.0, 0.0, 0.0, 0.0, 0.0,
                true, "maw-of-malmortius", 0.0, 0.0, 0.0, 0.0, 15.0);
        addItem("mejais-soulstealer", "Mejai's Soulstealer", 25.0, 0.0, 0.0, 20.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "mejais-soulstealer");
        addItem("mercurial-scimitar", "Mercurial Scimitar", 0.0, 0.0, 50.0, 0.0, 0.0, 0.0, 0.0, 35.0, 15.0, 0.0, 0.0, 0.0,
                true, "mercurial-scimitar");
        addItem("morellonomicon", "Morellonomicon", 88.0, 0.0, 0.0, 75.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "morellonomicon", 0.0, 0.0, 0.0, 0.0, 15.0);
        addItem("mortal-reminder", "Mortal Reminder", 0.0, 0.0, 35.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 25.0, 0.0, 0.0,
                true, "mortal-reminder", 0.0, 30.0, 0.0, 0.0);
        addItem("nashors-tooth", "Nashor's Tooth", 0.0, 0.0, 0.0, 80.0, 0.0, 50.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "nashors-tooth", 0.0, 0.0, 0.0, 0.0, 15.0);
        addItem("navori-flickerblade", "Navori Flickerblade", 0.0, 0.0, 0.0, 0.0, 0.0, 40.0, 0.0, 0.0, 0.0, 25.0, 0.0, 4.0,
                true, "navori-flickerblade");
        addItem("overlords-bloodmail", "Overlord's Bloodmail", 137.0, 0.0, 30.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "overlords-bloodmail");
        addItem("phantom-dancer", "Phantom Dancer", 0.0, 0.0, 0.0, 0.0, 0.0, 65.0, 0.0, 0.0, 0.0, 25.0, 0.0, 10.0,
                true, "phantom-dancer");
        addItem("rabadons-deathcap", "Rabadon's Deathcap", 0.0, 0.0, 0.0, 130.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "rabadons-deathcap");
        addItem("rapid-firecannon", "Rapid Firecannon", 0.0, 0.0, 0.0, 0.0, 0.0, 35.0, 0.0, 0.0, 0.0, 25.0, 0.0, 4.0,
                true, "rapid-firecannon");
        addItem("ravenous-hydra", "Ravenous Hydra", 0.0, 0.0, 65.0, 0.0, 0.0, 0.0, 0.0, 0.0, 17.0, 0.0, 0.0, 0.0,
                true, "ravenous-hydra", 0.0, 0.0, 0.0, 0.0, 15.0);
        addItem("riftmaker", "Riftmaker", 75.0, 0.0, 0.0, 70.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "riftmaker", 0.0, 0.0, 0.0, 0.0, 15.0);
        addItem("runaans-hurricane", "Runaan's Hurricane", 0.0, 0.0, 0.0, 0.0, 0.0, 40.0, 0.0, 0.0, 0.0, 25.0, 0.0, 5.0,
                true, "runaans-hurricane");
        addItem("rylais-crystal-scepter", "Rylai's Crystal Scepter", 100.0, 0.0, 0.0, 65.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "rylais-crystal-scepter");
        addItem("serpents-fang", "Serpent's Fang", 0.0, 0.0, 55.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "serpents-fang", 15.0, 0.0, 0.0, 0.0);
        addItem("seryldas-grudge", "Serylda's Grudge", 0.0, 0.0, 45.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "seryldas-grudge", 0.0, 35.0, 0.0, 0.0, 15.0);
        addItem("shadowflame", "Shadowflame", 0.0, 0.0, 0.0, 110.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "shadowflame", 0.0, 0.0, 15.0, 0.0);
        addItem("spear-of-shojin", "Spear of Shojin", 112.0, 0.0, 45.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "spear-of-shojin");
        addItem("spirit-visage", "Spirit Visage", 50.0, 20.0, 0.0, 0.0, 0.0, 0.0, 0.0, 50.0, 0.0, 0.0, 0.0, 0.0,
                true, "spirit-visage", 0.0, 0.0, 0.0, 0.0, 10.0);
        addItem("staff-of-flowing-water", "Staff of Flowing Water", 0.0, 0.0, 0.0, 35.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, 0.0,
                true, "staff-of-flowing-water", 0.0, 0.0, 0.0, 0.0, 10.0, 0.0, 0.0, 10.0);
        addItem("statikk-shiv", "Statikk Shiv", 0.0, 0.0, 45.0, 45.0, 0.0, 30.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0,
                true, "statikk-shiv");
        addItem("steraks-gage", "Sterak's Gage", 100.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "steraks-gage", 0.0, 0.0, 0.0, 0.0, 0.0, 20.0);
        addItem("stormrazor", "Stormrazor", 0.0, 0.0, 50.0, 0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 25.0, 0.0, 0.0,
                true, "stormrazor");
        addItem("stormsurge", "Stormsurge", 0.0, 0.0, 0.0, 90.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0,
                true, "stormsurge", 0.0, 0.0, 15.0, 0.0);
        addItem("stridebreaker", "Stridebreaker", 112.0, 0.0, 40.0, 0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "stridebreaker");
        addItem("sundered-sky", "Sundered Sky", 100.0, 0.0, 40.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "sundered-sky", 0.0, 0.0, 0.0, 0.0, 10.0);
        addItem("sunfire-aegis", "Sunfire Aegis", 88.0, 0.0, 0.0, 0.0, 0.0, 0.0, 50.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "sunfire-aegis", 0.0, 0.0, 0.0, 0.0, 10.0);
        addItem("terminus", "Terminus", 0.0, 0.0, 30.0, 0.0, 0.0, 35.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "terminus");
        addItem("the-collector", "The Collector", 0.0, 0.0, 50.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 25.0, 0.0, 0.0,
                true, "the-collector", 10.0, 0.0, 0.0, 0.0);
        addItem("thornmail", "Thornmail", 38.0, 0.0, 0.0, 0.0, 0.0, 0.0, 75.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "thornmail");
        addItem("titanic-hydra", "Titanic Hydra", 150.0, 0.0, 40.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "titanic-hydra");
        addItem("trinity-force", "Trinity Force", 83.0, 0.0, 36.0, 0.0, 0.0, 30.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "trinity-force", 0.0, 0.0, 0.0, 0.0, 15.0);
        addItem("unending-despair", "Unending Despair", 100.0, 0.0, 0.0, 0.0, 0.0, 0.0, 50.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "unending-despair", 0.0, 0.0, 0.0, 0.0, 15.0);
        addItem("void-staff", "Void Staff", 0.0, 0.0, 0.0, 95.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "void-staff", 0.0, 0.0, 0.0, 40.0);
        addItem("voltaic-cyclosword", "Voltaic Cyclosword", 0.0, 0.0, 55.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "voltaic-cyclosword", 10.0, 0.0, 0.0, 0.0, 10.0);
        addItem("warmogs-armor", "Warmog's Armor", 250.0, 20.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                true, "warmogs-armor");
        addItem("wits-end", "Wit's End", 0.0, 0.0, 0.0, 0.0, 0.0, 50.0, 0.0, 45.0, 0.0, 0.0, 0.0, 0.0,
                true, "wits-end", 0.0, 0.0, 0.0, 0.0, 0.0, 20.0);
        addItem("youmuus-ghostblade", "Youmuu's Ghostblade", 0.0, 0.0, 55.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0,
                true, "youmuus-ghostblade", 18.0, 0.0, 0.0, 0.0);
        addItem("yun-tal-wildarrows", "Yun Tal Wildarrows", 0.0, 0.0, 50.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 45.0,
                true, "yun-tal-wildarrows");
        addItem("zekes-convergence", "Zeke's Convergence", 75.0, 0.0, 0.0, 0.0, 0.0, 0.0, 25.0, 25.0, 0.0, 0.0, 0.0, 0.0,
                true, "zekes-convergence", 0.0, 0.0, 0.0, 0.0, 10.0);
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

    private void addItem(String id, String name, double hp, double hr, double ad, double ap, double td,
                         double as, double ar, double mr, double ls, double cc, double sr, double ms,
                         boolean hasPassive, String passiveId,
                         double apenFlat, double apenPercent, double mpenFlat, double mpenPercent,
                         double ch, double tn, double csd, double hm) {
        items.put(id, new ItemStatsRegistry(id, name, hp, hr, ad, ap, td, as, ar, mr, ls, cc, sr, ms,
                hasPassive, passiveId, apenFlat, apenPercent, mpenFlat, mpenPercent, ch, tn, csd, hm));
    }

    public ItemStatsRegistry getItem(String id) {
        return items.get(id);
    }

    public java.util.Collection<ItemStatsRegistry> getAllItems() {
        return items.values();
    }

    public void applyConfig(ConfigurationSection section) {
        if (section == null) return;
        for (ItemStatsRegistry item : items.values()) {
            ConfigurationSection itemSection = section.getConfigurationSection(item.getId().replace('.', '-'));
            if (itemSection != null) {
                item.applyConfig(itemSection);
            }
        }
    }
}