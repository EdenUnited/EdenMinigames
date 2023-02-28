package at.haha007.edenminigames.placeholder;

import at.haha007.edenminigames.games.Minigame;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public
class MinigamePlaceholders {
    @Getter
    private final Minigame minigame;
    @Getter(AccessLevel.PACKAGE)
    private final LiteralPlaceholderNode rootNode;

    MinigamePlaceholders(Minigame minigame) {
        this.minigame = minigame;
        rootNode = new LiteralPlaceholderNode(minigame.name());
    }


    public void register(PlaceholderNode<?> node) {
        rootNode.then(node);
    }
}
