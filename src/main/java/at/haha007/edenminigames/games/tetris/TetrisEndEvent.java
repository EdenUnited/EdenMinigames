package at.haha007.edenminigames.games.tetris;

import at.haha007.edenminigames.EdenMinigames;
import at.haha007.edenminigames.games.MinigameEvent;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class TetrisEndEvent extends MinigameEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    @Getter
    private final Player player;
    @Accessors(fluent = true)
    @Getter
    @Setter
    private int score;

    //canceling suppresses the entry in the database and the tetris_end message.
    @Setter
    @Getter
    private boolean cancelled;


    protected TetrisEndEvent(Player player, int score) {
        super(EdenMinigames.getGame(Tetris.class));
        this.player = player;
        this.score = score;
    }


    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
