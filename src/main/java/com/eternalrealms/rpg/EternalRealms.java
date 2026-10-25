package com.eternalrealms.rpg;

import com.eternalrealms.rpg.client.ClientSetup;
import com.eternalrealms.rpg.command.RpgCommands;
import com.eternalrealms.rpg.config.RpgConfig;
import com.eternalrealms.rpg.event.PlayerEvents;
import com.eternalrealms.rpg.event.XpEvents;
import com.eternalrealms.rpg.net.Net;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(EternalRealms.MODID)
public class EternalRealms {
    public static final String MODID = "eternalrealms";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public EternalRealms() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RpgConfig.COMMON, "eternalrealms-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, RpgConfig.CLIENT, "eternalrealms-client.toml");
        modBus.addListener(this::commonSetup);
        modBus.addListener(RpgConfig::onConfigEvent);

        MinecraftForge.EVENT_BUS.register(new XpEvents());
        MinecraftForge.EVENT_BUS.register(new PlayerEvents());
        MinecraftForge.EVENT_BUS.register(new RpgCommands());

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientSetup::init);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        Net.register();
        LOGGER.info("Eternal Realms RPG: Fase 1 carregada (XP, niveis, atributos, habilidades secundarias).");
    }
}
