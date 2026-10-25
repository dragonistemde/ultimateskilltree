package com.eternalrealms.rpg.net;

import com.eternalrealms.rpg.client.ClientState;
import com.eternalrealms.rpg.core.XpResult;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: estado completo do personagem + resumo do ganho recente (para notificacoes). */
public final class SyncMsg {
    public final CompoundNBT data;
    public final String srcKey;      // "" se nao houver ganho
    public final double charGain;
    public final int skillOrdinal;   // -1 se nao houver
    public final double skillGain;
    public final int levels;
    public final int skillLevels;

    public SyncMsg(CompoundNBT data, String srcKey, double charGain, int skillOrdinal, double skillGain, int levels, int skillLevels) {
        this.data = data;
        this.srcKey = srcKey;
        this.charGain = charGain;
        this.skillOrdinal = skillOrdinal;
        this.skillGain = skillGain;
        this.levels = levels;
        this.skillLevels = skillLevels;
    }

    public static SyncMsg of(CompoundNBT data, XpResult r) {
        if (r == null) return new SyncMsg(data, "", 0, -1, 0, 0, 0);
        return new SyncMsg(data, r.src == null ? "" : r.src.key, r.charGain,
                r.skill == null ? -1 : r.skill.ordinal(), r.skillGain, r.levels, r.skillLevels);
    }

    public static void encode(SyncMsg m, PacketBuffer buf) {
        buf.writeCompoundTag(m.data);
        buf.writeString(m.srcKey);
        buf.writeDouble(m.charGain);
        buf.writeVarInt(m.skillOrdinal + 1);
        buf.writeDouble(m.skillGain);
        buf.writeVarInt(m.levels);
        buf.writeVarInt(m.skillLevels);
    }

    public static SyncMsg decode(PacketBuffer buf) {
        CompoundNBT data = buf.readCompoundTag();
        String src = buf.readString(64);
        double cg = buf.readDouble();
        int so = buf.readVarInt() - 1;
        double sg = buf.readDouble();
        int lv = buf.readVarInt();
        int slv = buf.readVarInt();
        return new SyncMsg(data == null ? new CompoundNBT() : data, src, cg, so, sg, lv, slv);
    }

    public static void handle(SyncMsg m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientState.apply(m)));
        ctx.get().setPacketHandled(true);
    }
}
