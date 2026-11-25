package com.uniye.mysticartifacts.client.lightning;

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
public final class LightningBottleClientState {
    public static final long LIFETIME_TICKS = 8L;
    public static final long FIELD_LIFETIME_TICKS = 8L;
    private static final int MAX_CHAINS = 64;
    private static final List<Chain> CHAINS = new ArrayList<>();
    private static final List<Field> FIELDS = new ArrayList<>();

    private LightningBottleClientState() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            CHAINS.clear();
            FIELDS.clear();
            return;
        }
        trim(minecraft.level.getGameTime());
    }

    public static void accept(List<Vec3> path) {
        if (path.size() < 2) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        if (CHAINS.size() >= MAX_CHAINS) {
            CHAINS.remove(0);
        }
        CHAINS.add(new Chain(List.copyOf(path), minecraft.level.getGameTime()));
    }

    public static List<Chain> snapshot(ClientLevel level) {
        trim(level.getGameTime());
        return new ArrayList<>(CHAINS);
    }

    public static void acceptField(Vec3 center) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        if (FIELDS.size() >= MAX_CHAINS) FIELDS.remove(0);
        FIELDS.add(new Field(center, minecraft.level.getGameTime()));
    }

    public static List<Field> fieldSnapshot(ClientLevel level) {
        trim(level.getGameTime());
        return new ArrayList<>(FIELDS);
    }

    private static void trim(long currentTick) {
        CHAINS.removeIf(chain -> currentTick - chain.tick() >= LIFETIME_TICKS);
        FIELDS.removeIf(field -> currentTick - field.tick() >= FIELD_LIFETIME_TICKS);
    }

    public record Chain(List<Vec3> path, long tick) {
    }

    public record Field(Vec3 center, long tick) {
    }
}
