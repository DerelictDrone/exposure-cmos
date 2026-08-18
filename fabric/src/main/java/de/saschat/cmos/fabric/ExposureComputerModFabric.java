package de.saschat.cmos.fabric;

import de.saschat.cmos.ExposureComputerMod;
import de.saschat.cmos.Config;
import net.fabricmc.api.ModInitializer;

import net.neoforged.fml.config.ModConfig;
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;

public final class ExposureComputerModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        ExposureComputerMod.init();
        NeoForgeConfigRegistry.INSTANCE.register(ExposureComputerMod.MOD_ID, ModConfig.Type.SERVER, Config.Server.SPEC);
    }
}
