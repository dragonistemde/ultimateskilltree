package com.eternalrealms.rpg.core;

/** Habilidades secundárias (evoluem de forma independente do nível geral). */
public enum Skill {
    MINING("mining"),
    MELEE("melee"),
    ARCHERY("archery"),
    MAGIC("magic"),
    DEFENSE("defense"),
    SMITHING("smithing"),
    BUILDING("building"),
    FARMING("farming"),
    ALCHEMY("alchemy"),
    FISHING("fishing"),
    EXPLORATION("exploration"),
    SURVIVAL("survival"),
    WEAPON_MASTERY("weapon_mastery"),
    GATHERING("gathering"),
    SUMMONING("summoning");

    public final String id;

    Skill(String id) {
        this.id = id;
    }

    public static Skill byId(String id) {
        for (Skill s : values()) {
            if (s.id.equalsIgnoreCase(id) || s.name().equalsIgnoreCase(id)) return s;
        }
        return null;
    }
}
