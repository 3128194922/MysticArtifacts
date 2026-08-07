package com.uniye.mysticartifacts.network;

import com.uniye.mysticartifacts.client.event.AllSeeingEyeClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class PlayerListPacket {

    private final List<Entry> players;

    public record Entry(UUID uuid, String name) {
    }

    public PlayerListPacket(List<Entry> players) {
        this.players = players;
    }

    public static void encode(PlayerListPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.players.size());
        for (Entry entry : msg.players) {
            buf.writeUUID(entry.uuid());
            buf.writeUtf(entry.name());
        }
    }

    public static PlayerListPacket decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<Entry> players = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            players.add(new Entry(buf.readUUID(), buf.readUtf()));
        }
        return new PlayerListPacket(players);
    }

    public static void handle(PlayerListPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AllSeeingEyeClientHandler.handlePlayerList(msg));
        });
        ctx.get().setPacketHandled(true);
    }

    public static void sendTo(ServerPlayer player, List<Entry> players) {
        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new PlayerListPacket(players));
    }

    public List<Entry> getPlayers() {
        return players;
    }
}
