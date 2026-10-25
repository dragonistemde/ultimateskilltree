package com.eternalrealms.rpg.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Valores configuráveis em tempo de execução. Preenchidos a partir do arquivo de
 * configuração do Forge (RpgConfig) e lidos pelo núcleo, que não depende do Minecraft.
 */
public final class Settings {
    private Settings() {}

    public static boolean enabled = true;

    // Nível do personagem
    public static int maxLevel = 500;
    public static double xpBase = 20.0;
    public static double xpExponent = 1.25;
    public static double xpMultiplier = 1.0;
    public static int attrPointsPerLevel = 3;
    public static int skillPointsPerLevel = 1;
    public static int[] milestoneLevels = {10, 25, 50, 75, 100, 150, 200, 300, 400, 500};
    public static int milestoneAttrPoints = 5;
    public static int milestoneSkillPoints = 2;

    // Atributos
    public static int attrCap = 100;

    // Habilidades secundárias
    public static int skillMaxLevel = 100;
    public static double skillXpBase = 15.0;
    public static double skillXpExponent = 1.15;
    public static double skillXpRatio = 1.0;
    public static int skillMilestoneInterval = 25;
    public static int skillMilestonePoints = 1;

    // Antiexploração: tetos por janela
    public static int budgetWindowTicks = 1200;
    public static double budgetSoft = 0.5;
    public static double budgetMultiplier = 1.0;
    public static final double[] budgetCap = new double[XpSource.values().length];
    public static final boolean[] sourceDisabled = new boolean[XpSource.values().length];

    // Combate / multiplayer
    public static int shareRadius = 48;
    public static double minShare = 0.05;
    public static int levelGrace = 5;
    public static double levelFalloff = 40.0;
    public static double minLevelFactor = 0.1;
    public static int mobLevelCap = 500;

    // Rastreamento de blocos colocados
    public static int maxTracked = 200000;

    /** Sobrescritas de XP por bloco (id completo "modid:bloco"). */
    public static Map<String, BlockXpTable.Entry> blockOverrides = new HashMap<String, BlockXpTable.Entry>();

    static {
        resetDefaults();
    }

    public static void resetDefaults() {
        enabled = true;
        maxLevel = 500;
        xpBase = 20.0;
        xpExponent = 1.25;
        xpMultiplier = 1.0;
        attrPointsPerLevel = 3;
        skillPointsPerLevel = 1;
        milestoneLevels = new int[] {10, 25, 50, 75, 100, 150, 200, 300, 400, 500};
        milestoneAttrPoints = 5;
        milestoneSkillPoints = 2;
        attrCap = 100;
        skillMaxLevel = 100;
        skillXpBase = 15.0;
        skillXpExponent = 1.15;
        skillXpRatio = 1.0;
        skillMilestoneInterval = 25;
        skillMilestonePoints = 1;
        budgetWindowTicks = 1200;
        budgetSoft = 0.5;
        budgetMultiplier = 1.0;
        for (XpSource s : XpSource.values()) {
            budgetCap[s.ordinal()] = s.defaultCap;
            sourceDisabled[s.ordinal()] = false;
        }
        shareRadius = 48;
        minShare = 0.05;
        levelGrace = 5;
        levelFalloff = 40.0;
        minLevelFactor = 0.1;
        mobLevelCap = 500;
        maxTracked = 200000;
        blockOverrides = new HashMap<String, BlockXpTable.Entry>();
    }
}
