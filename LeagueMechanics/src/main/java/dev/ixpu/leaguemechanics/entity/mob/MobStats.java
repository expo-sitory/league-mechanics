package dev.ixpu.leaguemechanics.entity.mob;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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
        IRON_GOLEM,
        DROWNED,
        ZOMBIFIED_PIGLIN,
        PIGLIN,
        PIGLIN_BRUTE,
        BREEZE,
        CREAKING,
        WARDEN,
        EVOKER,
        PILLAGER,
        RAVAGER,
        VEX,
        VINDICATOR,
        HOGLIN,
        ZOGLIN,
        PARCHED,
        PHANTOM,
        ENDER_DRAGON,
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

    private static double IRON_GOLEM_AD;
    private static double IRON_GOLEM_AP;
    private static double IRON_GOLEM_AR;
    private static double IRON_GOLEM_MR;
    private static MobType IRON_GOLEM_TYPE;

    private static double DROWNED_AD;
    private static double DROWNED_AP;
    private static double DROWNED_AR;
    private static double DROWNED_MR;
    private static MobType DROWNED_TYPE;

    private static double ZOMBIFIED_PIGLIN_AD;
    private static double ZOMBIFIED_PIGLIN_AP;
    private static double ZOMBIFIED_PIGLIN_AR;
    private static double ZOMBIFIED_PIGLIN_MR;
    private static MobType ZOMBIFIED_PIGLIN_TYPE;

    private static double PIGLIN_AD;
    private static double PIGLIN_AP;
    private static double PIGLIN_AR;
    private static double PIGLIN_MR;
    private static MobType PIGLIN_TYPE;

    private static double PIGLIN_BRUTE_AD;
    private static double PIGLIN_BRUTE_AP;
    private static double PIGLIN_BRUTE_AR;
    private static double PIGLIN_BRUTE_MR;
    private static MobType PIGLIN_BRUTE_TYPE;

    private static double BREEZE_AD;
    private static double BREEZE_AP;
    private static double BREEZE_AR;
    private static double BREEZE_MR;
    private static MobType BREEZE_TYPE;

    private static double CREAKING_AD;
    private static double CREAKING_AP;
    private static double CREAKING_AR;
    private static double CREAKING_MR;
    private static MobType CREAKING_TYPE;

    private static double WARDEN_AD;
    private static double WARDEN_AP;
    private static double WARDEN_AR;
    private static double WARDEN_MR;
    private static MobType WARDEN_TYPE;

    private static double EVOKER_AD;
    private static double EVOKER_AP;
    private static double EVOKER_AR;
    private static double EVOKER_MR;
    private static MobType EVOKER_TYPE;

    private static double PILLAGER_AD;
    private static double PILLAGER_AP;
    private static double PILLAGER_AR;
    private static double PILLAGER_MR;
    private static MobType PILLAGER_TYPE;

    private static double RAVAGER_AD;
    private static double RAVAGER_AP;
    private static double RAVAGER_AR;
    private static double RAVAGER_MR;
    private static MobType RAVAGER_TYPE;

    private static double VEX_AD;
    private static double VEX_AP;
    private static double VEX_AR;
    private static double VEX_MR;
    private static MobType VEX_TYPE;

    private static double VINDICATOR_AD;
    private static double VINDICATOR_AP;
    private static double VINDICATOR_AR;
    private static double VINDICATOR_MR;
    private static MobType VINDICATOR_TYPE;

    private static double HOGLIN_AD;
    private static double HOGLIN_AP;
    private static double HOGLIN_AR;
    private static double HOGLIN_MR;
    private static MobType HOGLIN_TYPE;

    private static double ZOGLIN_AD;
    private static double ZOGLIN_AP;
    private static double ZOGLIN_AR;
    private static double ZOGLIN_MR;
    private static MobType ZOGLIN_TYPE;

    private static double PARCHED_AD;
    private static double PARCHED_AP;
    private static double PARCHED_AR;
    private static double PARCHED_MR;
    private static MobType PARCHED_TYPE;

    private static double PHANTOM_AD;
    private static double PHANTOM_AP;
    private static double PHANTOM_AR;
    private static double PHANTOM_MR;
    private static MobType PHANTOM_TYPE;

    private static double ENDER_DRAGON_AD;
    private static double ENDER_DRAGON_AP;
    private static double ENDER_DRAGON_AR;
    private static double ENDER_DRAGON_MR;
    private static MobType ENDER_DRAGON_TYPE;

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

        ConfigurationSection ironGolem = mobStatsSection.getConfigurationSection("iron_golem");
        if (ironGolem != null) {
            IRON_GOLEM_AD = ironGolem.getDouble("ad", 200.0);
            IRON_GOLEM_AP = ironGolem.getDouble("ap", 0.0);
            IRON_GOLEM_AR = ironGolem.getDouble("ar", 500.0);
            IRON_GOLEM_MR = ironGolem.getDouble("mr", 200.0);
            IRON_GOLEM_TYPE = MobType.valueOf(ironGolem.getString("type", "IRON_GOLEM"));
        } else {
            loadDefaultIronGolem();
        }

        ConfigurationSection drowned = mobStatsSection.getConfigurationSection("drowned");
        if (drowned != null) {
            DROWNED_AD = drowned.getDouble("ad", 75.0);
            DROWNED_AP = drowned.getDouble("ap", 30.0);
            DROWNED_AR = drowned.getDouble("ar", 220.0);
            DROWNED_MR = drowned.getDouble("mr", 135.0);
            DROWNED_TYPE = MobType.valueOf(drowned.getString("type", "UNDEAD"));
        } else {
            loadDefaultDrowned();
        }

        ConfigurationSection zombifiedPiglin = mobStatsSection.getConfigurationSection("zombified_piglin");
        if (zombifiedPiglin != null) {
            ZOMBIFIED_PIGLIN_AD = zombifiedPiglin.getDouble("ad", 95.0);
            ZOMBIFIED_PIGLIN_AP = zombifiedPiglin.getDouble("ap", 35.0);
            ZOMBIFIED_PIGLIN_AR = zombifiedPiglin.getDouble("ar", 240.0);
            ZOMBIFIED_PIGLIN_MR = zombifiedPiglin.getDouble("mr", 150.0);
            ZOMBIFIED_PIGLIN_TYPE = MobType.valueOf(zombifiedPiglin.getString("type", "UNDEAD"));
        } else {
            loadDefaultZombifiedPiglin();
        }

        ConfigurationSection piglin = mobStatsSection.getConfigurationSection("piglin");
        if (piglin != null) {
            PIGLIN_AD = piglin.getDouble("ad", 110.0);
            PIGLIN_AP = piglin.getDouble("ap", 40.0);
            PIGLIN_AR = piglin.getDouble("ar", 180.0);
            PIGLIN_MR = piglin.getDouble("mr", 140.0);
            PIGLIN_TYPE = MobType.valueOf(piglin.getString("type", "CRITTER"));
        } else {
            loadDefaultPiglin();
        }

        ConfigurationSection piglinBrute = mobStatsSection.getConfigurationSection("piglin_brute");
        if (piglinBrute != null) {
            PIGLIN_BRUTE_AD = piglinBrute.getDouble("ad", 180.0);
            PIGLIN_BRUTE_AP = piglinBrute.getDouble("ap", 50.0);
            PIGLIN_BRUTE_AR = piglinBrute.getDouble("ar", 320.0);
            PIGLIN_BRUTE_MR = piglinBrute.getDouble("mr", 180.0);
            PIGLIN_BRUTE_TYPE = MobType.valueOf(piglinBrute.getString("type", "CRITTER"));
        } else {
            loadDefaultPiglinBrute();
        }

        ConfigurationSection breeze = mobStatsSection.getConfigurationSection("breeze");
        if (breeze != null) {
            BREEZE_AD = breeze.getDouble("ad", 50.0);
            BREEZE_AP = breeze.getDouble("ap", 250.0);
            BREEZE_AR = breeze.getDouble("ar", 120.0);
            BREEZE_MR = breeze.getDouble("mr", 300.0);
            BREEZE_TYPE = MobType.valueOf(breeze.getString("type", "BREEZE"));
        } else {
            loadDefaultBreeze();
        }

        ConfigurationSection creaking = mobStatsSection.getConfigurationSection("creaking");
        if (creaking != null) {
            CREAKING_AD = creaking.getDouble("ad", 120.0);
            CREAKING_AP = creaking.getDouble("ap", 40.0);
            CREAKING_AR = creaking.getDouble("ar", 350.0);
            CREAKING_MR = creaking.getDouble("mr", 100.0);
            CREAKING_TYPE = MobType.valueOf(creaking.getString("type", "CREAKING"));
        } else {
            loadDefaultCreaking();
        }

        ConfigurationSection warden = mobStatsSection.getConfigurationSection("warden");
        if (warden != null) {
            WARDEN_AD = warden.getDouble("ad", 450.0);
            WARDEN_AP = warden.getDouble("ap", 100.0);
            WARDEN_AR = warden.getDouble("ar", 600.0);
            WARDEN_MR = warden.getDouble("mr", 400.0);
            WARDEN_TYPE = MobType.valueOf(warden.getString("type", "WARDEN"));
        } else {
            loadDefaultWarden();
        }

        ConfigurationSection evoker = mobStatsSection.getConfigurationSection("evoker");
        if (evoker != null) {
            EVOKER_AD = evoker.getDouble("ad", 30.0);
            EVOKER_AP = evoker.getDouble("ap", 450.0);
            EVOKER_AR = evoker.getDouble("ar", 100.0);
            EVOKER_MR = evoker.getDouble("mr", 300.0);
            EVOKER_TYPE = MobType.valueOf(evoker.getString("type", "CRITTER"));
        } else {
            loadDefaultEvoker();
        }

        ConfigurationSection pillager = mobStatsSection.getConfigurationSection("pillager");
        if (pillager != null) {
            PILLAGER_AD = pillager.getDouble("ad", 90.0);
            PILLAGER_AP = pillager.getDouble("ap", 50.0);
            PILLAGER_AR = pillager.getDouble("ar", 120.0);
            PILLAGER_MR = pillager.getDouble("mr", 110.0);
            PILLAGER_TYPE = MobType.valueOf(pillager.getString("type", "CRITTER"));
        } else {
            loadDefaultPillager();
        }

        ConfigurationSection ravager = mobStatsSection.getConfigurationSection("ravager");
        if (ravager != null) {
            RAVAGER_AD = ravager.getDouble("ad", 380.0);
            RAVAGER_AP = ravager.getDouble("ap", 80.0);
            RAVAGER_AR = ravager.getDouble("ar", 480.0);
            RAVAGER_MR = ravager.getDouble("mr", 200.0);
            RAVAGER_TYPE = MobType.valueOf(ravager.getString("type", "CRITTER"));
        } else {
            loadDefaultRavager();
        }

        ConfigurationSection vex = mobStatsSection.getConfigurationSection("vex");
        if (vex != null) {
            VEX_AD = vex.getDouble("ad", 45.0);
            VEX_AP = vex.getDouble("ap", 80.0);
            VEX_AR = vex.getDouble("ar", 80.0);
            VEX_MR = vex.getDouble("mr", 120.0);
            VEX_TYPE = MobType.valueOf(vex.getString("type", "CRITTER"));
        } else {
            loadDefaultVex();
        }

        ConfigurationSection vindicator = mobStatsSection.getConfigurationSection("vindicator");
        if (vindicator != null) {
            VINDICATOR_AD = vindicator.getDouble("ad", 160.0);
            VINDICATOR_AP = vindicator.getDouble("ap", 30.0);
            VINDICATOR_AR = vindicator.getDouble("ar", 140.0);
            VINDICATOR_MR = vindicator.getDouble("mr", 100.0);
            VINDICATOR_TYPE = MobType.valueOf(vindicator.getString("type", "CRITTER"));
        } else {
            loadDefaultVindicator();
        }

        ConfigurationSection hoglin = mobStatsSection.getConfigurationSection("hoglin");
        if (hoglin != null) {
            HOGLIN_AD = hoglin.getDouble("ad", 140.0);
            HOGLIN_AP = hoglin.getDouble("ap", 20.0);
            HOGLIN_AR = hoglin.getDouble("ar", 220.0);
            HOGLIN_MR = hoglin.getDouble("mr", 80.0);
            HOGLIN_TYPE = MobType.valueOf(hoglin.getString("type", "CRITTER"));
        } else {
            loadDefaultHoglin();
        }

        ConfigurationSection zoglin = mobStatsSection.getConfigurationSection("zoglin");
        if (zoglin != null) {
            ZOGLIN_AD = zoglin.getDouble("ad", 160.0);
            ZOGLIN_AP = zoglin.getDouble("ap", 25.0);
            ZOGLIN_AR = zoglin.getDouble("ar", 260.0);
            ZOGLIN_MR = zoglin.getDouble("mr", 100.0);
            ZOGLIN_TYPE = MobType.valueOf(zoglin.getString("type", "UNDEAD"));
        } else {
            loadDefaultZoglin();
        }

        ConfigurationSection parched = mobStatsSection.getConfigurationSection("parched");
        if (parched != null) {
            PARCHED_AD = parched.getDouble("ad", 110.0);
            PARCHED_AP = parched.getDouble("ap", 35.0);
            PARCHED_AR = parched.getDouble("ar", 180.0);
            PARCHED_MR = parched.getDouble("mr", 90.0);
            PARCHED_TYPE = MobType.valueOf(parched.getString("type", "PARCHED"));
        } else {
            loadDefaultParched();
        }

        ConfigurationSection phantom = mobStatsSection.getConfigurationSection("phantom");
        if (phantom != null) {
            PHANTOM_AD = phantom.getDouble("ad", 85.0);
            PHANTOM_AP = phantom.getDouble("ap", 40.0);
            PHANTOM_AR = phantom.getDouble("ar", 110.0);
            PHANTOM_MR = phantom.getDouble("mr", 150.0);
            PHANTOM_TYPE = MobType.valueOf(phantom.getString("type", "CRITTER"));
        } else {
            loadDefaultPhantom();
        }

        ConfigurationSection enderDragon = mobStatsSection.getConfigurationSection("ender_dragon");
        if (enderDragon != null) {
            ENDER_DRAGON_AD = enderDragon.getDouble("ad", 800.0);
            ENDER_DRAGON_AP = enderDragon.getDouble("ap", 600.0);
            ENDER_DRAGON_AR = enderDragon.getDouble("ar", 800.0);
            ENDER_DRAGON_MR = enderDragon.getDouble("mr", 600.0);
            ENDER_DRAGON_TYPE = MobType.valueOf(enderDragon.getString("type", "ENDER_DRAGON"));
        } else {
            loadDefaultEnderDragon();
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
        loadDefaultIronGolem();
        loadDefaultDrowned();
        loadDefaultZombifiedPiglin();
        loadDefaultPiglin();
        loadDefaultPiglinBrute();
        loadDefaultBreeze();
        loadDefaultCreaking();
        loadDefaultWarden();
        loadDefaultEvoker();
        loadDefaultPillager();
        loadDefaultRavager();
        loadDefaultVex();
        loadDefaultVindicator();
        loadDefaultHoglin();
        loadDefaultZoglin();
        loadDefaultParched();
        loadDefaultPhantom();
        loadDefaultEnderDragon();
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

    private static void loadDefaultIronGolem() {
        IRON_GOLEM_AD = 200.0;
        IRON_GOLEM_AP = 0.0;
        IRON_GOLEM_AR = 500.0;
        IRON_GOLEM_MR = 200.0;
        IRON_GOLEM_TYPE = MobType.IRON_GOLEM;
    }

    private static void loadDefaultDrowned() {
        DROWNED_AD = 75.0;
        DROWNED_AP = 30.0;
        DROWNED_AR = 220.0;
        DROWNED_MR = 135.0;
        DROWNED_TYPE = MobType.UNDEAD;
    }

    private static void loadDefaultZombifiedPiglin() {
        ZOMBIFIED_PIGLIN_AD = 95.0;
        ZOMBIFIED_PIGLIN_AP = 35.0;
        ZOMBIFIED_PIGLIN_AR = 240.0;
        ZOMBIFIED_PIGLIN_MR = 150.0;
        ZOMBIFIED_PIGLIN_TYPE = MobType.UNDEAD;
    }

    private static void loadDefaultPiglin() {
        PIGLIN_AD = 110.0;
        PIGLIN_AP = 40.0;
        PIGLIN_AR = 180.0;
        PIGLIN_MR = 140.0;
        PIGLIN_TYPE = MobType.CRITTER;
    }

    private static void loadDefaultPiglinBrute() {
        PIGLIN_BRUTE_AD = 180.0;
        PIGLIN_BRUTE_AP = 50.0;
        PIGLIN_BRUTE_AR = 320.0;
        PIGLIN_BRUTE_MR = 180.0;
        PIGLIN_BRUTE_TYPE = MobType.CRITTER;
    }

    private static void loadDefaultBreeze() {
        BREEZE_AD = 50.0;
        BREEZE_AP = 250.0;
        BREEZE_AR = 120.0;
        BREEZE_MR = 300.0;
        BREEZE_TYPE = MobType.BREEZE;
    }

    private static void loadDefaultCreaking() {
        CREAKING_AD = 120.0;
        CREAKING_AP = 40.0;
        CREAKING_AR = 350.0;
        CREAKING_MR = 100.0;
        CREAKING_TYPE = MobType.CREAKING;
    }

    private static void loadDefaultWarden() {
        WARDEN_AD = 450.0;
        WARDEN_AP = 100.0;
        WARDEN_AR = 600.0;
        WARDEN_MR = 400.0;
        WARDEN_TYPE = MobType.WARDEN;
    }

    private static void loadDefaultEvoker() {
        EVOKER_AD = 30.0;
        EVOKER_AP = 450.0;
        EVOKER_AR = 100.0;
        EVOKER_MR = 300.0;
        EVOKER_TYPE = MobType.EVOKER;
    }

    private static void loadDefaultPillager() {
        PILLAGER_AD = 90.0;
        PILLAGER_AP = 50.0;
        PILLAGER_AR = 120.0;
        PILLAGER_MR = 110.0;
        PILLAGER_TYPE = MobType.PILLAGER;
    }

    private static void loadDefaultRavager() {
        RAVAGER_AD = 380.0;
        RAVAGER_AP = 80.0;
        RAVAGER_AR = 480.0;
        RAVAGER_MR = 200.0;
        RAVAGER_TYPE = MobType.RAVAGER;
    }

    private static void loadDefaultVex() {
        VEX_AD = 45.0;
        VEX_AP = 80.0;
        VEX_AR = 80.0;
        VEX_MR = 120.0;
        VEX_TYPE = MobType.VEX;
    }

    private static void loadDefaultVindicator() {
        VINDICATOR_AD = 160.0;
        VINDICATOR_AP = 30.0;
        VINDICATOR_AR = 140.0;
        VINDICATOR_MR = 100.0;
        VINDICATOR_TYPE = MobType.VINDICATOR;
    }

    private static void loadDefaultHoglin() {
        HOGLIN_AD = 140.0;
        HOGLIN_AP = 20.0;
        HOGLIN_AR = 220.0;
        HOGLIN_MR = 80.0;
        HOGLIN_TYPE = MobType.HOGLIN;
    }

    private static void loadDefaultZoglin() {
        ZOGLIN_AD = 160.0;
        ZOGLIN_AP = 25.0;
        ZOGLIN_AR = 260.0;
        ZOGLIN_MR = 100.0;
        ZOGLIN_TYPE = MobType.ZOGLIN;
    }

    private static void loadDefaultParched() {
        PARCHED_AD = 110.0;
        PARCHED_AP = 35.0;
        PARCHED_AR = 180.0;
        PARCHED_MR = 90.0;
        PARCHED_TYPE = MobType.PARCHED;
    }

    private static void loadDefaultPhantom() {
        PHANTOM_AD = 85.0;
        PHANTOM_AP = 40.0;
        PHANTOM_AR = 110.0;
        PHANTOM_MR = 150.0;
        PHANTOM_TYPE = MobType.CRITTER;
    }

    private static void loadDefaultEnderDragon() {
        ENDER_DRAGON_AD = 800.0;
        ENDER_DRAGON_AP = 600.0;
        ENDER_DRAGON_AR = 800.0;
        ENDER_DRAGON_MR = 600.0;
        ENDER_DRAGON_TYPE = MobType.ENDER_DRAGON;
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
            case "minecraft:iron_golem" -> IRON_GOLEM_AD;
            case "minecraft:drowned" -> DROWNED_AD;
            case "minecraft:zombified_piglin" -> ZOMBIFIED_PIGLIN_AD;
            case "minecraft:piglin" -> PIGLIN_AD;
            case "minecraft:piglin_brute" -> PIGLIN_BRUTE_AD;
            case "minecraft:breeze" -> BREEZE_AD;
            case "minecraft:creaking" -> CREAKING_AD;
            case "minecraft:warden" -> WARDEN_AD;
            case "minecraft:evoker" -> EVOKER_AD;
            case "minecraft:pillager" -> PILLAGER_AD;
            case "minecraft:ravager" -> RAVAGER_AD;
            case "minecraft:vex" -> VEX_AD;
            case "minecraft:vindicator" -> VINDICATOR_AD;
            case "minecraft:hoglin" -> HOGLIN_AD;
            case "minecraft:zoglin" -> ZOGLIN_AD;
            case "minecraft:parched" -> PARCHED_AD;
            case "minecraft:phantom" -> PHANTOM_AD;
            case "minecraft:ender_dragon" -> ENDER_DRAGON_AD;

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
            case "minecraft:iron_golem" -> IRON_GOLEM_AP;
            case "minecraft:drowned" -> DROWNED_AP;
            case "minecraft:zombified_piglin" -> ZOMBIFIED_PIGLIN_AP;
            case "minecraft:piglin" -> PIGLIN_AP;
            case "minecraft:piglin_brute" -> PIGLIN_BRUTE_AP;
            case "minecraft:breeze" -> BREEZE_AP;
            case "minecraft:creaking" -> CREAKING_AP;
            case "minecraft:warden" -> WARDEN_AP;
            case "minecraft:evoker" -> EVOKER_AP;
            case "minecraft:pillager" -> PILLAGER_AP;
            case "minecraft:ravager" -> RAVAGER_AP;
            case "minecraft:vex" -> VEX_AP;
            case "minecraft:vindicator" -> VINDICATOR_AP;
            case "minecraft:hoglin" -> HOGLIN_AP;
            case "minecraft:zoglin" -> ZOGLIN_AP;
            case "minecraft:parched" -> PARCHED_AP;
            case "minecraft:phantom" -> PHANTOM_AP;
            case "minecraft:ender_dragon" -> ENDER_DRAGON_AP;
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
            case "minecraft:iron_golem" -> IRON_GOLEM_AR;
            case "minecraft:drowned" -> DROWNED_AR;
            case "minecraft:zombified_piglin" -> ZOMBIFIED_PIGLIN_AR;
            case "minecraft:piglin" -> PIGLIN_AR;
            case "minecraft:piglin_brute" -> PIGLIN_BRUTE_AR;
            case "minecraft:breeze" -> BREEZE_AR;
            case "minecraft:creaking" -> CREAKING_AR;
            case "minecraft:warden" -> WARDEN_AR;
            case "minecraft:evoker" -> EVOKER_AR;
            case "minecraft:pillager" -> PILLAGER_AR;
            case "minecraft:ravager" -> RAVAGER_AR;
            case "minecraft:vex" -> VEX_AR;
            case "minecraft:vindicator" -> VINDICATOR_AR;
            case "minecraft:hoglin" -> HOGLIN_AR;
            case "minecraft:zoglin" -> ZOGLIN_AR;
            case "minecraft:parched" -> PARCHED_AR;
            case "minecraft:phantom" -> PHANTOM_AR;
            case "minecraft:ender_dragon" -> ENDER_DRAGON_AR;
            default -> 0;
        };

        if (entity instanceof LivingEntity livingEntity) {
            int resistanceAmplifier = 0;

            for (PotionEffect effect : livingEntity.getActivePotionEffects()) {
                if (effect.getType() == PotionEffectType.RESISTANCE) {
                    resistanceAmplifier = Math.max(resistanceAmplifier, effect.getAmplifier() + 1);
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
            case "minecraft:iron_golem" -> IRON_GOLEM_MR;
            case "minecraft:drowned" -> DROWNED_MR;
            case "minecraft:zombified_piglin" -> ZOMBIFIED_PIGLIN_MR;
            case "minecraft:piglin" -> PIGLIN_MR;
            case "minecraft:piglin_brute" -> PIGLIN_BRUTE_MR;
            case "minecraft:breeze" -> BREEZE_MR;
            case "minecraft:creaking" -> CREAKING_MR;
            case "minecraft:warden" -> WARDEN_MR;
            case "minecraft:evoker" -> EVOKER_MR;
            case "minecraft:pillager" -> PILLAGER_MR;
            case "minecraft:ravager" -> RAVAGER_MR;
            case "minecraft:vex" -> VEX_MR;
            case "minecraft:vindicator" -> VINDICATOR_MR;
            case "minecraft:hoglin" -> HOGLIN_MR;
            case "minecraft:zoglin" -> ZOGLIN_MR;
            case "minecraft:parched" -> PARCHED_MR;
            case "minecraft:phantom" -> PHANTOM_MR;
            case "minecraft:ender_dragon" -> ENDER_DRAGON_MR;
            default -> 0;
        };

        if (entity instanceof LivingEntity livingEntity) {
            int resistanceAmplifier = 0;

            for (PotionEffect effect : livingEntity.getActivePotionEffects()) {
                if (effect.getType() == PotionEffectType.RESISTANCE) {
                    resistanceAmplifier = Math.max(resistanceAmplifier, effect.getAmplifier() + 1);
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
            case "minecraft:iron_golem" -> IRON_GOLEM_TYPE;
            case "minecraft:drowned" -> DROWNED_TYPE;
            case "minecraft:zombified_piglin" -> ZOMBIFIED_PIGLIN_TYPE;
            case "minecraft:piglin" -> PIGLIN_TYPE;
            case "minecraft:piglin_brute" -> PIGLIN_BRUTE_TYPE;
            case "minecraft:breeze" -> BREEZE_TYPE;
            case "minecraft:creaking" -> CREAKING_TYPE;
            case "minecraft:warden" -> WARDEN_TYPE;
            case "minecraft:evoker" -> EVOKER_TYPE;
            case "minecraft:pillager" -> PILLAGER_TYPE;
            case "minecraft:ravager" -> RAVAGER_TYPE;
            case "minecraft:vex" -> VEX_TYPE;
            case "minecraft:vindicator" -> VINDICATOR_TYPE;
            case "minecraft:hoglin" -> HOGLIN_TYPE;
            case "minecraft:zoglin" -> ZOGLIN_TYPE;
            case "minecraft:parched" -> PARCHED_TYPE;
            case "minecraft:phantom" -> PHANTOM_TYPE;
            case "minecraft:ender_dragon" -> ENDER_DRAGON_TYPE;
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
                entityType.equals("minecraft:wither_skeleton") ||
                entityType.equals("minecraft:iron_golem") ||
                entityType.equals("minecraft:drowned") ||
                entityType.equals("minecraft:zombified_piglin") ||
                entityType.equals("minecraft:piglin") ||
                entityType.equals("minecraft:piglin_brute") ||
                entityType.equals("minecraft:breeze") ||
                entityType.equals("minecraft:creaking") ||
                entityType.equals("minecraft:warden") ||
                entityType.equals("minecraft:evoker") ||
                entityType.equals("minecraft:pillager") ||
                entityType.equals("minecraft:ravager") ||
                entityType.equals("minecraft:vex") ||
                entityType.equals("minecraft:vindicator") ||
                entityType.equals("minecraft:hoglin") ||
                entityType.equals("minecraft:zoglin") ||
                entityType.equals("minecraft:parched") ||
                entityType.equals("minecraft:phantom") ||
                entityType.equals("minecraft:ender_dragon");

    }
}