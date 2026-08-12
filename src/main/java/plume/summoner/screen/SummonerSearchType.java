package plume.summoner.screen;

import com.blamejared.searchables.api.SearchableComponent;
import com.blamejared.searchables.api.SearchableType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import plume.summoner.client.favorites.SummonerFavorites;

import java.util.Locale;
import java.util.Optional;

/**
 * 召唤菜单的 Searchables 搜索类型（用法参照 Controlling 的 SEARCHABLE_KEYBINDINGS）：
 * - name（默认组件）：生物名称，过滤走 PinIn 拼音/中文/英文匹配
 * - categories：生物所属模组名称，"categories:" 后按模组过滤
 * - favorites：收藏过滤，"favorites:" 显示收藏夹
 * 输入框聚焦时自动弹出补全：空输入展示全部组件（favorites/categories），
 * 点击组件后展开第二层值补全（Controlling 式两级联动）。
 */
public final class SummonerSearchType {
    public static final SearchableType<EntityType<?>> TYPE = new SearchableType.Builder<EntityType<?>>()
            .component(SearchableComponent.create("favorites", type ->
                    isFavorite(type) ? Optional.of("true") : Optional.empty(),
                    (type, value) -> isFavorite(type) && "true".equalsIgnoreCase(value)))
            .component(SearchableComponent.create("categories", type ->
                    Optional.of(modDisplayName(type)),
                    (type, value) -> modDisplayName(type).toLowerCase(Locale.ROOT)
                            .contains(value.toLowerCase(Locale.ROOT))))
            .defaultComponent(SearchableComponent.create("name", type ->
                    Optional.of(type.getDescription().getString()),
                    (type, value) -> SummonerSearchContext.matches(type, value)))
            .build();

    private SummonerSearchType() {
    }

    private static boolean isFavorite(EntityType<?> type) {
        return SummonerFavorites.isFavorite(type);
    }

    /**
     * 生物所属模组的显示名：
     * 原版显示 "Minecraft"；其他模组取 mods.toml 的 displayName
     * （该值硬编码于模组自身 mods.toml，加载器不做本地化），缺失时回退 modid。
     */
    public static String modDisplayName(EntityType<?> type) {
        String namespace = BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace();
        if (namespace.equals("minecraft")) {
            return "Minecraft";
        }
        return net.minecraftforge.fml.ModList.get().getModContainerById(namespace)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(namespace);
    }
}
