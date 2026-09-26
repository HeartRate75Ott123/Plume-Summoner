package plume.summoner.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import plume.summoner.PlumeSummoner;

public final class NetworkHandler {
    // SummonRequestMessage 增加 count 字段后协议不兼容，bump 到 2；
    // 1.20.1 迁移新增名单限制开关的两个消息，bump 到 3
    public static final String PROTOCOL_VERSION = "3";

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
        CHANNEL.registerMessage(id++, SummonLimitToggleMessage.class,
                SummonLimitToggleMessage::encode, SummonLimitToggleMessage::decode, SummonLimitToggleMessage::handle);
        CHANNEL.registerMessage(id++, SummonLimitSyncMessage.class,
                SummonLimitSyncMessage::encode, SummonLimitSyncMessage::decode, SummonLimitSyncMessage::handle);
    }
}
