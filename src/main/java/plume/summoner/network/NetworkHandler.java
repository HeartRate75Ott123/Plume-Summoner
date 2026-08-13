package plume.summoner.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import plume.summoner.PlumeSummoner;

public final class NetworkHandler {
    // SummonRequestMessage 增加 count 字段后协议不兼容，bump 到 2
    public static final String PROTOCOL_VERSION = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PlumeSummoner.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private NetworkHandler() {
    }

    public static void register(IEventBus modEventBus) {
        int id = 0;
        CHANNEL.registerMessage(id++, SummonRequestMessage.class,
                SummonRequestMessage::encode, SummonRequestMessage::decode, SummonRequestMessage::handle);
        CHANNEL.registerMessage(id++, UnlockSyncMessage.class,
                UnlockSyncMessage::encode, UnlockSyncMessage::decode, UnlockSyncMessage::handle);
    }
}