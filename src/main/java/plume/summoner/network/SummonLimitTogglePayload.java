package plume.summoner.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import plume.summoner.PlumeSummoner;

/**
 * 客户端 → 服务端：切换「名单生物单次召唤数量限制」开关。
 * 服务端以自身记录的玩家数据为准，写入后回发 {@link SummonLimitSyncPayload}。
 */
public record SummonLimitTogglePayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<SummonLimitTogglePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PlumeSummoner.MOD_ID, "summon_limit_toggle"));

    public static final StreamCodec<ByteBuf, SummonLimitTogglePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SummonLimitTogglePayload::enabled,
            SummonLimitTogglePayload::new);

    @Override
    public Type<SummonLimitTogglePayload> type() {
        return TYPE;
    }
}
