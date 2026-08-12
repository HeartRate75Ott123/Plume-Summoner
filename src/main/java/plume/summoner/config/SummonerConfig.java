package plume.summoner.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class SummonerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /**
     * 解锁一种生物所需的击杀次数，默认 1。
     * 服务端在击杀事件时读取判定（联机时以服务器配置为准）。
     */
    public static final ModConfigSpec.IntValue KILLS_TO_UNLOCK = BUILDER
            .comment("How many kills are required to unlock a mob for summoning.")
            .defineInRange("killsToUnlock", 1, 1, 1000);

    /**
     * 召唤菜单黑名单：格式 "modid:entityid"（如 minecraft:zombie）。
     * 无效条目（格式不合法）在加载时被自动跳过，不会导致崩溃。
     */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLIST = BUILDER
            .comment(
                    "Entities hidden from the summon menu, one per entry, format: modid:entityid (e.g. minecraft:zombie).",
                    "Invalid entries are ignored.")
            .defineListAllowEmpty("blacklist", List.of(), () -> "", SummonerConfig::isValidEntry);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean isValidEntry(Object o) {
        // 空条目必须通过校验：NeoForge 配置界面用 spec.test 决定"删除/完成"按钮是否可用，
        // 若空条目被拒绝，含空条目的列表将无法删除任何条目（死锁）。空条目运行时无副作用。
        // 非空条目仍严格校验 modid:entityid 格式。
        return o instanceof String s && (s.isEmpty() || s.matches("[a-z0-9_\\-.]+:[a-z0-9_./\\-]+"));
    }

    /**
     * 实体是否在黑名单中：黑名单实体的击杀不计入解锁计数，菜单中也不显示。
     */
    public static boolean isBlacklisted(EntityType<?> type) {
        return BLACKLIST.get().contains(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC);
        // 游戏内配置界面（Mod List → Config）由客户端专用构造器注册 NeoForge 内置 ConfigurationScreen
    }

    private SummonerConfig() {
    }
}
