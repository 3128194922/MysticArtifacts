package com.uniye.mysticartifacts.client.flag;

import com.uniye.mysticartifacts.MysticArtifacts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FlagSpearClientState {
    public static final long LIFETIME_TICKS = 10L;
    public static final int MAX_TRAILS = 32;
    private static final List<Trail> TRAILS = new ArrayList<>();

    private FlagSpearClientState() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            TRAILS.clear();
            return;
        }
        trim(minecraft.level.getGameTime());
    }

    public static void accept(Vec3 center, float yaw, long seed) {
        if (!isFinite(center) || !Float.isFinite(yaw)) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        if (TRAILS.size() >= MAX_TRAILS) {
            TRAILS.remove(0);
        }
        TRAILS.add(new Trail(center, yaw, seed, minecraft.level.getGameTime()));
    }

    public static List<Trail> snapshot(ClientLevel level) {
        trim(level.getGameTime());
        return new ArrayList<>(TRAILS);
    }

    private static void trim(long currentTick) {
        TRAILS.removeIf(trail -> currentTick - trail.tick() >= LIFETIME_TICKS);
    }

    private static boolean isFinite(Vec3 value) {
        return value != null
                && Double.isFinite(value.x)
                && Double.isFinite(value.y)
                && Double.isFinite(value.z);
    }

    public record Trail(Vec3 center, float yaw, long seed, long tick) {
    }
}
