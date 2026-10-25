package com.eternalrealms.rpg.core;

/**
 * Fórmulas de XP de combate. Até o sistema de níveis de mobs (Fase 6) existir, o nível do
 * alvo é estimado a partir de vida e dano; depois basta passar o nível real.
 */
public final class MobXp {
    private MobXp() {}

    public static int estimateLevel(double maxHealth, double attackDamage) {
        double power = maxHealth + 3.0 * attackDamage;
        int lvl = 1 + (int) (power / 6.0);
        return Math.max(1, Math.min(Settings.mobLevelCap, lvl));
    }

    /**
     * Mobs muito abaixo do jogador rendem menos (até {@code minLevelFactor});
     * mobs acima rendem até 50% a mais.
     */
    public static double levelFactor(int playerLevel, int mobLevel) {
        int diff = playerLevel - mobLevel - Settings.levelGrace;
        if (diff > 0) return Math.max(Settings.minLevelFactor, 1.0 - diff / Settings.levelFalloff);
        int above = mobLevel - playerLevel;
        if (above > 0) return 1.0 + Math.min(0.5, above * 0.05);
        return 1.0;
    }

    public static double killXp(double maxHealth, boolean boss) {
        return (4.0 + maxHealth * 0.7) * (boss ? 8.0 : 1.0);
    }

    public static double damageXp(double effectiveDamage, boolean boss) {
        return effectiveDamage * 0.3 * (boss ? 2.0 : 1.0);
    }

    public static double takenXp(double amount) {
        return Math.min(amount, 20.0) * 0.4;
    }
}
