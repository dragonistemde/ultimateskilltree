package com.eternalrealms.rpg.core;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Pequenos rastreadores em memória (por jogador) usados contra repetição. */
public final class Trackers {
    private Trackers() {}

    /** Retorno decrescente para receitas/itens repetidos numa janela de tempo. */
    public static final class Repeat {
        private final Map<String, long[]> state = new HashMap<String, long[]>(); // {windowStart, count}
        private final long window;

        public Repeat(long windowTicks) {
            this.window = windowTicks;
        }

        /** Fator 1.0 na primeira vez, decaindo a cada repetição na janela (mínimo 0.1). */
        public double factor(UUID player, String key, long now) {
            String k = player + "|" + key;
            long[] s = state.get(k);
            if (s == null || now < s[0] || now - s[0] > window) {
                s = new long[] {now, 0};
                state.put(k, s);
            }
            double f = 1.0 / (1.0 + 0.25 * s[1]);
            s[1]++;
            return Math.max(0.1, f);
        }

        public void prune(long now) {
            Iterator<Map.Entry<String, long[]>> it = state.entrySet().iterator();
            while (it.hasNext()) {
                if (now - it.next().getValue()[0] > window) it.remove();
            }
        }

        public void forget(UUID player) {
            String p = player + "|";
            Iterator<String> it = state.keySet().iterator();
            while (it.hasNext()) if (it.next().startsWith(p)) it.remove();
        }
    }

    /** Mede variedade dos últimos blocos colocados: repetir o mesmo bloco rende menos. */
    public static final class Variety {
        private final Map<UUID, ArrayDeque<String>> recent = new HashMap<UUID, ArrayDeque<String>>();
        private static final int SIZE = 10;

        /** Fator entre 0.3 (sempre o mesmo bloco) e 1.0 (todos diferentes). */
        public double factor(UUID player, String blockId) {
            ArrayDeque<String> q = recent.get(player);
            if (q == null) {
                q = new ArrayDeque<String>();
                recent.put(player, q);
            }
            q.addLast(blockId);
            while (q.size() > SIZE) q.removeFirst();
            HashSet<String> distinct = new HashSet<String>(q);
            return 0.3 + 0.7 * (distinct.size() - 1) / (double) Math.max(1, SIZE - 1);
        }

        public void forget(UUID player) {
            recent.remove(player);
        }
    }
}
