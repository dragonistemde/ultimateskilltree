package com.eternalrealms.rpg.net;

import com.eternalrealms.rpg.EternalRealms;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public final class Net {
    private Net() {}

    private static final String PROTOCOL = "1";
    private static SimpleChannel channel;

    public static void register() {
        channel = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(EternalRealms.MODID, "main"),
                () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
        int id = 0;
        channel.registerMessage(id++, SyncMsg.class, SyncMsg::encode, SyncMsg::decode, SyncMsg::handle);
        channel.registerMessage(id++, AllocateAttrMsg.class, AllocateAttrMsg::encode, AllocateAttrMsg::decode, AllocateAttrMsg::handle);
    }

    public static void sendToPlayer(ServerPlayerEntity player, Object msg) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void sendToServer(Object msg) {
        channel.sendToServer(msg);
    }
}
