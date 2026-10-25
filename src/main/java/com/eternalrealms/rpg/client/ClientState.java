package com.eternalrealms.rpg.client;

import com.eternalrealms.rpg.config.RpgConfig;
import com.eternalrealms.rpg.core.PlayerRpgData;
import com.eternalrealms.rpg.core.Skill;
import com.eternalrealms.rpg.data.RpgNbt;
import com.eternalrealms.rpg.net.SyncMsg;

import java.util.ArrayList;
import java.util.List;

/**
 * Copia local (somente leitura) do estado enviado pelo servidor + fila de notificacoes.
 * Nao referencia classes exclusivas do cliente, entao e seguro carrega-la em qualquer lado.
 */
public final class ClientState {
    private ClientState() {}

    public static final int KIND_XP = 0;
    public static final int KIND_LEVEL = 1;
    public static final int KIND_SKILL = 2;
    private static final long GROUP_WINDOW_MS = 2500;

    public static final class Toast {
        public final int kind;
        public double charGain;
        public double skillGain;
        public Skill skill;
        public String srcKey = "";
        public int level;
        public long born;

        Toast(int kind, long born) {
            this.kind = kind;
            this.born = born;
        }
    }

    public static volatile PlayerRpgData data = new PlayerRpgData();
    /** Acessar sempre dentro de synchronized (TOASTS). */
    public static final List<Toast> TOASTS = new ArrayList<Toast>();

    public static void apply(SyncMsg m) {
        PlayerRpgData d = new PlayerRpgData();
        RpgNbt.read(m.data, d);
        data = d;
        long now = System.currentTimeMillis();
        Skill sk = m.skillOrdinal >= 0 && m.skillOrdinal < Skill.values().length ? Skill.values()[m.skillOrdinal] : null;
        synchronized (TOASTS) {
            if (RpgConfig.CL_SHOW_TOASTS.get() && m.charGain > 0) {
                Toast t = null;
                if (RpgConfig.CL_GROUP_TOASTS.get()) {
                    for (Toast o : TOASTS) {
                        if (o.kind == KIND_XP && now - o.born < GROUP_WINDOW_MS) t = o;
                    }
                }
                if (t == null) {
                    t = new Toast(KIND_XP, now);
                    TOASTS.add(t);
                }
                t.born = now;
                t.charGain += m.charGain;
                t.srcKey = m.srcKey;
                if (sk != null) {
                    if (t.skill == sk) {
                        t.skillGain += m.skillGain;
                    } else {
                        t.skill = sk;
                        t.skillGain = m.skillGain;
                    }
                }
            }
            if (RpgConfig.CL_SHOW_LEVELUPS.get()) {
                if (m.levels > 0) {
                    Toast t = new Toast(KIND_LEVEL, now);
                    t.level = d.level;
                    TOASTS.add(t);
                }
                if (m.skillLevels > 0 && sk != null) {
                    Toast t = new Toast(KIND_SKILL, now);
                    t.skill = sk;
                    t.level = d.skillLevels[sk.ordinal()];
                    TOASTS.add(t);
                }
            }
            while (TOASTS.size() > 6) TOASTS.remove(0);
        }
    }
}
