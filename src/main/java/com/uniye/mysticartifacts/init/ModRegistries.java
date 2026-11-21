package com.uniye.mysticartifacts.init;

import net.minecraftforge.eventbus.api.IEventBus;

public final class ModRegistries {
    private ModRegistries() {
    }

    public static void register(IEventBus eventBus) {
        ModItems.register(eventBus);
        ModEntities.register(eventBus);
        ModSounds.register(eventBus);
        ModCreativeModTabs.register(eventBus);
    }
}
