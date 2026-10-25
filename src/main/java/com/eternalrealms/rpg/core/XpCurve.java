package com.eternalrealms.rpg.core;

/** Curvas de experiência (nível geral e habilidades). Totalmente configuráveis. */
public final class XpCurve {
    private XpCurve() {}

    /** XP necessário para ir do nível {@code level} para {@code level + 1}. */
    public static long needed(int level) {
        double v = Settings.xpBase * Math.pow(Math.max(1, level), Settings.xpExponent);
        return Math.max(10L, Math.round(v));
    }

    /** XP de habilidade necessário para ir do nível {@code level} para {@code level + 1}. */
    public static long skillNeeded(int level) {
        double v = Settings.skillXpBase * Math.pow(level + 1, Settings.skillXpExponent);
        return Math.max(10L, Math.round(v));
    }

    /** XP total acumulado para alcançar o nível {@code level} partindo do nível 1. */
    public static long cumulative(int level) {
        long sum = 0;
        for (int l = 1; l < level; l++) sum += needed(l);
        return sum;
    }
}
