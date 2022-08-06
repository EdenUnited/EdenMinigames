package at.haha007.edenminigames.message;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Message {
    private static final MiniMessage mm = MiniMessage.miniMessage();

    @Nullable
    private String chat;
    @Nullable
    private String actionbar;
    @Nullable
    private Sound sound;
    @Nullable
    private MessageTitle title;

    public static Message parse(ConfigurationSection cfg) {
        Message message = new Message();
        if (cfg.contains("chat")) {
            message.chat = cfg.getString("chat");
        }
        message.title = MessageTitle.parse(cfg.getConfigurationSection("title"));
        message.actionbar = cfg.getString("actionbar");
        ConfigurationSection s = cfg.getConfigurationSection("sound");
        if (s != null) {
            @Subst("minecraft:block.anvil.land") String s1 = Objects.requireNonNull(s.getString("key"));
            if (!s1.matches("([a-z\\d_\\-.]+:)?[a-z\\d_\\-./]+")) {
                throw new RuntimeException("Invalid sound in configuration: " + s1);
            }
            Key key = Key.key(s1);
            Sound.Source source = Optional.ofNullable(s.getString("source"))
                    .map(String::toUpperCase)
                    .map(Sound.Source::valueOf)
                    .orElse(Sound.Source.MASTER);
            float volume = (float) s.getDouble("volume", 1);
            float pitch = (float) s.getDouble("pitch", 1);
            message.sound = Sound.sound(key, source, volume, pitch);
        }
        return message;
    }

    public void sendMessage(Player player, String... args) {
        Optional.ofNullable(chat)
                .map(s -> PlaceholderAPI.setPlaceholders(player, s))
                .map(s -> setPlaceholders(s, args))
                .map(mm::deserialize)
                .ifPresent(player::sendMessage);
        Optional.ofNullable(actionbar)
                .map(s -> PlaceholderAPI.setPlaceholders(player, s))
                .map(s -> setPlaceholders(s, args))
                .map(mm::deserialize)
                .ifPresent(player::sendActionBar);
        Optional.ofNullable(sound).ifPresent(player::playSound);
        Optional.ofNullable(title).ifPresent(t -> t.sendMessage(player));
    }

    private static String setPlaceholders(String s, String... args) {
        for (int i = 0; i < args.length; i++) {
            s = s.replace("{" + i + "}", args[i]);
        }
        return s;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static class MessageTitle {
        @Nullable
        private String title;
        @Nullable
        private String subtitle;
        @Nullable
        private Title.Times times;

        @Nullable
        private static MessageTitle parse(@Nullable ConfigurationSection cfg) {
            if (cfg == null || !(cfg.contains("title") || cfg.contains("subtitle")))
                return null;
            MessageTitle t = new MessageTitle();
            t.title = cfg.getString("title");
            t.subtitle = cfg.getString("subtitle");
            long in = cfg.getInt("in", 10);
            long stay = cfg.getInt("stay", 70);
            long out = cfg.getInt("out", 20);
            t.times = Title.Times.times(Duration.ofMillis(in * 50), Duration.ofMillis(stay * 50), Duration.ofMillis(out * 50));
            return t;
        }

        public void sendMessage(Player player, String... args) {
            String t = Optional.ofNullable(this.title)
                    .map(k -> PlaceholderAPI.setPlaceholders(player, k))
                    .map(s -> setPlaceholders(s, args))
                    .orElse("");
            String s = Optional.ofNullable(subtitle)
                    .map(k -> PlaceholderAPI.setPlaceholders(player, k))
                    .map(str -> setPlaceholders(str, args))
                    .orElse("");
            Title title = Title.title(mm.deserialize(t), mm.deserialize(s), times);
            player.showTitle(title);
        }
    }
}
