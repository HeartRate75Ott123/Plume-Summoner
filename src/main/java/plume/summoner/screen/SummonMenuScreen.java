package plume.summoner.screen;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
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
    private final List<SummonEntityWidget> widgets = new ArrayList<>();
    private String lastSearch = "";
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
        this.searchBar.setResponder(text -> {
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
        if (!this.widgets.isEmpty() && maxScroll() > 0) {
            this.scrollOffset = Math.max(0, Math.min(maxScroll(), this.scrollOffset - (float) delta * SCROLL_SPEED));
            applyScroll();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseY < TOP) {
            return this.searchBar.mouseClicked(mouseX, mouseY, button)
                    || this.closeButton.mouseClicked(mouseX, mouseY, button);
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
        return new SearchWidget(
                (int) (getWindow().getGuiScaledWidth() / 2f - (getWindow().getGuiScaledWidth() / 4f / 2) - 5),
                5,
                (int) (getWindow().getGuiScaledWidth() / 4f),
                20);
    }

    private Button createCloseButton() {
        return Button.builder(Component.literal("\u00d7"), button -> this.onClose())
                .bounds((int) (getWindow().getGuiScaledWidth() / 2f + (getWindow().getGuiScaledWidth() / 8f) + 35), 5, 20, 20)
                .build();
    }

    private Window getWindow() {
        return Minecraft.getInstance().getWindow();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}