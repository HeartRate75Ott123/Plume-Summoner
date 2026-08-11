package plume.summoner.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import plume.summoner.handler.SummonHandler;

import java.util.function.Supplier;

/**
 * 客户端 -> 服务器：请求召唤指定实体。
 */
public class SummonRequestMessage {
    private final ResourceLocation entityType;

    public SummonRequestMessage(ResourceLocation entityType) {
        this.entityType = entityType;
    }

    public static void encode(SummonRequestMessage msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.entityType);
    }

    public static SummonRequestMessage decode(FriendlyByteBuf buf) {
        return new SummonRequestMessage(buf.readResourceLocation());
    }

    public static void handle(SummonRequestMessage msg, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> SummonHandler.onSummonRequest(msg.entityType, context));
        context.setPacketHandled(true);
    }
}