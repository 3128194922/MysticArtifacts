package com.uniye.mysticartifacts.client.deathscythe;

import com.uniye.mysticartifacts.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class DeathScytheClientState {
    public static final int MAX_ACTIVE = 16;
    public static final int MAX_DURATION_TICKS = 60;
    private static final DeathScytheClientState CLIENT = new DeathScytheClientState();

    private final List<Slash> slashes = new ArrayList<>();
    private ClientLevel currentLevel;

    public static void accept(Vec3 origin, Vec3 target, int sequence, int durationTicks, long seed) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;
        CLIENT.useLevel(level);
        CLIENT.addSlash(origin, target, sequence, durationTicks, seed,
                level.getGameTime(), Config.DeathScytheSlashEffectTicks);
    }

    public static List<Slash> snapshot(ClientLevel level) {
        CLIENT.useLevel(level);
        return CLIENT.snapshot(level.getGameTime());
    }

    public boolean addSlash(Vec3 origin, Vec3 target, int sequence, int durationTicks,
                            long seed, long startTick, int configuredLifetime) {
        if (!validPoint(origin) || !validPoint(target) || sequence <= 0
                || durationTicks <= 0 || durationTicks > MAX_DURATION_TICKS
                || configuredLifetime <= 0) {
            return false;
        }
        trim(startTick);
        if (slashes.size() == MAX_ACTIVE) slashes.remove(0);
        int duration = Math.min(durationTicks, Math.min(configuredLifetime, MAX_DURATION_TICKS));
        slashes.add(new Slash(origin, target, sequence, duration, seed, startTick));
        return true;
    }

    public List<Slash> snapshot(long currentTick) {
        trim(currentTick);
        return List.copyOf(slashes);
    }

    private void useLevel(ClientLevel level) {
        if (currentLevel != level) {
            slashes.clear();
            currentLevel = level;
        }
    }

    private void trim(long currentTick) {
        slashes.removeIf(slash -> currentTick < slash.startTick()
                || currentTick - slash.startTick() >= slash.durationTicks());
    }

    private static boolean validPoint(Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y)
                && Double.isFinite(point.z)
                && Math.abs(point.x) <= 30_000_000.0D
                && Math.abs(point.y) <= 30_000_000.0D
                && Math.abs(point.z) <= 30_000_000.0D;
    }

    public record Slash(Vec3 origin, Vec3 target, int sequence,
                        int durationTicks, long seed, long startTick) {
    }
}
