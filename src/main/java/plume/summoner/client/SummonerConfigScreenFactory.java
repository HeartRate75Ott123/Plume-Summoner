package plume.summoner.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import plume.summoner.config.SummonerConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cloth Config 自动生成的配置界面（Mod List → Config → Plume Summoner）。
 * 保存时写回 ForgeConfigSpec（set + save 立即落盘，无需重启）。
 */
public final class SummonerConfigScreenFactory {
    private static final Component TITLE = Component.translatable("gui.plume_summoner.config.title");
    private static final Component GENERAL = Component.translatable("gui.plume_summoner.config.category.general");
    private static final Component KILLS_TO_UNLOCK = Component.translatable("plume_summoner.configuration.killsToUnlock");
    private static final Component BLACKLIST = Component.translatable("plume_summoner.configuration.blacklist");

    private SummonerConfigScreenFactory() {
    }

    public static Screen create(Screen parent) {
        int[] kills = {SummonerConfig.KILLS_TO_UNLOCK.get()};
        List<String> blacklist = new ArrayList<>(SummonerConfig.BLACKLIST.get());

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(TITLE)
                .setSavingRunnable(() -> {
                    SummonerConfig.KILLS_TO_UNLOCK.set(kills[0]);
                    // set() 会用 isValidEntry 校验每个条目，格式非法的条目（如无冒号）会抛异常
                    // 导致整份配置写不进去，这里先按加载时的规则过滤掉（与"无效条目被忽略"语义一致）
                    SummonerConfig.BLACKLIST.set(blacklist.stream()
                            .filter(SummonerConfig::isValidEntry)
                            .toList());
                    SummonerConfig.SPEC.save();
                });

        ConfigEntryBuilder entry = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(GENERAL);

        general.addEntry(entry.startIntField(KILLS_TO_UNLOCK, SummonerConfig.KILLS_TO_UNLOCK.get())
                .setDefaultValue(1)
                .setMin(1)
                .setMax(1000)
                .setSaveConsumer(value -> kills[0] = value)
                .build());

        general.addEntry(new BorderedStringListEntry(
                BLACKLIST,
                blacklist,
                false,
                Optional::empty,
                value -> {
                    blacklist.clear();
                    blacklist.addAll(value);
                },
                List::of,
                Component.translatable("text.cloth-config.reset_value")));

        return builder.build();
    }
}