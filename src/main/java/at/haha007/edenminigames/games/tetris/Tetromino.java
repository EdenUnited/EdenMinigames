package at.haha007.edenminigames.games.tetris;

import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

enum Tetromino {
    I(new byte[][][]{
            {
                    {0, 0, 0, 0},
                    {1, 1, 1, 1},
                    {0, 0, 0, 0},
                    {0, 0, 0, 0}
            },
            {
                    {0, 0, 1, 0},
                    {0, 0, 1, 0},
                    {0, 0, 1, 0},
                    {0, 0, 1, 0}
            }
    }),
    J(new byte[][][]{
            {
                    {0, 0, 0},
                    {1, 1, 1},
                    {0, 0, 1}
            },
            {
                    {0, 1, 0},
                    {0, 1, 0},
                    {1, 1, 0}
            },
            {
                    {0, 0, 0},
                    {1, 0, 0},
                    {1, 1, 1}
            },
            {
                    {0, 1, 1},
                    {0, 1, 0},
                    {0, 1, 0}
            }
    }),
    L(new byte[][][]{
            {
                    {0, 0, 0},
                    {1, 1, 1},
                    {1, 0, 0}
            },
            {
                    {1, 1, 0},
                    {0, 1, 0},
                    {0, 1, 0}
            },
            {
                    {0, 0, 0},
                    {0, 0, 1},
                    {1, 1, 1}
            },
            {
                    {0, 1, 0},
                    {0, 1, 0},
                    {0, 1, 1}
            }
    }),
    O(new byte[][][]{
            {
                    {1, 1},
                    {1, 1}
            }
    }),
    S(new byte[][][]{
            {
                    {0, 0, 0},
                    {0, 1, 1},
                    {1, 1, 0}
            },
            {
                    {1, 0, 0},
                    {1, 1, 0},
                    {0, 1, 0}
            }
    }),
    T(new byte[][][]{
            {
                    {0, 0, 0},
                    {1, 1, 1},
                    {0, 1, 0}
            },
            {
                    {0, 1, 0},
                    {1, 1, 0},
                    {0, 1, 0}
            },
            {
                    {0, 0, 0},
                    {0, 1, 0},
                    {1, 1, 1}
            },
            {
                    {0, 1, 0},
                    {0, 1, 1},
                    {0, 1, 0}
            }
    }),
    Z(new byte[][][]{
            {
                    {0, 0, 0},
                    {1, 1, 0},
                    {0, 1, 1}
            },
            {
                    {0, 0, 1},
                    {0, 1, 1},
                    {0, 1, 0}
            }
    });


    private final Tetromino[][][] rotations;

    Tetromino(byte[][][] rotations) {
        Tetromino[][][] a = new Tetromino[rotations.length][][];
        for (int i = 0; i < rotations.length; i++) {
            a[i] = new Tetromino[rotations[i].length][];
            for (int j = 0; j < rotations[i].length; j++) {
                a[i][j] = new Tetromino[rotations[i][j].length];
                for (int k = 0; k < rotations[i][j].length; k++) {
                    a[i][j][k] = rotations[i][j][k] == 0 ? null : this;
                }
            }
        }
        this.rotations = a;
    }

    public BlockVector2 centerOffset(int rotation) {
        Tetromino[][] t = getRotation(rotation);
        return BlockVector2.at(t.length / 2, t[0].length / 2);
    }

    public Tetromino[][] getRotation(int rotation) {
        rotation = rotation % rotations.length;
        if (rotation < 0)
            rotation += rotations.length;
        return rotations[rotation];
    }

    public BlockType block() {
        return switch (this) {
            case I -> BlockTypes.RED_TERRACOTTA;
            case J -> BlockTypes.BLUE_TERRACOTTA;
            case L -> BlockTypes.ORANGE_TERRACOTTA;
            case O -> BlockTypes.YELLOW_TERRACOTTA;
            case S -> BlockTypes.PINK_TERRACOTTA;
            case T -> BlockTypes.LIGHT_BLUE_TERRACOTTA;
            case Z -> BlockTypes.LIME_TERRACOTTA;
        };
    }
}
