package plume.summoner;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import plume.summoner.config.SummonerConfig;
import plume.summoner.handler.LivingDeathHandler;
import plume.summoner.network.NetworkHandler;

@Mod(PlumeSummoner.MOD_ID)
public class PlumeSummoner {
    public static final String MOD_ID = "plume_summoner";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PlumeSummoner() {
        SummonerConfig.register();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        NetworkHandler.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(new LivingDeathHandler());
    }
}