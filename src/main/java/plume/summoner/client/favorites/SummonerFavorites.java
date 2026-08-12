package plume.summoner.client.favorites;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;
import plume.summoner.PlumeSummoner;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * 收藏数据：纯客户端，跟随当前存档（不跨存档）。
 * - 单机：存在世界目录 saves/&lt;name&gt;/plume_summoner-favorites.dat
 * - 联机：按服务器 ip 分别存游戏目录下 plume_summoner-favorites-&lt;ip&gt;.dat
 * 进入世界时调用 {@link #loadForCurrentWorld()} 加载，修改即保存。
 */
public final class SummonerFavorites {
    private static final String FILE_NAME = PlumeSummoner.MOD_ID + "-favorites.dat";
    private static final String NBT_KEY = "favorites";

    private static final Set<String> favorites = new HashSet<>();

    private SummonerFavorites() {
    }

    public static boolean isFavorite(EntityType<?> type) {
        return favorites.contains(key(type));
    }

    public static void toggle(EntityType<?> type) {
        String key = key(type);
        if (!favorites.remove(key)) {
            favorites.add(key);
        }
        save();
    }

    private static String key(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
    }

    /**
     * 进入世界时调用：按当前存档加载收藏。
     */
    public static void loadForCurrentWorld() {
        favorites.clear();
        try {
            Path file = favoritesFile();
            if (file == null) {
                return;
            }
            CompoundTag compound = NbtIo.read(file);
            if (compound == null) {
                return;
            }
            ListTag list = compound.getList(NBT_KEY, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); ++i) {
                favorites.add(list.getString(i));
            }
        } catch (Exception exception) {
            PlumeSummoner.LOGGER.error("Couldn't load favorites list", exception);
        }
    }

    public static void save() {
        try {
            Path file = favoritesFile();
            if (file == null) {
                return;
            }
            ListTag list = new ListTag();
            for (String s : favorites) {
                list.add(StringTag.valueOf(s));
            }
            CompoundTag compound = new CompoundTag();
            compound.put(NBT_KEY, list);
            NbtIo.write(compound, file);
        } catch (Exception exception) {
            PlumeSummoner.LOGGER.error("Couldn't save favorites list", exception);
        }
    }

    private static Path favoritesFile() {
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
