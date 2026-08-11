package plume.summoner.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class SearchWidget extends EditBox {
    public SearchWidget(int x, int y, int width, int height) {
        super(Minecraft.getInstance().font, x, y, width, height, Component.empty());
    }
}