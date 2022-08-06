package at.haha007.edenminigames.message;

import lombok.SneakyThrows;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MessageHandler {
    private final Map<String, Message> messages = new HashMap<>();

    @SneakyThrows
    public MessageHandler(JavaPlugin plugin) {
        //load configs
        File file = new File(plugin.getDataFolder(), "messages.yml");
        Reader resource = new InputStreamReader(Objects.requireNonNull(plugin.getResource("messages.yml")));
        plugin.saveResource("messages.yml", false);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(resource);

        //check for missing entries
        boolean changed = false;
        for (String key : defaults.getKeys(false)) {
            if (cfg.contains(key))
                continue;
            changed = true;
            cfg.set(key, defaults.get(key));
        }

        //if missing entries were added save the file
        if (changed) cfg.save(file);

        //load messages
        for (String key : cfg.getKeys(false)) {
            messages.put(key, Message.parse(Objects.requireNonNull(cfg.getConfigurationSection(key))));
        }
    }

    public void sendMessage(String key, Player player, String... args) {
        if (player == null)
            return;
        Message msg = messages.get(key);
        msg.sendMessage(player, args);
    }
}
