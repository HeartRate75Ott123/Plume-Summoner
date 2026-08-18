package plume.summoner.handler;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import plume.summoner.config.SummonerConfig;
import plume.summoner.data.PlayerSummonDataProvider;
import plume.summoner.network.NetworkHandler;
import plume.summoner.network.UnlockSyncMessage;

import java.util.List;

public class LivingDeathHandler {
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        net.minecraft.world.entity.LivingEntity victim = event.getEntity();
        if (victim instanceof Player) {
            return;
        }
        if (!(victim instanceof Mob mob)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }

        PlayerSummonDataProvider data = (PlayerSummonDataProvider) player;
        // 白名单/黑名单排除的实体：击杀不计数、不触发解锁提示（菜单中也不显示）
        if (!SummonerConfig.isAllowed(mob.getType())) {
            return;
        }
        if (data.isSummonUnlocked(mob.getType())) {
            return;
        }
        // 达到配置的击杀次数才解锁（默认 1）
        if (data.onMobKilled(mob.getType())) {
            player.displayClientMessage(
                    Component.translatable("message.plume_summoner.unlocked_mob", mob.getType().getDescription()),
                    true);
            sendUnlockSync(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendUnlockSync(player);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        // 玩家死亡重生时 PlayerList.respawn 创建全新 ServerPlayer 实例，
        // 不经过 readAdditionalSaveData，mixin 字段会丢空；
        // 在 Clone 事件（restoreFrom 末尾触发）把击杀计数从旧实例拷贝过来。
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) {
            return;
        }
        PlayerSummonDataProvider oldData = (PlayerSummonDataProvider) event.getOriginal();
        PlayerSummonDataProvider newData = (PlayerSummonDataProvider) newPlayer;
        newData.setKillCounts(oldData.getKillCounts());
        // 重生后客户端仍是死前快照，重发同步让界面刷新为真实状态
        sendUnlockSync(newPlayer);
    }

    public static void sendUnlockSync(ServerPlayer player) {
        PlayerSummonDataProvider data = (PlayerSummonDataProvider) player;
        List<ResourceLocation> ids = data.getSummonUnlockedTypes().stream()
                .map(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type))
                .toList();
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new UnlockSyncMessage(ids));
    }
}