package plume.summoner.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;
import plume.summoner.PlumeSummoner;

import java.nio.file.Path;

/**
 * 界面偏好（纯客户端，跟随当前存档，不跨存档）：
 * - closeAfterSummon：召唤后是否关闭界面（true=关闭，false=保持），默认 true
 * - summonCount：单次召唤数量，默认 1
 * 存储方式与收藏一致：单机存世界目录、联机按服务器 ip 存游戏目录。
 * 进入世界时调用 {@link #loadForCurrentWorld()}，修改即保存。
 */
public final class SummonerUiPrefs {
    private static final String FILE_NAME = PlumeSummoner.MOD_ID + "-ui-prefs.dat";
    private static final String NBT_CLOSE = "close_after_summon";
    private static final String NBT_COUNT = "summon_count";

    private static boolean closeAfterSummon = true;
    private static int summonCount = 1;

    private SummonerUiPrefs() {
    }

    public static boolean closeAfterSummon() {
        return closeAfterSummon;
    }

    public static int summonCount() {
        return summonCount;
    }

    public static void setCloseAfterSummon(boolean value) {
        closeAfterSummon = value;
        save();
    }

    /**
     * 设置单次召唤数量（仅接受正整数）。
     */
    public static void setSummonCount(int value) {
        summonCount = Math.max(1, value);
        save();
    }

    public static void loadForCurrentWorld() {
        closeAfterSummon = true;
        summonCount = 1;
        try {
            Path file = prefsFile();
            if (file == null) {
                return;
            }
            CompoundTag compound = NbtIo.read(file);
            if (compound == null) {
                return;
            }
            if (compound.contains(NBT_CLOSE)) {
                closeAfterSummon = compound.getBoolean(NBT_CLOSE);
            }
            if (compound.contains(NBT_COUNT)) {
                summonCount = Math.max(1, compound.getInt(NBT_COUNT));
            }
        } catch (Exception exception) {
            PlumeSummoner.LOGGER.error("Couldn't load UI prefs", exception);
        }
    }

    public static void save() {
        try {
            Path file = prefsFile();
            if (file == null) {
                return;
            }
            CompoundTag compound = new CompoundTag();
            compound.putBoolean(NBT_CLOSE, closeAfterSummon);
            compound.putInt(NBT_COUNT, summonCount);
            NbtIo.write(compound, file);
        } catch (Exception exception) {
            PlumeSummoner.LOGGER.error("Couldn't save UI prefs", exception);
        }
    }

    private static Path prefsFile() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            return mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        }
        String key = mc.getCurrentServer() != null
                ? "server_" + mc.getCurrentServer().ip.replaceAll("[^a-zA-Z0-9_.-]", "_")
                : "unknown";
        return FMLPaths.GAMEDIR.get().resolve(key + "-" + FILE_NAME);
    }
}
