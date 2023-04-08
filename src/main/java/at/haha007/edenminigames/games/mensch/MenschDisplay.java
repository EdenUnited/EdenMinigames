package at.haha007.edenminigames.games.mensch;

public interface MenschDisplay {
    void movePiece(int player, int from, int to);
    void displayDice(int number);

    void clear(int playerCount);

    void saveDice(int number);

    void savePiece(int piece);
}
