package at.haha007.edenminigames.utils;

import at.haha007.edenminigames.EdenMinigames;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

public class ScoreTracker {
    private static final NamespacedKey SCORES_KEY = new NamespacedKey(EdenMinigames.instance(), "score");
    private final String key;
    private final int maxSavedScores;
    private final TreeSet<PlayerScore> scores = new TreeSet<>(Comparator.comparingLong(s -> s.score().score()));

    public ScoreTracker(String key, int maxSavedScores) {
        this.key = key;
        this.maxSavedScores = maxSavedScores;
        File file = getScoreFile();
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String s : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(s);
            if (section == null) continue;
            PlayerScore score = PlayerScore.fromYaml(section);
            scores.add(score);
        }

    }

    public void save() throws IOException {
        File file = getScoreFile();
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        YamlConfiguration config = new YamlConfiguration();
        int i = 0;
        for (PlayerScore score : scores) {
            ConfigurationSection section = config.createSection(String.valueOf(i++));
            score.toYaml(section);
        }
        config.save(file);
    }

    public void saveAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(EdenMinigames.instance(), () -> {
            try {
                save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public String getKey() {
        return key;
    }

    private File getScoreFile() {
        return new File(EdenMinigames.instance().getDataFolder(), "scores/" + key + ".yml");
    }

    public void addScore(Player player, long score) {
        try {
            long oldScore = getScore(player);
            if (oldScore > score) return;
        } catch (IllegalArgumentException ignored) {
        }
        long time = System.currentTimeMillis();
        String name = player.getName();
        String displayName = MiniMessage.miniMessage().serialize(player.displayName());
        UUID uuid = player.getUniqueId();
        Score s = new Score(score, time);
        PlayerScore ps = new PlayerScore(s, uuid, name, displayName);
        scores.removeIf(sco -> sco.uuid().equals(uuid));
        scores.add(ps);
        if (scores.size() > maxSavedScores) {
            scores.pollLast();
        }
        saveToPdc(player, score);
    }

    private void saveToPdc(Player player, long score) {
        PersistentDataContainer playerPdc = player.getPersistentDataContainer();
        PersistentDataAdapterContext ac = player.getPersistentDataContainer().getAdapterContext();
        PersistentDataContainer scoresPdc = playerPdc.getOrDefault(SCORES_KEY, PersistentDataType.TAG_CONTAINER, ac.newPersistentDataContainer());
        NamespacedKey key = new NamespacedKey(EdenMinigames.instance(), this.key);
        scoresPdc.set(key, PersistentDataType.LONG, score);
        playerPdc.set(SCORES_KEY, PersistentDataType.TAG_CONTAINER, scoresPdc);
    }

    public long getScore(Player player) throws IllegalArgumentException {
        PersistentDataContainer playerPdc = player.getPersistentDataContainer();
        PersistentDataContainer scoresPdc = playerPdc.get(SCORES_KEY, PersistentDataType.TAG_CONTAINER);
        if (scoresPdc == null) throw new IllegalArgumentException("no score found");
        NamespacedKey key = new NamespacedKey(EdenMinigames.instance(), this.key);
        Long score = scoresPdc.get(key, PersistentDataType.LONG);
        if (score == null) throw new IllegalArgumentException("no score found");
        return score;
    }

    public PlayerScore getPlayerScore(Player player) {
        return scores.stream().filter(s -> s.uuid().equals(player.getUniqueId())).findFirst().orElse(null);
    }

    public PlayerScore getPlayerScore(UUID uuid) {
        return scores.stream().filter(s -> s.uuid().equals(uuid)).findFirst().orElse(null);
    }

    public List<PlayerScore> asList() {
        return List.copyOf(scores);
    }

    public TreeSet<PlayerScore> getScores() {
        return scores;
    }

    public int getPlace(Player player) {
        int i = 0;
        for (PlayerScore score : scores) {
            if (score.uuid().equals(player.getUniqueId())) return i;
            i++;
        }
        return scores.size() + 1;
    }

    public PlayerScore getPlace(int finalI) {
        int i = 0;
        for (PlayerScore score : scores) {
            if (i == finalI) return score;
            i++;
        }
        return null;
    }

    public record PlayerScore(Score score, UUID uuid, String name, String displayName) {
        private static PlayerScore fromYaml(ConfigurationSection section) {
            long score = section.getLong("score");
            long time = section.getLong("time");
            String uuidString = section.getString("uuid");
            if (uuidString == null) throw new IllegalArgumentException("uuid is null");
            UUID uuid = UUID.fromString(uuidString);
            String name = section.getString("name");
            String displayName = section.getString("displayName");
            Score sc = new Score(score, time);
            return new PlayerScore(sc, uuid, name, displayName);
        }

        private void toYaml(ConfigurationSection section) {
            section.set("score", score().score());
            section.set("time", score().time());
            section.set("uuid", uuid().toString());
            section.set("name", name());
            section.set("displayName", displayName());
        }
    }

    public record Score(long score, long time) {
    }
}
