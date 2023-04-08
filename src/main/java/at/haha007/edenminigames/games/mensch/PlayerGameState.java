package at.haha007.edenminigames.games.mensch;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class PlayerGameState {
    private final int playerIndex;
    private final int[] positions = new int[]{0, 1, 2, 3};

    public void returnToStart(int piece) {
        positions[piece] = piece;
    }
}
