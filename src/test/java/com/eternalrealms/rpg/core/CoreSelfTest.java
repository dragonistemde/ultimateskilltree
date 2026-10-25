package com.eternalrealms.rpg.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Autoteste do núcleo (sem JUnit/Minecraft). Executar: java com.eternalrealms.rpg.core.CoreSelfTest */
public final class CoreSelfTest {
    private static int failures = 0;
    private static int checks = 0;

    private static void check(boolean ok, String msg) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("FALHOU: " + msg);
        }
    }

    public static void main(String[] args) {
        curve();
        levelUpAndPoints();
        milestones();
        maxLevel();
        skillsIndependent();
        allocation();
        budgetAntiExploit();
        engineBudgetAndMultiplier();
        blockTable();
        placementTracker();
        contributions();
        repeatAndVariety();
        mobFormulas();
        clampAfterConfigChange();
        System.out.println(checks + " verificações, " + failures + " falhas");
        System.exit(failures == 0 ? 0 : 1);
    }

    static void curve() {
        Settings.resetDefaults();
        boolean mono = true;
        for (int l = 1; l < 499; l++) if (XpCurve.needed(l + 1) < XpCurve.needed(l)) mono = false;
        check(mono, "curva de XP monotônica");
        check(XpCurve.needed(1) >= 10 && XpCurve.needed(1) <= 40, "nível 1 não é lento: " + XpCurve.needed(1));
        check(XpCurve.needed(499) > XpCurve.needed(100), "níveis altos mais caros");
        System.out.println("  XP p/ lv1=" + XpCurve.needed(1) + " lv10=" + XpCurve.needed(10) + " lv100=" + XpCurve.needed(100)
                + " lv499=" + XpCurve.needed(499) + " acumulado até 100=" + XpCurve.cumulative(100));
    }

    static void levelUpAndPoints() {
        Settings.resetDefaults();
        PlayerRpgData d = new PlayerRpgData();
        int gained = d.addCharXp(XpCurve.needed(1) + 1);
        check(gained == 1 && d.level == 2, "sobe 1 nível");
        check(d.attrPoints == Settings.attrPointsPerLevel && d.skillPoints == Settings.skillPointsPerLevel, "pontos por nível");
        check(d.xp > 0 && d.xp < 5, "resto de XP preservado");
        PlayerRpgData e = new PlayerRpgData();
        int g2 = e.addCharXp(1_000_000);
        check(g2 > 20 && e.level == 1 + g2, "grande concessão sobe vários níveis");
    }

    static void milestones() {
        Settings.resetDefaults();
        PlayerRpgData d = new PlayerRpgData();
        d.addCharXp(XpCurve.cumulative(10) + 1); // alcança nível 10
        check(d.level == 10, "alcançou nível 10: " + d.level);
        int expectedAttr = 9 * Settings.attrPointsPerLevel + Settings.milestoneAttrPoints;
        int expectedSkill = 9 * Settings.skillPointsPerLevel + Settings.milestoneSkillPoints;
        check(d.attrPoints == expectedAttr, "bônus de marco (atributo): " + d.attrPoints + " vs " + expectedAttr);
        check(d.skillPoints == expectedSkill, "bônus de marco (habilidade): " + d.skillPoints + " vs " + expectedSkill);
    }

    static void maxLevel() {
        Settings.resetDefaults();
        Settings.maxLevel = 5;
        PlayerRpgData d = new PlayerRpgData();
        d.addCharXp(10_000_000);
        check(d.level == 5, "respeita nível máximo configurável");
        check(d.xp == 0 && d.xpToNext() == 0, "sem XP no nível máximo");
        check(d.addCharXp(100) == 0, "não sobe além do máximo");
        Settings.resetDefaults();
    }

    static void skillsIndependent() {
        Settings.resetDefaults();
        PlayerRpgData d = new PlayerRpgData();
        d.addSkillXp(Skill.MINING, XpCurve.skillNeeded(0) + 1);
        check(d.skillLevels[Skill.MINING.ordinal()] == 1, "Mineração sobe");
        check(d.skillLevels[Skill.ARCHERY.ordinal()] == 0, "Arquearia independente");
        check(d.level == 1, "nível geral independente de XP de habilidade");
        Settings.skillMaxLevel = 3;
        PlayerRpgData e = new PlayerRpgData();
        e.addSkillXp(Skill.SMITHING, 1_000_000);
        check(e.skillLevels[Skill.SMITHING.ordinal()] == 3, "limite máximo de habilidade configurável");
        Settings.resetDefaults();
        PlayerRpgData f = new PlayerRpgData();
        f.addSkillXp(Skill.FARMING, 100_000_000);
        check(f.skillLevels[Skill.FARMING.ordinal()] == 100, "habilidade chega a 100");
        check(f.skillPoints == 4 * Settings.skillMilestonePoints, "marcos de habilidade (25/50/75/100)");
    }

    static void allocation() {
        Settings.resetDefaults();
        PlayerRpgData d = new PlayerRpgData();
        d.attrPoints = 10;
        check(d.allocate(AttrType.STRENGTH, 4) == 4 && d.attrs[0] == 4 && d.attrPoints == 6, "aloca pontos");
        check(d.allocate(AttrType.STRENGTH, 99) == 6 && d.attrPoints == 0, "não gasta mais do que tem");
        check(d.allocate(AttrType.VITALITY, 1) == 0, "sem saldo, nada acontece");
        check(d.allocate(AttrType.VITALITY, -5) == 0, "quantidade negativa ignorada");
        Settings.attrCap = 5;
        PlayerRpgData e = new PlayerRpgData();
        e.attrPoints = 50;
        check(e.allocate(AttrType.LUCK, 50) == 5 && e.attrs[AttrType.LUCK.ordinal()] == 5, "respeita limite do atributo");
        Settings.resetDefaults();
    }

    static void budgetAntiExploit() {
        Settings.resetDefaults();
        PlayerBudget b = new PlayerBudget();
        double total = 0;
        for (int i = 0; i < 100000; i++) total += b.apply(XpSource.BLOCK_ORE, 10, 100);
        double cap = Settings.budgetCap[XpSource.BLOCK_ORE.ordinal()];
        check(total <= cap + 1e-6, "teto por janela respeitado: " + total + " <= " + cap);
        check(total > cap * 0.9, "teto quase alcançável: " + total);
        check(b.apply(XpSource.BLOCK_ORE, 10, 101) < 0.5, "janela ainda esgotada");
        check(b.apply(XpSource.BLOCK_ORE, 10, 100 + Settings.budgetWindowTicks) == 10, "nova janela restabelece");
        check(b.apply(XpSource.KILL, 10, 100) == 10, "tetos são independentes por fonte");
        check(b.apply(XpSource.EXPLORE_BIOME, 1000, 0) == 1000, "fonte ilimitada (cap 0)");
        double first = new PlayerBudget().apply(XpSource.BLOCK_ORE, 10, 0);
        check(first == 10, "XP integral no início");
    }

    static void engineBudgetAndMultiplier() {
        Settings.resetDefaults();
        PlayerRpgData d = new PlayerRpgData();
        UUID id = UUID.randomUUID();
        XpResult r = XpEngine.award(d, id, XpSource.BLOCK_ORE, Skill.MINING, 10, 0);
        check(r.charGain == 10 && r.skillGain == 10, "concede XP de personagem e habilidade");
        Settings.xpMultiplier = 2.0;
        XpResult r2 = XpEngine.award(d, id, XpSource.BLOCK_ORE, Skill.MINING, 10, 0);
        check(r2.charGain == 20, "multiplicador global aplicado");
        Settings.xpMultiplier = 1.0;
        Settings.sourceDisabled[XpSource.KILL.ordinal()] = true;
        check(XpEngine.award(d, id, XpSource.KILL, Skill.MELEE, 50, 0).isEmpty(), "fonte desativada não rende XP");
        Settings.enabled = false;
        check(XpEngine.award(d, id, XpSource.BLOCK_ORE, Skill.MINING, 10, 0).isEmpty(), "mod desativado não rende XP");
        Settings.resetDefaults();
        // grind de 1 minuto não passa muito do teto da fonte
        PlayerRpgData g = new PlayerRpgData();
        UUID gid = UUID.randomUUID();
        double sum = 0;
        for (int i = 0; i < 50000; i++) sum += XpEngine.award(g, gid, XpSource.KILL, Skill.MELEE, 20, 5).charGain;
        check(sum <= Settings.budgetCap[XpSource.KILL.ordinal()] + 1e-6, "grind de abates limitado: " + sum);
        XpResult dir = XpEngine.awardDirect(new PlayerRpgData(), Skill.MINING, 100, 50);
        check(dir.charGain == 100 && dir.skillGain == 50, "concessão direta");
        XpEngine.forget(id);
        XpEngine.forget(gid);
    }

    static void blockTable() {
        Settings.resetDefaults();
        check(BlockXpTable.classify("minecraft:diamond_ore").xp == 40, "diamante");
        check(BlockXpTable.classify("minecraft:nether_gold_ore").xp == 8, "ouro do Nether ≠ ouro");
        check(BlockXpTable.classify("minecraft:gold_ore").xp == 14, "ouro");
        check(BlockXpTable.classify("minecraft:coal_ore").xp < BlockXpTable.classify("minecraft:diamond_ore").xp, "raridade proporcional");
        check(BlockXpTable.classify("minecraft:ancient_debris").xp == 70, "ancient debris");
        check(BlockXpTable.classify("minecraft:obsidian").xp == 25, "obsidiana");
        check(BlockXpTable.classify("othermod:tin_ore").source == XpSource.BLOCK_ORE, "minério de outro mod (_ore)");
        check(BlockXpTable.classify("minecraft:stone").source == XpSource.BLOCK_STONE, "pedra");
        check(BlockXpTable.classify("minecraft:dirt").xp < BlockXpTable.classify("minecraft:stone").xp, "terra < pedra");
        check(BlockXpTable.classify("minecraft:redstone_block") == null, "redstone_block não é minério");
        check(BlockXpTable.classify("minecraft:oak_log").skill == Skill.GATHERING, "tronco");
        check(BlockXpTable.classify("minecraft:pumpkin_stem") == null, "caule de abóbora não rende");
        check(BlockXpTable.classify("minecraft:wheat").source == XpSource.CROP_HARVEST, "trigo");
        check(BlockXpTable.classify("minecraft:oak_leaves") == null, "folhas não rendem");
        Object[] ov = BlockXpTable.parseOverride("minecraft:stone=3;BLOCK_STONE;mining");
        check(ov != null, "parse de sobrescrita");
        Settings.blockOverrides.put((String) ov[0], (BlockXpTable.Entry) ov[1]);
        check(BlockXpTable.classify("minecraft:stone").xp == 3, "sobrescrita por bloco aplicada");
        Object[] zero = BlockXpTable.parseOverride("minecraft:dirt=0");
        Settings.blockOverrides.put((String) zero[0], (BlockXpTable.Entry) zero[1]);
        check(BlockXpTable.classify("minecraft:dirt") == null, "sobrescrita com 0 desativa");
        check(BlockXpTable.parseOverride("lixo") == null && BlockXpTable.parseOverride("a:b=x") == null, "sobrescrita inválida rejeitada");
        Settings.resetDefaults();
    }

    static void placementTracker() {
        Settings.resetDefaults();
        PlacementTracker t = new PlacementTracker();
        t.markPlaced("overworld", 42L);
        check(t.consumePlaced("overworld", 42L), "bloco colocado por jogador é reconhecido");
        check(!t.consumePlaced("overworld", 42L), "reconhecimento é consumido");
        check(!t.consumePlaced("nether", 42L), "dimensões separadas");
        check(t.tryCredit("overworld", 7L), "primeiro crédito de construção");
        check(!t.tryCredit("overworld", 7L), "colocar/quebrar/colocar na mesma posição não rende de novo");
        Settings.maxTracked = 100;
        PlacementTracker big = new PlacementTracker();
        for (long i = 0; i < 1000; i++) big.markPlaced("d", i);
        check(big.exportPlaced().get("d").length == 100, "tamanho limitado");
        check(!big.consumePlaced("d", 0), "mais antigas descartadas");
        check(big.consumePlaced("d", 999), "mais recentes mantidas");
        Settings.resetDefaults();
        PlacementTracker a = new PlacementTracker();
        a.markPlaced("x", 1);
        a.markPlaced("x", 2);
        a.tryCredit("x", 3);
        PlacementTracker b = new PlacementTracker();
        for (Map.Entry<String, long[]> e : a.exportPlaced().entrySet()) b.importPlaced(e.getKey(), e.getValue());
        for (Map.Entry<String, long[]> e : a.exportCredited().entrySet()) b.importCredited(e.getKey(), e.getValue());
        check(b.consumePlaced("x", 1) && b.consumePlaced("x", 2), "exportar/importar preserva posições");
        check(!b.tryCredit("x", 3), "exportar/importar preserva créditos");
    }

    static void contributions() {
        Settings.resetDefaults();
        ContributionTracker t = new ContributionTracker();
        UUID mob = UUID.randomUUID(), a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        t.record(mob, a, Skill.MELEE, 15, 10);
        t.record(mob, a, Skill.ARCHERY, 5, 11);
        t.record(mob, b, Skill.MAGIC, 20, 12);
        t.record(mob, c, Skill.MELEE, 0.5, 12);
        ContributionTracker.Entry e = t.take(mob);
        check(t.take(mob) == null, "registro é removido após o abate");
        Set<UUID> all = new HashSet<UUID>();
        Collections.addAll(all, a, b, c);
        Map<UUID, Double> s = ContributionTracker.computeShares(e, all);
        double sum = 0;
        for (double v : s.values()) sum += v;
        check(Math.abs(sum - 1.0) < 1e-9, "participações somam 1");
        check(!s.containsKey(c), "participação mínima exigida");
        check(Math.abs(s.get(a) - 0.5) < 1e-9 && Math.abs(s.get(b) - 0.5) < 1e-9, "divisão proporcional ao dano");
        check(e.dominantSkill(a) == Skill.MELEE, "habilidade dominante do jogador");
        Set<UUID> only = new HashSet<UUID>();
        only.add(b);
        Map<UUID, Double> s2 = ContributionTracker.computeShares(e, only);
        check(s2.size() == 1 && Math.abs(s2.get(b) - 1.0) < 1e-9, "inelegíveis (longe) não recebem; restante é redistribuído");
        check(ContributionTracker.computeShares(null, all).isEmpty(), "sem registro, sem XP");
        t.record(mob, a, Skill.MELEE, 1, 0);
        t.prune(100000, 6000);
        check(t.size() == 0, "limpeza de registros antigos");
    }

    static void repeatAndVariety() {
        Trackers.Repeat r = new Trackers.Repeat(6000);
        UUID p = UUID.randomUUID();
        double f0 = r.factor(p, "stick", 0);
        double f1 = r.factor(p, "stick", 1);
        double f50 = 0;
        for (int i = 0; i < 50; i++) f50 = r.factor(p, "stick", 2);
        check(f0 == 1.0 && f1 < f0 && f50 >= 0.1 && f50 < 0.2, "retorno decrescente por repetição: " + f0 + "," + f1 + "," + f50);
        check(r.factor(p, "diamond_sword", 3) == 1.0, "itens diferentes não se afetam");
        check(r.factor(p, "stick", 7000) == 1.0, "janela expira");
        Trackers.Variety v = new Trackers.Variety();
        double last = 0;
        for (int i = 0; i < 20; i++) last = v.factor(p, "minecraft:dirt");
        check(Math.abs(last - 0.3) < 1e-9, "repetir o mesmo bloco: fator mínimo");
        Trackers.Variety v2 = new Trackers.Variety();
        double last2 = 0;
        for (int i = 0; i < 20; i++) last2 = v2.factor(p, "block" + i);
        check(Math.abs(last2 - 1.0) < 1e-9, "variedade total: fator máximo");
    }

    static void mobFormulas() {
        Settings.resetDefaults();
        int zl = MobXp.estimateLevel(20, 3);
        check(zl >= 3 && zl <= 8, "nível estimado de zumbi: " + zl);
        check(MobXp.levelFactor(zl, zl) == 1.0, "mesmo nível: fator 1");
        check(MobXp.levelFactor(100, zl) == Settings.minLevelFactor, "mob muito inferior rende o mínimo");
        check(MobXp.levelFactor(zl, zl + 20) == 1.5, "mob superior rende até +50%");
        check(MobXp.levelFactor(20, 10) < 1.0 && MobXp.levelFactor(20, 10) > Settings.minLevelFactor, "queda gradual");
        check(MobXp.killXp(300, true) > 100 * MobXp.killXp(20, false) / 20, "chefes rendem muito mais");
        check(MobXp.takenXp(1000) == MobXp.takenXp(20), "XP defensivo limitado por golpe");
        check(MobXp.damageXp(10, true) == 2 * MobXp.damageXp(10, false), "dano em chefe vale mais");
    }

    static void clampAfterConfigChange() {
        Settings.resetDefaults();
        PlayerRpgData d = new PlayerRpgData();
        d.level = 400;
        d.attrs[0] = 80;
        d.skillLevels[0] = 90;
        Settings.maxLevel = 200;
        Settings.attrCap = 50;
        Settings.skillMaxLevel = 60;
        d.clamp();
        check(d.level == 200, "nível limitado");
        check(d.attrs[0] == 50 && d.attrPoints == 30, "pontos excedentes devolvidos, não perdidos");
        check(d.skillLevels[0] == 60, "habilidade limitada");
        Settings.resetDefaults();
    }
}
