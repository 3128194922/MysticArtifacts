package com.uniye.mysticartifacts.client.deathscythe;

import java.util.UUID;

/** 服务端记录目标的客户端镜像，专门供描边渲染读取。 */
public final class DeathScytheTargetClientState {
    private static UUID targetUUID;

    private DeathScytheTargetClientState() {
    }

    public static void setTarget(UUID target) {
        targetUUID = target;
    }

    public static UUID targetUUID() {
        return targetUUID;
    }

    public static void clear() {
        targetUUID = null;
    }
}
