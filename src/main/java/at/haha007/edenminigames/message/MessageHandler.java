package at.haha007.edenminigames.message;

import at.haha007.edenminigames.EdenMinigames;
import lombok.SneakyThrows;
import net.kyori.adventure.audience.Audience;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

public class MessageHandler {
    private final Map<String, Message> messages = new HashMap<>();
    private final Queue<String> sentMessages = new LinkedList<>();

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
        for (String sectionKey : cfg.getKeys(false)) {
            ConfigurationSection section = cfg.getConfigurationSection(sectionKey);
            if (section == null)
                throw new RuntimeException("Invalid message section: " + sectionKey);
            for (String key : section.getKeys(false)) {
                messages.put(sectionKey + "." + key, Message.parse(Objects.requireNonNull(section.getConfigurationSection(key))));
            }
        }
    }

    public void save() {
        JavaPlugin plugin = EdenMinigames.instance();
        File file = new File(plugin.getDataFolder(), "messages.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        Map<String, Message> messagesInYml = new HashMap<>();
        for (String sectionKey : cfg.getKeys(false)) {
            ConfigurationSection section = cfg.getConfigurationSection(sectionKey);
            if (section == null)
                throw new RuntimeException("Invalid message section: " + sectionKey);
            for (String key : section.getKeys(false)) {
                messagesInYml.put(sectionKey + "." + key, Message.parse(Objects.requireNonNull(section.getConfigurationSection(key))));
            }
        }
        messages.entrySet().stream().filter(e -> !e.getValue().equals(messagesInYml.get(e.getKey()))).forEach(e -> {
            ConfigurationSection section = cfg.createSection(e.getKey());
            e.getValue().save(section);
        });
        try {
            cfg.save(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Message message(String key) {
        return messages.get(key);
    }

    public void sendMessage(String key, Player player, String... args) {
        sendMessage(key, player, player, args);
    }

    public Queue<String> sentMessages() {
        return sentMessages;
    }

    public void sendMessage(String key, Audience audience, Player papiParsed, String... args) {
        if (audience == null)
            return;
        sentMessages.add(key);
        if (sentMessages.size() > 100)
            sentMessages.poll();
        Message msg = messages.get(key);
        if (msg == null) {
            msg = new Message();
            msg.chat("<red>Missing message with %d parameters: %s".formatted(args.length, key));
            messages.put(key, msg);
            save();
        }
        msg.sendMessage(audience, papiParsed, args);
    }

    public Set<String> keys() {
        return messages.keySet();
    }
}
