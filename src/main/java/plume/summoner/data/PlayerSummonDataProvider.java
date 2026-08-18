package plume.summoner.data;

import net.minecraft.world.entity.EntityType;

import java.util.Map;
import java.util.Set;

public interface PlayerSummonDataProvider {
    Set<EntityType<?>> getSummonUnlockedTypes();

    boolean isSummonUnlocked(EntityType<?> type);

    void addSummonUnlock(EntityType<?> type);

    /**
     * 记录一次击杀，达到配置的击杀阈值（killsToUnlock）返回 true 表示本次击杀完成解锁。
     */
    boolean onMobKilled(EntityType<?> type);

    /**
     * 击杀计数原始数据（实体 id -> 击杀数），供玩家重生时跨实例拷贝。
     */
    Map<String, Integer> getKillCounts();

    /**
     * 整体替换击杀计数（重生时从旧玩家实例拷贝）。
     */
    void setKillCounts(Map<String, Integer> counts);
}