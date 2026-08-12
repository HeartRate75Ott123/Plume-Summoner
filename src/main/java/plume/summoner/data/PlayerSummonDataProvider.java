package plume.summoner.data;

import net.minecraft.world.entity.EntityType;

import java.util.Set;

public interface PlayerSummonDataProvider {
    Set<EntityType<?>> getSummonUnlockedTypes();

    boolean isSummonUnlocked(EntityType<?> type);

    void addSummonUnlock(EntityType<?> type);

    /**
     * 记录一次击杀，达到配置的击杀阈值（killsToUnlock）返回 true 表示本次击杀完成解锁。
     */
    boolean onMobKilled(EntityType<?> type);
}