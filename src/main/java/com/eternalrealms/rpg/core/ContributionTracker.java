package com.eternalrealms.rpg.core;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Registra o dano de cada jogador a cada alvo, para distribuir XP de abate em multiplayer. */
public final class ContributionTracker {

    public static final class Entry {
        final Map<UUID, EnumMap<Skill, Double>> perPlayer = new HashMap<UUID, EnumMap<Skill, Double>>();
        long lastTick;

        public double total(UUID p) {
            EnumMap<Skill, Double> m = perPlayer.get(p);
            double t = 0;
            if (m != null) for (double v : m.values()) t += v;
            return t;
        }

        public double grandTotal() {
            double t = 0;
            for (UUID p : perPlayer.keySet()) t += total(p);
            return t;
        }

        /** Habilidade que mais causou dano deste jogador. */
        public Skill dominantSkill(UUID p) {
            EnumMap<Skill, Double> m = perPlayer.get(p);
            Skill best = null;
            double bv = -1;
            if (m != null) {
                for (Map.Entry<Skill, Double> e : m.entrySet()) {
                    if (e.getValue() > bv) {
                        bv = e.getValue();
                        best = e.getKey();
                    }
                }
            }
            return best;
        }
    }

    private final Map<UUID, Entry> targets = new HashMap<UUID, Entry>();

    public void record(UUID target, UUID player, Skill skill, double dmg, long now) {
        if (dmg <= 0) return;
        Entry e = targets.get(target);
        if (e == null) {
            e = new Entry();
            targets.put(target, e);
        }
        e.lastTick = now;
        EnumMap<Skill, Double> m = e.perPlayer.get(player);
        if (m == null) {
            m = new EnumMap<Skill, Double>(Skill.class);
            e.perPlayer.put(player, m);
        }
        Double cur = m.get(skill);
        m.put(skill, (cur == null ? 0 : cur) + dmg);
    }

    /** Remove e devolve o registro do alvo. */
    public Entry take(UUID target) {
        return targets.remove(target);
    }

    public void prune(long now, long maxAgeTicks) {
        Iterator<Map.Entry<UUID, Entry>> it = targets.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().lastTick > maxAgeTicks) it.remove();
        }
    }

    public int size() {
        return targets.size();
    }

    /**
     * Participações (somam 1.0) apenas entre jogadores elegíveis e com participação mínima.
     * Quem causou menos que {@code Settings.minShare} do total elegível não recebe.
     */
    public static Map<UUID, Double> computeShares(Entry e, Set<UUID> eligible) {
        Map<UUID, Double> out = new HashMap<UUID, Double>();
        if (e == null) return out;
        double total = 0;
        for (UUID p : e.perPlayer.keySet()) if (eligible.contains(p)) total += e.total(p);
        if (total <= 0) return out;
        double kept = 0;
        for (UUID p : e.perPlayer.keySet()) {
            if (!eligible.contains(p)) continue;
            double share = e.total(p) / total;
            if (share >= Settings.minShare) {
                out.put(p, share);
                kept += share;
            }
        }
        if (kept <= 0) return new HashMap<UUID, Double>();
        for (Map.Entry<UUID, Double> en : out.entrySet()) en.setValue(en.getValue() / kept);
        return out;
    }
}
