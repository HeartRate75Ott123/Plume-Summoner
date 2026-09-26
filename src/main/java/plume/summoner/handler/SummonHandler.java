package plume.summoner.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import plume.summoner.PlumeSummoner;
import plume.summoner.config.SummonerConfig;
import plume.summoner.data.PlayerSummonDataProvider;
import plume.summoner.data.SummonLimits;

public final class SummonHandler {
    private static final String OWNER_TAG = "plume_summoner_owner";

    private SummonHandler() {
    }

    public static void onSummonRequest(ResourceLocation entityType, int count, NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityType);
        if (type == null || !((PlayerSummonDataProvider) player).isSummonUnlocked(type)) {
            return;
        }
        int clamped = Math.max(1, Math.min(count, SummonerConfig.MAX_SUMMON_COUNT.get()));
        // 名单生物单次召唤数量限制：玩家开关开启且该生物在名单内时，再按名单上限截断
        int listedLimit = SummonLimits.limitOf(type);
        if (listedLimit > 0 && ((PlayerSummonDataProvider) player).isSummonLimitEnabled() && clamped > listedLimit) {
            clamped = listedLimit;
            player.displayClientMessage(
                    Component.translatable("message.plume_summoner.summon_limit_clamped", clamped), true);
        }
        int summoned = 0;
        for (int i = 0; i < clamped; i++) {
            if (summon(player, type)) {
                summoned++;
            }
        }
        if (summoned > 0) {
            // 多次召唤不刷屏：只发一条汇总消息
            player.displayClientMessage(
                    summoned > 1
                            ? Component.translatable("message.plume_summoner.summoned_count",
                            summoned, type.getDescription())
                            : Component.translatable("message.plume_summoner.summoned",
                            type.getDescription()),
                    false);
        }
    }

    /**
     * 尝试召唤一只实体，成功返回 true。
     */
    public static boolean summon(ServerPlayer player, EntityType<?> type) {
        ServerLevel level = player.serverLevel();

        Entity entity = type.create(level);
        if (entity == null) {
            PlumeSummoner.LOGGER.warn("Failed to create entity of type {}", type);
            return false;
        }

        BlockPos blockPos = BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(3.0D)));
        Vec3 pos = Vec3.atBottomCenterOf(blockPos);
        entity.moveTo(pos.x, pos.y, pos.z, player.getYRot() + 180.0F, 0.0F);

        if (entity instanceof Mob mob) {
            mob.getPersistentData().putUUID(OWNER_TAG, player.getUUID());
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPos), MobSpawnType.MOB_SUMMONED, null, null);
        }

        if (!level.addFreshEntity(entity)) {
            PlumeSummoner.LOGGER.warn("Failed to summon {} for {}", type, player.getName().getString());
            return false;
        }
        return true;
    }
}