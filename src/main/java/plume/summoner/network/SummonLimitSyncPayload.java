package plume.summoner.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import plume.summoner.PlumeSummoner;

/**
 * 服务端 → 客户端：同步「名单生物单次召唤数量限制」的真实状态。
 * 登录、死亡重生、以及每次切换后由服务端下发，客户端界面只做展示。
 *
 * @param enabled     开关是否启用
 * @param listedCount 服务端当前已加载的名单条目数（客户端不自己解析数据包，避免两边不一致）
 */
public record SummonLimitSyncPayload(boolean enabled, int listedCount) implements CustomPacketPayload {
    public static final Type<SummonLimitSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PlumeSummoner.MOD_ID, "summon_limit_sync"));

    public static final StreamCodec<ByteBuf, SummonLimitSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SummonLimitSyncPayload::enabled,
            ByteBufCodecs.VAR_INT, SummonLimitSyncPayload::listedCount,
            SummonLimitSyncPayload::new);

    @Override
    public Type<SummonLimitSyncPayload> type() {
        return TYPE;
    }
}
