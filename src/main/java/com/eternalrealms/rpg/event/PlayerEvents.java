package com.eternalrealms.rpg.event;

import com.eternalrealms.rpg.core.PlayerRpgData;
import com.eternalrealms.rpg.core.Skill;
import com.eternalrealms.rpg.core.XpEngine;
import com.eternalrealms.rpg.core.XpSource;
import com.eternalrealms.rpg.data.RpgSavedData;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

/** Login/logout, sincronizacao periodica e exploracao. Nao ha XP passiva por tempo parado. */
public class PlayerEvents {
    private int counter = 0;

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity p = (ServerPlayerEntity) e.getPlayer();
        RpgSavedData sd = RpgSavedData.get(p.getServer());
        PlayerRpgData d = sd.dataOf(p.getUniqueID());
        d.clamp();
        if (d.discovered.isEmpty()) {
            d.discovered.add("dim:" + p.world.getDimensionKey().getLocation());
        }
        sd.markDirty();
        Sync.sendFull(p, null);
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getPlayer() instanceof ServerPlayerEntity) Sync.sendFull((ServerPlayerEntity) e.getPlayer(), null);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        XpEngine.forget(e.getPlayer().getUniqueID());
        Sync.forget(e.getPlayer().getUniqueID());
        XpEvents.forget(e.getPlayer().getUniqueID());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        counter++;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        if (counter % 10 == 0) Sync.flush(server);
        if (counter % 1200 == 0) XpEvents.tickMaintenance(server.getWorld(World.OVERWORLD).getGameTime());
    }

    @SubscribeEvent
    public void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity p = (ServerPlayerEntity) e.getPlayer();
        if (!XpHooks.eligible(p)) return;
        RpgSavedData sd = XpHooks.saved(p);
        PlayerRpgData d = sd.dataOf(p.getUniqueID());
        String key = "dim:" + e.getTo().getLocation();
        if (d.discovered.add(key)) {
            sd.markDirty();
            XpHooks.award(p, XpSource.EXPLORE_DIM, Skill.EXPLORATION, 100.0);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side != LogicalSide.SERVER) return;
        if (!(e.player instanceof ServerPlayerEntity) || e.player.ticksExisted % 100 != 0) return;
        ServerPlayerEntity p = (ServerPlayerEntity) e.player;
        if (!XpHooks.eligible(p)) return;
        Biome biome = p.world.getBiome(p.getPosition());
        ResourceLocation id = biome.getRegistryName();
        if (id == null) return;
        RpgSavedData sd = XpHooks.saved(p);
        PlayerRpgData d = sd.dataOf(p.getUniqueID());
        if (d.discovered.add("biome:" + id)) { // recompensa principal uma unica vez por jogador
            sd.markDirty();
            XpHooks.award(p, XpSource.EXPLORE_BIOME, Skill.EXPLORATION, 25.0);
        }
    }
}
