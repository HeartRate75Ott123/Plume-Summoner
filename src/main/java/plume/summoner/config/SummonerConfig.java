package plume.summoner.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class SummonerConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue MAX_SUMMONS_PER_PLAYER = BUILDER
            .comment("A single player can only have this many mobs summoned at once.")
            .defineInRange("maxSummonsPerPlayer", 10, 1, 50);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    private SummonerConfig() {
    }
}