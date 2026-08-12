package plume.summoner.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import plume.summoner.PlumeSummoner;
import plume.summoner.data.PlayerSummonDataProvider;
import plume.summoner.network.SummonRequestPayload;

public final class SummonHandler {
    private static final String OWNER_TAG = "plume_summoner_owner";

    private SummonHandler() {
    }

    public static void onSummonRequest(SummonRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(payload.entityType());
            if (type == null || !((PlayerSummonDataProvider) player).isSummonUnlocked(type)) {
                return;
            }
            summon(player, type);
        });
    }

    public static void summon(ServerPlayer player, EntityType<?> type) {
        ServerLevel level = player.serverLevel();

        Entity entity = type.create(level);
        if (entity == null) {
            PlumeSummoner.LOGGER.warn("Failed to create entity of type {}", type);
            return;
        }

        BlockPos blockPos = BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(3.0D)));
        blockPos = blockPos.atY(level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockPos.getX(), blockPos.getZ()));
        Vec3 pos = Vec3.atBottomCenterOf(blockPos);
        // 正确面向玩家：实体朝向 = 从实体位置指向玩家的方位角（MC yaw：0 朝南、90 朝西）
        float yaw = (float) Math.toDegrees(Math.atan2(-(player.getX() - pos.x), player.getZ() - pos.z));
        entity.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);

        if (entity instanceof Mob mob) {
            mob.getPersistentData().putUUID(OWNER_TAG, player.getUUID());
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPos), MobSpawnType.MOB_SUMMONED, null);
        }

        if (!level.addFreshEntity(entity)) {
            PlumeSummoner.LOGGER.warn("Failed to summon {} for {}", type, player.getName().getString());
            return;
        }
        player.displayClientMessage(
                Component.translatable("message.plume_summoner.summoned", entity.getDisplayName()),
                false);
    }
}