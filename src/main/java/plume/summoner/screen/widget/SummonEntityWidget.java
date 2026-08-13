package plume.summoner.screen.widget;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Quaternionf;
import plume.summoner.PlumeSummoner;
import plume.summoner.client.PlumeSummonerClient;
import plume.summoner.client.SummonerUiPrefs;
import plume.summoner.client.favorites.SummonerFavorites;
import plume.summoner.network.NetworkHandler;
import plume.summoner.network.SummonRequestMessage;

/**
 * 召唤菜单网格格子（参照 Remorphed 的 ShapeWidget/EntityWidget）。
 * 渲染实体模型时用 try/catch 兜底（部分实体在 GUI 渲染会崩），
 * 崩溃后不再渲染该格，避免拖垮整个游戏。
 * 右上角星标：点击切换收藏（纹理素材来自 Cherished Worlds，staricon/emptystaricon）。
 */
public class SummonEntityWidget extends AbstractButton {
    private static final ResourceLocation STAR_ICON = new ResourceLocation(
            PlumeSummoner.MOD_ID, "textures/gui/staricon.png");
    private static final ResourceLocation EMPTY_STAR_ICON = new ResourceLocation(
            PlumeSummoner.MOD_ID, "textures/gui/emptystaricon.png");
    private static final int STAR_SIZE = 9;

    private final Screen parent;
    private final EntityType<?> type;
    private final Mob entity;
    private final int size;
    private final int baseY;
    private boolean crashed;
    private boolean hoveringStar;

    public SummonEntityWidget(int x, int y, int width, int height, EntityType<?> type, Mob entity, Screen parent) {
        super(x, y, width, height, Component.empty());
        this.parent = parent;
        this.type = type;
        this.entity = entity;
        this.size = (int) (25 * (1 / Math.max(entity.getBbHeight(), entity.getBbWidth())));
        this.baseY = y;
    }

    /**
     * hover 时显示的提示：星标上显示收藏切换提示，否则已解锁显示名称、未解锁显示解锁提示。
     */
    public Component tooltipText() {
        if (this.hoveringStar) {
            return Component.translatable(isFavorite()
                    ? "gui.plume_summoner.unfavorite"
                    : "gui.plume_summoner.favorite");
        }
        return unlocked()
                ? type.getDescription()
                : Component.translatable("tooltip.plume_summoner.locked");
    }

    private boolean isFavorite() {
        return SummonerFavorites.isFavorite(this.type);
    }

    private int starX() {
        return getX() + getWidth() - STAR_SIZE - 2;
    }

    private int starY() {
        return getY() + 2;
    }

    /**
     * 星标命中区放大到格子右上角约 20x20 区域，避免 9x9 星形太小点偏导致无法切换。
     */
    private boolean isOverStar(double mouseX, double mouseY) {
        return mouseX >= starX() - 4 && mouseX < starX() + STAR_SIZE + 11
                && mouseY >= starY() - 4 && mouseY < starY() + STAR_SIZE + 11;
    }

    /**
     * 滚动基准位置（滚动前 y），滚动时由屏幕统一 setPosition。
     */
    public int baseY() {
        return this.baseY;
    }

    private boolean unlocked() {
        return PlumeSummonerClient.UNLOCKED_TYPES.contains(type);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        boolean unlocked = unlocked();

        int borderColor = isHovered() ? 0xFFAAAAAA : 0xFF333333;
        guiGraphics.fill(getX() - 1, getY() - 1, getX() + getWidth() + 1, getY() + getHeight() + 1, borderColor);
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x14000000);

        if (unlocked) {
            if (!crashed) {
                renderEntity(guiGraphics);
            }
        } else {
            guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x4A000000);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, type.getDescription(),
                    getX() + getWidth() / 2, getY() + getHeight() - 30, 0x9A9A9A);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font,
                    Component.translatable("gui.plume_summoner.locked"),
                    getX() + getWidth() / 2, getY() + 8, 0xFFFFFF);
        }

        renderFavoriteStar(guiGraphics, mouseX, mouseY);
    }

    /**
     * 右上角星标：收藏显示实心星，未收藏显示空心星（无 hover 高亮，避免色块问题）。
     */
    private void renderFavoriteStar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        this.hoveringStar = isOverStar(mouseX, mouseY);
        ResourceLocation icon = isFavorite() ? STAR_ICON : EMPTY_STAR_ICON;
        guiGraphics.blit(icon, starX(), starY(), 0, 0, STAR_SIZE, STAR_SIZE, STAR_SIZE, STAR_SIZE);
    }

    /**
     * 参照 Remorphed EntityWidget.renderShape：InventoryScreen.renderEntityInInventory + 崩溃兜底。
     * 1.20.1 的签名是 6 参数（无 Vector3f 平移参数）。
     */
    private void renderEntity(GuiGraphics guiGraphics) {
        try {
            InventoryScreen.renderEntityInInventory(guiGraphics,
                    getX() + getWidth() / 2,
                    (int) (getY() + getHeight() * .75f),
                    size,
                    new Quaternionf().rotationXYZ((float) Math.PI, 0, 0),
                    new Quaternionf().rotationXYZ(0.43633232F, (float) Math.PI, (float) Math.PI),
                    entity);
        } catch (Exception e) {
            PlumeSummoner.LOGGER.error("Error while rendering {}", type, e);
            this.crashed = true;
            MultiBufferSource.BufferSource immediate = Minecraft.getInstance().renderBuffers().bufferSource();
            immediate.endBatch();
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            dispatcher.setRenderShadow(true);
            RenderSystem.getModelViewStack().popPose();
            Lighting.setupFor3DItems();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean inside = mouseX >= getX() && mouseX < getX() + getWidth()
                && mouseY >= getY() && mouseY < getY() + getHeight();
        if (inside && button == 0) {
            // 点击星标切换收藏（不触发召唤，也不改变默认排序位置）
            if (isOverStar(mouseX, mouseY)) {
                SummonerFavorites.toggle(type);
                return true;
            }
            if (unlocked()) {
                int count = SummonerUiPrefs.summonCount();
                NetworkHandler.CHANNEL.sendToServer(
                        new SummonRequestMessage(BuiltInRegistries.ENTITY_TYPE.getKey(type), count));
                if (SummonerUiPrefs.closeAfterSummon()) {
                    parent.onClose();
                }
                return true;
            }
        }
        return inside;
    }

    @Override
    public void onPress() {
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}