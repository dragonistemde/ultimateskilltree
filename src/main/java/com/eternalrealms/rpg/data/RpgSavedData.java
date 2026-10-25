package com.eternalrealms.rpg.data;

import com.eternalrealms.rpg.EternalRealms;
import com.eternalrealms.rpg.core.PlacementTracker;
import com.eternalrealms.rpg.core.PlayerRpgData;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Dados persistentes do mod, salvos junto do mundo (dimensao Overworld).
 * Guardar por UUID aqui (em vez de capability) mantem a progressao intacta apos a morte,
 * troca de dimensao e reinicio do servidor.
 */
public class RpgSavedData extends WorldSavedData {
    public static final String NAME = EternalRealms.MODID + "_data";
    private static final int TAG_COMPOUND = 10;

    private final Map<UUID, PlayerRpgData> players = new HashMap<UUID, PlayerRpgData>();
    public final PlacementTracker placement = new PlacementTracker();

    public RpgSavedData() {
        super(NAME);
    }

    public static RpgSavedData get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        return overworld.getSavedData().getOrCreate(RpgSavedData::new, NAME);
    }

    public PlayerRpgData dataOf(UUID id) {
        PlayerRpgData d = players.get(id);
        if (d == null) {
            d = new PlayerRpgData();
            players.put(id, d);
            markDirty();
        }
        return d;
    }

    @Override
    public void read(CompoundNBT nbt) {
        players.clear();
        ListNBT list = nbt.getList("players", TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundNBT c = list.getCompound(i);
            try {
                UUID id = UUID.fromString(c.getString("id"));
                PlayerRpgData d = new PlayerRpgData();
                RpgNbt.read(c.getCompound("data"), d);
                players.put(id, d);
            } catch (IllegalArgumentException e) {
                EternalRealms.LOGGER.warn("Registro de jogador invalido ignorado: {}", c.getString("id"));
            }
        }
        CompoundNBT placed = nbt.getCompound("placed");
        for (String dim : placed.keySet()) placement.importPlaced(dim, placed.getLongArray(dim));
        CompoundNBT credited = nbt.getCompound("credited");
        for (String dim : credited.keySet()) placement.importCredited(dim, credited.getLongArray(dim));
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt) {
        ListNBT list = new ListNBT();
        for (Map.Entry<UUID, PlayerRpgData> e : players.entrySet()) {
            CompoundNBT c = new CompoundNBT();
            c.putString("id", e.getKey().toString());
            c.put("data", RpgNbt.write(e.getValue()));
            list.add(c);
        }
        nbt.put("players", list);
        CompoundNBT placed = new CompoundNBT();
        for (Map.Entry<String, long[]> e : placement.exportPlaced().entrySet()) placed.putLongArray(e.getKey(), e.getValue());
        nbt.put("placed", placed);
        CompoundNBT credited = new CompoundNBT();
        for (Map.Entry<String, long[]> e : placement.exportCredited().entrySet()) credited.putLongArray(e.getKey(), e.getValue());
        nbt.put("credited", credited);
        return nbt;
    }
}
