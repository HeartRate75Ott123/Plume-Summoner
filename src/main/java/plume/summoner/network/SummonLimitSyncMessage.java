package plume.summoner.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import plume.summoner.client.SummonerUiPrefs;

import java.util.function.Supplier;

/**
 * 服务器 -> 客户端：同步「名单生物单次召唤数量限制」的真实状态。
 * 登录、死亡重生、以及每次切换后由服务端下发，客户端界面只做展示。
 * 名单条目数一并下发，客户端不必自己解析数据包。
 */
public class SummonLimitSyncMessage {
    private final boolean enabled;
    private final int listedCount;

    public SummonLimitSyncMessage(boolean enabled, int listedCount) {
        this.enabled = enabled;
        this.listedCount = listedCount;
    }

    public static void encode(SummonLimitSyncMessage msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.enabled);
        buf.writeVarInt(msg.listedCount);
    }

    public static SummonLimitSyncMessage decode(FriendlyByteBuf buf) {
        return new SummonLimitSyncMessage(buf.readBoolean(), buf.readVarInt());
    }

    public static void handle(SummonLimitSyncMessage msg, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> SummonerUiPrefs.applySummonLimitSync(msg.enabled, msg.listedCount));
        context.setPacketHandled(true);
    }
}
