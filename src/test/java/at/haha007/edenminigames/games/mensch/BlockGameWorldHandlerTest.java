package at.haha007.edenminigames.games.mensch;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

class BlockGameWorldHandlerTest {
    @Test
    void testBlockIndexList() {
        for (int i = 0; i < 4; i++) {
            boolean worldPositionOutOfIndex = IntStream.range(0, 48).map(j -> MenschGame.worldPosition(j, 0)).anyMatch(j -> j < 0 || j >= 40 + 32);
            Assertions.assertFalse(worldPositionOutOfIndex);
        }
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 48; j++) {
                int shouldBeJ = MenschGame.playerPosition(MenschGame.worldPosition(j, i), i);
                Assertions.assertEquals(j, shouldBeJ);
            }
            boolean worldPositionOutOfIndex = IntStream.range(0, 48).map(j -> MenschGame.worldPosition(j, 0)).anyMatch(j -> j < 0 || j >= 40 + 32);
            Assertions.assertFalse(worldPositionOutOfIndex);
        }
        Assertions.assertEquals(MenschGame.BOARD_COORDINATES.size(), 16 + 16 + 40);
        for (int i = 0; i < MenschGame.BOARD_COORDINATES.size(); i++) {
            Assertions.assertEquals(MenschGame.BOARD_COORDINATES.get(i).index(), i);
        }
    }


    @Test
    void displayBoards() {

        for (int y = 10; y >= 0; y--) {
            System.out.println();
            for (int x = 0; x < 11; x++) {
                System.out.print(getChar(x, y) + " ");
            }
        }
        System.out.println();
        System.out.println();
        for (int y = 10; y >= 0; y--) {
            System.out.println();
            for (int x = 0; x < 11; x++) {
                System.out.print(getPlayerChar(x, y, 0) + " ");
            }
        }
        System.out.println();
        System.out.println();
        for (int y = 10; y >= 0; y--) {
            System.out.println();
            for (int x = 0; x < 11; x++) {
                System.out.print(getPlayerChar(x, y, 1) + " ");
            }
        }
        System.out.println();
        System.out.println();
        for (int y = 10; y >= 0; y--) {
            System.out.println();
            for (int x = 0; x < 11; x++) {
                System.out.print(getPlayerChar(x, y, 2) + " ");
            }
        }
        System.out.println();
        System.out.println();
        for (int y = 10; y >= 0; y--) {
            System.out.println();
            for (int x = 0; x < 11; x++) {
                System.out.print(getPlayerChar(x, y, 3) + " ");
            }
        }
        System.out.println();
        System.out.println();
    }

    private char getPlayerChar(int x, int y, int i) {
        for (var c : MenschGame.BOARD_COORDINATES) {
            if (c.x() != x || c.y() != y)
                continue;
            int worldCoordinate = c.index();
            int index = MenschGame.playerPosition(worldCoordinate, i);
            return Integer.toHexString(index % 16).charAt(0);
        }
        return '•';
    }

    private char getChar(int x, int y) {
        for (var c : MenschGame.BOARD_COORDINATES) {
            if (c.x() == x && c.y() == y)
                return Integer.toHexString(c.index() % 16).charAt(0);
        }
        return '•';
    }
}