package com.eternalrealms.rpg.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Antiexploração de blocos:
 *  - "placed": posições onde um jogador colocou um bloco (quebrá-las não rende XP de mineração);
 *  - "credited": posições que já renderam XP de construção (evita colocar/quebrar em ciclo).
 * Tamanho limitado por dimensão (descarta as mais antigas).
 */
public final class PlacementTracker {
    private final Map<String, LinkedHashSet<Long>> placed = new HashMap<String, LinkedHashSet<Long>>();
    private final Map<String, LinkedHashSet<Long>> credited = new HashMap<String, LinkedHashSet<Long>>();

    public void markPlaced(String dim, long pos) {
        add(placed, dim, pos);
    }

    /** Retorna true (e esquece a posição) se o bloco quebrado havia sido colocado por jogador. */
    public boolean consumePlaced(String dim, long pos) {
        LinkedHashSet<Long> s = placed.get(dim);
        return s != null && s.remove(pos);
    }

    /** Retorna true apenas na primeira vez que a posição rende XP de construção. */
    public boolean tryCredit(String dim, long pos) {
        return add(credited, dim, pos);
    }

    private static boolean add(Map<String, LinkedHashSet<Long>> m, String dim, long pos) {
        LinkedHashSet<Long> s = m.get(dim);
        if (s == null) {
            s = new LinkedHashSet<Long>();
            m.put(dim, s);
        }
        boolean added = s.add(pos);
        if (added && s.size() > Settings.maxTracked) {
            Iterator<Long> it = s.iterator();
            it.next();
            it.remove();
        }
        return added;
    }

    public Map<String, long[]> exportPlaced() {
        return export(placed);
    }

    public Map<String, long[]> exportCredited() {
        return export(credited);
    }

    public void importPlaced(String dim, long[] v) {
        load(placed, dim, v);
    }

    public void importCredited(String dim, long[] v) {
        load(credited, dim, v);
    }

    private static Map<String, long[]> export(Map<String, LinkedHashSet<Long>> m) {
        Map<String, long[]> out = new HashMap<String, long[]>();
        for (Map.Entry<String, LinkedHashSet<Long>> e : m.entrySet()) {
            long[] a = new long[e.getValue().size()];
            int i = 0;
            for (Long l : e.getValue()) a[i++] = l;
            out.put(e.getKey(), a);
        }
        return out;
    }

    private static void load(Map<String, LinkedHashSet<Long>> m, String dim, long[] v) {
        LinkedHashSet<Long> s = new LinkedHashSet<Long>();
        for (long l : v) s.add(l);
        m.put(dim, s);
    }
}
