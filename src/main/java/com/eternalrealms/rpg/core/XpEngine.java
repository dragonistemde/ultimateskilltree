package com.eternalrealms.rpg.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Motor central de experiência. Recebe eventos de atividade, aplica ativação por fonte,
 * tetos antiexploração e multiplicadores, e atualiza nível geral e habilidade.
 * Executa somente no servidor (thread principal).
 */
public final class XpEngine {
    private XpEngine() {}

    private static final Map<UUID, PlayerBudget> BUDGETS = new HashMap<UUID, PlayerBudget>();

    /** Concede XP de uma atividade. {@code baseXp} é o valor-base antes de multiplicadores. */
    public static XpResult award(PlayerRpgData d, UUID id, XpSource src, Skill skill, double baseXp, long now) {
        XpResult r = new XpResult(src, skill);
        if (!Settings.enabled || baseXp <= 0 || Settings.sourceDisabled[src.ordinal()]) return r;
        PlayerBudget b = BUDGETS.get(id);
        if (b == null) {
            b = new PlayerBudget();
            BUDGETS.put(id, b);
        }
        double granted = b.apply(src, baseXp, now);
        if (granted <= 0.0001) return r;
        double mult = Settings.xpMultiplier;
        r.charGain = granted * mult;
        r.levels = d.addCharXp(r.charGain);
        if (skill != null) {
            r.skillGain = granted * mult * Settings.skillXpRatio;
            r.skillLevels = d.addSkillXp(skill, r.skillGain);
        }
        return r;
    }

    /** Concessão direta (comandos/administração): sem tetos nem multiplicadores. */
    public static XpResult awardDirect(PlayerRpgData d, Skill skill, double charXp, double skillXp) {
        XpResult r = new XpResult(XpSource.COMMAND, skill);
        if (charXp > 0) {
            r.charGain = charXp;
            r.levels = d.addCharXp(charXp);
        }
        if (skill != null && skillXp > 0) {
            r.skillGain = skillXp;
            r.skillLevels = d.addSkillXp(skill, skillXp);
        }
        return r;
    }

    public static void forget(UUID id) {
        BUDGETS.remove(id);
    }
}
