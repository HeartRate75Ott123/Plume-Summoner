package plume.summoner.screen.widget;

import com.blamejared.searchables.api.SearchableComponent;
import com.blamejared.searchables.api.SearchablesConstants;
import com.blamejared.searchables.api.TokenRange;
import com.blamejared.searchables.api.autcomplete.AutoComplete;
import com.blamejared.searchables.api.autcomplete.AutoCompletingEditBox;
import com.blamejared.searchables.api.autcomplete.CompletionSuggestion;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import plume.summoner.screen.SummonerSearchContext;
import plume.summoner.screen.SummonerSearchType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * 搜索框（参照 Controlling 的 NewKeyBindsScreen 用法）：
 * 继承 Searchables 的 AutoCompletingEditBox，支持 "组件:值" 语法补全弹窗、
 * ↑↓/PgUp/PgDn/Enter 键盘导航、右键清空、语法高亮。
 * <p>
 * 补全建议表由本类接管：Searchables 生成建议时强制按值字典序排序，
 * 无法表达网格顺序（已解锁置顶→原版→模组→无翻译键掉底、组内按语言排序），
 * 这里按网格规则重新生成建议列表并反射写回 AutoComplete.suggestions，
 * 不改动 Searchables 本体。
 */
public class SearchWidget extends AutoCompletingEditBox<EntityType<?>> {
    private static SearchWidget instance;
    private static final Field SUGGESTIONS_FIELD = findSuggestionsField();

    public SearchWidget(int x, int y, int width, int height) {
        super(Minecraft.getInstance().font, x, y, width, height, Component.empty(),
                SummonerSearchType.TYPE, () -> {
                    SearchWidget current = instance;
                    return current == null ? new ArrayList<>() : current.suggestionEntries();
                });
        instance = this;
        setHint(Component.translatable("screen.plume_summoner.search"));
        addResponder(this::overrideSuggestions);
    }

    private static Field findSuggestionsField() {
        try {
            Field field = AutoComplete.class.getDeclaredField("suggestions");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    /**
     * 补全建议的候选集（多组件叠加局限 + 网格顺序）：
     * 光标正在输入 "组件:值" 时，先用当前 token 之前的查询过滤全部实体，
     * 使后输入组件的补全只从已过滤结果中建议；结果始终按网格排序。
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
        return SummonerSearchContext.search("");
    }

    /**
     * 按网格顺序重写 AutoComplete 的建议表（覆盖 Searchables 的字典序结果）。
     */
    private void overrideSuggestions(String value) {
        if (SUGGESTIONS_FIELD == null) {
            return;
        }
        try {
            SUGGESTIONS_FIELD.set(autoComplete(), computeSuggestions(value));
        } catch (IllegalAccessException e) {
            // 忽略：反射失败时沿用 Searchables 原建议表
        }
    }

    private List<CompletionSuggestion> computeSuggestions(String value) {
        int position = getCursorPosition();
        TokenRange replacementRange = completionVisitor().rangeAt(position);
        if (replacementRange == null || !replacementRange.contains(position)) {
            return List.of();
        }
        TokenRange suggestionRange = replacementRange.rangeAtPosition(position);
        int suggestionIndex = replacementRange.rangeIndexAtPosition(position);
        if (suggestionIndex == 0) {
            // 组件名建议（favorites/categories/name），沿用 Searchables 逻辑
            String componentPrefix = suggestionRange.substring(value, position);
            return SummonerSearchType.TYPE.getSuggestionsForComponent(componentPrefix, replacementRange.simplify());
        }
        if (suggestionIndex != 1 && suggestionIndex != 2) {
            return List.of();
        }
        String componentName = replacementRange.range(0).substring(value);
        SearchableComponent<EntityType<?>> component = SummonerSearchType.TYPE.component(componentName).orElse(null);
        if (component == null) {
            return List.of();
        }
        String termPrefix = suggestionIndex == 1 ? "" : suggestionRange.substring(value, position);
        if (!termPrefix.isEmpty() && (termPrefix.charAt(0) == '"' || termPrefix.charAt(0) == '\'' || termPrefix.charAt(0) == '`')) {
            termPrefix = termPrefix.substring(1);
        }
        String lowerPrefix = termPrefix.toLowerCase(Locale.ROOT);
        List<CompletionSuggestion> suggestions = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        TokenRange replacement = replacementRange.simplify();
        for (EntityType<?> entry : suggestionEntries()) {
            Optional<String> string = component.getToString().apply(entry);
            if (string.isEmpty()) {
                continue;
            }
            String s = string.get();
            if (s.toLowerCase(Locale.ROOT).startsWith(lowerPrefix) && seen.add(s)) {
                suggestions.add(new CompletionSuggestion(
                        componentName + ":" + SearchablesConstants.QUOTE.apply(s),
                        Component.literal(s), " ", replacement));
            }
        }
        return suggestions;
    }
}