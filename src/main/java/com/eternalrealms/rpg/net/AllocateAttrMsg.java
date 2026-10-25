package com.eternalrealms.rpg.net;

import com.eternalrealms.rpg.core.AttrType;
import com.eternalrealms.rpg.core.PlayerRpgData;
import com.eternalrealms.rpg.data.RpgSavedData;
import com.eternalrealms.rpg.event.Sync;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: pedido de investir pontos em um atributo. O servidor valida tudo. */
public final class AllocateAttrMsg {
    private static final int MAX_AMOUNT = 100;

    public final int attrOrdinal;
    public final int amount;

    public AllocateAttrMsg(int attrOrdinal, int amount) {
        this.attrOrdinal = attrOrdinal;
        this.amount = amount;
    }

    public static void encode(AllocateAttrMsg m, PacketBuffer buf) {
        buf.writeVarInt(m.attrOrdinal);
        buf.writeVarInt(m.amount);
    }

    public static AllocateAttrMsg decode(PacketBuffer buf) {
        return new AllocateAttrMsg(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(AllocateAttrMsg m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity p = ctx.get().getSender();
            if (p == null || p.getServer() == null) return;
            AttrType[] types = AttrType.values();
            if (m.attrOrdinal < 0 || m.attrOrdinal >= types.length || m.amount <= 0 || m.amount > MAX_AMOUNT) return;
            RpgSavedData sd = RpgSavedData.get(p.getServer());
            PlayerRpgData d = sd.dataOf(p.getUniqueID());
            if (d.allocate(types[m.attrOrdinal], m.amount) > 0) {
                sd.markDirty();
                Sync.sendFull(p, null);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
