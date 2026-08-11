package plume.summoner.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import plume.summoner.PlumeSummoner;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 可召唤实体数据（参照 Remorphed 4.2 的 populateUnlockedRenderEntities）：
 * 所有实体在后台线程一次性 create() 并缓存，主线程只读，避免打开菜单卡顿。
 */
public final class SummonEntitiesData {
    private static final CopyOnWriteArrayList<EntityType<?>> TYPES = new CopyOnWriteArrayList<>();
    private static final ConcurrentHashMap<EntityType<?>, Mob> RENDER_ENTITIES = new ConcurrentHashMap<>();
    private static final AtomicInteger VERSION = new AtomicInteger(0);
    private static final AtomicBoolean LOADED = new AtomicBoolean(false);
    private static final AtomicReference<CompletableFuture<Void>> LOADING = new AtomicReference<>();

    private SummonEntitiesData() {
    }

    /**
     * 幂等地启动（或复用）后台加载任务。可多次调用（如每次打开菜单）。
     */
    public static CompletableFuture<Void> loadAsync() {
        if (LOADED.get()) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> pending = LOADING.get();
        if (pending != null && !pending.isDone()) {
            return pending;
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (LOADING.compareAndSet(null, future)) {
            CompletableFuture.runAsync(() -> {
                try {
                    SummonerSearchContext.context();
                    loadAll();
                    LOADED.set(true);
                    VERSION.incrementAndGet();
                    future.complete(null);
                } catch (Throwable t) {
                    PlumeSummoner.LOGGER.error("Failed to load summonable entities", t);
                    future.completeExceptionally(t);
                }
            });
            return future;
        }
        return LOADING.get();
    }

    private static void loadAll() {
        TYPES.clear();
        RENDER_ENTITIES.clear();
        Minecraft minecraft = Minecraft.getInstance();
        int created = 0;
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type == EntityType.PLAYER) {
                continue;
            }
            try {
                Entity entity = type.create(minecraft.level);
                if (entity instanceof Mob mob) {
                    TYPES.add(type);
                    RENDER_ENTITIES.put(type, mob);
                    created++;
                }
            } catch (Exception e) {
                PlumeSummoner.LOGGER.debug("Skipping entity type {} for the summon menu", type);
            }
        }
        PlumeSummoner.LOGGER.info("Loaded {} entities for the summon menu", created);
    }

    /**
     * 当前索引版本号：实体列表重建后 +1，搜索索引据此判断是否重建。
     */
    public static int version() {
        return VERSION.get();
    }

    public static boolean isLoaded() {
        return LOADED.get();
    }

    public static List<EntityType<?>> types() {
        return TYPES;
    }

    public static Mob renderEntity(EntityType<?> type) {
        return RENDER_ENTITIES.get(type);
    }
}