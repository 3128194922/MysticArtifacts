package com.uniye.mysticartifacts.kubejs;

import com.uniye.mysticartifacts.event.KatanaBlockEvent;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraftforge.common.MinecraftForge;

/** Optional KubeJS bridge, loaded through kubejs.plugins.txt only when KubeJS is installed. */
public class KubeJSKatanaPlugin extends KubeJSPlugin {
    private static boolean forgeListenerRegistered;

    public KubeJSKatanaPlugin() {
        registerForgeListener();
    }

    @Override
    public void registerEvents() {
        KatanaKubeEvents.GROUP.register();
    }

    private static synchronized void registerForgeListener() {
        if (forgeListenerRegistered) return;
        MinecraftForge.EVENT_BUS.addListener(KubeJSKatanaPlugin::onKatanaBlocked);
        forgeListenerRegistered = true;
    }

    private static void onKatanaBlocked(KatanaBlockEvent event) {
        if (event.getBlocker().getServer() == null) return;
        KatanaKubeEvents.BLOCKED.post(
                ScriptType.SERVER,
                new KatanaBlockEventJS(event.getBlocker().getServer(), event)
        );
    }
}
