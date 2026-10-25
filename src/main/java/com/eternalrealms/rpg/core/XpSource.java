package com.eternalrealms.rpg.core;

/**
 * Fontes de experiência. Cada fonte tem um teto padrão de XP-base por janela de tempo
 * (0 = ilimitado). Os tetos são configuráveis.
 */
public enum XpSource {
    BLOCK_ORE("block_ore", 500),
    BLOCK_STONE("block_stone", 250),
    BLOCK_SOFT("block_soft", 120),
    LOG("log", 250),
    CROP_HARVEST("crop_harvest", 250),
    CROP_PLANT("crop_plant", 40),
    BUILD("build", 60),
    DAMAGE_DEALT("damage_dealt", 700),
    DAMAGE_TAKEN("damage_taken", 150),
    SHIELD_BLOCK("shield_block", 60),
    KILL("kill", 2500),
    BREED("breed", 150),
    CRAFT("craft", 400),
    SMELT("smelt", 200),
    REPAIR("repair", 100),
    EXPLORE_BIOME("explore_biome", 0),
    EXPLORE_DIM("explore_dim", 0),
    COMMAND("command", 0);

    public final String key;
    public final double defaultCap;

    XpSource(String key, double defaultCap) {
        this.key = key;
        this.defaultCap = defaultCap;
    }

    public static XpSource byName(String n) {
        for (XpSource s : values()) {
            if (s.name().equalsIgnoreCase(n) || s.key.equalsIgnoreCase(n)) return s;
        }
        return null;
    }
}
