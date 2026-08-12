package plume.summoner.screen.widget;

import com.blamejared.searchables.api.autcomplete.AutoCompletingEditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import plume.summoner.screen.SummonEntitiesData;
import plume.summoner.screen.SummonerSearchType;

import java.util.ArrayList;

/**
 * 搜索框（参照 Controlling 的 NewKeyBindsScreen 用法）：
 * 继承 Searchables 的 AutoCompletingEditBox，支持 "组件:值" 语法补全弹窗、
 * ↑↓/PgUp/PgDn/Enter 键盘导航、右键清空、语法高亮。
 */
public class SearchWidget extends AutoCompletingEditBox<EntityType<?>> {
    public SearchWidget(int x, int y, int width, int height) {
        super(Minecraft.getInstance().font, x, y, width, height, Component.empty(),
                SummonerSearchType.TYPE, () -> new ArrayList<>(SummonEntitiesData.types()));
        setHint(Component.translatable("screen.plume_summoner.search"));
    }
}
