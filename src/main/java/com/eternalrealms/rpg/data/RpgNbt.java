package com.eternalrealms.rpg.data;

import com.eternalrealms.rpg.core.AttrType;
import com.eternalrealms.rpg.core.PlayerRpgData;
import com.eternalrealms.rpg.core.Skill;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;

/** Conversão PlayerRpgData <-> NBT. Usa chaves por nome, tolerante à adição de novos atributos/habilidades. */
public final class RpgNbt {
    private RpgNbt() {}

    private static final int TAG_COMPOUND = 10;
    private static final int TAG_STRING = 8;

    public static CompoundNBT write(PlayerRpgData d) {
        CompoundNBT n = new CompoundNBT();
        n.putInt("level", d.level);
        n.putDouble("xp", d.xp);
        n.putInt("attrPoints", d.attrPoints);
        n.putInt("skillPoints", d.skillPoints);
        CompoundNBT a = new CompoundNBT();
        for (AttrType t : AttrType.values()) a.putInt(t.id, d.attrs[t.ordinal()]);
        n.put("attrs", a);
        CompoundNBT s = new CompoundNBT();
        for (Skill k : Skill.values()) {
            CompoundNBT c = new CompoundNBT();
            c.putInt("l", d.skillLevels[k.ordinal()]);
            c.putDouble("x", d.skillXp[k.ordinal()]);
            s.put(k.id, c);
        }
        n.put("skills", s);
        ListNBT disc = new ListNBT();
        for (String x : d.discovered) disc.add(StringNBT.valueOf(x));
        n.put("discovered", disc);
        return n;
    }

    public static void read(CompoundNBT n, PlayerRpgData d) {
        d.level = n.contains("level") ? n.getInt("level") : 1;
        d.xp = n.getDouble("xp");
        d.attrPoints = n.getInt("attrPoints");
        d.skillPoints = n.getInt("skillPoints");
        CompoundNBT a = n.getCompound("attrs");
        for (AttrType t : AttrType.values()) d.attrs[t.ordinal()] = a.getInt(t.id);
        CompoundNBT s = n.getCompound("skills");
        for (Skill k : Skill.values()) {
            if (s.contains(k.id, TAG_COMPOUND)) {
                CompoundNBT c = s.getCompound(k.id);
                d.skillLevels[k.ordinal()] = c.getInt("l");
                d.skillXp[k.ordinal()] = c.getDouble("x");
            }
        }
        d.discovered.clear();
        ListNBT disc = n.getList("discovered", TAG_STRING);
        for (int i = 0; i < disc.size(); i++) d.discovered.add(disc.getString(i));
        d.clamp();
    }
}
