package dev.ixpu.leaguemechanics.item.shop;

import dev.ixpu.leaguemechanics.item.ItemStatsData;
import dev.ixpu.leaguemechanics.item.ItemStatsRegistry;
import dev.ixpu.leaguemechanics.util.ItemModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.List;

public class ItemShopRegistry {
    private static ItemShopRegistry instance;
    private final List<ShopItem> shopItems = new ArrayList<>();
    private final Map<String, ShopItem> shopItemsById = new HashMap<>();

    private ItemShopRegistry() {
        registerItems();
    }

    public static ItemShopRegistry getInstance() {
        if (instance == null) {
            instance = new ItemShopRegistry();
        }
        return instance;
    }

    private void registerItems() {
        ItemStatsData statsData = ItemStatsData.getInstance();
        ItemShopData shopData = ItemShopData.getInstance();

        String[] itemIds = {
                "amplifying-tome", "blasting-wand", "needlessly-large-rod",
                "cull", "long-sword", "pickaxe", "b.f-sword",
                "cloth-armor", "chain-vest",
                "null-magic-mantle", "negatron-cloak",
                "ruby-crystal",
                "rejuvenation-bead",
                "faeri-charm",
                "dagger",
                "boots",
                "dorans-blade", "dorans-bow", "dorans-helm", "dorans-ring", "dorans-shield", "dark-seal",
                "blightting-jewel", "flendish-codex", "lost-chapter", "fated-ashes",
                "hextech-alternator", "oblivion-orb",
                "executioners-calling",
                "caulfields-warhammer", "hexdrinker", "serrated-dirk", "phage",
                "crystalline-bracer", "giants-belt", "kindlegem", "bramble-vest",
                "cloak-of-agility", "noonquiver", "rectrix", "recurve-bow",
                "scouts-slingshot", "last-whisper", "hearthbound-axe", "tunneler",
                "spectres-cowl", "winged-moonplate", "wardens-mail", "bamis-cinder",
                "verdant-barrier", "aether-wisp", "bandleglass-mirror",
                "vampiric-scepter", "the-brutalizer", "steel-sigil", "zeal",
                "berserkers-greaves", "mercurys-treads", "plated-steelcaps", "sorcerers-shoes",
                "glowing-mote",
                "forbidden-idol", "haunting-guise", "sheen", "tiamat",
                "abyssal-mask", "ardent-censer", "atmas-reckoning", "bandlepipes",
                "banshees-veil", "black-cleaver", "blackfire-torch", "blade-of-the-ruined-king",
                "bloodletters-curse", "bloodthirster", "chempunk-chainsword", "cosmic-drive",
                "cryptbloom", "dead-mans-plate", "deaths-dance", "eclipse",
                "edge-of-night", "endless-hunger", "essence-reaver", "experimental-hexplate",
                "force-of-nature", "guardian-angel", "guinsoos-rageblade", "heartsteel",
                "hexoptics-c44", "horizon-focus", "hubris", "hullbreaker",
                "iceborn-gauntlet", "immortal-shieldbow", "imperial-mandate", "infinity-edge",
                "jaksho-the-protean", "knights-vow", "kraken-slayer", "liandrys-torment",
                "lich-bane", "lord-dominiks-regards", "ludens-echo", "malignance",
                "maw-of-malmortius", "mejais-soulstealer", "mercurial-scimitar", "morellonomicon",
                "mortal-reminder", "nashors-tooth", "navori-flickerblade", "overlords-bloodmail",
                "phantom-dancer", "rabadons-deathcap", "rapid-firecannon", "ravenous-hydra",
                "riftmaker", "runaans-hurricane", "rylais-crystal-scepter", "serpents-fang",
                "seryldas-grudge", "shadowflame", "spear-of-shojin", "spirit-visage",
                "staff-of-flowing-water", "statikk-shiv", "steraks-gage", "stormrazor",
                "stormsurge", "stridebreaker", "sundered-sky", "sunfire-aegis",
                "terminus", "the-collector", "thornmail", "titanic-hydra",
                "trinity-force", "unending-despair", "void-staff", "voltaic-cyclosword",
                "warmogs-armor", "wits-end", "youmuus-ghostblade", "yun-tal-wildarrows",
                "zekes-convergence"
        };

        for (String itemId : itemIds) {
            ItemStatsRegistry stats = statsData.getItem(itemId);
            if (stats != null && shopData.hasItem(itemId)) {
                ShopItem shopItem = new ShopItem(
                        stats,
                        shopData.getPrice(itemId),
                        shopData.getLimit(itemId),
                        shopData.getRequiredItems(itemId)
                );
                shopItems.add(shopItem);
                shopItemsById.put(shopItem.getId(), shopItem);
            }
        }
    }

    public List<ShopItem> getAllShopItems() {
        return Collections.unmodifiableList(shopItems);
    }

    public ShopItem getShopItem(String id) {
        return shopItemsById.get(id);
    }

    public static class ShopItem {
        private final ItemStatsRegistry stats;
        private final int price;
        private final int limit;
        private final List<String> requiredItems;

        public ShopItem(ItemStatsRegistry stats, int price, int limit, List<String> requiredItems) {
            this.stats = stats;
            this.price = price;
            this.limit = limit;
            this.requiredItems = requiredItems;
        }

        public ItemStatsRegistry getStats() {
            return stats;
        }

        public int getPrice() {
            return ItemShopData.getInstance().getPrice(getId());
        }

        public int getLimit() {
            return limit;
        }

        public List<String> getRequiredItems() {
            return requiredItems;
        }

        public String getDisplayName() {
            return stats.getName();
        }

        public String getId() {
            return stats.getId();
        }

        public int getEffectivePrice(Player player) {
            ItemShopData shopData = ItemShopData.getInstance();
            int total = shopData.getPrice(getId());

            for (String reqId : requiredItems) {
                total += shopData.getPrice(reqId);
            }

            if (player != null) {
                for (Map.Entry<String, Integer> entry : getRequiredCounts().entrySet()) {
                    int covered = Math.min(countOwnedLeagueItemId(player, entry.getKey()), entry.getValue());
                    total -= shopData.getPrice(entry.getKey()) * covered;
                }
            }

            return total;
        }

        public int countOwnedComponents(Player player) {
            int covered = 0;
            for (Map.Entry<String, Integer> entry : getRequiredCounts().entrySet()) {
                covered += Math.min(countOwnedLeagueItemId(player, entry.getKey()), entry.getValue());
            }
            return covered;
        }

        private Map<String, Integer> getRequiredCounts() {
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (String reqId : requiredItems) {
                counts.merge(reqId, 1, Integer::sum);
            }
            return counts;
        }

        private static int countOwnedLeagueItemId(Player player, String itemId) {
            int count = 0;
            for (ItemStack inv : player.getInventory().getContents()) {
                if (inv != null && !inv.getType().isAir() && itemId.equals(ItemModifier.getItemId(inv))) {
                    count++;
                }
            }
            return count;
        }

    }
}