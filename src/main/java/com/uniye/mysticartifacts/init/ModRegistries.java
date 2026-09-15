package com.uniye.mysticartifacts.init;

import com.uniye.mysticartifacts.revolver.RevolverRegistries;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ModRegistries {
    private ModRegistries() {
    }

    public static void register(IEventBus eventBus) {
        ModItems.register(eventBus);
        ModEntities.register(eventBus);
        ModSounds.register(eventBus);
        ModCreativeModTabs.register(eventBus);
        RevolverRegistries.MENUS.register(eventBus);
        RevolverRegistries.SOUNDS.register(eventBus);
    }
}
