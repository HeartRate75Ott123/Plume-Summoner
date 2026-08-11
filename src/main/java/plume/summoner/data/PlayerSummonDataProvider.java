package plume.summoner.data;

import net.minecraft.world.entity.EntityType;

import java.util.Set;

public interface PlayerSummonDataProvider {
    Set<EntityType<?>> getSummonUnlockedTypes();

    boolean isSummonUnlocked(EntityType<?> type);

    void addSummonUnlock(EntityType<?> type);
}