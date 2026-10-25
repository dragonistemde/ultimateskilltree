package com.eternalrealms.rpg.core;

/** Resultado de uma concessão de XP (também usado para agrupar notificações). */
public final class XpResult {
    public XpSource src;
    public Skill skill;
    public double charGain;
    public double skillGain;
    public int levels;
    public int skillLevels;

    public XpResult(XpSource src, Skill skill) {
        this.src = src;
        this.skill = skill;
    }

    public boolean isEmpty() {
        return charGain <= 0 && skillGain <= 0 && levels == 0 && skillLevels == 0;
    }

    public XpResult copy() {
        XpResult r = new XpResult(src, skill);
        r.charGain = charGain;
        r.skillGain = skillGain;
        r.levels = levels;
        r.skillLevels = skillLevels;
        return r;
    }

    /** Agrega outro resultado a este (soma ganhos; mantém a fonte/habilidade mais recentes). */
    public void merge(XpResult o) {
        charGain += o.charGain;
        levels += o.levels;
        skillLevels += o.skillLevels;
        src = o.src;
        if (o.skill != null) {
            if (o.skill == skill) {
                skillGain += o.skillGain;
            } else {
                skill = o.skill;
                skillGain = o.skillGain;
            }
        }
    }
}
