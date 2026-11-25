package com.uniye.mysticartifacts.network;

import com.uniye.mysticartifacts.client.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class LightningBottlePacket {
    private static final int MAX_PATH_POINTS = 66;

    private final List<Vec3> path;
    private final Vec3 fieldCenter;

    public LightningBottlePacket(List<Vec3> path) {
        this(path, null);
    }

    public LightningBottlePacket(List<Vec3> path, Vec3 fieldCenter) {
        this.path = List.copyOf(path);
        this.fieldCenter = fieldCenter;
    }

    public static void encode(LightningBottlePacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.path.size());
        for (Vec3 point : message.path) {
            buffer.writeDouble(point.x);
            buffer.writeDouble(point.y);
            buffer.writeDouble(point.z);
        }
        buffer.writeBoolean(message.fieldCenter != null);
        if (message.fieldCenter != null) {
            buffer.writeDouble(message.fieldCenter.x);
            buffer.writeDouble(message.fieldCenter.y);
            buffer.writeDouble(message.fieldCenter.z);
        }
    }

    public static LightningBottlePacket decode(FriendlyByteBuf buffer) {
        int pointCount = Math.min(MAX_PATH_POINTS, Math.max(0, buffer.readVarInt()));
        List<Vec3> path = new ArrayList<>(pointCount);
        for (int i = 0; i < pointCount; i++) {
            path.add(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
        }
        Vec3 fieldCenter = buffer.readBoolean()
                ? new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble())
                : null;
        return new LightningBottlePacket(path, fieldCenter);
    }

    public static void handle(LightningBottlePacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleLightningBottle(message.path, message.fieldCenter)
        ));
        context.setPacketHandled(true);
    }

    public static void send(ServerLevel level, Vec3 center, List<Vec3> path) {
        send(level, center, path, null);
    }

    public static void send(ServerLevel level, Vec3 center, List<Vec3> path, Vec3 fieldCenter) {
        NetworkHandler.INSTANCE.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        center.x, center.y, center.z, 96.0D, level.dimension())),
                new LightningBottlePacket(path, fieldCenter)
        );
    }
}
