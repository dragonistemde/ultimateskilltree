package com.eternalrealms.rpg.event;

import com.eternalrealms.rpg.core.XpResult;
import com.eternalrealms.rpg.data.RpgNbt;
import com.eternalrealms.rpg.data.RpgSavedData;
import com.eternalrealms.rpg.net.Net;
import com.eternalrealms.rpg.net.SyncMsg;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Sincronizacao servidor -> cliente em lotes (evita um pacote por bloco minerado). */
public final class Sync {
    private Sync() {}

    private static final Map<UUID, XpResult> PENDING = new HashMap<UUID, XpResult>();

    public static void queue(UUID id, XpResult r) {
        XpResult cur = PENDING.get(id);
        if (cur == null) PENDING.put(id, r.copy());
        else cur.merge(r);
    }

    public static void flush(MinecraftServer server) {
        if (server == null || PENDING.isEmpty()) return;
        Iterator<Map.Entry<UUID, XpResult>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, XpResult> e = it.next();
            ServerPlayerEntity p = server.getPlayerList().getPlayerByUUID(e.getKey());
            if (p != null) sendFull(p, e.getValue());
            it.remove();
        }
    }

    public static void sendFull(ServerPlayerEntity p, XpResult r) {
        RpgSavedData sd = RpgSavedData.get(p.getServer());
        Net.sendToPlayer(p, SyncMsg.of(RpgNbt.write(sd.dataOf(p.getUniqueID())), r));
    }

    public static void forget(UUID id) {
        PENDING.remove(id);
    }
}
