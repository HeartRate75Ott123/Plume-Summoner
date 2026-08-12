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
import plume.summoner.client.favorites.SummonerFavorites;
import plume.summoner.screen.SummonEntitiesData;
import plume.summoner.screen.SummonMenuScreen;

import java.util.LinkedHashSet;
import java.util.Set;

public final class PlumeSummonerClient {
    public static final String KEY_CATEGORY = "key.categories.plume_summoner";

    public static final KeyMapping OPEN_SUMMON_MENU = new KeyMapping(
            "key.plume_summoner.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            KEY_CATEGORY);

    // LinkedHashSet 保持解锁顺序（旧→新），供菜单"新解锁的排最前"排序使用
    public static final Set<EntityType<?>> UNLOCKED_TYPES = new LinkedHashSet<>();

    private PlumeSummonerClient() {
    }

    @EventBusSubscriber(modid = PlumeSummoner.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(OPEN_SUMMON_MENU);
        }

        @SubscribeEvent
        public static void onConfigReload(net.neoforged.fml.event.config.ModConfigEvent.Reloading event) {
            // 配置文件被修改（含外部编辑）后重载实体列表，黑名单立即生效
            if (event.getConfig().getModId().equals(PlumeSummoner.MOD_ID)) {
                SummonEntitiesData.reloadAsync();
            }
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

        @SubscribeEvent
        public static void onJoinWorld(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
            // 进入世界（单机/联机）时按当前存档加载收藏、清空上次搜索内容
            SummonerFavorites.loadForCurrentWorld();
            SummonMenuScreen.resetLastSearch();
        }
    }
}