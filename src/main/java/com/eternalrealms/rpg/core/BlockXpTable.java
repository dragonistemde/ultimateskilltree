package com.eternalrealms.rpg.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Classificação de blocos quebrados em fonte/habilidade/XP-base. Configurável por sobrescritas. */
public final class BlockXpTable {
    private BlockXpTable() {}

    public static final class Entry {
        public final XpSource source;
        public final Skill skill;
        public final double xp;

        public Entry(XpSource source, Skill skill, double xp) {
            this.source = source;
            this.skill = skill;
            this.xp = xp;
        }
    }

    private static final Set<String> STONE = new HashSet<String>(Arrays.asList(
            "stone", "cobblestone", "mossy_cobblestone", "granite", "diorite", "andesite",
            "netherrack", "basalt", "blackstone", "end_stone", "sandstone", "red_sandstone",
            "terracotta", "magma_block"));
    private static final Set<String> SOFT = new HashSet<String>(Arrays.asList(
            "dirt", "coarse_dirt", "grass_block", "podzol", "mycelium", "sand", "red_sand",
            "gravel", "clay", "soul_sand", "soul_soil", "snow_block"));
    private static final Set<String> CROPS = new HashSet<String>(Arrays.asList(
            "wheat", "carrots", "potatoes", "beetroots", "nether_wart", "melon", "pumpkin",
            "sugar_cane", "cactus", "cocoa"));

    /** @param id id completo do bloco, p.ex. "minecraft:diamond_ore". Retorna null se não rende XP. */
    public static Entry classify(String id) {
        Entry o = Settings.blockOverrides.get(id);
        if (o != null) return o.xp > 0 ? o : null;
        int c = id.indexOf(':');
        String p = c >= 0 ? id.substring(c + 1) : id;

        if (p.equals("ancient_debris")) return new Entry(XpSource.BLOCK_ORE, Skill.MINING, 70);
        if (p.equals("obsidian")) return new Entry(XpSource.BLOCK_ORE, Skill.MINING, 25);
        if (p.equals("crying_obsidian")) return new Entry(XpSource.BLOCK_ORE, Skill.MINING, 30);
        if (p.endsWith("_ore")) {
            double xp = 12;
            if (p.contains("coal")) xp = 6;
            else if (p.contains("copper")) xp = 8;
            else if (p.contains("nether_gold")) xp = 8;
            else if (p.contains("iron")) xp = 10;
            else if (p.contains("redstone")) xp = 10;
            else if (p.contains("quartz")) xp = 10;
            else if (p.contains("lapis")) xp = 14;
            else if (p.contains("gold")) xp = 14;
            else if (p.contains("diamond")) xp = 40;
            else if (p.contains("emerald")) xp = 45;
            return new Entry(XpSource.BLOCK_ORE, Skill.MINING, xp);
        }
        if (STONE.contains(p) || p.endsWith("terracotta")) return new Entry(XpSource.BLOCK_STONE, Skill.MINING, 0.6);
        if (SOFT.contains(p)) return new Entry(XpSource.BLOCK_SOFT, Skill.MINING, 0.25);
        if (p.endsWith("_log") || p.endsWith("_wood") || p.equals("crimson_stem") || p.equals("warped_stem")) {
            return new Entry(XpSource.LOG, Skill.GATHERING, 1.5);
        }
        if (CROPS.contains(p)) {
            double xp = (p.equals("melon") || p.equals("pumpkin") || p.equals("nether_wart")) ? 1.5 : 1.2;
            return new Entry(XpSource.CROP_HARVEST, Skill.FARMING, xp);
        }
        return null;
    }

    /**
     * Formato: {@code modid:bloco=xp} ou {@code modid:bloco=xp;FONTE;HABILIDADE}.
     * Retorna null se inválido.
     */
    public static Object[] parseOverride(String line) {
        try {
            int eq = line.indexOf('=');
            if (eq < 0) return null;
            String id = line.substring(0, eq).trim();
            String[] parts = line.substring(eq + 1).split(";");
            double xp = Double.parseDouble(parts[0].trim());
            XpSource src = parts.length > 1 ? XpSource.byName(parts[1].trim()) : XpSource.BLOCK_ORE;
            Skill sk = parts.length > 2 ? Skill.byId(parts[2].trim()) : Skill.MINING;
            if (src == null || sk == null || id.isEmpty()) return null;
            return new Object[] {id, new Entry(src, sk, xp)};
        } catch (RuntimeException e) {
            return null;
        }
    }
}
