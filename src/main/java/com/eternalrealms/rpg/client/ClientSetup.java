package com.eternalrealms.rpg.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

/** Registro do lado cliente. So e carregada via DistExecutor em clientes. */
public final class ClientSetup {
    private ClientSetup() {}

    private static KeyBinding openKey;

    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientSetup::clientSetup);
        MinecraftForge.EVENT_BUS.addListener(ClientSetup::onKey);
        MinecraftForge.EVENT_BUS.addListener(HudOverlay::onRender);
    }

    private static void clientSetup(FMLClientSetupEvent e) {
        openKey = new KeyBinding("key.eternalrealms.open", GLFW.GLFW_KEY_K, "key.categories.eternalrealms");
        ClientRegistry.registerKeyBinding(openKey);
    }

    private static void onKey(InputEvent.KeyInputEvent e) {
        if (openKey == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.currentScreen == null && openKey.isPressed()) {
            mc.displayGuiScreen(new RpgScreen());
        }
    }
}
