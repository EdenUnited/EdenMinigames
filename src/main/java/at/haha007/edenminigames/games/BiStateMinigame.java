package at.haha007.edenminigames.games;

import at.haha007.edenminigames.EdenMinigames;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Predicate;

public abstract class BiStateMinigame extends Minigame {
    private List<? extends Player> playing;

    protected BiStateMinigame(String configurationKey) {
        super(configurationKey);
    }

    @Override
    public List<? extends Player> start() {
        if (playing != null) return null;

        List<? extends Player> players = playersInLobbyArea().stream().filter(Predicate.not(EdenMinigames::isBlocked)).toList();
        if (players.isEmpty()) return null;

        players = start(players);

        this.playing = players;
        return players;
    }

    @Override
    public void stop() {
        for (Player player : playing) {
            player.teleport(lobbySpawn);
        }
        playing = null;
    }

    @Override
    public void stop(Player player) {
        player.teleport(lobbySpawn);
        if (playing == null)
            return;
        playing.removeIf(p -> p == player);
        if (playing.isEmpty())
            stop();
    }

    @Override
    public boolean isPlaying(Player player) {
        return playing != null && playing.contains(player);
    }
}
