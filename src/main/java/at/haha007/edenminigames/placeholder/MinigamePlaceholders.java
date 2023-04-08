package at.haha007.edenminigames.placeholder;

import at.haha007.edenminigames.games.Game;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public
class MinigamePlaceholders {
    @Getter
    private final Game minigame;
    @Getter(AccessLevel.PACKAGE)
    private final LiteralPlaceholderNode rootNode;

    MinigamePlaceholders(Game minigame) {
        this.minigame = minigame;
        rootNode = new LiteralPlaceholderNode(minigame.getClass().getSimpleName());
    }


    public void register(PlaceholderNode<?> node) {
        rootNode.then(node);
    }
}
