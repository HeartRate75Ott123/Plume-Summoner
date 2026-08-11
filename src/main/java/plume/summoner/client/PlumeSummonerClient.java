package plume.summoner.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import plume.summoner.PlumeSummoner;
import plume.summoner.screen.SummonMenuScreen;

import java.util.HashSet;
import java.util.Set;

public final class PlumeSummonerClient {
    public static final String KEY_CATEGORY = "key.categories.plume_summoner";

    public static final KeyMapping OPEN_SUMMON_MENU = new KeyMapping(
            "key.plume_summoner.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            KEY_CATEGORY);

    public static final Set<EntityType<?>> UNLOCKED_TYPES = new HashSet<>();

    private PlumeSummonerClient() {
    }

    @EventBusSubscriber(modid = PlumeSummoner.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(OPEN_SUMMON_MENU);
        }
    }

    @EventBusSubscriber(modid = PlumeSummoner.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
    public static class GameEvents {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return;
            }
            while (OPEN_SUMMON_MENU.consumeClick()) {
                if (minecraft.screen instanceof SummonMenuScreen) {
                    minecraft.setScreen(null);
                } else {
                    minecraft.setScreen(new SummonMenuScreen());
                }
            }
        }
    }
}