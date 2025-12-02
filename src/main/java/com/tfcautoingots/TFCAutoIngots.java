package com.tfcautoingots;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(TFCAutoIngots.MODID)
public class TFCAutoIngots {
    public static final String MODID = "tfcautoingots";
    private static final Logger LOGGER = LogUtils.getLogger();

    public TFCAutoIngots(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("TFC AutoIngots initialized! Generating textures for ingots from any mod.");
    }

    public static Logger getLogger() {
        return LOGGER;
    }
}

