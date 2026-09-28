package com.uniye.mysticartifacts.network;

import com.uniye.mysticartifacts.client.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public final class FlagSpearTrailPacket {
    private final Vec3 center;
    private final float yaw;
    private final long seed;
    private final boolean valid;

    public FlagSpearTrailPacket(Vec3 center, float yaw, long seed) {
        this.center = center;
        this.yaw = yaw;
        this.seed = seed;
        this.valid = isFinite(center) && Float.isFinite(yaw);
    }

    public static void encode(FlagSpearTrailPacket message, FriendlyByteBuf buffer) {
        buffer.writeDouble(message.center.x);
        buffer.writeDouble(message.center.y);
        buffer.writeDouble(message.center.z);
        buffer.writeFloat(message.yaw);
        buffer.writeLong(message.seed);
    }

    public static FlagSpearTrailPacket decode(FriendlyByteBuf buffer) {
        return new FlagSpearTrailPacket(
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                buffer.readFloat(),
                buffer.readLong()
        );
    }

    public static void handle(FlagSpearTrailPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (!message.valid) {
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleFlagSpearTrail(message.center, message.yaw, message.seed)
        ));
        context.setPacketHandled(true);
    }

    public static void send(ServerLevel level, Vec3 center, float yaw, long seed) {
        NetworkHandler.INSTANCE.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        center.x, center.y, center.z, 96.0D, level.dimension())),
                new FlagSpearTrailPacket(center, yaw, seed)
        );
    }

    private static boolean isFinite(Vec3 value) {
        return value != null
                && Double.isFinite(value.x)
                && Double.isFinite(value.y)
                && Double.isFinite(value.z);
    }
}
