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

    public static void sendUnlockSync(ServerPlayer player) {
        PlayerSummonDataProvider data = (PlayerSummonDataProvider) player;
        List<ResourceLocation> ids = data.getSummonUnlockedTypes().stream()
                .map(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type))
                .toList();
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new UnlockSyncMessage(ids));
    }
}