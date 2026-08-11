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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import plume.summoner.PlumeSummoner;
import plume.summoner.config.SummonerConfig;
import plume.summoner.data.PlayerSummonDataProvider;

public final class SummonHandler {
    private static final String OWNER_TAG = "plume_summoner_owner";

    private SummonHandler() {
    }

    public static void onSummonRequest(ResourceLocation entityType, NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityType);
        if (type == null || !((PlayerSummonDataProvider) player).isSummonUnlocked(type)) {
            return;
        }
        summon(player, type);
    }

    public static void summon(ServerPlayer player, EntityType<?> type) {
        ServerLevel level = player.serverLevel();

        long count = level.getEntitiesOfClass(Mob.class,
                        AABB.ofSize(player.position(), 256.0D, 256.0D, 256.0D),
                        mob -> mob.getPersistentData().hasUUID(OWNER_TAG)
                                && player.getUUID().equals(mob.getPersistentData().getUUID(OWNER_TAG)))
                .size();
        if (count >= SummonerConfig.MAX_SUMMONS_PER_PLAYER.get()) {
            player.displayClientMessage(
                    Component.translatable("message.plume_summoner.limit_reached",
                            SummonerConfig.MAX_SUMMONS_PER_PLAYER.get()),
                    true);
            return;
        }

        Entity entity = type.create(level);
        if (entity == null) {
            PlumeSummoner.LOGGER.warn("Failed to create entity of type {}", type);
            return;
        }

        BlockPos blockPos = BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(3.0D)));
        blockPos = blockPos.atY(level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockPos.getX(), blockPos.getZ()));
        Vec3 pos = Vec3.atBottomCenterOf(blockPos);
        entity.moveTo(pos.x, pos.y, pos.z, player.getYRot() + 180.0F, 0.0F);

        if (entity instanceof Mob mob) {
            mob.getPersistentData().putUUID(OWNER_TAG, player.getUUID());
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPos), MobSpawnType.MOB_SUMMONED, null, null);
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