package at.haha007.edenminigames.games.utils;

import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Optional;

@AllArgsConstructor
@Accessors(fluent = true)
@Getter
public class BoardCoordinateCalculator {
    private final BlockVector3 location;
    private final BlockVector2 boardSize;
    private final Scale scale;
    private final int height;

    public Optional<BlockVector2> getBoardCoordinate(BlockVector3 worldCoordinate) {
        worldCoordinate = worldCoordinate.subtract(location);
        if (worldCoordinate.getX() < 0 || worldCoordinate.getY() < 0 || worldCoordinate.getZ() < 0)
            return Optional.empty();
        if (worldCoordinate.getY() > height)
            return Optional.empty();
        int pieceX = worldCoordinate.getX() % scale.scale;
        int pieceZ = worldCoordinate.getZ() % scale.scale;
        if (pieceX >= scale.fieldSize)
            return Optional.empty();
        if (pieceZ >= scale.fieldSize)
            return Optional.empty();
        worldCoordinate = worldCoordinate.divide(scale.scale);
        return Optional.of(BlockVector2.at(worldCoordinate.getZ(), worldCoordinate.getX()));
    }

    public BlockVector3 getWorldCoordinate(BlockVector2 boardCoordinate) {
        boardCoordinate = boardCoordinate.multiply(scale.scale);
        return location.add(boardCoordinate.getZ(), 0, boardCoordinate.getX());
    }

    public double boardSize() {
        return boardSize.multiply(scale.scale + 1).length();
    }


    public record Scale(int scale, int fieldSize) {
        public Scale {
            if (fieldSize < 1 || fieldSize > scale)
                throw new IllegalArgumentException();
        }
    }
}
