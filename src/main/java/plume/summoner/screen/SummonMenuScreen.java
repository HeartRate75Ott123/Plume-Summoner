package plume.summoner.screen;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import plume.summoner.client.SummonerUiPrefs;
import plume.summoner.screen.widget.SearchWidget;
import plume.summoner.screen.widget.SummonEntityWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * 召唤菜单（界面结构一比一复刻 Remorphed 4.2 的 RemorphedScreen）：
 * 顶部控件条 + 7 列实体网格，实体在后台线程加载，打开菜单不卡主线程。
 * 右侧滚动条：滚轮滚动、拖拽滑块、点击轨道跳转三者都支持。
 */
public class SummonMenuScreen extends Screen {
    private static final int TOP = 35;
    private static final float SCROLL_SPEED = 10.0F;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_X_OFFSET = 4;
    private static final int SCROLLBAR_MIN_THUMB = 20;

    private final SearchWidget searchBar = createSearchBar();
    private final Button closeButton = createCloseButton();
    private final Button closeBehaviorButton = createCloseBehaviorButton();
    private final EditBox countField = createCountField();
    private final List<SummonEntityWidget> widgets = new ArrayList<>();
    // 关闭 GUI 时保存上次搜索内容，下次打开时恢复（静态字段跨 Screen 实例存活）
    private static String lastSearch = "";
    private boolean loaded;

    private float scrollOffset;
    private boolean scrollDragging;
    private float dragStartOffset;
    private double dragStartY;

    public SummonMenuScreen() {
        super(Component.translatable("screen.plume_summoner.title"));
    }

    @Override
    protected void init() {
        this.widgets.clear();
        this.scrollOffset = 0;
        this.addRenderableWidget(this.searchBar);
        this.addRenderableWidget(this.closeButton);
        this.addRenderableWidget(this.closeBehaviorButton);
        this.addRenderableWidget(this.countField);
        this.searchBar.addResponder(text -> {
            this.lastSearch = text;
            if (this.loaded) {
                this.repopulate();
            }
        });
        this.searchBar.setValue(this.lastSearch);
        this.setInitialFocus(this.searchBar);

        SummonEntitiesData.loadAsync().thenRun(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                minecraft.execute(this::onEntitiesLoaded);
            }
        });
    }

    @Override
    public void onClose() {
        // 关闭 GUI 时保存上次搜索框里的内容（供下次打开恢复）
        lastSearch = this.searchBar.getValue();
        super.onClose();
    }

    /**
     * 进入新存档时清空上次搜索内容（同一存档内关闭再打开会保留）。
     */
    public static void resetLastSearch() {
        lastSearch = "";
    }

    private void onEntitiesLoaded() {
        this.loaded = true;
        this.repopulate();
    }

    /**
     * 主线程重建网格（搜索过滤 + 7 列平铺，对照 RemorphedScreen.populateShapeWidgets）。
     * 1.20.1 的 Screen 没有 removeWidget，直接清 children 里的旧格子。
     */
    private void repopulate() {
        this.children().removeIf(child -> child instanceof SummonEntityWidget);
        this.widgets.clear();

        List<EntityType<?>> matches = SummonerSearchContext.search(this.lastSearch);
        if (matches.isEmpty()) {
            return;
        }

        float columnWidth = (getWindow().getGuiScaledWidth() - 27) / 7f;
        float rowHeight = getWindow().getGuiScaledHeight() / 5f;

        for (int i = 0; i < matches.size(); i++) {
            EntityType<?> type = matches.get(i);
            SummonEntityWidget widget = new SummonEntityWidget(
                    (int) (columnWidth * (i % 7) + 15),
                    (int) (rowHeight * (i / 7) + TOP),
                    (int) columnWidth,
                    (int) rowHeight,
                    type,
                    SummonEntitiesData.renderEntity(type),
                    this);
            this.addRenderableWidget(widget);
            this.widgets.add(widget);
        }

        float max = maxScroll();
        this.scrollOffset = Math.min(this.scrollOffset, max);
        applyScroll();
    }

    private int totalRows() {
        return (this.widgets.size() + 6) / 7;
    }

    private float rowHeight() {
        return getWindow().getGuiScaledHeight() / 5f;
    }

    private float maxScroll() {
        int rows = totalRows();
        if (rows == 0) {
            return 0;
        }
        float content = rows * rowHeight();
        return Math.max(0, content - (this.height - TOP));
    }

    /**
     * 将 scrollOffset 应用到所有格子的 y 位置。
     */
    private void applyScroll() {
        for (SummonEntityWidget widget : this.widgets) {
            widget.setPosition(widget.getX(), (int) (widget.baseY() - this.scrollOffset));
        }
    }

    private int scrollbarX() {
        return this.width - SCROLLBAR_X_OFFSET - SCROLLBAR_WIDTH;
    }

    private int scrollbarViewport() {
        return this.height - TOP;
    }

    private float scrollbarThumbHeight() {
        float viewport = scrollbarViewport();
        float max = maxScroll();
        if (max <= 0) {
            return viewport;
        }
        float content = totalRows() * rowHeight();
        float thumb = viewport * viewport / content;
        return Math.max(SCROLLBAR_MIN_THUMB, thumb);
    }

    private float scrollbarThumbY() {
        float viewport = scrollbarViewport();
        float max = maxScroll();
        if (max <= 0) {
            return TOP;
        }
        return TOP + (this.scrollOffset / max) * (viewport - scrollbarThumbHeight());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        if (!this.loaded) {
            guiGraphics.drawCenteredString(this.font,
                    Component.translatable("screen.plume_summoner.loading"),
                    this.width / 2, this.height / 2, 0xFFFFFF);
        }

        this.searchBar.render(guiGraphics, mouseX, mouseY, partialTick);
        this.closeButton.render(guiGraphics, mouseX, mouseY, partialTick);
        this.closeBehaviorButton.render(guiGraphics, mouseX, mouseY, partialTick);
        drawCountLabel(guiGraphics);
        this.countField.render(guiGraphics, mouseX, mouseY, partialTick);

        double scaledFactor = this.minecraft.getWindow().getGuiScale();
        guiGraphics.pose().pushPose();
        RenderSystem.enableScissor(0, 0,
                (int) ((double) this.width * scaledFactor),
                (int) ((double) (this.height - TOP) * scaledFactor));

        for (SummonEntityWidget widget : this.widgets) {
            if (widget.getY() + widget.getHeight() > TOP && widget.getY() < this.height) {
                widget.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        RenderSystem.disableScissor();
        guiGraphics.pose().popPose();

        renderScrollbar(guiGraphics);

        for (SummonEntityWidget widget : this.widgets) {
            if (widget.isHovered()
                    && widget.getY() + widget.getHeight() > TOP
                    && widget.getY() < this.height) {
                guiGraphics.renderTooltip(this.font, widget.tooltipText(), mouseX, mouseY);
                break;
            }
        }

        // 补全弹窗不是 addRenderableWidget 注册的，需手动渲染（Controlling 同款做法）
        this.searchBar.autoComplete().render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderScrollbar(GuiGraphics guiGraphics) {
        if (maxScroll() <= 0) {
            return;
        }
        int x = scrollbarX();
        int viewport = scrollbarViewport();
        guiGraphics.fill(x, TOP, x + SCROLLBAR_WIDTH, TOP + viewport, 0x40FFFFFF);
        float thumbY = scrollbarThumbY();
        float thumbH = scrollbarThumbHeight();
        guiGraphics.fill(x, (int) thumbY, x + SCROLLBAR_WIDTH, (int) (thumbY + thumbH), 0xFFFFFFFF);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 补全弹窗优先处理滚轮（弹窗仅在聚焦时可见，避免未聚焦时吞掉网格滚轮）
        if (this.searchBar.isFocused() && this.searchBar.autoComplete().mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        if (!this.widgets.isEmpty() && maxScroll() > 0) {
            this.scrollOffset = Math.max(0, Math.min(maxScroll(), this.scrollOffset - (float) delta * SCROLL_SPEED));
            applyScroll();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 点击非数量输入框区域时让其失焦并提交（空/非法值重置为默认）
        if (this.countField.isFocused() && !this.countField.isMouseOver(mouseX, mouseY)) {
            this.countField.setFocused(false);
            this.commitCount();
        }
        // 数量输入框右键：清空内容（随后失焦时按空值重置为默认 1）
        if (button == 1 && this.countField.isMouseOver(mouseX, mouseY)) {
            this.countField.setValue("");
            this.searchBar.setFocused(false);
            this.setFocused(this.countField);
            return true;
        }
        // 左上角控件优先处理：切换按钮 + 数量输入框
        if (this.closeBehaviorButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.countField.mouseClicked(mouseX, mouseY, button)) {
            // 手动实现聚焦分发：EditBox 的 mouseClicked 只算光标位置，不设置焦点，
            // 不 setFocused 则键盘输入进不去；同时与搜索框互斥失焦。
            this.searchBar.setFocused(false);
            this.setFocused(this.countField);
            return true;
        }
        // 搜索框 + 其补全弹窗（弹窗仅在搜索框聚焦时渲染/可交互）都转发给搜索框。
        // 注意：autoComplete().isMouseOver 不看聚焦状态，必须叠加 isFocused 判断，
        // 否则未聚焦时弹窗区域（与网格第一行重叠）会吞掉实体格子的点击。
        boolean onSearchUi = mouseY < TOP
                || (this.searchBar.isFocused() && this.searchBar.autoComplete().isMouseOver(mouseX, mouseY));
        if (onSearchUi) {
            if (this.searchBar.mouseClicked(mouseX, mouseY, button)) {
                // 手动实现 Screen 默认的聚焦分发（点击才聚焦，补全弹窗与光标才显示）
                this.setFocused(this.searchBar);
                return true;
            }
            return mouseY < TOP && this.closeButton.mouseClicked(mouseX, mouseY, button);
        }

        // 点击网格区域时让搜索框失焦，收起补全弹窗
        if (this.searchBar.isFocused()) {
            this.searchBar.setFocused(false);
        }

        if (mouseX >= scrollbarX() && mouseX < scrollbarX() + SCROLLBAR_WIDTH) {
            if (maxScroll() > 0) {
                float thumbY = scrollbarThumbY();
                float thumbH = scrollbarThumbHeight();
                if (mouseY >= thumbY && mouseY <= thumbY + thumbH) {
                    this.scrollDragging = true;
                    this.dragStartOffset = this.scrollOffset;
                    this.dragStartY = mouseY;
                } else {
                    float viewport = scrollbarViewport();
                    float max = maxScroll();
                    float target = (float) (mouseY - TOP - thumbH / 2) / (viewport - thumbH) * max;
                    this.scrollOffset = Math.max(0, Math.min(max, target));
                    applyScroll();
                }
            }
            return true;
        }

        for (SummonEntityWidget widget : this.widgets) {
            if (widget.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.scrollDragging) {
            float viewport = scrollbarViewport();
            float thumbH = scrollbarThumbHeight();
            float max = maxScroll();
            float delta = (float) (mouseY - this.dragStartY);
            this.scrollOffset = Math.max(0, Math.min(max, this.dragStartOffset + delta * (max / (viewport - thumbH))));
            applyScroll();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.scrollDragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private SearchWidget createSearchBar() {
        int width = searchBarWidth();
        return new SearchWidget(searchBarX(), 5, width, 20);
    }

    private int searchBarWidth() {
        return (int) (getWindow().getGuiScaledWidth() / 4f);
    }

    /**
     * 左移避开左上角控件（切换按钮 + 数量输入框占约 175px），保持其余空间居中。
     */
    private int searchBarX() {
        int leftSpace = 178;
        int x = (int) (getWindow().getGuiScaledWidth() / 2f - searchBarWidth() / 2f);
        return Math.max(x, leftSpace);
    }

    private Button createCloseButton() {
        // 紧贴搜索框右侧
        return Button.builder(Component.literal("\u00d7"), button -> this.onClose())
                .bounds(searchBarX() + searchBarWidth() + 6, 5, 20, 20)
                .build();
    }

    private Button createCloseBehaviorButton() {
        Button button = Button.builder(closeBehaviorLabel(), btn -> {
                    SummonerUiPrefs.setCloseAfterSummon(!SummonerUiPrefs.closeAfterSummon());
                    btn.setMessage(closeBehaviorLabel());
                })
                .bounds(5, 5, 62, 20)
                .build();
        return button;
    }

    private Component closeBehaviorLabel() {
        return Component.translatable(SummonerUiPrefs.closeAfterSummon()
                ? "gui.plume_summoner.close_after"
                : "gui.plume_summoner.keep_open");
    }

    private EditBox createCountField() {
        EditBox field = new EditBox(Minecraft.getInstance().font, 132, 5, 40, 20,
                Component.translatable("gui.plume_summoner.summon_count"));
        field.setMaxLength(3);
        field.setFilter(s -> s.chars().allMatch(Character::isDigit));
        field.setValue(String.valueOf(SummonerUiPrefs.summonCount()));
        return field;
    }

    /**
     * 数量输入框失焦时校验：清空/非法/非整数 → 重置为默认 1；合法非 1 → 存档保持。
     */
    private void commitCount() {
        String text = this.countField.getValue();
        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            value = 0;
        }
        if (value <= 0) {
            this.countField.setValue("1");
            SummonerUiPrefs.setSummonCount(1);
        } else {
            // 规范化显示：去掉前导零（如 01 -> 1）
            this.countField.setValue(String.valueOf(value));
            SummonerUiPrefs.setSummonCount(value);
        }
    }

    private void drawCountLabel(GuiGraphics guiGraphics) {
        guiGraphics.drawString(this.font,
                Component.translatable("gui.plume_summoner.summon_count"),
                75, 9, 0xFFFFFF);
    }

    private Window getWindow() {
        return Minecraft.getInstance().getWindow();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}