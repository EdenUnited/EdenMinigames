package at.haha007.edenminigames.games.jnr;

import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class CheckPoint {
    private final BlockVector3 position;
    private final float yaw;
    private final float pitch;

    public CheckPoint(BlockVector3 position, float yaw, float pitch) {
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public BlockVector3 getPosition() {
        return position;
    }

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public void teleport(Player player, World world) {
        player.teleport(new Location(world, position.getX(), position.getY(), position.getZ(), yaw, pitch).add(0.5, 0, 0.5));
    }
}
