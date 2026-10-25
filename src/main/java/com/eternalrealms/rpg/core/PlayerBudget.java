package com.eternalrealms.rpg.core;

/**
 * Teto de XP por fonte e por janela de tempo, com retorno decrescente.
 * Até {@code budgetSoft} do teto o XP é integral; depois cai linearmente até zero.
 */
public final class PlayerBudget {
    private final double[] used = new double[XpSource.values().length];
    private final long[] windowStart = new long[XpSource.values().length];

    /** Retorna o XP-base efetivamente concedido (já com retorno decrescente) e o contabiliza. */
    public double apply(XpSource src, double amount, long now) {
        int i = src.ordinal();
        double cap = Settings.budgetCap[i] * Settings.budgetMultiplier;
        if (cap <= 0) return amount; // ilimitado
        if (now < windowStart[i] || now - windowStart[i] >= Settings.budgetWindowTicks) {
            windowStart[i] = now;
            used[i] = 0;
        }
        double soft = cap * Settings.budgetSoft;
        double factor;
        if (used[i] <= soft) {
            factor = 1.0;
        } else if (used[i] >= cap) {
            factor = 0.0;
        } else {
            factor = 1.0 - (used[i] - soft) / (cap - soft);
        }
        double granted = Math.min(amount * factor, cap - used[i]);
        if (granted <= 0) return 0;
        used[i] += granted;
        return granted;
    }
}
