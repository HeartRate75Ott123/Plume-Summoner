package plume.summoner.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import plume.summoner.handler.SummonHandler;

import java.util.function.Supplier;

/**
 * 客户端 -> 服务器：请求召唤指定实体，count 为单次召唤数量。
 */
public class SummonRequestMessage {
    private final ResourceLocation entityType;
    private final int count;

    public SummonRequestMessage(ResourceLocation entityType, int count) {
        this.entityType = entityType;
        this.count = count;
    }

    public static void encode(SummonRequestMessage msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.entityType);
        buf.writeInt(msg.count);
    }

    public static SummonRequestMessage decode(FriendlyByteBuf buf) {
        return new SummonRequestMessage(buf.readResourceLocation(), buf.readInt());
    }

    public static void handle(SummonRequestMessage msg, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> SummonHandler.onSummonRequest(msg.entityType, msg.count, context));
        context.setPacketHandled(true);
    }
}