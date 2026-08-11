package plume.summoner.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import plume.summoner.PlumeSummoner;
import plume.summoner.screen.SummonMenuScreen;

import java.util.HashSet;
import java.util.List;
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

    public static void applyUnlockSync(List<ResourceLocation> ids) {
        UNLOCKED_TYPES.clear();
        for (ResourceLocation id : ids) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
            if (type != null) {
                UNLOCKED_TYPES.add(type);
            }
        }
    }

    @Mod.EventBusSubscriber(modid = PlumeSummoner.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(OPEN_SUMMON_MENU);
        }
    }

    @Mod.EventBusSubscriber(modid = PlumeSummoner.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class GameEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
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