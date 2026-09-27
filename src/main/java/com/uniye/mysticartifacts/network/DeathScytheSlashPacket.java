package com.uniye.mysticartifacts.network;

import com.uniye.mysticartifacts.client.deathscythe.DeathScytheClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class DeathScytheSlashPacket {
    private final Vec3 origin;
    private final Vec3 target;
    private final int sequence;
    private final int durationTicks;
    private final long seed;

    public DeathScytheSlashPacket(Vec3 origin, Vec3 target, int sequence,
                                  int durationTicks, long seed) {
        this.origin = origin;
        this.target = target;
        this.sequence = sequence;
        this.durationTicks = durationTicks;
        this.seed = seed;
    }

    public static void encode(DeathScytheSlashPacket message, FriendlyByteBuf buffer) {
        buffer.writeDouble(message.origin.x);
        buffer.writeDouble(message.origin.y);
        buffer.writeDouble(message.origin.z);
        buffer.writeDouble(message.target.x);
        buffer.writeDouble(message.target.y);
        buffer.writeDouble(message.target.z);
        buffer.writeInt(message.sequence);
        buffer.writeInt(message.durationTicks);
        buffer.writeLong(message.seed);
    }

    public static DeathScytheSlashPacket decode(FriendlyByteBuf buffer) {
        return new DeathScytheSlashPacket(
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                buffer.readInt(), buffer.readInt(), buffer.readLong());
    }

    public static void handle(DeathScytheSlashPacket message,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (!validPoint(message.origin) || !validPoint(message.target)
                || message.sequence <= 0 || message.durationTicks <= 0
                || message.durationTicks > DeathScytheClientState.MAX_DURATION_TICKS) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> DeathScytheClientState.accept(message.origin, message.target,
                        message.sequence, message.durationTicks, message.seed)));
        context.setPacketHandled(true);
    }

    private static boolean validPoint(Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y)
                && Double.isFinite(point.z)
                && Math.abs(point.x) <= 30_000_000.0D
                && Math.abs(point.y) <= 30_000_000.0D
                && Math.abs(point.z) <= 30_000_000.0D;
    }
}
