package at.haha007.edenminigames.placeholder;

import at.haha007.edenminigames.EdenMinigames;
import at.haha007.edenminigames.games.Game;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PlaceholderManager extends PlaceholderExpansion {
    private final List<MinigamePlaceholders> placeholders = new ArrayList<>();

    public PlaceholderManager() {
        EdenMinigames.instance().registeredGames().stream()
                .filter(g -> g instanceof PlaceholderInitializer).forEach(this::register);
    }

    private void register(Game minigame) {
        if (!(minigame instanceof PlaceholderInitializer)) throw new RuntimeException();
        MinigamePlaceholders minigamePlaceholders = new MinigamePlaceholders(minigame);
        ((PlaceholderInitializer) minigame).register(minigamePlaceholders);
        placeholders.add(minigamePlaceholders);
    }

    @Override
    public @NotNull String getIdentifier() {
        return "minigames";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(",", EdenMinigames.instance().getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return EdenMinigames.instance().getDescription().getVersion();
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String s = null;
        PlaceholderContext context = new PlaceholderContext(player, params.split(":"));
        for (MinigamePlaceholders placeholder : placeholders) {
            s = placeholder.rootNode().execute(context, 0);
            if (s != null) break;
        }
        return s;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        return onRequest(player, params);
    }
}
