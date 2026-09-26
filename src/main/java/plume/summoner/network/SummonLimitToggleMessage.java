package plume.summoner.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import plume.summoner.data.PlayerSummonDataProvider;
import plume.summoner.data.SummonLimits;

import java.util.function.Supplier;

/**
 * 客户端 -> 服务器：切换「名单生物单次召唤数量限制」开关。
 * 服务端以自身记录的玩家数据为准，写入后回发 {@link SummonLimitSyncMessage}。
 */
public class SummonLimitToggleMessage {
    private final boolean enabled;

    public SummonLimitToggleMessage(boolean enabled) {
        this.enabled = enabled;
    }

    public static void encode(SummonLimitToggleMessage msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.enabled);
    }

    public static SummonLimitToggleMessage decode(FriendlyByteBuf buf) {
        return new SummonLimitToggleMessage(buf.readBoolean());
    }

    public static void handle(SummonLimitToggleMessage msg, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            ((PlayerSummonDataProvider) player).setSummonLimitEnabled(msg.enabled);
            // 以服务端写入的结果回发，客户端界面据此纠正显示
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SummonLimitSyncMessage(msg.enabled, SummonLimits.listedCount()));
        });
        context.setPacketHandled(true);
    }
}
