package plume.summoner.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import plume.summoner.config.SummonerConfig;
import plume.summoner.data.PlayerSummonDataProvider;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Mixin(Player.class)
public abstract class PlayerEntityMixin implements PlayerSummonDataProvider {
    // 旧版数据键：已解锁集合（String 列表），迁移到计数结构
    private static final String LEGACY_NBT_KEY = "plume_summoner_unlocked_mobs";
    // 新版数据键：击杀计数（id -> count），count >= killsToUnlock 即解锁
    private static final String NBT_KEY = "plume_summoner_kill_counts";

    @Unique
    private final Map<String, Integer> plumeSummoner$killCounts = new HashMap<>();

    @Override
    public Set<EntityType<?>> getSummonUnlockedTypes() {
        Set<EntityType<?>> result = new LinkedHashSet<>();
        int threshold = SummonerConfig.KILLS_TO_UNLOCK.get();
        for (Map.Entry<String, Integer> entry : this.plumeSummoner$killCounts.entrySet()) {
            if (entry.getValue() >= threshold) {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.tryParse(entry.getKey()));
                if (type != null) {
                    result.add(type);
                }
            }
        }
        return result;
    }

    @Override
    public boolean isSummonUnlocked(EntityType<?> type) {
        return countFor(type) >= SummonerConfig.KILLS_TO_UNLOCK.get();
    }

    @Override
    public void addSummonUnlock(EntityType<?> type) {
        // 强制解锁：计数设为极大值
        this.plumeSummoner$killCounts.put(key(type), Integer.MAX_VALUE);
    }

    @Override
    public boolean onMobKilled(EntityType<?> type) {
        int threshold = SummonerConfig.KILLS_TO_UNLOCK.get();
        int count = this.plumeSummoner$killCounts.merge(key(type), 1, Integer::sum);
        return count == threshold;
    }

    private int countFor(EntityType<?> type) {
        return this.plumeSummoner$killCounts.getOrDefault(key(type), 0);
    }

    private static String key(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void plumeSummoner$saveUnlocked(CompoundTag tag, CallbackInfo ci) {
        ListTag list = new ListTag();
        for (Map.Entry<String, Integer> entry : this.plumeSummoner$killCounts.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("id", entry.getKey());
            entryTag.putInt("count", entry.getValue());
            list.add(entryTag);
        }
        tag.put(NBT_KEY, list);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void plumeSummoner$loadUnlocked(CompoundTag tag, CallbackInfo ci) {
        this.plumeSummoner$killCounts.clear();
        ListTag list = tag.getList(NBT_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            this.plumeSummoner$killCounts.put(entry.getString("id"), entry.getInt("count"));
        }
        // 兼容旧存档：旧格式是解锁 id 字符串列表，迁移为已解锁（计数置极大值）
        ListTag legacy = tag.getList(LEGACY_NBT_KEY, Tag.TAG_STRING);
        for (int i = 0; i < legacy.size(); i++) {
            String id = legacy.getString(i);
            this.plumeSummoner$killCounts.merge(id, Integer.MAX_VALUE, Math::max);
        }
    }
}
