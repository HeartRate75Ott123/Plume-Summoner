package plume.summoner.client;

import me.shedaniel.clothconfig2.gui.entries.AbstractTextFieldListListEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 字符串列表条目（黑名单用）：行为与 Cloth 的 StringList 一致，
 * 但每个单元格（含新增的空行）额外渲染一圈白色边框，未聚焦时也清晰可见。
 */
public class BorderedStringListEntry
        extends AbstractTextFieldListListEntry<String, BorderedStringListEntry.BorderedCell, BorderedStringListEntry> {

    public BorderedStringListEntry(Component fieldName, List<String> list, boolean requiresRestart,
                                   Supplier<Optional<Component[]>> tooltipSupplier,
                                   Consumer<List<String>> saveConsumer,
                                   Supplier<List<String>> defaultValue,
                                   Component resetButtonKey) {
        super(fieldName, list, requiresRestart, tooltipSupplier, saveConsumer, defaultValue,
                resetButtonKey, true, true, true, BorderedCell::new);
        // Cloth 用 requiresRestart 控制初始折叠：false 时列表折叠只显示标题行，
        // 条目不可见易被误认为"丢失"，这里强制默认展开
        this.setExpanded(true);
    }

    @Override
    public BorderedStringListEntry self() {
        return this;
    }

    @Override
    protected BorderedCell getFromValue(String value) {
        return new BorderedCell(value, this);
    }

    public static class BorderedCell extends AbstractTextFieldListListEntry.AbstractTextFieldListCell<String, BorderedCell, BorderedStringListEntry> {

        public BorderedCell(String value, BorderedStringListEntry list) {
            super(value, list);
        }

        @Override
        public String getValue() {
            return this.widget.getValue();
        }

        @Override
        public Optional<Component> getError() {
            return Optional.empty();
        }

        @Override
        protected String substituteDefault(String value) {
            return value;
        }

        @Override
        protected boolean isValidText(String text) {
            return true;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            super.render(guiGraphics, index, top, left, width, height, mouseX, mouseY, hovered, partialTick);
            if (this.listListEntry.isEditable()) {
                guiGraphics.fill(left, top + 12, left + width - 12, top + 13,
                        this.getConfigError().isPresent() ? 0xFFFF5566 : 0xFFE0E0E0);
            }
        }
    }
}