package plume.summoner.screen;

import me.towdium.pinin.DictLoader;
import me.towdium.pinin.PinIn;
import me.towdium.pinin.searchers.Searcher;
import me.towdium.pinin.searchers.TreeSearcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import plume.summoner.PlumeSummoner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 拼音搜索上下文（参照 JustEnoughCharacters 的 Match 类）：
 * 全局唯一的 PinIn 实例 + 共享 TreeSearcher 索引。
 * 字典构建较重，因此在后台线程首次调用 context() 时完成，
 * 避免打开菜单时卡住主线程。
 */
public final class SummonerSearchContext {
    private static volatile PinIn context;
    private static volatile TreeSearcher<EntityType<?>> tree;
    private static volatile int indexedVersion = -1;

    private SummonerSearchContext() {
    }

    /**
     * 线程安全地获取（或构建）唯一 PinIn 实例。
     * 字典 data.txt 约 2 万条拼音数据，耗时数百毫秒，务必在后台线程首次调用。
     */
    public static PinIn context() {
        PinIn result = context;
        if (result == null) {
            synchronized (SummonerSearchContext.class) {
                if (context == null) {
                    context = new PinIn(new DictLoader.Default())
                            .config()
                            .accelerate(true)
                            .commit();
                }
                result = context;
            }
        }
        return result;
    }

    /**
     * 模糊音全开（JEC 默认值），键位用全拼。
     */
    public static void applyConfig() {
        context().config()
                .keyboard(me.towdium.pinin.Keyboard.QUANPIN)
                .fZh2Z(true)
                .fSh2S(true)
                .fCh2C(true)
                .fAng2An(true)
                .fIng2In(true)
                .fEng2En(true)
                .fU2V(true)
                .commit();
    }

    /**
     * 重建搜索索引。实体列表版本变化时自动触发。
     */
    private static synchronized void rebuild(int version) {
        applyConfig();
        TreeSearcher<EntityType<?>> newTree = new TreeSearcher<>(Searcher.Logic.CONTAIN, context());
        for (EntityType<?> type : SummonEntitiesData.types()) {
            newTree.put(type.getDescription().getString(), type);
            newTree.put(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(), type);
        }
        tree = newTree;
        indexedVersion = version;
    }

    /**
     * 搜索：优先拼音/索引匹配，兜底做一次忽略大小写的子串匹配。
     */
    public static List<EntityType<?>> search(String query) {
        String text = query == null ? "" : query.trim();
        List<EntityType<?>> matches = new ArrayList<>();

        if (!text.isEmpty()) {
            TreeSearcher<EntityType<?>> current = tree;
            int version = SummonEntitiesData.version();
            if (current == null || indexedVersion != version) {
                rebuild(version);
                current = tree;
            }
            Set<EntityType<?>> seen = new HashSet<>();
            for (EntityType<?> type : current.search(text)) {
                if (seen.add(type)) {
                    matches.add(type);
                }
            }
            if (matches.isEmpty()) {
                String lower = text.toLowerCase(java.util.Locale.ROOT);
                for (EntityType<?> type : SummonEntitiesData.types()) {
                    if (type.getDescription().getString().toLowerCase(java.util.Locale.ROOT).contains(lower)
                            || BuiltInRegistries.ENTITY_TYPE.getKey(type).toString().contains(lower)) {
                        matches.add(type);
                    }
                }
            }
        } else {
            matches.addAll(SummonEntitiesData.types());
        }

        matches.sort(Comparator.comparing(type -> type.getDescription().getString()));
        return matches;
    }
}