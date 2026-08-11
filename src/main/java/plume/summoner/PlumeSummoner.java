package plume.summoner;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import plume.summoner.config.SummonerConfig;
import plume.summoner.handler.LivingDeathHandler;
import plume.summoner.network.NetworkHandler;

@Mod(PlumeSummoner.MOD_ID)
public class PlumeSummoner {
    public static final String MOD_ID = "plume_summoner";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PlumeSummoner(IEventBus modEventBus, ModContainer modContainer) {
        SummonerConfig.register(modContainer);
        NetworkHandler.register(modEventBus);
        NeoForge.EVENT_BUS.register(new LivingDeathHandler());
    }
}