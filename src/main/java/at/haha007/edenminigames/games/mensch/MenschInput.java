package at.haha007.edenminigames.games.mensch;

import org.bukkit.entity.Player;

import java.util.function.Consumer;

public interface MenschInput {
    void setActivePlayer(Player player, int[] pieces, int color);

    //piece index
    void clickPieceCallback(Consumer<Integer> clickCallback);

    void throwDiceCallback(Runnable callback);
}
