package at.haha007.edenminigames.utils;

import com.sk89q.worldedit.math.BlockVector3;

import java.util.Arrays;

public enum Utils {
    ;

    public static BlockVector3 blockVector3(String string) {
        if (string == null) return BlockVector3.ZERO;
        int[] a = Arrays.stream(string.split(",")).mapToInt(Integer::parseInt).toArray();
        return BlockVector3.at(a[0], a[1], a[2]);
    }
}
