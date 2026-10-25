package com.eternalrealms.rpg.event;

import com.eternalrealms.rpg.core.PlayerRpgData;
import com.eternalrealms.rpg.core.Settings;
import com.eternalrealms.rpg.core.Skill;
import com.eternalrealms.rpg.core.XpEngine;
import com.eternalrealms.rpg.core.XpResult;
import com.eternalrealms.rpg.core.XpSource;
import com.eternalrealms.rpg.data.RpgSavedData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.common.util.FakePlayer;

/** Pontos de entrada usados pelos eventos para conceder XP no servidor. */
public final class XpHooks {
    private XpHooks() {}

    /** Somente jogadores reais, em modo sobrevivencia/aventura. Sem FakePlayers (automacao). */
    public static boolean eligible(PlayerEntity p) {
        return Settings.enabled
                && p instanceof ServerPlayerEntity
                && !(p instanceof FakePlayer)
                && !p.isSpectator()
                && !p.isCreative()
                && p.getServer() != null;
    }

    public static RpgSavedData saved(ServerPlayerEntity p) {
        return RpgSavedData.get(p.getServer());
    }

    public static void award(ServerPlayerEntity p, XpSource src, Skill skill, double baseXp) {
        if (baseXp <= 0) return;
        RpgSavedData sd = saved(p);
        PlayerRpgData d = sd.dataOf(p.getUniqueID());
        XpResult r = XpEngine.award(d, p.getUniqueID(), src, skill, baseXp, p.world.getGameTime());
        if (r.isEmpty()) return;
        sd.markDirty();
        Sync.queue(p.getUniqueID(), r);
    }
}
