package plume.summoner.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import plume.summoner.PlumeSummoner;

import java.util.List;

public record UnlockSyncPayload(List<ResourceLocation> unlockedTypes) implements CustomPacketPayload {
    public static final Type<UnlockSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PlumeSummoner.MOD_ID, "unlock_sync"));

    public static final StreamCodec<ByteBuf, UnlockSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), UnlockSyncPayload::unlockedTypes,
                    UnlockSyncPayload::new);

    @Override
    public Type<UnlockSyncPayload> type() {
        return TYPE;
    }
}