package plume.summoner.data;

import net.minecraft.world.entity.EntityType;

import java.util.Set;

public interface PlayerSummonDataProvider {
    Set<EntityType<?>> getSummonUnlockedTypes();

    boolean isSummonUnlocked(EntityType<?> type);

    void addSummonUnlock(EntityType<?> type);

    /**
     * 记录一次击杀计数。返回 true 表示恰好达到解锁阈值（本次击杀触发解锁）。
     */
    boolean onMobKilled(EntityType<?> type);
}
