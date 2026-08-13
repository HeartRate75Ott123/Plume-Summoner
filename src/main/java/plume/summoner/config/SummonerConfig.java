package plume.summoner.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.List;

public final class SummonerConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    /**
     * 解锁一种生物所需的击杀次数，默认 1。
     * 服务端在击杀事件时读取判定（联机时以服务器配置为准）。
     */
    public static final ForgeConfigSpec.IntValue KILLS_TO_UNLOCK = BUILDER
            .comment("How many kills are required to unlock a mob for summoning.")
            .defineInRange("killsToUnlock", 1, 1, 1000);

    /**
     * 单次召唤数量上限，默认 50。
     * 服务端强制截断：客户端即使输入更大的数量，也不会超过此上限（防刷实体）。
     */
    public static final ForgeConfigSpec.IntValue MAX_SUMMON_COUNT = BUILDER
            .comment("Maximum number of mobs summonable in a single request, default 50.",
                    "The server clamps the count to this value even if the client requests more.")
            .defineInRange("maxSummonCount", 50, 1, 1000);

    /**
     * 召唤菜单黑名单：格式 "modid:entityid"（如 minecraft:zombie），或关键字 "all"。
     * "all" 表示所有生物均被隐藏，此时白名单中明确列出的生物作为例外仍显示。
     * 无效条目（格式不合法）在加载时被自动跳过，不会导致崩溃。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLIST = BUILDER
            .comment(
                    "Entities hidden from the summon menu, one per entry, format: modid:entityid (e.g. minecraft:zombie).",
                    "Use \"all\" to hide every entity; entries in the whitelist are then shown as exceptions.",
                    "Invalid entries are ignored.")
            .defineListAllowEmpty("blacklist", List.of(), SummonerConfig::isValidEntry);

    /**
     * 召唤菜单白名单：格式 "modid:entityid"（如 minecraft:zombie），或关键字 "all"。
     * 默认为 "all"（所有生物均可见）。黑名单优先：黑名单有内容时会从白名单结果中减去。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WHITELIST = BUILDER
            .comment(
                    "Entities shown in the summon menu, one per entry, format: modid:entityid (e.g. minecraft:zombie).",
                    "Defaults to \"all\" (every entity visible). The blacklist is applied on top of this.",
                    "Invalid entries are ignored.")
            .defineListAllowEmpty("whitelist", List.of("all"), SummonerConfig::isValidEntry);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    /**
     * 空条目必须通过校验：Forge 配置界面用 spec.test 决定"删除/完成"按钮是否可用，
     * 若空条目被拒绝，含空条目的列表将无法删除任何条目（死锁）。空条目运行时无副作用。
     * 非空条目允许关键字 "all" 或严格 modid:entityid 格式。
     */
    public static boolean isValidEntry(Object o) {
        return o instanceof String s && (s.isEmpty() || s.equalsIgnoreCase("all")
                || s.matches("[a-z0-9_\\-.]+:[a-z0-9_./\\-]+"));
    }

    /**
     * 实体是否在召唤菜单中显示、其击杀是否计入解锁：
     * 黑名单优先——黑名单为 "all" 时仅白名单明确列出的实体可见；黑名单含具体实体时减去该实体。
     */
    public static boolean isAllowed(EntityType<?> type) {
        String key = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        List<? extends String> whitelist = WHITELIST.get();
        List<? extends String> blacklist = BLACKLIST.get();
        boolean inWhitelist = whitelist.contains(key);
        boolean whitelistAll = whitelist.contains("all");
        boolean inBlacklist = blacklist.contains(key);
        boolean blacklistAll = blacklist.contains("all");
        // 黑名单 "all"：仅白名单明确列出的实体可见
        if (blacklistAll) {
            return inWhitelist;
        }
        // 黑名单含具体实体：从白名单结果中减去
        if (inBlacklist) {
            return false;
        }
        // 白名单 "all" 或明确列出则可见
        return whitelistAll || inWhitelist;
    }

    /**
     * @deprecated 语义已被 {@link #isAllowed} 取代，仅为兼容保留。
     */
    @Deprecated
    public static boolean isBlacklisted(EntityType<?> type) {
        return !isAllowed(type);
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    private SummonerConfig() {
    }
}