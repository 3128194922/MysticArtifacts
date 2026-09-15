package com.uniye.mysticartifacts.compat;

import com.uniye.mysticartifacts.event.CodexAnvilHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

public final class QuarkCompat {
    private QuarkCompat() {
    }

    public static void register() {
        if (ModList.get().isLoaded("quark")) {
            MinecraftForge.EVENT_BUS.register(new CodexAnvilHandler());
        }
    }
}
