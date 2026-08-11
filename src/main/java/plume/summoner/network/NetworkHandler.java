package plume.summoner.network;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import plume.summoner.PlumeSummoner;
import plume.summoner.client.PlumeSummonerClient;
import plume.summoner.handler.SummonHandler;

public final class NetworkHandler {
    private NetworkHandler() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NetworkHandler::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PlumeSummoner.MOD_ID).versioned("1").optional();
        registrar.playToServer(SummonRequestPayload.TYPE, SummonRequestPayload.STREAM_CODEC,
                NetworkHandler::handleSummonRequest);
        registrar.playToClient(UnlockSyncPayload.TYPE, UnlockSyncPayload.STREAM_CODEC,
                NetworkHandler::handleUnlockSync);
    }

    private static void handleSummonRequest(SummonRequestPayload payload, IPayloadContext context) {
        SummonHandler.onSummonRequest(payload, context);
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
}