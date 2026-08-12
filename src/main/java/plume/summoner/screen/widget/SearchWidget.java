package plume.summoner.screen.widget;

import com.blamejared.searchables.api.autcomplete.AutoCompletingEditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import plume.summoner.screen.SummonEntitiesData;
import plume.summoner.screen.SummonerSearchContext;
import plume.summoner.screen.SummonerSearchType;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索框（参照 Controlling 的 NewKeyBindsScreen 用法）：
 * 继承 Searchables 的 AutoCompletingEditBox，支持 "组件:值" 语法补全弹窗、
 * ↑↓/PgUp/PgDn/Enter 键盘导航、右键清空、语法高亮。
 */
public class SearchWidget extends AutoCompletingEditBox<EntityType<?>> {
    private static SearchWidget instance;

    public SearchWidget(int x, int y, int width, int height) {
        super(Minecraft.getInstance().font, x, y, width, height, Component.empty(),
                SummonerSearchType.TYPE, () -> {
                    SearchWidget current = instance;
                    return current == null ? new ArrayList<>() : current.suggestionEntries();
                });
        instance = this;
        setHint(Component.translatable("screen.plume_summoner.search"));
    }

    /**
     * 补全建议的候选集（多组件叠加局限）：
     * 光标正在输入 "组件:值" 时，先用当前 token 之前的查询过滤全部实体，
     * 使后输入组件（如 categories:minecraft 后再输 name:）的补全只从已过滤结果中建议。
     */
    private List<EntityType<?>> suggestionEntries() {
        String text = getValue();
        int pos = getCursorPosition();
        int start = text.lastIndexOf(' ', pos) + 1;
        if (text.substring(start).contains(":")) {
            String rest = text.substring(0, start).trim();
            if (!rest.isEmpty()) {
                return SummonerSearchContext.search(rest);
            }
        }
        return new ArrayList<>(SummonEntitiesData.types());
    }
}
