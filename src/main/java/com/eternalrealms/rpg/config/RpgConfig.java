package com.eternalrealms.rpg.config;

import com.eternalrealms.rpg.core.BlockXpTable;
import com.eternalrealms.rpg.core.Settings;
import com.eternalrealms.rpg.core.XpSource;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Configuração do mod (COMMON = servidor/mundo; CLIENT = interface). */
public final class RpgConfig {
    private RpgConfig() {}

    public static final ForgeConfigSpec COMMON;
    public static final ForgeConfigSpec CLIENT;

    private static final ForgeConfigSpec.BooleanValue ENABLED;
    private static final ForgeConfigSpec.IntValue MAX_LEVEL;
    private static final ForgeConfigSpec.DoubleValue XP_BASE;
    private static final ForgeConfigSpec.DoubleValue XP_EXP;
    private static final ForgeConfigSpec.DoubleValue XP_MULT;
    private static final ForgeConfigSpec.IntValue ATTR_PER_LEVEL;
    private static final ForgeConfigSpec.IntValue SKILL_PER_LEVEL;
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> MILESTONES;
    private static final ForgeConfigSpec.IntValue MILESTONE_ATTR;
    private static final ForgeConfigSpec.IntValue MILESTONE_SKILL;
    private static final ForgeConfigSpec.IntValue ATTR_CAP;
    private static final ForgeConfigSpec.IntValue SKILL_MAX;
    private static final ForgeConfigSpec.DoubleValue SKILL_BASE;
    private static final ForgeConfigSpec.DoubleValue SKILL_EXP;
    private static final ForgeConfigSpec.DoubleValue SKILL_RATIO;
    private static final ForgeConfigSpec.IntValue SKILL_MS_INTERVAL;
    private static final ForgeConfigSpec.IntValue SKILL_MS_POINTS;
    private static final ForgeConfigSpec.IntValue WINDOW;
    private static final ForgeConfigSpec.DoubleValue SOFT;
    private static final ForgeConfigSpec.DoubleValue BUDGET_MULT;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BUDGET_OVERRIDES;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISABLED_SOURCES;
    private static final ForgeConfigSpec.IntValue SHARE_RADIUS;
    private static final ForgeConfigSpec.DoubleValue MIN_SHARE;
    private static final ForgeConfigSpec.IntValue LEVEL_GRACE;
    private static final ForgeConfigSpec.DoubleValue LEVEL_FALLOFF;
    private static final ForgeConfigSpec.DoubleValue MIN_LEVEL_FACTOR;
    private static final ForgeConfigSpec.IntValue MAX_TRACKED;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLOCK_OVERRIDES;

    public static final ForgeConfigSpec.BooleanValue CL_SHOW_TOASTS;
    public static final ForgeConfigSpec.BooleanValue CL_GROUP_TOASTS;
    public static final ForgeConfigSpec.BooleanValue CL_SHOW_VALUES;
    public static final ForgeConfigSpec.BooleanValue CL_SHOW_BAR;
    public static final ForgeConfigSpec.BooleanValue CL_SHOW_LEVELUPS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("general");
        ENABLED = b.comment("Chave geral. Se false, nenhuma XP de RPG e concedida.").define("enabled", true);
        b.pop();

        b.push("character");
        MAX_LEVEL = b.comment("Nivel maximo do personagem.").defineInRange("maxLevel", 500, 1, 5000);
        XP_BASE = b.comment("XP para o proximo nivel = xpBase * nivel ^ xpExponent.")
                .defineInRange("xpBase", 20.0, 1.0, 100000.0);
        XP_EXP = b.defineInRange("xpExponent", 1.25, 0.5, 4.0);
        XP_MULT = b.comment("Multiplicador global de toda a XP (velocidade de progressao).")
                .defineInRange("xpMultiplier", 1.0, 0.0, 1000.0);
        ATTR_PER_LEVEL = b.defineInRange("attributePointsPerLevel", 3, 0, 100);
        SKILL_PER_LEVEL = b.defineInRange("skillPointsPerLevel", 1, 0, 100);
        MILESTONES = b.comment("Niveis com recompensa extra de pontos.")
                .defineList("milestoneLevels", Arrays.asList(10, 25, 50, 75, 100, 150, 200, 300, 400, 500),
                        o -> o instanceof Integer);
        MILESTONE_ATTR = b.defineInRange("milestoneAttributePoints", 5, 0, 1000);
        MILESTONE_SKILL = b.defineInRange("milestoneSkillPoints", 2, 0, 1000);
        b.pop();

        b.push("attributes");
        ATTR_CAP = b.comment("Maximo de pontos investidos em cada atributo principal (100 a 500).")
                .defineInRange("attributeCap", 100, 1, 500);
        b.pop();

        b.push("secondarySkills");
        SKILL_MAX = b.defineInRange("skillMaxLevel", 100, 1, 1000);
        SKILL_BASE = b.comment("XP de habilidade para o proximo nivel = skillXpBase * (nivel+1) ^ skillXpExponent.")
                .defineInRange("skillXpBase", 15.0, 1.0, 100000.0);
        SKILL_EXP = b.defineInRange("skillXpExponent", 1.15, 0.5, 4.0);
        SKILL_RATIO = b.comment("XP de habilidade concedida por unidade de XP de personagem.")
                .defineInRange("skillXpRatio", 1.0, 0.0, 100.0);
        SKILL_MS_INTERVAL = b.comment("A cada N niveis de habilidade, concede pontos de habilidade (0 desativa).")
                .defineInRange("milestoneInterval", 25, 0, 1000);
        SKILL_MS_POINTS = b.defineInRange("milestonePoints", 1, 0, 100);
        b.pop();

        b.push("antiExploit");
        WINDOW = b.comment("Janela (ticks) dos tetos de XP por fonte. 1200 = 1 minuto.")
                .defineInRange("budgetWindowTicks", 1200, 20, 1728000);
        SOFT = b.comment("Fracao do teto a partir da qual o XP decai linearmente ate zero.")
                .defineInRange("budgetSoftThreshold", 0.5, 0.0, 1.0);
        BUDGET_MULT = b.comment("Multiplicador de todos os tetos.").defineInRange("budgetMultiplier", 1.0, 0.01, 1000.0);
        BUDGET_OVERRIDES = b.comment("Tetos por fonte (XP-base por janela; 0 = ilimitado). Formato FONTE=valor. Fontes: "
                + Arrays.toString(XpSource.values()))
                .defineList("budgetOverrides", Collections.<String>emptyList(), o -> o instanceof String);
        DISABLED_SOURCES = b.comment("Fontes de XP desativadas individualmente.")
                .defineList("disabledSources", Collections.<String>emptyList(), o -> o instanceof String);
        MAX_TRACKED = b.comment("Maximo de posicoes de blocos colocados rastreadas por dimensao.")
                .defineInRange("maxTrackedBlocksPerDimension", 200000, 1000, 5000000);
        b.pop();

        b.push("combat");
        SHARE_RADIUS = b.comment("Raio (blocos) em que jogadores dividem a XP de um abate.")
                .defineInRange("killShareRadius", 48, 1, 512);
        MIN_SHARE = b.comment("Participacao minima no dano para receber XP de abate.")
                .defineInRange("killMinShare", 0.05, 0.0, 1.0);
        LEVEL_GRACE = b.defineInRange("levelGrace", 5, 0, 500);
        LEVEL_FALLOFF = b.comment("Niveis de diferenca ate o XP cair ao minimo.")
                .defineInRange("levelFalloff", 40.0, 1.0, 1000.0);
        MIN_LEVEL_FACTOR = b.defineInRange("minLevelFactor", 0.1, 0.0, 1.0);
        b.pop();

        b.push("blocks");
        BLOCK_OVERRIDES = b.comment("XP por bloco. Formato modid:bloco=xp ou modid:bloco=xp;FONTE;HABILIDADE "
                + "(ex.: examplemod:ruby_ore=30;BLOCK_ORE;mining). xp=0 desativa o bloco.")
                .defineList("blockXp", Collections.<String>emptyList(), o -> o instanceof String);
        b.pop();

        COMMON = b.build();

        ForgeConfigSpec.Builder c = new ForgeConfigSpec.Builder();
        c.push("notifications");
        CL_SHOW_TOASTS = c.comment("Mostrar notificacoes de XP.").define("showXpToasts", true);
        CL_GROUP_TOASTS = c.comment("Agrupar ganhos de XP em uma unica notificacao.").define("groupXpToasts", true);
        CL_SHOW_VALUES = c.comment("Mostrar os valores numericos de XP.").define("showXpValues", true);
        CL_SHOW_LEVELUPS = c.comment("Mostrar avisos de subida de nivel.").define("showLevelUps", true);
        CL_SHOW_BAR = c.comment("Mostrar barra de XP compacta na HUD.").define("showHudBar", true);
        c.pop();
        CLIENT = c.build();
    }

    public static void onConfigEvent(ModConfig.ModConfigEvent event) {
        if (event.getConfig().getSpec() == COMMON) bake();
    }

    private static void bake() {
        Settings.enabled = ENABLED.get();
        Settings.maxLevel = MAX_LEVEL.get();
        Settings.xpBase = XP_BASE.get();
        Settings.xpExponent = XP_EXP.get();
        Settings.xpMultiplier = XP_MULT.get();
        Settings.attrPointsPerLevel = ATTR_PER_LEVEL.get();
        Settings.skillPointsPerLevel = SKILL_PER_LEVEL.get();
        List<? extends Integer> ms = MILESTONES.get();
        int[] arr = new int[ms.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = ms.get(i);
        Settings.milestoneLevels = arr;
        Settings.milestoneAttrPoints = MILESTONE_ATTR.get();
        Settings.milestoneSkillPoints = MILESTONE_SKILL.get();
        Settings.attrCap = ATTR_CAP.get();
        Settings.skillMaxLevel = SKILL_MAX.get();
        Settings.skillXpBase = SKILL_BASE.get();
        Settings.skillXpExponent = SKILL_EXP.get();
        Settings.skillXpRatio = SKILL_RATIO.get();
        Settings.skillMilestoneInterval = SKILL_MS_INTERVAL.get();
        Settings.skillMilestonePoints = SKILL_MS_POINTS.get();
        Settings.budgetWindowTicks = WINDOW.get();
        Settings.budgetSoft = SOFT.get();
        Settings.budgetMultiplier = BUDGET_MULT.get();
        Settings.shareRadius = SHARE_RADIUS.get();
        Settings.minShare = MIN_SHARE.get();
        Settings.levelGrace = LEVEL_GRACE.get();
        Settings.levelFalloff = LEVEL_FALLOFF.get();
        Settings.minLevelFactor = MIN_LEVEL_FACTOR.get();
        Settings.maxTracked = MAX_TRACKED.get();

        for (XpSource s : XpSource.values()) {
            Settings.budgetCap[s.ordinal()] = s.defaultCap;
            Settings.sourceDisabled[s.ordinal()] = false;
        }
        for (String line : BUDGET_OVERRIDES.get()) {
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            XpSource s = XpSource.byName(line.substring(0, eq).trim());
            if (s == null) continue;
            try {
                Settings.budgetCap[s.ordinal()] = Double.parseDouble(line.substring(eq + 1).trim());
            } catch (NumberFormatException ignored) {
                // linha invalida: ignora
            }
        }
        for (String name : DISABLED_SOURCES.get()) {
            XpSource s = XpSource.byName(name.trim());
            if (s != null) Settings.sourceDisabled[s.ordinal()] = true;
        }
        Map<String, BlockXpTable.Entry> ov = new HashMap<String, BlockXpTable.Entry>();
        for (String line : BLOCK_OVERRIDES.get()) {
            Object[] p = BlockXpTable.parseOverride(line);
            if (p != null) ov.put((String) p[0], (BlockXpTable.Entry) p[1]);
        }
        Settings.blockOverrides = ov;
    }
}
