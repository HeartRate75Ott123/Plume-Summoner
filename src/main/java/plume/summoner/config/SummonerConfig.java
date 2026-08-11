package plume.summoner.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class SummonerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_SUMMONS_PER_PLAYER = BUILDER
            .comment("A single player can only have this many mobs summoned at once.")
            .defineInRange("maxSummonsPerPlayer", 10, 1, 50);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    private SummonerConfig() {
    }
}