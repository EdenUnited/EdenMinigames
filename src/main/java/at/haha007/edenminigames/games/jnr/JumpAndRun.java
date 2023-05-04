package at.haha007.edenminigames.games.jnr;

import at.haha007.edenminigames.EdenMinigames;
import at.haha007.edenminigames.games.utils.Path;
import at.haha007.edenminigames.games.utils.PathVisualizer;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JumpAndRun {
    private final List<CheckPoint> checkPoints = new ArrayList<>();
    private final World world;
    private final String key;
    private Path path;
    private PathVisualizer visualizer;

    public static List<JumpAndRun> loadAll() {
        List<JumpAndRun> list = new ArrayList<>();
        File jnrFolder = new File(EdenMinigames.instance().getDataFolder(), "jnr");
        if (!jnrFolder.exists()) jnrFolder.mkdirs();
        for (File file : jnrFolder.listFiles()) {
            if (!file.getName().endsWith(".yml")) continue;
            String key = file.getName().replace(".yml", "");
            JumpAndRun jnr = new JumpAndRun(key);
            list.add(jnr);
        }
        return list;
    }

    public JumpAndRun(String key) {
        this.key = key;
        File file = getFile();
        if (!file.exists()) {
            throw new IllegalArgumentException("JumpAndRun " + key + " does not exist!");
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        world = Bukkit.getWorld(Objects.requireNonNull(config.getString("world")));
        ConfigurationSection cpSection = config.getConfigurationSection("checkpoints");
        if (cpSection == null) throw new IllegalArgumentException("checkpoints is null");
        for (String checkpointKey : cpSection.getKeys(false)) {
            String value = cpSection.getString(checkpointKey);
            if (value == null) throw new IllegalArgumentException("checkpoint " + checkpointKey + " is null");
            String[] split = value.split(",");
            int[] ints = new int[3];
            for (int i = 0; i < 3; i++) {
                ints[i] = Integer.parseInt(split[i]);
            }
            float[] floats = new float[2];
            for (int i = 0; i < 2; i++) {
                floats[i] = Float.parseFloat(split[i + 3]);
            }
            BlockVector3 pos = BlockVector3.at(ints[0], ints[1], ints[2]);
            CheckPoint checkPoint = new CheckPoint(pos, floats[0], floats[1]);
            checkPoints.add(checkPoint);
        }
    }

    public JumpAndRun(String key, Location start) {
        this.key = key;
        this.world = start.getWorld();
        BlockVector3 pos = BlockVector3.at(start.getBlockX(), start.getBlockY(), start.getBlockZ());
        CheckPoint checkPoint = new CheckPoint(pos, start.getYaw(), start.getPitch());
        checkPoints.add(checkPoint);
    }

    public void save() throws IOException {
        File file = getFile();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("world", world.getName());
        ConfigurationSection cpSection = config.createSection("checkpoints");
        for (int i = 0; i < checkPoints.size(); i++) {
            CheckPoint checkPoint = checkPoints.get(i);
            String sb = checkPoint.getPosition().getX() + "," +
                    checkPoint.getPosition().getY() + "," +
                    checkPoint.getPosition().getZ() + "," +
                    checkPoint.getYaw() + "," +
                    checkPoint.getPitch();
            cpSection.set(String.valueOf(i), sb);
        }
        config.save(file);
    }

    public File getFile() {
        return new File(getDataFolder(), key + ".yml");
    }

    public void updateVisualizer() {
        if (checkPoints.isEmpty()) {
            path = null;
            if (visualizer != null)
                visualizer.hideAll();
            visualizer = null;
            return;
        }
        Vector halfBlock = new Vector(0.5, 0.5, 0.5);
        List<Vector> path = checkPoints.stream()
                .map(CheckPoint::getPosition)
                .map(pos -> new Vector(pos.getX(), pos.getY(), pos.getZ()))
                .map(v -> v.add(halfBlock))
                .toList();
        if (this.path != null)
            this.path.setPath(path);
        else
            this.path = new Path(world, path);
        if (this.visualizer == null)
            this.visualizer = new PathVisualizer(this.path);
    }

    public void showPath(Player player) {
        if (visualizer == null) updateVisualizer();
        if (visualizer == null) return;
        visualizer.show(player);
    }

    public void hidePath(Player player) {
        if (visualizer == null)  updateVisualizer();
        if (visualizer == null) return;
        visualizer.hide(player);
    }

    public void tpToCheckpoint(Player player, int checkpoint) {
        if (checkpoint >= checkPoints.size()) checkpoint = checkPoints.size() - 1;
        if (checkpoint < 0) checkpoint = 0;
        checkPoints.get(checkpoint).teleport(player, world);
    }

    public String getKey() {
        return key;
    }

    public World getWorld() {
        return world;
    }

    public List<CheckPoint> getCheckPoints() {
        return checkPoints;
    }

    private static File getDataFolder() {
        return new File(EdenMinigames.instance().getDataFolder(), "jnr");
    }

    public void addCheckpoint(Location location) {
        BlockVector3 pos = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        CheckPoint checkPoint = new CheckPoint(pos, location.getYaw(), location.getPitch());
        checkPoints.add(checkPoint);
    }

    public int getNearestCheckpointIndex(Location location) {
        int index = -1;
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < checkPoints.size(); i++) {
            BlockVector3 checkPoint = checkPoints.get(i).getPosition();
            Location loc = new Location(world, checkPoint.getX(), checkPoint.getY(), checkPoint.getZ());
            double distance = loc.distance(location);
            if (distance < minDistance) {
                minDistance = distance;
                index = i;
            }
        }
        return index;
    }

    public void hidePath() {
        if (visualizer == null) return;
        visualizer.hideAll();
    }
}
