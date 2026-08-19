package de.saschat.cmos.registry;

import de.saschat.cmos.ExposureComputerMod;
import de.saschat.cmos.util.Location;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;


import io.github.mortuusars.exposure.world.camera.film.properties.FilmProperties;

public class ComponentRegistry {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT = DeferredRegister.create(ExposureComputerMod.MOD_ID, Registries.DATA_COMPONENT_TYPE);

    public static final RegistrySupplier<DataComponentType<Location>> LOCATION = DATA_COMPONENT.register(ResourceLocation.fromNamespaceAndPath(ExposureComputerMod.MOD_ID, "receiver_location"), () -> {
        return DataComponentType.<Location>builder().networkSynchronized(Location.STREAM_CODEC).persistent(Location.CODEC).build();
    });

    public static final RegistrySupplier<DataComponentType<FilmProperties>> FILM_PROPERTIES = DATA_COMPONENT.register("film_properties",
    ()->{return DataComponentType.<FilmProperties>builder().networkSynchronized(FilmProperties.STREAM_CODEC).persistent(FilmProperties.CODEC).build();});

    public static void register() {
        DATA_COMPONENT.register();
    }

}
