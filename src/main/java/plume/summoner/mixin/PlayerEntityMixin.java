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
import plume.summoner.data.PlayerSummonDataProvider;

import java.util.HashSet;
import java.util.Set;

@Mixin(Player.class)
public abstract class PlayerEntityMixin implements PlayerSummonDataProvider {
    private static final String NBT_KEY = "plume_summoner_unlocked_mobs";

    @Unique
    private final Set<String> plumeSummoner$unlockedMobs = new HashSet<>();

    @Override
    public Set<EntityType<?>> getSummonUnlockedTypes() {
        Set<EntityType<?>> result = new HashSet<>();
        for (String id : this.plumeSummoner$unlockedMobs) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.tryParse(id));
            if (type != null) {
                result.add(type);
            }
        }
        return result;
    }

    @Override
    public boolean isSummonUnlocked(EntityType<?> type) {
        return this.plumeSummoner$unlockedMobs.contains(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
    }

    @Override
    public void addSummonUnlock(EntityType<?> type) {
        this.plumeSummoner$unlockedMobs.add(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void plumeSummoner$saveUnlocked(CompoundTag tag, CallbackInfo ci) {
        ListTag list = new ListTag();
        for (String id : this.plumeSummoner$unlockedMobs) {
            list.add(StringTag.valueOf(id));
        }
        tag.put(NBT_KEY, list);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void plumeSummoner$loadUnlocked(CompoundTag tag, CallbackInfo ci) {
        this.plumeSummoner$unlockedMobs.clear();
        ListTag list = tag.getList(NBT_KEY, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            this.plumeSummoner$unlockedMobs.add(list.getString(i));
        }
    }
}