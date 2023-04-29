package at.haha007.edenminigames.utils;

import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class ConfigUtils {
    public static BlockVector3 blockVector3(ConfigurationSection config) {
        return BlockVector3.at(config.getInt("x"), config.getInt("y"), config.getInt("z"));
    }

    @NotNull
    public static Location location(ConfigurationSection config) {
        String worldName = config.getString("world");
        if (worldName == null) throw new IllegalArgumentException("world is null");
        World world = Bukkit.getWorld(worldName);
        if (world == null) throw new IllegalArgumentException("world not found: " + worldName);
        int x = config.getInt("x");
        int y = config.getInt("y");
        int z = config.getInt("z");
        float yaw = (float) config.getDouble("yaw", 0);
        float pitch = (float) config.getDouble("pitch", 0);
        return new Location(world, x, y, z, yaw, pitch);
    }
}
