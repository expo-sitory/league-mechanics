package dev.ixpu.leaguemechanics.entity.mob;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.configuration.ConfigurationSection;

import dev.ixpu.leaguemechanics.LeagueMechanics;


public class MobStats {

    public enum MobType {
        UNDEAD,
        ARTHROPOD,
        CRITTER,
        ENDERMAN,
        WITCH,
        SLIME,
        BLAZE,
        GHAST,
        OTHER
    }

    private static LeagueMechanics plugin;

    private static double ZOMBIE_AD;
    private static double ZOMBIE_AP;
    private static double ZOMBIE_AR;
    private static double ZOMBIE_MR;
    private static MobType ZOMBIE_TYPE;

    private static double SKELETON_AD;
    private static double SKELETON_AP;
    private static double SKELETON_AR;
    private static double SKELETON_MR;
    private static MobType SKELETON_TYPE;

    private static double SPIDER_AD;
    private static double SPIDER_AP;
    private static double SPIDER_AR;
    private static double SPIDER_MR;
    private static MobType SPIDER_TYPE;

    private static double CAVE_SPIDER_AD;
    private static double CAVE_SPIDER_AP;
    private static double CAVE_SPIDER_AR;
    private static double CAVE_SPIDER_MR;
    private static MobType CAVE_SPIDER_TYPE;

    private static double CREEPER_AD;
    private static double CREEPER_AP;
    private static double CREEPER_AR;
    private static double CREEPER_MR;
    private static MobType CREEPER_TYPE;

    private static double ENDERMAN_AD;
    private static double ENDERMAN_AP;
    private static double ENDERMAN_AR;
    private static double ENDERMAN_MR;
    private static MobType ENDERMAN_TYPE;

    private static double WITCH_AD;
    private static double WITCH_AP;
    private static double WITCH_AR;
    private static double WITCH_MR;
    private static MobType WITCH_TYPE;

    private static double SLIME_AD;
    private static double SLIME_AP;
    private static double SLIME_AR;
    private static double SLIME_MR;
    private static MobType SLIME_TYPE;

    private static double MAGMA_CUBE_AD;
    private static double MAGMA_CUBE_AP;
    private static double MAGMA_CUBE_AR;
    private static double MAGMA_CUBE_MR;
    private static MobType MAGMA_CUBE_TYPE;

    private static double BLAZE_AD;
    private static double BLAZE_AP;
    private static double BLAZE_AR;
    private static double BLAZE_MR;
    private static MobType BLAZE_TYPE;

    private static double GHAST_AD;
    private static double GHAST_AP;
    private static double GHAST_AR;
    private static double GHAST_MR;
    private static MobType GHAST_TYPE;

    private static double STRAY_AD;
    private static double STRAY_AP;
    private static double STRAY_AR;
    private static double STRAY_MR;
    private static MobType STRAY_TYPE;

    private static double HUSK_AD;
    private static double HUSK_AP;
    private static double HUSK_AR;
    private static double HUSK_MR;
    private static MobType HUSK_TYPE;

    private static double ZOMBIE_VILLAGER_AD;
    private static double ZOMBIE_VILLAGER_AP;
    private static double ZOMBIE_VILLAGER_AR;
    private static double ZOMBIE_VILLAGER_MR;
    private static MobType ZOMBIE_VILLAGER_TYPE;

    private static double WITHER_SKELETON_AD;
    private static double WITHER_SKELETON_AP;
    private static double WITHER_SKELETON_AR;
    private static double WITHER_SKELETON_MR;
    private static MobType WITHER_SKELETON_TYPE;

    public static void initialize(LeagueMechanics pluginInstance) {
        plugin = pluginInstance;
        loadMobStatsFromConfig();
    }

    private static void loadMobStatsFromConfig() {
        if (plugin == null) {
            loadDefaultValues();
            return;
        }

        ConfigurationSection mobStatsSection = plugin.getConfig().getConfigurationSection("mob-stats");
        if (mobStatsSection == null) {
            loadDefaultValues();
            return;
        }

        ConfigurationSection zombie = mobStatsSection.getConfigurationSection("zombie");
        if (zombie != null) {
            ZOMBIE_AD = zombie.getDouble("ad", 55.0);
            ZOMBIE_AP = zombie.getDouble("ap", 25.0);
            ZOMBIE_AR = zombie.getDouble("ar", 230.0);
            ZOMBIE_MR = zombie.getDouble("mr", 125.0);
            ZOMBIE_TYPE = MobType.valueOf(zombie.getString("type", "UNDEAD"));
        } else {
            loadDefaultZombie();
        }

        ConfigurationSection skeleton = mobStatsSection.getConfigurationSection("skeleton");
        if (skeleton != null) {
            SKELETON_AD = skeleton.getDouble("ad", 74.0);
            SKELETON_AP = skeleton.getDouble("ap", 150.0);
            SKELETON_AR = skeleton.getDouble("ar", 130.0);
            SKELETON_MR = skeleton.getDouble("mr", 345.0);
            SKELETON_TYPE = MobType.valueOf(skeleton.getString("type", "UNDEAD"));
        } else {
            loadDefaultSkeleton();
        }

        ConfigurationSection spider = mobStatsSection.getConfigurationSection("spider");
        if (spider != null) {
            SPIDER_AD = spider.getDouble("ad", 30.0);
            SPIDER_AP = spider.getDouble("ap", 10.0);
            SPIDER_AR = spider.getDouble("ar", 140.0);
            SPIDER_MR = spider.getDouble("mr", 0.0);
            SPIDER_TYPE = MobType.valueOf(spider.getString("type", "ARTHROPOD"));
        } else {
            loadDefaultSpider();
        }

        ConfigurationSection caveSpider = mobStatsSection.getConfigurationSection("cave_spider");
        if (caveSpider != null) {
            CAVE_SPIDER_AD = caveSpider.getDouble("ad", 25.0);
            CAVE_SPIDER_AP = caveSpider.getDouble("ap", 15.0);
            CAVE_SPIDER_AR = caveSpider.getDouble("ar", 80.0);
            CAVE_SPIDER_MR = caveSpider.getDouble("mr", 225.0);
            CAVE_SPIDER_TYPE = MobType.valueOf(caveSpider.getString("type", "ARTHROPOD"));
        } else {
            loadDefaultCaveSpider();
        }

        ConfigurationSection creeper = mobStatsSection.getConfigurationSection("creeper");
        if (creeper != null) {
            CREEPER_AD = creeper.getDouble("ad", 0.0);
            CREEPER_AP = creeper.getDouble("ap", 0.0);
            CREEPER_AR = creeper.getDouble("ar", 0.0);
            CREEPER_MR = creeper.getDouble("mr", 500.0);
            CREEPER_TYPE = MobType.valueOf(creeper.getString("type", "CRITTER"));
        } else {
            loadDefaultCreeper();
        }

        ConfigurationSection enderman = mobStatsSection.getConfigurationSection("enderman");
        if (enderman != null) {
            ENDERMAN_AD = enderman.getDouble("ad", 70.0);
            ENDERMAN_AP = enderman.getDouble("ap", 70.0);
            ENDERMAN_AR = enderman.getDouble("ar", 220.0);
            ENDERMAN_MR = enderman.getDouble("mr", 220.0);
            ENDERMAN_TYPE = MobType.valueOf(enderman.getString("type", "ENDERMAN"));
        } else {
            loadDefaultEnderman();
        }

        ConfigurationSection witch = mobStatsSection.getConfigurationSection("witch");
        if (witch != null) {
            WITCH_AD = witch.getDouble("ad", 0.0);
            WITCH_AP = witch.getDouble("ap", 350.0);
            WITCH_AR = witch.getDouble("ar", 55.0);
            WITCH_MR = witch.getDouble("mr", 530.0);
            WITCH_TYPE = MobType.valueOf(witch.getString("type", "WITCH"));
        } else {
            loadDefaultWitch();
        }

        ConfigurationSection slime = mobStatsSection.getConfigurationSection("slime");
        if (slime != null) {
            SLIME_AD = slime.getDouble("ad", 20.0);
            SLIME_AP = slime.getDouble("ap", 20.0);
            SLIME_AR = slime.getDouble("ar", 850.0);
            SLIME_MR = slime.getDouble("mr", 10.0);
            SLIME_TYPE = MobType.valueOf(slime.getString("type", "SLIME"));
        } else {
            loadDefaultSlime();
        }

        ConfigurationSection magmaCube = mobStatsSection.getConfigurationSection("magma_cube");
        if (magmaCube != null) {
            MAGMA_CUBE_AD = magmaCube.getDouble("ad", 25.0);
            MAGMA_CUBE_AP = magmaCube.getDouble("ap", 15.0);
            MAGMA_CUBE_AR = magmaCube.getDouble("ar", 400.0);
            MAGMA_CUBE_MR = magmaCube.getDouble("mr", 400.0);
            MAGMA_CUBE_TYPE = MobType.valueOf(magmaCube.getString("type", "SLIME"));
        } else {
            loadDefaultMagmaCube();
        }

        ConfigurationSection blaze = mobStatsSection.getConfigurationSection("blaze");
        if (blaze != null) {
            BLAZE_AD = blaze.getDouble("ad", 40.0);
            BLAZE_AP = blaze.getDouble("ap", 770.0);
            BLAZE_AR = blaze.getDouble("ar", 999.0);
            BLAZE_MR = blaze.getDouble("mr", 340.0);
            BLAZE_TYPE = MobType.valueOf(blaze.getString("type", "BLAZE"));
        } else {
            loadDefaultBlaze();
        }

        ConfigurationSection ghast = mobStatsSection.getConfigurationSection("ghast");
        if (ghast != null) {
            GHAST_AD = ghast.getDouble("ad", 0.0);
            GHAST_AP = ghast.getDouble("ap", 740.0);
            GHAST_AR = ghast.getDouble("ar", 50.0);
            GHAST_MR = ghast.getDouble("mr", 800.0);
            GHAST_TYPE = MobType.valueOf(ghast.getString("type", "GHAST"));
        } else {
            loadDefaultGhast();
        }

        ConfigurationSection stray = mobStatsSection.getConfigurationSection("stray");
        if (stray != null) {
            STRAY_AD = stray.getDouble("ad", 25.0);
            STRAY_AP = stray.getDouble("ap", 100.0);
            STRAY_AR = stray.getDouble("ar", 150.0);
            STRAY_MR = stray.getDouble("mr", 25.0);
            STRAY_TYPE = MobType.valueOf(stray.getString("type", "UNDEAD"));
        } else {
            loadDefaultStray();
        }

        ConfigurationSection husk = mobStatsSection.getConfigurationSection("husk");
        if (husk != null) {
            HUSK_AD = husk.getDouble("ad", 80.0);
            HUSK_AP = husk.getDouble("ap", 20.0);
            HUSK_AR = husk.getDouble("ar", 260.0);
            HUSK_MR = husk.getDouble("mr", 57.0);
            HUSK_TYPE = MobType.valueOf(husk.getString("type", "UNDEAD"));
        } else {
            loadDefaultHusk();
        }

        ConfigurationSection zombieVillager = mobStatsSection.getConfigurationSection("zombie_villager");
        if (zombieVillager != null) {
            ZOMBIE_VILLAGER_AD = zombieVillager.getDouble("ad", 80.0);
            ZOMBIE_VILLAGER_AP = zombieVillager.getDouble("ap", 20.0);
            ZOMBIE_VILLAGER_AR = zombieVillager.getDouble("ar", 120.0);
            ZOMBIE_VILLAGER_MR = zombieVillager.getDouble("mr", 46.0);
            ZOMBIE_VILLAGER_TYPE = MobType.valueOf(zombieVillager.getString("type", "UNDEAD"));
        } else {
            loadDefaultZombieVillager();
        }

        ConfigurationSection witherSkeleton = mobStatsSection.getConfigurationSection("wither_skeleton");
        if (witherSkeleton != null) {
            WITHER_SKELETON_AD = witherSkeleton.getDouble("ad", 357.0);
            WITHER_SKELETON_AP = witherSkeleton.getDouble("ap", 15.0);
            WITHER_SKELETON_AR = witherSkeleton.getDouble("ar", 555.0);
            WITHER_SKELETON_MR = witherSkeleton.getDouble("mr", 120.0);
            WITHER_SKELETON_TYPE = MobType.valueOf(witherSkeleton.getString("type", "UNDEAD"));
        } else {
            loadDefaultWitherSkeleton();
        }
    }

    private static void loadDefaultValues() {
        loadDefaultZombie();
        loadDefaultSkeleton();
        loadDefaultSpider();
        loadDefaultCaveSpider();
        loadDefaultCreeper();
        loadDefaultEnderman();
        loadDefaultWitch();
        loadDefaultSlime();
        loadDefaultMagmaCube();
        loadDefaultBlaze();
        loadDefaultGhast();
        loadDefaultStray();
        loadDefaultHusk();
        loadDefaultZombieVillager();
        loadDefaultWitherSkeleton();
    }

    private static void loadDefaultZombie() {
        ZOMBIE_AD = 55.0;
        ZOMBIE_AP = 25.0;
        ZOMBIE_AR = 230.0;
        ZOMBIE_MR = 125.0;
        ZOMBIE_TYPE = MobType.UNDEAD;
    }

    private static void loadDefaultSkeleton() {
        SKELETON_AD = 74.0;
        SKELETON_AP = 150.0;
        SKELETON_AR = 130.0;
        SKELETON_MR = 345.0;
        SKELETON_TYPE = MobType.UNDEAD;
    }

    private static void loadDefaultSpider() {
        SPIDER_AD = 30.0;
        SPIDER_AP = 10.0;
        SPIDER_AR = 140.0;
        SPIDER_MR = 0.0;
        SPIDER_TYPE = MobType.ARTHROPOD;
    }

    private static void loadDefaultCaveSpider() {
        CAVE_SPIDER_AD = 25.0;
        CAVE_SPIDER_AP = 15.0;
        CAVE_SPIDER_AR = 80.0;
        CAVE_SPIDER_MR = 225.0;
        CAVE_SPIDER_TYPE = MobType.ARTHROPOD;
    }

    private static void loadDefaultCreeper() {
        CREEPER_AD = 0.0;
        CREEPER_AP = 0.0;
        CREEPER_AR = 0.0;
        CREEPER_MR = 500.0;
        CREEPER_TYPE = MobType.CRITTER;
    }

    private static void loadDefaultEnderman() {
        ENDERMAN_AD = 70.0;
        ENDERMAN_AP = 70.0;
        ENDERMAN_AR = 220.0;
        ENDERMAN_MR = 220.0;
        ENDERMAN_TYPE = MobType.ENDERMAN;
    }

    private static void loadDefaultWitch() {
        WITCH_AD = 0.0;
        WITCH_AP = 350.0;
        WITCH_AR = 55.0;
        WITCH_MR = 530.0;
        WITCH_TYPE = MobType.WITCH;
    }

    private static void loadDefaultSlime() {
        SLIME_AD = 20.0;
        SLIME_AP = 20.0;
        SLIME_AR = 850.0;
        SLIME_MR = 10.0;
        SLIME_TYPE = MobType.SLIME;
    }

    private static void loadDefaultMagmaCube() {
        MAGMA_CUBE_AD = 25.0;
        MAGMA_CUBE_AP = 15.0;
        MAGMA_CUBE_AR = 400.0;
        MAGMA_CUBE_MR = 400.0;
        MAGMA_CUBE_TYPE = MobType.SLIME;
    }

    private static void loadDefaultBlaze() {
        BLAZE_AD = 40.0;
        BLAZE_AP = 770.0;
        BLAZE_AR = 999.0;
        BLAZE_MR = 340.0;
        BLAZE_TYPE = MobType.BLAZE;
    }

    private static void loadDefaultGhast() {
        GHAST_AD = 0.0;
        GHAST_AP = 740.0;
        GHAST_AR = 50.0;
        GHAST_MR = 800.0;
        GHAST_TYPE = MobType.GHAST;
    }

    private static void loadDefaultStray() {
        STRAY_AD = 25.0;
        STRAY_AP = 100.0;
        STRAY_AR = 150.0;
        STRAY_MR = 25.0;
        STRAY_TYPE = MobType.UNDEAD;
    }

    private static void loadDefaultHusk() {
        HUSK_AD = 80.0;
        HUSK_AP = 20.0;
        HUSK_AR = 260.0;
        HUSK_MR = 57.0;
        HUSK_TYPE = MobType.UNDEAD;
    }

    private static void loadDefaultZombieVillager() {
        ZOMBIE_VILLAGER_AD = 80.0;
        ZOMBIE_VILLAGER_AP = 20.0;
        ZOMBIE_VILLAGER_AR = 120.0;
        ZOMBIE_VILLAGER_MR = 46.0;
        ZOMBIE_VILLAGER_TYPE = MobType.UNDEAD;
    }

    private static void loadDefaultWitherSkeleton() {
        WITHER_SKELETON_AD = 357.0;
        WITHER_SKELETON_AP = 15.0;
        WITHER_SKELETON_AR = 555.0;
        WITHER_SKELETON_MR = 120.0;
        WITHER_SKELETON_TYPE = MobType.UNDEAD;
    }

    public static double getMobAD(Entity entity) {
        if (entity == null) return 0;

        String entityType = entity.getType().getKey().toString();
        double baseAd = switch (entityType) {
            case "minecraft:zombie" -> ZOMBIE_AD;
            case "minecraft:skeleton" -> SKELETON_AD;
            case "minecraft:spider" -> SPIDER_AD;
            case "minecraft:cave_spider" -> CAVE_SPIDER_AD;
            case "minecraft:creeper" -> CREEPER_AD;
            case "minecraft:enderman" -> ENDERMAN_AD;
            case "minecraft:witch" -> WITCH_AD;
            case "minecraft:slime" -> SLIME_AD;
            case "minecraft:magma_cube" -> MAGMA_CUBE_AD;
            case "minecraft:blaze" -> BLAZE_AD;
            case "minecraft:ghast" -> GHAST_AD;
            case "minecraft:stray" -> STRAY_AD;
            case "minecraft:husk" -> HUSK_AD;
            case "minecraft:zombie_villager" -> ZOMBIE_VILLAGER_AD;
            case "minecraft:wither_skeleton" -> WITHER_SKELETON_AD;
            default -> 0;
        };

        if (entity instanceof LivingEntity livingEntity) {
            int weaknessAmplifier = 0;
            int strengthAmplifier = 0;

            for (PotionEffect effect : livingEntity.getActivePotionEffects()) {
                switch (effect.getType().getName()) {
                    case "weakness":
                        weaknessAmplifier = Math.max(weaknessAmplifier, effect.getAmplifier() + 1);
                        break;
                    case "strength":
                        strengthAmplifier = Math.max(strengthAmplifier, effect.getAmplifier() + 1);
                        break;
                }
            }

            if (weaknessAmplifier > 0) {
                baseAd *= (1.0 - 0.2 * weaknessAmplifier);
            }
            if (strengthAmplifier > 0) {
                baseAd *= (1.0 + 0.2 * strengthAmplifier);
            }
        }

        return baseAd;
    }

    public static double getMobAP(Entity entity) {
        if (entity == null) return 0;

        String entityType = entity.getType().getKey().toString();
        return switch (entityType) {
            case "minecraft:zombie" -> ZOMBIE_AP;
            case "minecraft:skeleton" -> SKELETON_AP;
            case "minecraft:spider" -> SPIDER_AP;
            case "minecraft:cave_spider" -> CAVE_SPIDER_AP;
            case "minecraft:creeper" -> CREEPER_AP;
            case "minecraft:enderman" -> ENDERMAN_AP;
            case "minecraft:witch" -> WITCH_AP;
            case "minecraft:slime" -> SLIME_AP;
            case "minecraft:magma_cube" -> MAGMA_CUBE_AP;
            case "minecraft:blaze" -> BLAZE_AP;
            case "minecraft:ghast" -> GHAST_AP;
            case "minecraft:stray" -> STRAY_AP;
            case "minecraft:husk" -> HUSK_AP;
            case "minecraft:zombie_villager" -> ZOMBIE_VILLAGER_AP;
            case "minecraft:wither_skeleton" -> WITHER_SKELETON_AP;
            default -> 0;
        };
    }

    public static double getMobAR(Entity entity) {
        if (entity == null) return 0;

        String entityType = entity.getType().getKey().toString();
        double baseAr = switch (entityType) {
            case "minecraft:zombie" -> ZOMBIE_AR;
            case "minecraft:skeleton" -> SKELETON_AR;
            case "minecraft:spider" -> SPIDER_AR;
            case "minecraft:cave_spider" -> CAVE_SPIDER_AR;
            case "minecraft:creeper" -> CREEPER_AR;
            case "minecraft:enderman" -> ENDERMAN_AR;
            case "minecraft:witch" -> WITCH_AR;
            case "minecraft:slime" -> SLIME_AR;
            case "minecraft:magma_cube" -> MAGMA_CUBE_AR;
            case "minecraft:blaze" -> BLAZE_AR;
            case "minecraft:ghast" -> GHAST_AR;
            case "minecraft:stray" -> STRAY_AR;
            case "minecraft:husk" -> HUSK_AR;
            case "minecraft:zombie_villager" -> ZOMBIE_VILLAGER_AR;
            case "minecraft:wither_skeleton" -> WITHER_SKELETON_AR;
            default -> 0;
        };

        if (entity instanceof LivingEntity livingEntity) {
            int resistanceAmplifier = 0;

            for (PotionEffect effect : livingEntity.getActivePotionEffects()) {
                switch (effect.getType().getName()) {
                    case "resistance":
                        resistanceAmplifier = Math.max(resistanceAmplifier, effect.getAmplifier() + 1);
                        break;
                }
            }

            if (resistanceAmplifier > 0) {
                baseAr *= (1.0 + 0.2 * resistanceAmplifier);
            }
        }

        return baseAr;
    }

    public static double getMobMR(Entity entity) {
        if (entity == null) return 0;

        String entityType = entity.getType().getKey().toString();
        double baseMr = switch (entityType) {
            case "minecraft:zombie" -> ZOMBIE_MR;
            case "minecraft:skeleton" -> SKELETON_MR;
            case "minecraft:spider" -> SPIDER_MR;
            case "minecraft:cave_spider" -> CAVE_SPIDER_MR;
            case "minecraft:creeper" -> CREEPER_MR;
            case "minecraft:enderman" -> ENDERMAN_MR;
            case "minecraft:witch" -> WITCH_MR;
            case "minecraft:slime" -> SLIME_MR;
            case "minecraft:magma_cube" -> MAGMA_CUBE_MR;
            case "minecraft:blaze" -> BLAZE_MR;
            case "minecraft:ghast" -> GHAST_MR;
            case "minecraft:stray" -> STRAY_MR;
            case "minecraft:husk" -> HUSK_MR;
            case "minecraft:zombie_villager" -> ZOMBIE_VILLAGER_MR;
            case "minecraft:wither_skeleton" -> WITHER_SKELETON_MR;
            default -> 0;
        };

        if (entity instanceof LivingEntity livingEntity) {
            int resistanceAmplifier = 0;

            for (PotionEffect effect : livingEntity.getActivePotionEffects()) {
                switch (effect.getType().getName()) {
                    case "resistance":
                        resistanceAmplifier = Math.max(resistanceAmplifier, effect.getAmplifier() + 1);
                        break;
                }
            }

            if (resistanceAmplifier > 0) {
                baseMr *= (1.0 + 0.2 * resistanceAmplifier);
            }
        }

        return baseMr;
    }

    public static MobType getMobType(Entity entity) {
        if (entity == null) return MobType.OTHER;

        String entityType = entity.getType().getKey().toString();
        return switch (entityType) {
            case "minecraft:zombie" -> ZOMBIE_TYPE;
            case "minecraft:skeleton" -> SKELETON_TYPE;
            case "minecraft:spider" -> SPIDER_TYPE;
            case "minecraft:cave_spider" -> CAVE_SPIDER_TYPE;
            case "minecraft:creeper" -> CREEPER_TYPE;
            case "minecraft:enderman" -> ENDERMAN_TYPE;
            case "minecraft:witch" -> WITCH_TYPE;
            case "minecraft:slime" -> SLIME_TYPE;
            case "minecraft:magma_cube" -> MAGMA_CUBE_TYPE;
            case "minecraft:blaze" -> BLAZE_TYPE;
            case "minecraft:ghast" -> GHAST_TYPE;
            case "minecraft:stray" -> STRAY_TYPE;
            case "minecraft:husk" -> HUSK_TYPE;
            case "minecraft:zombie_villager" -> ZOMBIE_VILLAGER_TYPE;
            case "minecraft:wither_skeleton" -> WITHER_SKELETON_TYPE;
            default -> MobType.OTHER;
        };
    }

    public static boolean isSupportedMob(Entity entity) {
        if (entity == null) return false;

        String entityType = entity.getType().getKey().toString();
        return entityType.equals("minecraft:zombie") ||
                entityType.equals("minecraft:skeleton") ||
                entityType.equals("minecraft:spider") ||
                entityType.equals("minecraft:cave_spider") ||
                entityType.equals("minecraft:creeper") ||
                entityType.equals("minecraft:enderman") ||
                entityType.equals("minecraft:witch") ||
                entityType.equals("minecraft:slime") ||
                entityType.equals("minecraft:magma_cube") ||
                entityType.equals("minecraft:blaze") ||
                entityType.equals("minecraft:ghast") ||
                entityType.equals("minecraft:stray") ||
                entityType.equals("minecraft:husk") ||
                entityType.equals("minecraft:zombie_villager") ||
                entityType.equals("minecraft:wither_skeleton");
    }
}