package com.eternalrealms.rpg.client;

import com.eternalrealms.rpg.core.AttrType;
import com.eternalrealms.rpg.core.PlayerRpgData;
import com.eternalrealms.rpg.core.Settings;
import com.eternalrealms.rpg.core.Skill;
import com.eternalrealms.rpg.net.AllocateAttrMsg;
import com.eternalrealms.rpg.net.Net;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.List;

/** Tela do personagem: nivel, XP, pontos, distribuicao de atributos e habilidades secundarias. */
public class RpgScreen extends Screen {
    private final List<Button> plusButtons = new ArrayList<Button>();

    public RpgScreen() {
        super(new TranslationTextComponent("screen.eternalrealms.title"));
    }

    @Override
    protected void init() {
        plusButtons.clear();
        int left = this.width / 2 - 230;
        int top = 62;
        for (final AttrType t : AttrType.values()) {
            int y = top + t.ordinal() * 20;
            Button b = new Button(left + 150, y - 3, 20, 16, new StringTextComponent("+"),
                    btn -> Net.sendToServer(new AllocateAttrMsg(t.ordinal(), hasShiftDown() ? 5 : 1)));
            plusButtons.add(b);
            this.addButton(b);
        }
        updateButtons();
    }

    @Override
    public void tick() {
        super.tick();
        updateButtons();
    }

    private void updateButtons() {
        PlayerRpgData d = ClientState.data;
        for (int i = 0; i < plusButtons.size(); i++) {
            plusButtons.get(i).active = d.attrPoints > 0 && d.attrs[i] < Settings.attrCap;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partial) {
        this.renderBackground(ms);
        PlayerRpgData d = ClientState.data;
        int cx = this.width / 2;

        drawCenteredString(ms, this.font, this.title.getString(), cx, 14, 0xFFFFFF);
        drawCenteredString(ms, this.font, I18n.format("screen.eternalrealms.level", d.level, Settings.maxLevel), cx, 28, 0xFFD54F);

        // barra de XP do personagem
        int bw = 260, bx = cx - bw / 2, by = 40;
        fill(ms, bx - 1, by - 1, bx + bw + 1, by + 9, 0xFF000000);
        fill(ms, bx, by, bx + (int) (bw * d.progress()), by + 8, 0xFF4FC3F7);
        String xpText = d.xpToNext() <= 0 ? I18n.format("screen.eternalrealms.maxlevel")
                : String.format("%.0f / %d XP", d.xp, d.xpToNext());
        drawCenteredString(ms, this.font, xpText, cx, by, 0xFFFFFF);
        drawCenteredString(ms, this.font,
                I18n.format("screen.eternalrealms.points", d.attrPoints, d.skillPoints), cx, 52, 0xE0E0E0);

        // atributos (coluna esquerda)
        int left = cx - 230, top = 62;
        this.font.drawStringWithShadow(ms, I18n.format("screen.eternalrealms.attributes"), left, top - 12, 0xFFFFFF);
        for (AttrType t : AttrType.values()) {
            int y = top + t.ordinal() * 20;
            this.font.drawStringWithShadow(ms, I18n.format("attr.eternalrealms." + t.id), left, y, 0xE0E0E0);
            this.font.drawStringWithShadow(ms, d.attrs[t.ordinal()] + " / " + Settings.attrCap, left + 95, y, 0xFFFFFF);
        }
        this.font.drawStringWithShadow(ms, I18n.format("screen.eternalrealms.shift_hint"), left, top + 10 * 20 + 2, 0x909090);

        // habilidades secundarias (coluna direita)
        int sx = cx - 30, sy = 62;
        this.font.drawStringWithShadow(ms, I18n.format("screen.eternalrealms.skills"), sx, sy - 12, 0xFFFFFF);
        for (Skill s : Skill.values()) {
            int y = sy + s.ordinal() * 12;
            int lv = d.skillLevels[s.ordinal()];
            this.font.drawStringWithShadow(ms, I18n.format("skill.eternalrealms." + s.id), sx, y, 0xE0E0E0);
            this.font.drawStringWithShadow(ms, String.valueOf(lv), sx + 120, y, 0xFFFFFF);
            long need = d.skillXpToNext(s);
            double prog = need <= 0 ? 1.0 : Math.min(1.0, d.skillXp[s.ordinal()] / need);
            int px = sx + 150, pw = 100;
            fill(ms, px, y + 1, px + pw, y + 7, 0xFF202020);
            fill(ms, px, y + 1, px + (int) (pw * prog), y + 7, 0xFF9CE59C);
        }

        super.render(ms, mouseX, mouseY, partial);
    }
}
