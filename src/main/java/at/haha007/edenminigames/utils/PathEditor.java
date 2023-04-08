package at.haha007.edenminigames.utils;

import at.haha007.edencommands.CommandContext;
import at.haha007.edencommands.annotations.AnnotatedCommandLoader;
import at.haha007.edencommands.annotations.Command;
import at.haha007.edencommands.tree.CommandBuilder;
import at.haha007.edenminigames.EdenMinigames;
import at.haha007.edenminigames.games.utils.Path;
import at.haha007.edenminigames.games.utils.PathVisualizer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PathEditor implements Listener {
    private final Map<Player, String> editing = new HashMap<>();
    private final Map<String, Path> paths = new HashMap<>();
    private final Map<Path, PathVisualizer> visualizers = new HashMap<>();

    public PathEditor(CommandBuilder<?> cmd) {
        Bukkit.getPluginManager().registerEvents(this, EdenMinigames.instance());
        AnnotatedCommandLoader loader = new AnnotatedCommandLoader(EdenMinigames.instance());
        loader.addDefaultArgumentParsers();
        loader.addAnnotated(this);
        loader.getCommands().forEach(cmd::then);
    }

    @Command("edit path_name{type:string}")
    private void editCommand(CommandContext context) {
        if (!(context.sender() instanceof Player player)) return;
        String pathName = context.parameter("path_name");
        startEditing(player, pathName);
    }

    @Command("exit")
    private void stopCommand(CommandContext context) {
        if (!(context.sender() instanceof Player player)) return;
        if (!editing.containsKey(player)) {
            player.sendMessage(Component.text("You are not editing a path!"));
            return;
        }
        stopEditing(player);
    }

    @Command("insert index{type:int}")
    private void insertCommand(CommandContext context) {
        if (!(context.sender() instanceof Player player)) return;
        if (!editing.containsKey(player)) {
            player.sendMessage(Component.text("You are not editing a path!"));
            return;
        }
        Path path = paths.get(editing.get(player));
        if (path == null) return;
        int index = context.parameter("index");
        if (index < 0 || index > path.getPath().size()) {
            player.sendMessage(Component.text("Index out of bounds!"));
            return;
        }
        path.addNode(index, player.getLocation().toVector());
        player.sendMessage(Component.text("Inserted point at index " + index));
    }

    @Command("move path")
    private void moveCommand(CommandContext context) {
        if (!(context.sender() instanceof Player player)) return;
        if (!editing.containsKey(player)) {
            player.sendMessage(Component.text("You are not editing a path!"));
            return;
        }
        Path path = paths.get(editing.get(player));
        if (path == null) return;
        Vector from = path.getStart();
        Vector to = player.getLocation().toVector();
        Vector diff = to.subtract(from);
        path.setPath(path.getPath().stream().map(v -> v.add(diff)).toList());
        player.sendMessage(Component.text("Moved path to your location"));
    }

    @Command("move node index{type:int}")
    private void moveNodeCommand(CommandContext context) {
        if (!(context.sender() instanceof Player player)) return;
        if (!editing.containsKey(player)) {
            player.sendMessage(Component.text("You are not editing a path!"));
            return;
        }
        Path path = paths.get(editing.get(player));
        if (path == null) return;
        int index = context.parameter("index");
        if (index < 0 || index >= path.getPath().size()) {
            player.sendMessage(Component.text("Index out of bounds!"));
            return;
        }
        List<Vector> points = path.getPath();
        points.set(index, player.getLocation().toVector());
        path.setPath(points);
        player.sendMessage(Component.text("Moved node to your location"));
    }

    @Command("near")
    private void nearCommand(CommandContext context) {
        if (!(context.sender() instanceof Player player)) return;
        if (!editing.containsKey(player)) {
            player.sendMessage(Component.text("You are not editing a path!"));
            return;
        }
        Path path = paths.get(editing.get(player));
        if (path == null) return;
        int nearest = -1;
        double nearestDistance = Double.MAX_VALUE;
        List<Vector> points = path.getPath();
        Vector loc = player.getLocation().toVector();
        for (int i = 0; i < points.size(); i++) {
            double distance = points.get(i).distanceSquared(loc);
            if (distance < nearestDistance) {
                nearest = i;
                nearestDistance = distance;
            }
        }

        player.sendMessage(Component.text("Nearest point is %d with distance %.2f".formatted(nearest, Math.sqrt(nearestDistance))));
    }

    @Command("delete path_name{type:string}")
    private void deleteCommand(CommandContext context) {
        if (!(context.sender() instanceof Player player)) return;
        String pathName = context.parameter("path_name");
        File file = getPathFile(pathName);
        if (!file.exists()) {
            player.sendMessage(Component.text("Path " + pathName + " does not exist!"));
            return;
        }
        file.delete();
        player.sendMessage(Component.text("Deleted path " + pathName));
    }


    public void startEditing(Player player, String pathName) {
        if (paths.containsKey(pathName)) {
            player.sendMessage(Component.text("Someone is already editing this path!"));
            return;
        }
        if (editing.containsKey(player)) stopEditing(player);
        editing.put(player, pathName);
        Path path = loadPath(pathName);
        if (path == null) {
            path = new Path(player.getWorld(), List.of(player.getLocation().toVector()));
            paths.put(pathName, path);
        }
        PathVisualizer visualizer = new PathVisualizer(path);
        visualizers.put(path, visualizer);
        visualizer.show(player);
        player.sendMessage(Component.text("Started editing path " + pathName));
    }

    public void stopEditing(Player player) {
        String pathName = editing.remove(player);
        if (pathName == null) return;
        Path path = paths.remove(pathName);
        if (path == null) return;
        PathVisualizer visualizer = visualizers.remove(path);
        if (visualizer == null) return;
        visualizer.hide(player);
        savePath(path, pathName);
        player.sendMessage(Component.text("Stopped editing path " + pathName));
    }

    private File getPathFile(String pathName) {
        return new File(EdenMinigames.instance().getDataFolder(), "paths/" + pathName + ".yml");
    }

    private Path loadPath(YamlConfiguration cfg) {
        String worldName = cfg.getString("world");
        List<Vector> vectors = new ArrayList<>();
        ConfigurationSection section = cfg.getConfigurationSection("path");
        if (section == null) throw new IllegalArgumentException("path is null");
        for (String key : section.getKeys(false)) {
            Vector vector = section.getVector(key);
            vectors.add(vector);
        }
        if (worldName == null) throw new IllegalArgumentException("world is null");
        return new Path(Bukkit.getWorld(worldName), vectors);
    }

    private Path loadPath(String pathName) {
        File file = getPathFile(pathName);
        if (!file.exists()) return null;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        return loadPath(cfg);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored") // we don't care if the file already exists
    private void savePath(Path path, String pathName) {
        File folder = new File(EdenMinigames.instance().getDataFolder(), "paths");
        if (!folder.exists()) folder.mkdirs();
        File file = getPathFile(pathName);
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("world", path.getWorld().getName());
        ConfigurationSection section = cfg.createSection("path");
        for (int i = 0; i < path.size(); i++) {
            section.set(String.valueOf(i), path.get(i));
        }
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            cfg.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
