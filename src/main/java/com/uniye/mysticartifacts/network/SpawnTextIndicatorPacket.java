package com.uniye.mysticartifacts.network;

import com.uniye.mysticartifacts.client.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class SpawnTextIndicatorPacket {
    private final double x;
    private final double y;
    private final double z;
    private final String text;
    private final int color;
    private final boolean outline;
    private final int outlineColor;
    private final float size;

    public SpawnTextIndicatorPacket(double x, double y, double z, String text, int color, boolean outline, int outlineColor, float size) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.text = text;
        this.color = color;
        this.outline = outline;
        this.outlineColor = outlineColor;
        this.size = size;
    }

    public static void encode(SpawnTextIndicatorPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeUtf(msg.text);
        buf.writeInt(msg.color);
        buf.writeBoolean(msg.outline);
        buf.writeInt(msg.outlineColor);
        buf.writeFloat(msg.size);
    }

    public static SpawnTextIndicatorPacket decode(FriendlyByteBuf buf) {
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        String text = buf.readUtf();
        int color = buf.readInt();
        boolean outline = buf.readBoolean();
        int outlineColor = buf.readInt();
        float size = buf.readFloat();
        return new SpawnTextIndicatorPacket(x, y, z, text, color, outline, outlineColor, size);
    }

    public static void handle(SpawnTextIndicatorPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSpawnTextIndicator(msg));
        });
        ctx.get().setPacketHandled(true);
    }

    public static void sendTo(ServerPlayer player, Vec3 pos, String text, int color, boolean outline, int outlineColor, float size) {
        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new SpawnTextIndicatorPacket(pos.x, pos.y, pos.z, text, color, outline, outlineColor, size));
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public String getText() {
        return text;
    }

    public int getColor() {
        return color;
    }

    public boolean isOutline() {
        return outline;
    }

    public int getOutlineColor() {
        return outlineColor;
    }

    public float getSize() {
        return size;
    }
}
