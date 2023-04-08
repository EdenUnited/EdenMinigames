package at.haha007.edenminigames.games;

import at.haha007.edencommands.tree.LiteralCommandNode.LiteralCommandBuilder;
import org.bukkit.entity.Player;

import java.util.List;

public interface Game {
    void stop(Player player);

    List<Player> activePlayers();

    void initCommand(LiteralCommandBuilder cmd);
}
