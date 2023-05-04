package at.haha007.edenminigames;

import at.haha007.edenminigames.games.Game;
import at.haha007.edenminigames.games.connectfour.ConnectFourGame;
import at.haha007.edenminigames.games.creepermadness.CreeperMadnessGame;
import at.haha007.edenminigames.games.jnr.JumpAndRunGame;
import at.haha007.edenminigames.games.mensch.MenschGame;
import at.haha007.edenminigames.games.tetris.TetrisGame;
import at.haha007.edenminigames.games.tntfight.TntFightGame;
import at.haha007.edenminigames.message.MessageHandler;
import at.haha007.edenminigames.placeholder.PlaceholderManager;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Accessors(fluent = true)
public final class EdenMinigames extends JavaPlugin implements Listener {

    private static EdenMinigames instance;
    private MinigameCommand command;
    @Getter
    private final List<Game> registeredGames = new ArrayList<>();
    @Getter
    private final Map<String, Class<? extends Game>> allGames = Map.of(
            "mensch", MenschGame.class,
            "tetris", TetrisGame.class,
            "jnr", JumpAndRunGame.class,
            "connect4", ConnectFourGame.class,
            "tnt_fight", TntFightGame.class,
            "creeper_madness", CreeperMadnessGame.class
    );
    private MessageHandler messageHandler;
    @Getter
    private PlaceholderManager placeholderManager;

    @Override
    public void onEnable() {
        instance = this;

        loadConfig();

        new CommandBlocker();

        //register minigames
        List<String> enabledGames = getConfig().getStringList("enabled");
        List<String> disabledGames = allGames.keySet().stream().filter(k -> !enabledGames.contains(k)).toList();
        getConfig().set("disabled", disabledGames);
        for (Iterator<String> iterator = enabledGames.iterator(); iterator.hasNext(); ) {
            String enabledGame = iterator.next();
            Class<? extends Game> game = allGames.get(enabledGame);
            if (game == null) {
                iterator.remove();
                continue;
            }
            try {
                registeredGames.add(game.getDeclaredConstructor().newInstance());
                getLogger().info("Registered minigame: " + enabledGame);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        placeholderManager = new PlaceholderManager();
        placeholderManager.register();

        command = new MinigameCommand(getConfig().getString("command", "games"));

        saveConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        //unregister minigames
        for (Game registeredMinigame : registeredGames) {
            registeredMinigame.activePlayers().forEach(registeredMinigame::stop);
        }
        if (placeholderManager != null) {
            placeholderManager.unregister();
            placeholderManager = null;
        }
        registeredGames.clear();

        //unregister command
        command.unregister();
        HandlerList.unregisterAll((Plugin) this);
    }

    private void loadConfig() {
        messageHandler = new MessageHandler(this);
        saveDefaultConfig();
        reloadConfig();
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
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
                ConfigurationSection section = config.getConfigurationSection(k);
                if (section == null) section = config.createSection(k);
                setDefaults(section, child);
            }
        });
    }

    public void reload() {
        onDisable();
        onEnable();
    }

    public static boolean isInGame(Player player) {
        return instance.registeredGames.stream().anyMatch(game -> game.activePlayers().contains(player));
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

    public static MessageHandler messenger() {
        return instance().messageHandler;
    }

}
