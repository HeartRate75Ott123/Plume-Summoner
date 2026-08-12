package plume.summoner.screen;

import me.towdium.pinin.DictLoader;
import me.towdium.pinin.PinIn;
import me.towdium.pinin.searchers.Searcher;
import me.towdium.pinin.searchers.TreeSearcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import plume.summoner.PlumeSummoner;
import plume.summoner.client.PlumeSummonerClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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
     * 搜索：交给 searchables 解析（支持 "name:" 默认组件 / "categories:" 模组 / "favorites:" 收藏
     * 等组件谓词，name 组件内部走 PinIn 拼音/中文/英文匹配），结果再按
     * "新解锁的 → 原版/模组 → 翻译缺失的"三层排序。
     */
    public static List<EntityType<?>> search(String query) {
        List<EntityType<?>> matches = new ArrayList<>(
                SummonerSearchType.TYPE.filterEntries(SummonEntitiesData.types(), query == null ? "" : query));
        matches.sort(SORTER);
        return matches;
    }

    /**
     * 单个生物是否匹配查询（供 searchables 的 defaultComponent 过滤使用）。
     * 与 search() 同一套匹配规则：PinIn 拼音/索引 + 忽略大小写子串兜底。
     */
    public static boolean matches(EntityType<?> type, String query) {
        String text = query == null ? "" : query.trim();
        if (text.isEmpty()) {
            return true;
        }
        TreeSearcher<EntityType<?>> current = tree;
        int version = SummonEntitiesData.version();
        if (current == null || indexedVersion != version) {
            rebuild(version);
            current = tree;
        }
        if (current.search(text).contains(type)) {
            return true;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return type.getDescription().getString().toLowerCase(Locale.ROOT).contains(lower)
                || BuiltInRegistries.ENTITY_TYPE.getKey(type).toString().toLowerCase(Locale.ROOT).contains(lower);
    }

    // ---------- 排序 ----------
    // 排序规则（用户需求）：
    // 1. 已解锁的排最前，内部按解锁时间倒序（新解锁的在最前）
    // 2. 其余按 原版(minecraft) → 其他模组 顺序
    // 3. 翻译键未配置（翻译结果仍是 entity.x.x 原始样式）的丢到最后

    private static int tier(EntityType<?> type) {
        if (PlumeSummonerClient.UNLOCKED_TYPES.contains(type)) {
            return 0;
        }
        return hasTranslation(type) ? 1 : 2;
    }

    private static boolean hasTranslation(EntityType<?> type) {
        try {
            return net.minecraft.locale.Language.getInstance()
                    .has(type.getDescriptionId());
        } catch (Exception e) {
            return true;
        }
    }

    private static int unlockIndex(EntityType<?> type) {
        int i = 0;
        for (EntityType<?> t : PlumeSummonerClient.UNLOCKED_TYPES) {
            if (t == type) {
                return i;
            }
            i++;
        }
        return Integer.MAX_VALUE;
    }

    private static String namespace(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace();
    }

    private static int compareByNamespace(EntityType<?> a, EntityType<?> b) {
        String na = namespace(a);
        String nb = namespace(b);
        boolean vanillaA = na.equals("minecraft");
        boolean vanillaB = nb.equals("minecraft");
        if (vanillaA != vanillaB) {
            return vanillaA ? -1 : 1;
        }
        if (!na.equals(nb)) {
            // 不同模组：按模组显示名排序（中文语言下显示中文名，无翻译回退 modid）
            int c = SummonerSearchType.modDisplayName(a).compareTo(SummonerSearchType.modDisplayName(b));
            if (c != 0) {
                return c;
            }
        }
        // 模组内部按显示名排序（中文语言下按中文名，英文按英文名）
        return a.getDescription().getString().compareTo(b.getDescription().getString());
    }

    private static final Comparator<EntityType<?>> SORTER = (a, b) -> {
        int ta = tier(a);
        int tb = tier(b);
        if (ta != tb) {
            return Integer.compare(ta, tb);
        }
        if (ta == 0) {
            // 已解锁：解锁索引大的（新解锁）在前
            return Integer.compare(unlockIndex(b), unlockIndex(a));
        }
        return compareByNamespace(a, b);
    };
}
