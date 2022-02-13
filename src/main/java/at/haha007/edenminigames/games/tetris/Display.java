package at.haha007.edenminigames.games.tetris;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockTypes;

class Display {
    private final BlockVector3 location;
    private final World world;

    Display(BlockVector3 location, World world) {
        this.location = location;
        this.world = world;
    }

    public void update(Tetromino[][] field, Tetromino falling, BlockVector2 fallingPos, int rotation, Tetromino next) {
        Tetromino[][] f = falling == null ? new Tetromino[0][0] : falling.getRotation(rotation);
        BlockVector2 offset = fallingPos.subtract(falling == null ? BlockVector2.ZERO : falling.centerOffset(rotation));
        try (EditSession es = WorldEdit.getInstance().newEditSession(world)) {
            for (int y = 19; y >= 0; y--) {
                for (int x = 0; x < 10; x++) {
                    boolean d = false;
                    try {
                        if (f[x - offset.getX()][y - offset.getZ()] != null)
                            d = true;
                    } catch (ArrayIndexOutOfBoundsException ignored) {
                    }
                    if (field[x][y] != null) {
                        es.setBlock(toWorldPos(x, y), field[x][y].block());
                    } else if (d) {
                        es.setBlock(toWorldPos(x, y), falling.block());
                    } else {
                        es.setBlock(toWorldPos(x, y), BlockTypes.AIR);
                    }
                }
            }
            Tetromino[][] n = next.getRotation(3);
            offset = next.centerOffset(3);
            for (int x = 3; x < 7; x++) {
                for (int y = 21; y < 25; y++) {
                    es.setBlock(toWorldPos(x, y), BlockTypes.AIR);
                }
            }
            for (int x = 0; x < n.length; x++) {
                for (int y = 0; y < n[x].length; y++) {
                    if (n[x][y] == null) continue;
                    es.setBlock(toWorldPos(x + 5, y + 22).subtract(offset.getX(), offset.getZ(), 0), next.block());
                }
            }
        } catch (WorldEditException e) {
            e.printStackTrace();
        }
    }

    private BlockVector3 toWorldPos(int x, int y) {
        return BlockVector3.at(location.getX() + x, location.getY() + y, location.getZ());
    }
}
