package com.eternalrealms.rpg.event;

import com.eternalrealms.rpg.core.BlockXpTable;
import com.eternalrealms.rpg.core.ContributionTracker;
import com.eternalrealms.rpg.core.MobXp;
import com.eternalrealms.rpg.core.PlayerRpgData;
import com.eternalrealms.rpg.core.Settings;
import com.eternalrealms.rpg.core.Skill;
import com.eternalrealms.rpg.core.Trackers;
import com.eternalrealms.rpg.core.XpSource;
import com.eternalrealms.rpg.data.RpgSavedData;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropsBlock;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Todas as fontes de XP baseadas em eventos do jogo (somente lado servidor). */
public class XpEvents {
    private static final ContributionTracker CONTRIB = new ContributionTracker();
    private static final Trackers.Repeat REPEAT = new Trackers.Repeat(6000);
    private static final Trackers.Variety VARIETY = new Trackers.Variety();
    private static final Map<UUID, Long> LAST_SHIELD = new HashMap<UUID, Long>();

    // ---------- utilitarios ----------

    private static String dimKey(IWorld w) {
        if (w instanceof World) return ((World) w).getDimensionKey().getLocation().toString();
        return "unknown";
    }

    private static boolean isHostile(Entity e) {
        return e instanceof IMob;
    }

    private static double attackOf(LivingEntity e) {
        ModifiableAttributeInstance a = e.getAttribute(Attributes.ATTACK_DAMAGE);
        return a == null ? 2.0 : a.getValue();
    }

    private static boolean isSeedBlock(Block b) {
        return b instanceof CropsBlock || b instanceof NetherWartBlock;
    }

    private static boolean isMature(BlockState st) {
        Block b = st.getBlock();
        if (b instanceof CropsBlock) return ((CropsBlock) b).isMaxAge(st);
        if (b instanceof NetherWartBlock) return st.get(NetherWartBlock.AGE) >= 3;
        return true;
    }

    public static void forget(UUID id) {
        LAST_SHIELD.remove(id);
        REPEAT.forget(id);
        VARIETY.forget(id);
    }

    public static void tickMaintenance(long now) {
        CONTRIB.prune(now, 6000);
        REPEAT.prune(now);
    }

    // ---------- mineracao, colheita, construcao ----------

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockEvent.BreakEvent e) {
        if (e.getWorld().isRemote() || !XpHooks.eligible(e.getPlayer())) return;
        ServerPlayerEntity p = (ServerPlayerEntity) e.getPlayer();
        BlockState st = e.getState();
        ResourceLocation rl = st.getBlock().getRegistryName();
        if (rl == null) return;
        RpgSavedData sd = XpHooks.saved(p);
        // consome o marcador sempre, para que blocos colocados nunca rendam XP de quebra
        boolean wasPlaced = sd.placement.consumePlaced(dimKey(e.getWorld()), e.getPos().toLong());
        BlockXpTable.Entry en = BlockXpTable.classify(rl.toString());
        if (en == null || wasPlaced) return;
        if (en.source == XpSource.CROP_HARVEST && !isMature(st)) return;
        XpHooks.award(p, en.source, en.skill, en.xp);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockEvent.EntityPlaceEvent e) {
        if (e.getWorld().isRemote() || !(e.getEntity() instanceof PlayerEntity) || !XpHooks.eligible((PlayerEntity) e.getEntity())) return;
        ServerPlayerEntity p = (ServerPlayerEntity) e.getEntity();
        BlockState st = e.getPlacedBlock();
        ResourceLocation rl = st.getBlock().getRegistryName();
        if (rl == null) return;
        if (isSeedBlock(st.getBlock())) {
            if (BlockXpTable.classify(rl.toString()) != null) XpHooks.award(p, XpSource.CROP_PLANT, Skill.FARMING, 0.4);
            return;
        }
        RpgSavedData sd = XpHooks.saved(p);
        String dim = dimKey(e.getWorld());
        long pos = e.getPos().toLong();
        sd.placement.markPlaced(dim, pos);
        sd.markDirty();
        if (sd.placement.tryCredit(dim, pos)) {
            double f = VARIETY.factor(p.getUniqueID(), rl.toString());
            XpHooks.award(p, XpSource.BUILD, Skill.BUILDING, 0.12 * f);
        }
    }

    // ---------- combate ----------

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDamage(LivingDamageEvent e) {
        LivingEntity target = e.getEntityLiving();
        if (target.world.isRemote || e.getAmount() <= 0) return;
        DamageSource ds = e.getSource();
        Entity attacker = ds.getTrueSource();
        long now = target.world.getGameTime();

        // causar dano a hostis
        if (attacker instanceof ServerPlayerEntity && target != attacker && isHostile(target)
                && XpHooks.eligible((PlayerEntity) attacker)) {
            ServerPlayerEntity p = (ServerPlayerEntity) attacker;
            Skill skill = null;
            if (ds.isMagicDamage() && !"thorns".equals(ds.getDamageType())) skill = Skill.MAGIC;
            else if (ds.getImmediateSource() instanceof AbstractArrowEntity) skill = Skill.ARCHERY;
            else if (ds.getImmediateSource() == attacker && !ds.isMagicDamage() && !ds.isExplosion()) skill = Skill.MELEE;
            if (skill != null) {
                double eff = Math.min(e.getAmount(), target.getHealth()); // dano real, limitado pela vida restante
                boolean boss = !target.isNonBoss();
                int mobLevel = MobXp.estimateLevel(target.getMaxHealth(), attackOf(target));
                PlayerRpgData d = XpHooks.saved(p).dataOf(p.getUniqueID());
                double xp = MobXp.damageXp(eff, boss) * MobXp.levelFactor(d.level, mobLevel);
                XpHooks.award(p, XpSource.DAMAGE_DEALT, skill, xp);
                CONTRIB.record(target.getUniqueID(), p.getUniqueID(), skill, eff, now);
            }
        }

        // receber dano de hostis
        if (target instanceof ServerPlayerEntity && attacker instanceof LivingEntity && isHostile(attacker)
                && XpHooks.eligible((PlayerEntity) target) && !ds.isUnblockable()) {
            ServerPlayerEntity p = (ServerPlayerEntity) target;
            LivingEntity mob = (LivingEntity) attacker;
            int mobLevel = MobXp.estimateLevel(mob.getMaxHealth(), attackOf(mob));
            PlayerRpgData d = XpHooks.saved(p).dataOf(p.getUniqueID());
            XpHooks.award(p, XpSource.DAMAGE_TAKEN, Skill.DEFENSE,
                    MobXp.takenXp(e.getAmount()) * MobXp.levelFactor(d.level, mobLevel));
        }
    }

    /** Bloqueio com escudo: LivingDamageEvent nao dispara se o golpe e totalmente bloqueado. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onAttack(LivingAttackEvent e) {
        LivingEntity target = e.getEntityLiving();
        if (target.world.isRemote || !(target instanceof ServerPlayerEntity)) return;
        Entity attacker = e.getSource().getTrueSource();
        if (!(attacker instanceof LivingEntity) || !isHostile(attacker)) return;
        ServerPlayerEntity p = (ServerPlayerEntity) target;
        if (!XpHooks.eligible(p) || !p.isActiveItemStackBlocking()) return;
        long now = p.world.getGameTime();
        Long last = LAST_SHIELD.get(p.getUniqueID());
        if (last != null && now - last < 10) return;
        LAST_SHIELD.put(p.getUniqueID(), now);
        XpHooks.award(p, XpSource.SHIELD_BLOCK, Skill.DEFENSE, 1.5);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeath(LivingDeathEvent e) {
        LivingEntity dead = e.getEntityLiving();
        if (dead.world.isRemote || !(dead.world instanceof ServerWorld) || dead instanceof PlayerEntity) return;
        ContributionTracker.Entry entry = CONTRIB.take(dead.getUniqueID());
        if (entry == null || !isHostile(dead)) return;

        ServerWorld world = (ServerWorld) dead.world;
        double maxDistSq = (double) Settings.shareRadius * Settings.shareRadius;
        Set<UUID> eligible = new HashSet<UUID>();
        Map<UUID, ServerPlayerEntity> byId = new HashMap<UUID, ServerPlayerEntity>();
        for (ServerPlayerEntity sp : world.getPlayers()) {
            if (XpHooks.eligible(sp) && sp.getDistanceSq(dead) <= maxDistSq) {
                eligible.add(sp.getUniqueID());
                byId.put(sp.getUniqueID(), sp);
            }
        }
        Map<UUID, Double> shares = ContributionTracker.computeShares(entry, eligible);
        if (shares.isEmpty()) return;

        boolean boss = !dead.isNonBoss();
        int mobLevel = MobXp.estimateLevel(dead.getMaxHealth(), attackOf(dead));
        double base = MobXp.killXp(dead.getMaxHealth(), boss);
        for (Map.Entry<UUID, Double> s : shares.entrySet()) {
            ServerPlayerEntity sp = byId.get(s.getKey());
            if (sp == null) continue;
            PlayerRpgData d = XpHooks.saved(sp).dataOf(sp.getUniqueID());
            Skill skill = entry.dominantSkill(sp.getUniqueID());
            XpHooks.award(sp, XpSource.KILL, skill, base * s.getValue() * MobXp.levelFactor(d.level, mobLevel));
        }
    }

    // ---------- natureza ----------

    @SubscribeEvent
    public void onBreed(BabyEntitySpawnEvent e) {
        PlayerEntity pl = e.getCausedByPlayer();
        if (pl == null || !XpHooks.eligible(pl)) return;
        XpHooks.award((ServerPlayerEntity) pl, XpSource.BREED, Skill.FARMING, 3.0);
    }

    // ---------- fabricacao ----------

    private static boolean isGear(Item it) {
        return it instanceof SwordItem || it instanceof ToolItem || it instanceof ArmorItem || it instanceof BowItem
                || it instanceof CrossbowItem || it instanceof ShieldItem || it instanceof TridentItem;
    }

    @SubscribeEvent
    public void onCrafted(PlayerEvent.ItemCraftedEvent e) {
        PlayerEntity pl = e.getPlayer();
        if (pl.world.isRemote || !XpHooks.eligible(pl)) return;
        ItemStack st = e.getCrafting();
        Item it = st.getItem();
        if (!isGear(it)) return; // itens triviais nao rendem
        ServerPlayerEntity p = (ServerPlayerEntity) pl;
        double complexity = 2.0 + Math.min(st.getMaxDamage(), 2100) / 150.0;
        double f = REPEAT.factor(p.getUniqueID(), "craft:" + it.getRegistryName(), p.world.getGameTime());
        XpHooks.award(p, XpSource.CRAFT, Skill.SMITHING, complexity * f);
    }

    @SubscribeEvent
    public void onSmelted(PlayerEvent.ItemSmeltedEvent e) {
        PlayerEntity pl = e.getPlayer();
        if (pl.world.isRemote || !XpHooks.eligible(pl)) return;
        ItemStack st = e.getSmelting();
        ResourceLocation rl = st.getItem().getRegistryName();
        if (rl == null) return;
        String path = rl.getPath();
        Skill skill;
        double per;
        if (path.endsWith("_ingot") || path.equals("netherite_scrap")) {
            skill = Skill.SMITHING;
            per = 1.5;
        } else if (st.getItem().isFood()) {
            skill = Skill.FARMING;
            per = 0.8;
        } else {
            return;
        }
        ServerPlayerEntity p = (ServerPlayerEntity) pl;
        double f = REPEAT.factor(p.getUniqueID(), "smelt:" + rl, p.world.getGameTime());
        XpHooks.award(p, XpSource.SMELT, skill, per * Math.min(st.getCount(), 8) * f);
    }

    @SubscribeEvent
    public void onAnvilRepair(AnvilRepairEvent e) {
        PlayerEntity pl = e.getPlayer();
        if (pl.world.isRemote || !XpHooks.eligible(pl)) return;
        ServerPlayerEntity p = (ServerPlayerEntity) pl;
        double f = REPEAT.factor(p.getUniqueID(), "anvil", p.world.getGameTime());
        XpHooks.award(p, XpSource.REPAIR, Skill.SMITHING, 4.0 * f);
    }
}
