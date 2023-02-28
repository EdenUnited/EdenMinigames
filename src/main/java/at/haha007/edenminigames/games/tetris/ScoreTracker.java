package at.haha007.edenminigames.games.tetris;

import at.haha007.edenminigames.EdenMinigames;
import at.haha007.edenminigames.SqliteDatabase;
import at.haha007.edenminigames.placeholder.ArgumentPlaceholderNode;
import at.haha007.edenminigames.placeholder.LiteralPlaceholderNode;
import at.haha007.edenminigames.placeholder.MinigamePlaceholders;
import at.haha007.edenminigames.placeholder.PlaceholderNode;
import lombok.Cleanup;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Blocking;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ScoreTracker implements Listener {
    private final List<Score> cache = new ArrayList<>();


    ScoreTracker() {
        Bukkit.getPluginManager().registerEvents(this, EdenMinigames.instance());
        SqliteDatabase database = EdenMinigames.database();
        try {
            @Cleanup PreparedStatement ps = database.prepareStatement("CREATE TABLE IF NOT EXISTS tetris_scores(player BINARY(16), score INT(32), time BIGINT(64))");
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        List<Score> topScores = getTopScores(0, 0, 100);
        cache.addAll(topScores);
        cache.sort(Comparator.comparingInt(Score::score));
    }

    public void register(MinigamePlaceholders placeholders) {
        PlaceholderNode<?> top = new LiteralPlaceholderNode("top");
        top.then(new ArgumentPlaceholderNode<>(this::getScoreUnsafe)
                .then(new LiteralPlaceholderNode("name").executor(c -> Bukkit.getOfflinePlayer(c.<Score>argument(0).player).getName()))
                .then(new LiteralPlaceholderNode("score").executor(c -> c.<Score>argument(0).score + "")));
        placeholders.register(top);
    }

    private Score getScoreUnsafe(String s) {
        try {
            int place = Integer.parseInt(s);
            return getScore(place);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    void unregister() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onTetrisScore(TetrisEndEvent event) {
        Player player = event.getPlayer();
        int score = event.score();
        addScoreAsync(new Score(player.getUniqueId(), score, System.currentTimeMillis()));
    }

    public void addScoreAsync(Score score) {
        Bukkit.getScheduler().runTaskAsynchronously(EdenMinigames.instance(), () -> addScore(score));
        synchronized (cache) {
            cache.add(score);
            cache.sort(Comparator.comparingInt(Score::score));
        }
    }

    @Blocking
    public List<Score> getTopScores(long minTime, int offset, int count) {
        SqliteDatabase database = EdenMinigames.database();
        try {
            @Cleanup PreparedStatement ps = database.prepareStatement("SELECT * FROM tetris_scores WHERE time > %s ORDER BY score DESC LIMIT %s OFFSET %s".formatted(minTime, count, offset));
            @Cleanup ResultSet rs = ps.executeQuery();
            List<Score> scores = new ArrayList<>();
            while (rs.next()) {
                UUID uuid = SqliteDatabase.uuid(rs.getBytes("player"));
                int score = rs.getInt("score");
                long time = rs.getInt("time");
                scores.add(new Score(uuid, score, time));
            }
            scores.sort((a, b) -> Integer.compare(b.score, a.score));
            return scores;
        } catch (SQLException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public void addScore(Score score) {
        SqliteDatabase database = EdenMinigames.database();
        try {
            @Cleanup PreparedStatement ps = database.prepareStatement("INSERT INTO tetris_scores VALUES(?,?,?)");
            ps.setBytes(1, SqliteDatabase.bytes(score.player));
            ps.setInt(2, score.score);
            ps.setLong(3, score.time);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Score getScore(int place) {
        List<Score> list = getTopScores(0, place, 1);
        if (list.isEmpty()) return null;
        return list.get(0);
    }

    public record Score(UUID player, int score, long time) {
    }
}
