package plume.summoner.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import plume.summoner.PlumeSummoner;

/**
 * 客户端专用构造器（不会在专用服务器加载）：
 * 注册 NeoForge 内置通用配置界面，Mod List → Plume Summoner → Config 按钮即可用，
 * 自动为 ModConfigSpec 的条目（killsToUnlock / blacklist）生成可编辑的 GUI。
 */
@Mod(value = PlumeSummoner.MOD_ID, dist = Dist.CLIENT)
public class PlumeSummonerClientMod {

    public PlumeSummonerClientMod(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
