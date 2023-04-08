package at.haha007.edenminigames.message;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
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

    public void save(ConfigurationSection cfg) {
        cfg.set("chat", chat);
        cfg.set("actionbar", actionbar);
        if (sound != null) {
            cfg.set("sound.key", sound.name());
            cfg.set("sound.source", sound.source().name());
            cfg.set("sound.volume", sound.volume());
            cfg.set("sound.pitch", sound.pitch());
        }
        ConfigurationSection titleCfg = cfg.createSection("title");
        if (title != null)
            title.save(titleCfg);
        else
            titleCfg.set("title", null);
    }

    public void sendMessage(Audience audience, Player papiParsed, String... args) {
        Optional.ofNullable(chat)
                .map(s -> PlaceholderAPI.setPlaceholders(papiParsed, s))
                .map(s -> setPlaceholders(s, args))
                .map(mm::deserialize)
                .ifPresent(audience::sendMessage);
        Optional.ofNullable(actionbar)
                .map(s -> PlaceholderAPI.setPlaceholders(papiParsed, s))
                .map(s -> setPlaceholders(s, args))
                .map(mm::deserialize)
                .ifPresent(audience::sendActionBar);
        Optional.ofNullable(sound).ifPresent(audience::playSound);
        Optional.ofNullable(title).ifPresent(t -> t.sendMessage(audience, papiParsed));
    }

    public void chat(String chat) {
        this.chat = chat;
    }

    public String chat() {
        return chat;
    }

    public void actionbar(String actionbar) {
        this.actionbar = actionbar;
    }

    public String actionbar() {
        return actionbar;
    }

    public void sound(Sound sound) {
        this.sound = sound;
    }

    public Sound sound() {
        return sound;
    }

    public void title(MessageTitle title) {
        this.title = title;
    }

    public MessageTitle title() {
        return title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        if (!Objects.equals(chat, message.chat)) return false;
        if (!Objects.equals(actionbar, message.actionbar)) return false;
        if (!Objects.equals(sound, message.sound)) return false;
        return Objects.equals(title, message.title);
    }

    @Override
    public int hashCode() {
        int result = chat != null ? chat.hashCode() : 0;
        result = 31 * result + (actionbar != null ? actionbar.hashCode() : 0);
        result = 31 * result + (sound != null ? sound.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        return result;
    }

    public static class MessageTitle {
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

        public void save(ConfigurationSection titleCfg) {
            titleCfg.set("title", title);
            titleCfg.set("subtitle", subtitle);
            if (times != null) {
                titleCfg.set("in", times.fadeIn().toMillis() / 50);
                titleCfg.set("stay", times.stay().toMillis() / 50);
                titleCfg.set("out", times.fadeOut().toMillis() / 50);
            } else {
                titleCfg.set("in", 10);
                titleCfg.set("stay", 70);
                titleCfg.set("out", 20);
            }
        }

        public void sendMessage(Audience audience, Player papiParsed, String... args) {
            String t = Optional.ofNullable(this.title)
                    .map(k -> PlaceholderAPI.setPlaceholders(papiParsed, k))
                    .map(s -> setPlaceholders(s, args))
                    .orElse("");
            String s = Optional.ofNullable(subtitle)
                    .map(k -> PlaceholderAPI.setPlaceholders(papiParsed, k))
                    .map(str -> setPlaceholders(str, args))
                    .orElse("");
            Title title = Title.title(mm.deserialize(t), mm.deserialize(s), times);
            audience.showTitle(title);
        }

        public void title(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }

        public void subtitle(String subtitle) {
            this.subtitle = subtitle;
        }

        public String subtitle() {
            return subtitle;
        }

        public void times(Title.Times times) {
            if(times == null)
                times = Title.Times.times(Duration.ofMillis(10 * 50), Duration.ofMillis(70 * 50), Duration.ofMillis(20 * 50));
            this.times = times;
        }

        public Title.Times times() {
            return times;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MessageTitle that = (MessageTitle) o;

            if (!Objects.equals(title, that.title)) return false;
            if (!Objects.equals(subtitle, that.subtitle)) return false;
            return Objects.equals(times, that.times);
        }

        @Override
        public int hashCode() {
            int result = title != null ? title.hashCode() : 0;
            result = 31 * result + (subtitle != null ? subtitle.hashCode() : 0);
            result = 31 * result + (times != null ? times.hashCode() : 0);
            return result;
        }
    }

    private static String setPlaceholders(String s, String... args) {
        for (int i = 0; i < args.length; i++) {
            s = s.replace("{" + i + "}", args[i]);
        }
        return s;
    }
}
