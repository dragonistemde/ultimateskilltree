package com.eternalrealms.rpg.client;

import com.eternalrealms.rpg.config.RpgConfig;
import com.eternalrealms.rpg.core.PlayerRpgData;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import java.util.Iterator;

/** HUD discreta: barra de XP compacta + notificacoes de XP / nivel. */
public final class HudOverlay {
    private HudOverlay() {}

    private static final long LIFETIME_MS = 3500;

    public static void onRender(RenderGameOverlayEvent.Post e) {
        if (e.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameSettings.hideGUI) return;
        MatrixStack ms = e.getMatrixStack();
        FontRenderer font = mc.fontRenderer;
        int width = mc.getMainWindow().getScaledWidth();

        if (RpgConfig.CL_SHOW_BAR.get()) {
            PlayerRpgData d = ClientState.data;
            int x = 6, y = 6, w = 90, h = 5;
            AbstractGui.fill(ms, x - 1, y - 1, x + w + 1, y + h + 1, 0xAA000000);
            AbstractGui.fill(ms, x, y, x + (int) (w * d.progress()), y + h, 0xFF4FC3F7);
            font.drawStringWithShadow(ms, I18n.format("hud.eternalrealms.level", d.level), x, y + h + 3, 0xFFFFFFFF);
        }

        long now = System.currentTimeMillis();
        int y = 6;
        synchronized (ClientState.TOASTS) {
            Iterator<ClientState.Toast> it = ClientState.TOASTS.iterator();
            while (it.hasNext()) {
                ClientState.Toast t = it.next();
                if (now - t.born > LIFETIME_MS) {
                    it.remove();
                    continue;
                }
                String text = format(t);
                int color = t.kind == ClientState.KIND_LEVEL ? 0xFFFFD54F : t.kind == ClientState.KIND_SKILL ? 0xFF9CE59C : 0xFFE0E0A0;
                int tw = font.getStringWidth(text);
                font.drawStringWithShadow(ms, text, width - tw - 8, y, color);
                y += 11;
            }
        }
    }

    private static String format(ClientState.Toast t) {
        switch (t.kind) {
            case ClientState.KIND_LEVEL:
                return I18n.format("hud.eternalrealms.levelup", t.level);
            case ClientState.KIND_SKILL:
                return I18n.format("hud.eternalrealms.skillup", I18n.format("skill.eternalrealms." + t.skill.id), t.level);
            default:
                StringBuilder sb = new StringBuilder(String.format("+%.1f XP", t.charGain));
                if (t.skill != null) {
                    sb.append("  ").append(I18n.format("skill.eternalrealms." + t.skill.id))
                            .append(String.format(" +%.1f", t.skillGain));
                }
                if (!t.srcKey.isEmpty()) sb.append("  [").append(I18n.format("source.eternalrealms." + t.srcKey)).append(']');
                return sb.toString();
        }
    }
}
