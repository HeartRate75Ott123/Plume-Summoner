package plume.summoner.network;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import plume.summoner.PlumeSummoner;
import plume.summoner.client.PlumeSummonerClient;
import plume.summoner.client.SummonerUiPrefs;
import plume.summoner.data.PlayerSummonDataProvider;
import plume.summoner.data.SummonLimits;
import plume.summoner.handler.SummonHandler;

public final class NetworkHandler {
    private NetworkHandler() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NetworkHandler::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        // 协议版本随 payload 集合变更递增；optional() 允许对端缺少本模组的通道时不硬崩
        PayloadRegistrar registrar = event.registrar(PlumeSummoner.MOD_ID).versioned("2").optional();
        registrar.playToServer(SummonRequestPayload.TYPE, SummonRequestPayload.STREAM_CODEC,
                NetworkHandler::handleSummonRequest);
        registrar.playToServer(SummonLimitTogglePayload.TYPE, SummonLimitTogglePayload.STREAM_CODEC,
                NetworkHandler::handleSummonLimitToggle);
        registrar.playToClient(UnlockSyncPayload.TYPE, UnlockSyncPayload.STREAM_CODEC,
                NetworkHandler::handleUnlockSync);
        registrar.playToClient(SummonLimitSyncPayload.TYPE, SummonLimitSyncPayload.STREAM_CODEC,
                NetworkHandler::handleSummonLimitSync);
    }

    private static void handleSummonRequest(SummonRequestPayload payload, IPayloadContext context) {
        SummonHandler.onSummonRequest(payload, context);
    }

    private static void handleSummonLimitToggle(SummonLimitTogglePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ((PlayerSummonDataProvider) player).setSummonLimitEnabled(payload.enabled());
            // 以服务端写入的结果回发，客户端界面据此纠正显示
            PacketDistributor.sendToPlayer(player,
                    new SummonLimitSyncPayload(payload.enabled(), SummonLimits.listedCount()));
        });
    }

    private static void handleUnlockSync(UnlockSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.flow().isClientbound()) {
                return;
            }
            PlumeSummonerClient.UNLOCKED_TYPES.clear();
            for (ResourceLocation id : payload.unlockedTypes()) {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
                if (type != null) {
                    PlumeSummonerClient.UNLOCKED_TYPES.add(type);
                }
            }
        });
    }

    private static void handleSummonLimitSync(SummonLimitSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.flow().isClientbound()) {
                return;
            }
            SummonerUiPrefs.applySummonLimitSync(payload.enabled(), payload.listedCount());
        });
    }
}
