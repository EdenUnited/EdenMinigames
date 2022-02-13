package at.haha007.edenminigames;

import at.haha007.edenminigames.games.tetris.Tetris;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;

public final class EdenMinigames extends JavaPlugin {

    private static EdenMinigames instance;
    private MinigameCommand command;
    private final List<Minigame> registeredMinigames = new ArrayList<>();
    private final Map<String, Class<? extends Minigame>> allGames = Map.of(
            "tetris", Tetris.class);

    @Override
    public void onEnable() {
        instance = this;
        loadConfig();

        command = new MinigameCommand(getConfig().getString("command"));
        loadGames();

        saveConfig();
        // Plugin startup logic
    }

    private void loadGames() {
        FileConfiguration cfg = getConfig();
        List<String> enabled = cfg.getStringList("enabled");
        List<String> disabled = cfg.getStringList("disabled");

        enabled.removeIf(Predicate.not(allGames::containsKey));
        disabled.removeIf(Predicate.not(allGames::containsKey));
        disabled.removeIf(enabled::contains);

        enabled.sort(String::compareTo);
        disabled.sort(String::compareTo);

        enabled.forEach(key -> {
            try {
                Constructor<? extends Minigame> constructor = allGames.get(key).getDeclaredConstructor();
                constructor.setAccessible(true);
                Minigame game = constructor.newInstance();
                registeredMinigames.add(game);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                logger().severe(String.format("Invalid constructor for game: %s", key));
                e.printStackTrace();
            }
        });

        cfg.set("enabled", enabled);
        cfg.set("disabled", disabled);
    }

    private void loadConfig() {
        saveDefaultConfig();
        reloadConfig();

        Reader defaults = getTextResource("defaults.yml");
        if (defaults == null) {
            logger().severe("The default configuration sections were not found!");
            Thread.dumpStack();
            return;
        }
        setDefaults(getConfig(), YamlConfiguration.loadConfiguration(defaults));
        saveConfig();
    }

    private void setDefaults(ConfigurationSection config, ConfigurationSection defaults) {
        defaults.getKeys(false).forEach(k -> {
            if (!config.contains(k, true)) {
                config.set(k, defaults.get(k));
            }
            if (defaults.get(k) instanceof ConfigurationSection child) {
                setDefaults(config.getConfigurationSection(k), child);
            }
        });
    }

    @Override
    public void onDisable() {
        //unregister minigames
        for (Minigame registeredMinigame : registeredMinigames) {
            registeredMinigame.unregister();
        }
        registeredMinigames.clear();

        //unregister command
        command.unregister();
    }

    public static void reload() {
        instance.onDisable();
        instance.onEnable();
    }

    public static List<Minigame> registeredGames() {
        return instance.registeredMinigames;
    }

    public static Minigame getGame(String key) {
        Class<? extends Minigame> clazz = instance.allGames.get(key);
        return instance.registeredMinigames.stream().filter(g -> g.getClass() == clazz).findAny().orElse(null);
    }

    public static boolean isBlocked(Player player) {
        return instance.registeredMinigames.stream().anyMatch(game -> game.isPlaying(player));
    }

    public static Logger logger() {
        return instance.getLogger();
    }

    public static FileConfiguration config() {
        return instance.getConfig();
    }

    public static EdenMinigames instance() {
        return instance;
    }
}
