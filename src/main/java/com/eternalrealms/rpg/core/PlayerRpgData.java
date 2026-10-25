package com.eternalrealms.rpg.core;

import java.util.HashSet;
import java.util.Set;

/** Estado de progressão de um jogador. Sem dependências do Minecraft. */
public final class PlayerRpgData {
    public int level = 1;
    public double xp = 0;
    public int attrPoints = 0;
    public int skillPoints = 0;
    public final int[] attrs = new int[AttrType.values().length];
    public final int[] skillLevels = new int[Skill.values().length];
    public final double[] skillXp = new double[Skill.values().length];
    public final Set<String> discovered = new HashSet<String>();

    public long xpToNext() {
        return level >= Settings.maxLevel ? 0L : XpCurve.needed(level);
    }

    public double progress() {
        long need = xpToNext();
        return need <= 0 ? 1.0 : Math.min(1.0, xp / need);
    }

    public long skillXpToNext(Skill s) {
        return skillLevels[s.ordinal()] >= Settings.skillMaxLevel ? 0L : XpCurve.skillNeeded(skillLevels[s.ordinal()]);
    }

    /** Adiciona XP de personagem. Retorna quantos níveis foram ganhos. */
    public int addCharXp(double amount) {
        if (amount <= 0 || level >= Settings.maxLevel) return 0;
        xp += amount;
        int gained = 0;
        while (level < Settings.maxLevel) {
            long need = XpCurve.needed(level);
            if (xp < need) break;
            xp -= need;
            level++;
            gained++;
            onLevelUp(level);
        }
        if (level >= Settings.maxLevel) xp = 0;
        return gained;
    }

    private void onLevelUp(int newLevel) {
        attrPoints += Settings.attrPointsPerLevel;
        skillPoints += Settings.skillPointsPerLevel;
        for (int m : Settings.milestoneLevels) {
            if (m == newLevel) {
                attrPoints += Settings.milestoneAttrPoints;
                skillPoints += Settings.milestoneSkillPoints;
            }
        }
    }

    /** Adiciona XP a uma habilidade secundária. Retorna quantos níveis foram ganhos. */
    public int addSkillXp(Skill s, double amount) {
        int i = s.ordinal();
        if (amount <= 0 || skillLevels[i] >= Settings.skillMaxLevel) return 0;
        skillXp[i] += amount;
        int gained = 0;
        while (skillLevels[i] < Settings.skillMaxLevel) {
            long need = XpCurve.skillNeeded(skillLevels[i]);
            if (skillXp[i] < need) break;
            skillXp[i] -= need;
            skillLevels[i]++;
            gained++;
            int interval = Settings.skillMilestoneInterval;
            if (interval > 0 && skillLevels[i] % interval == 0) skillPoints += Settings.skillMilestonePoints;
        }
        if (skillLevels[i] >= Settings.skillMaxLevel) skillXp[i] = 0;
        return gained;
    }

    /** Investe pontos em um atributo, respeitando saldo e limite. Retorna quantos foram aplicados. */
    public int allocate(AttrType t, int amount) {
        if (amount <= 0) return 0;
        int i = t.ordinal();
        int room = Settings.attrCap - attrs[i];
        int n = Math.min(amount, Math.min(attrPoints, room));
        if (n <= 0) return 0;
        attrs[i] += n;
        attrPoints -= n;
        return n;
    }

    public int attrTotal() {
        int sum = 0;
        for (int v : attrs) sum += v;
        return sum;
    }

    /** Ajusta valores fora dos limites (ex.: após reduzir limites na configuração). */
    public void clamp() {
        if (level < 1) level = 1;
        if (level > Settings.maxLevel) {
            level = Settings.maxLevel;
            xp = 0;
        }
        if (xp < 0) xp = 0;
        if (attrPoints < 0) attrPoints = 0;
        if (skillPoints < 0) skillPoints = 0;
        for (int i = 0; i < attrs.length; i++) {
            if (attrs[i] < 0) attrs[i] = 0;
            if (attrs[i] > Settings.attrCap) {
                attrPoints += attrs[i] - Settings.attrCap;
                attrs[i] = Settings.attrCap;
            }
        }
        for (int i = 0; i < skillLevels.length; i++) {
            if (skillLevels[i] < 0) skillLevels[i] = 0;
            if (skillLevels[i] > Settings.skillMaxLevel) {
                skillLevels[i] = Settings.skillMaxLevel;
                skillXp[i] = 0;
            }
            if (skillXp[i] < 0) skillXp[i] = 0;
        }
    }
}
