package plume.summoner.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import plume.summoner.PlumeSummoner;

public record SummonRequestPayload(ResourceLocation entityType) implements CustomPacketPayload {
    public static final Type<SummonRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PlumeSummoner.MOD_ID, "summon_request"));

    public static final StreamCodec<ByteBuf, SummonRequestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, SummonRequestPayload::entityType,
                    SummonRequestPayload::new);

    @Override
    public Type<SummonRequestPayload> type() {
        return TYPE;
    }
}