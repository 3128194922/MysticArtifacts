package com.uniye.mysticartifacts.network;

import com.uniye.mysticartifacts.client.network.ClientPacketHandler;
import com.uniye.mysticartifacts.sculk.SculkSymbioteServerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** 服务端批量同步一次 tick 内抵达佩戴者的震动位置。 */
public final class SculkSymbioteVibrationPacket {
    private static final int MAX_MARKERS = 256;
    private final List<Marker> markers;

    public SculkSymbioteVibrationPacket(List<Marker> markers) {
        this.markers = List.copyOf(markers.subList(0, Math.min(MAX_MARKERS, markers.size())));
    }

    public static void encode(SculkSymbioteVibrationPacket message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.markers.size());
        for (Marker marker : message.markers) {
            buf.writeDouble(marker.position().x);
            buf.writeDouble(marker.position().y);
            buf.writeDouble(marker.position().z);
            buf.writeVarInt(marker.frequency());
            buf.writeBoolean(marker.sourceId() != null);
            if (marker.sourceId() != null) buf.writeUUID(marker.sourceId());
        }
    }

    public static SculkSymbioteVibrationPacket decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_MARKERS) {
            throw new IllegalArgumentException("Invalid sculk symbiote marker count: " + count);
        }
        List<Marker> markers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Vec3 position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            int frequency = buf.readVarInt();
            UUID sourceId = buf.readBoolean() ? buf.readUUID() : null;
            markers.add(new Marker(position, frequency, sourceId));
        }
        return new SculkSymbioteVibrationPacket(markers);
    }

    public static void handle(SculkSymbioteVibrationPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT, () -> () -> ClientPacketHandler.handleSculkSymbioteVibrations(message.markers)));
        context.setPacketHandled(true);
    }

    public static void sendTo(ServerPlayer player, List<SculkSymbioteServerHandler.MarkerData> markers) {
        List<Marker> packetMarkers = markers.stream()
                .map(marker -> new Marker(marker.position(), marker.frequency(), marker.sourceId()))
                .toList();
        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new SculkSymbioteVibrationPacket(packetMarkers));
    }

    public List<Marker> markers() {
        return markers;
    }

    public record Marker(Vec3 position, int frequency, UUID sourceId) {
    }
}
