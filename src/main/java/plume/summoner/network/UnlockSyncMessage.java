package plume.summoner.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import plume.summoner.client.PlumeSummonerClient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 服务器 -> 客户端：同步已解锁的实体列表。
 */
public class UnlockSyncMessage {
    private final List<ResourceLocation> unlockedTypes;

    public UnlockSyncMessage(List<ResourceLocation> unlockedTypes) {
        this.unlockedTypes = unlockedTypes;
    }

    public static void encode(UnlockSyncMessage msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.unlockedTypes.size());
        for (ResourceLocation id : msg.unlockedTypes) {
            buf.writeResourceLocation(id);
        }
    }

    public static UnlockSyncMessage decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<ResourceLocation> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ids.add(buf.readResourceLocation());
        }
        return new UnlockSyncMessage(ids);
    }

    public static void handle(UnlockSyncMessage msg, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> PlumeSummonerClient.applyUnlockSync(msg.unlockedTypes));
        context.setPacketHandled(true);
    }
}