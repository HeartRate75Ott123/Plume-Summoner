package plume.summoner.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import plume.summoner.PlumeSummoner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 名单生物单次召唤数量限制（数据驱动，服务端 reload listener）。
 * <p>
 * 数据位置：{@code data/<namespace>/summon_limits/*.json}，文件内容为
 * {@code { "modid:entityid": number, ... }}。允许 datapack 覆盖 / 新增，
 * {@code /reload} 后立即生效。
 * <p>
 * 客户端只读取模组内置的那份，用于界面提示（见 {@code ClientSummonLimits}）；
 * 真正生效的判断永远在服务端。
 */
public final class SummonLimits extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "summon_limits";

    /** 共享实例：reload 监听器与服务端查询共用同一份数据。 */
    public static final SummonLimits INSTANCE = new SummonLimits();

    /** entityid -> 单次召唤数量上限。整体替换，读取端不做加锁。 */
    private static volatile Object2IntMap<ResourceLocation> limits = new Object2IntOpenHashMap<>();

    private SummonLimits() {
        super(new Gson(), DIRECTORY);
    }

    /**
     * 在 mod 构造器里注册（GAME bus）。
     * 1.21.1 用 NeoForge 的 {@link AddReloadListenerEvent}。
     */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(SummonLimits::onAddReloadListeners);
    }

    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    /**
     * @return 该生物的单次召唤上限；不在名单内返回 -1。
     */
    public static int limitOf(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null) {
            return -1;
        }
        Object2IntMap<ResourceLocation> current = limits;
        return current.containsKey(id) ? current.getInt(id) : -1;
    }

    /**
     * 当前已加载的名单条目数（界面提示用）。
     */
    public static int listedCount() {
        return limits.size();
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager, ProfilerFiller profiler) {
        // 按文件名排序后依次写入，冲突时文件名（命名空间+文件名）字典序靠后者覆盖，
        // 不依赖 scanDirectory 的 Map 迭代顺序，结果确定可复现。
        List<Map.Entry<ResourceLocation, JsonElement>> entries = new ArrayList<>(jsons.entrySet());
        entries.sort(Comparator.comparing(entry -> entry.getKey().toString()));

        Object2IntOpenHashMap<ResourceLocation> result = new Object2IntOpenHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries) {
            String file = entry.getKey().toString();
            JsonElement root = entry.getValue();
            if (!root.isJsonObject()) {
                PlumeSummoner.LOGGER.warn("Ignoring summon limit file {}: root must be a JSON object", file);
                continue;
            }
            for (Map.Entry<String, JsonElement> limit : root.getAsJsonObject().entrySet()) {
                ResourceLocation id = ResourceLocation.tryParse(limit.getKey());
                if (id == null) {
                    PlumeSummoner.LOGGER.warn("Ignoring summon limit entry '{}' in {}: not a valid modid:entityid",
                            limit.getKey(), file);
                    continue;
                }
                int count;
                try {
                    count = GsonHelper.convertToInt(limit.getValue(), "summon limit of " + limit.getKey());
                } catch (RuntimeException e) {
                    PlumeSummoner.LOGGER.warn("Ignoring summon limit entry '{}' in {}: value is not a number",
                            limit.getKey(), file);
                    continue;
                }
                if (count < 1) {
                    PlumeSummoner.LOGGER.warn("Ignoring summon limit entry '{}' in {}: value must be >= 1",
                            limit.getKey(), file);
                    continue;
                }
                result.put(id, count);
            }
        }
        limits = result;
        PlumeSummoner.LOGGER.info("Loaded {} summon limit entries", result.size());
    }
}
