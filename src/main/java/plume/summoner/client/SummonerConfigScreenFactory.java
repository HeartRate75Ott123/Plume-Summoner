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
    private static final Component MAX_SUMMONS = Component.translatable("gui.plume_summoner.config.maxSummons");
    private static final Component KILLS_TO_UNLOCK = Component.translatable("plume_summoner.configuration.killsToUnlock");
    private static final Component BLACKLIST = Component.translatable("plume_summoner.configuration.blacklist");

    private SummonerConfigScreenFactory() {
    }

    public static Screen create(Screen parent) {
        int[] maxSummons = {SummonerConfig.MAX_SUMMONS_PER_PLAYER.get()};
        int[] kills = {SummonerConfig.KILLS_TO_UNLOCK.get()};
        List<String> blacklist = new ArrayList<>(SummonerConfig.BLACKLIST.get());

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(TITLE)
                .setSavingRunnable(() -> {
                    SummonerConfig.MAX_SUMMONS_PER_PLAYER.set(maxSummons[0]);
                    SummonerConfig.KILLS_TO_UNLOCK.set(kills[0]);
                    SummonerConfig.BLACKLIST.set(List.copyOf(blacklist));
                    SummonerConfig.SPEC.save();
                });

        ConfigEntryBuilder entry = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(GENERAL);

        general.addEntry(entry.startIntField(MAX_SUMMONS, SummonerConfig.MAX_SUMMONS_PER_PLAYER.get())
                .setDefaultValue(10)
                .setMin(1)
                .setMax(50)
                .setSaveConsumer(value -> maxSummons[0] = value)
                .build());

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