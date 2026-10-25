package com.eternalrealms.rpg.command;

import com.eternalrealms.rpg.core.PlayerRpgData;
import com.eternalrealms.rpg.core.Skill;
import com.eternalrealms.rpg.core.XpEngine;
import com.eternalrealms.rpg.core.XpResult;
import com.eternalrealms.rpg.data.RpgSavedData;
import com.eternalrealms.rpg.event.Sync;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * /rpg info                      - mostra o proprio progresso
 * /rpg givexp <quantidade>       - (op) concede XP de personagem, sem tetos
 * /rpg giveskillxp <hab> <qtd>   - (op) concede XP a uma habilidade secundaria
 * /rpg givepoints <attr|skill> <qtd> - (op) concede pontos
 */
public class RpgCommands {

    @SubscribeEvent
    public void onRegister(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("rpg")
                .then(Commands.literal("info").executes(c -> info(c.getSource())))
                .then(Commands.literal("givexp").requires(s -> s.hasPermissionLevel(2))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100000000))
                                .executes(c -> giveXp(c.getSource(), null, IntegerArgumentType.getInteger(c, "amount")))))
                .then(Commands.literal("giveskillxp").requires(s -> s.hasPermissionLevel(2))
                        .then(Commands.argument("skill", StringArgumentType.word())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100000000))
                                        .executes(c -> giveXp(c.getSource(), StringArgumentType.getString(c, "skill"),
                                                IntegerArgumentType.getInteger(c, "amount"))))))
                .then(Commands.literal("givepoints").requires(s -> s.hasPermissionLevel(2))
                        .then(Commands.argument("kind", StringArgumentType.word())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100000))
                                        .executes(c -> givePoints(c.getSource(), StringArgumentType.getString(c, "kind"),
                                                IntegerArgumentType.getInteger(c, "amount")))))));
    }

    private static int info(CommandSource src) throws CommandSyntaxException {
        ServerPlayerEntity p = src.asPlayer();
        PlayerRpgData d = RpgSavedData.get(p.getServer()).dataOf(p.getUniqueID());
        src.sendFeedback(new StringTextComponent(String.format(
                "Nivel %d | XP %.0f/%d | Pontos: %d atributo, %d habilidade",
                d.level, d.xp, d.xpToNext(), d.attrPoints, d.skillPoints)), false);
        StringBuilder sb = new StringBuilder("Habilidades: ");
        for (Skill s : Skill.values()) {
            int lv = d.skillLevels[s.ordinal()];
            if (lv > 0 || d.skillXp[s.ordinal()] > 0) sb.append(s.id).append(' ').append(lv).append("  ");
        }
        src.sendFeedback(new StringTextComponent(sb.toString()), false);
        return 1;
    }

    private static int giveXp(CommandSource src, String skillName, int amount) throws CommandSyntaxException {
        ServerPlayerEntity p = src.asPlayer();
        Skill skill = null;
        if (skillName != null) {
            skill = Skill.byId(skillName);
            if (skill == null) {
                src.sendFeedback(new StringTextComponent("Habilidade desconhecida: " + skillName), false);
                return 0;
            }
        }
        RpgSavedData sd = RpgSavedData.get(p.getServer());
        PlayerRpgData d = sd.dataOf(p.getUniqueID());
        XpResult r = skillName == null
                ? XpEngine.awardDirect(d, null, amount, 0)
                : XpEngine.awardDirect(d, skill, 0, amount);
        sd.markDirty();
        Sync.sendFull(p, r);
        src.sendFeedback(new StringTextComponent("Concedido. Nivel atual: " + d.level), true);
        return 1;
    }

    private static int givePoints(CommandSource src, String kind, int amount) throws CommandSyntaxException {
        ServerPlayerEntity p = src.asPlayer();
        RpgSavedData sd = RpgSavedData.get(p.getServer());
        PlayerRpgData d = sd.dataOf(p.getUniqueID());
        if ("attr".equalsIgnoreCase(kind) || "attribute".equalsIgnoreCase(kind)) d.attrPoints += amount;
        else if ("skill".equalsIgnoreCase(kind)) d.skillPoints += amount;
        else {
            src.sendFeedback(new StringTextComponent("Use: attr ou skill"), false);
            return 0;
        }
        sd.markDirty();
        Sync.sendFull(p, null);
        return 1;
    }
}
