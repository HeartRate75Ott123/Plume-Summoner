package plume.summoner.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.List;

public final class SummonerConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue MAX_SUMMONS_PER_PLAYER = BUILDER
            .comment("A single player can only have this many mobs summoned at once.")
            .defineInRange("maxSummonsPerPlayer", 10, 1, 50);

    /**
     * 解锁一种生物所需的击杀次数，默认 1。
     * 服务端在击杀事件时读取判定（联机时以服务器配置为准）。
     */
    public static final ForgeConfigSpec.IntValue KILLS_TO_UNLOCK = BUILDER
            .comment("How many kills are required to unlock a mob for summoning.")
            .defineInRange("killsToUnlock", 1, 1, 1000);

    /**
     * 召唤菜单黑名单：格式 "modid:entityid"（如 minecraft:zombie）。
     * 无效条目（格式不合法）在加载时被自动跳过，不会导致崩溃。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLIST = BUILDER
            .comment(
                    "Entities hidden from the summon menu, one per entry, format: modid:entityid (e.g. minecraft:zombie).",
                    "Invalid entries are ignored.")
            .defineListAllowEmpty("blacklist", List.of(), SummonerConfig::isValidEntry);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    /**
     * 空条目必须通过校验：Forge 配置界面用 spec.test 决定"删除/完成"按钮是否可用，
     * 若空条目被拒绝，含空条目的列表将无法删除任何条目（死锁）。空条目运行时无副作用。
     * 非空条目仍严格校验 modid:entityid 格式。
     */
    public static boolean isValidEntry(Object o) {
        return o instanceof String s && (s.isEmpty() || s.matches("[a-z0-9_\\-.]+:[a-z0-9_./\\-]+"));
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    private SummonerConfig() {
    }
}