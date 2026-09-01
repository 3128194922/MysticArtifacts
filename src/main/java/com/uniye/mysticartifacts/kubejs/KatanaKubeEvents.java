package com.uniye.mysticartifacts.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

/** KubeJS event group for Muramasa block results. */
public interface KatanaKubeEvents {
    EventGroup GROUP = EventGroup.of("KatanaEvents");

    /** Fires after every successful Muramasa block; event.perfect marks a perfect block. */
    EventHandler BLOCKED = GROUP.server("blocked", () -> KatanaBlockEventJS.class);
}
