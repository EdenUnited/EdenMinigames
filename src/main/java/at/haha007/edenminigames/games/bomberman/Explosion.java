package at.haha007.edenminigames.games.bomberman;

import at.haha007.edenminigames.EdenMinigames;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

class Explosion {
    private final BlockVector3 position;
    private final int taskId;
    private final Set<BlockVector3> fire = new HashSet<>();
    private final int distance;
    private boolean blinker = false;
    private final Random random = new Random();
    private final World world;

    public Explosion(World world, BlockVector3 position, int distance) {
        this.position = position;
        this.world = world;
        this.distance = distance;
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(EdenMinigames.instance(), this::blink, 5, 5);
        Bukkit.getScheduler().runTaskLater(EdenMinigames.instance(), this::explosion, 70);
        Bukkit.getScheduler().runTaskLater(EdenMinigames.instance(), this::stop, 80);
    }

    private void stop() {
        EditSession es = WorldEdit.getInstance().newEditSession(world);
        es.setBlocks(fire, BlockTypes.AIR);
        es.setBlock(position, BlockTypes.AIR);
        es.close();
    }

    private void blink() {
        EditSession es = WorldEdit.getInstance().newEditSession(world);
        blinker = !blinker;
        es.setBlock(position, blinker ? BlockTypes.RED_TERRACOTTA : BlockTypes.TNT);
        es.close();
    }

    private void explosion() {
        Bukkit.getScheduler().cancelTask(taskId);
        EditSession es = WorldEdit.getInstance().newEditSession(world);
        es.setBlock(position, BlockTypes.FIRE);
        for (int i = 1; i <= distance; i++) {
            BlockVector3 bp = position.add(-i, 0, 0);
            BlockState b = es.getBlock(bp);
            if (!(b.isAir() || b.getBlockType() == BlockTypes.FIRE)) {
                explodeWall(bp);
                break;
            }
            es.setBlock(bp, BlockTypes.FIRE);
            fire.add(bp);
        }
        for (int i = 1; i <= distance; i++) {
            BlockVector3 bp = position.add(i, 0, 0);
            BlockState b = es.getBlock(bp);
            if (!(b.isAir() || b.getBlockType() == BlockTypes.FIRE)) {
                explodeWall(bp);
                break;
            }
            es.setBlock(bp, BlockTypes.FIRE);
            fire.add(bp);
        }
        for (int i = 1; i <= distance; i++) {
            BlockVector3 bp = position.add(0, 0, -i);
            BlockState b = es.getBlock(bp);
            if (!(b.isAir() || b.getBlockType() == BlockTypes.FIRE)) {
                explodeWall(bp);
                break;
            }
            es.setBlock(bp, BlockTypes.FIRE);
            fire.add(bp);
        }
        for (int i = 1; i <= distance; i++) {
            BlockVector3 bp = position.add(0, 0, i);
            BlockState b = es.getBlock(bp);
            if (!(b.isAir() || b.getBlockType() == BlockTypes.FIRE)) {
                explodeWall(bp);
                break;
            }
            es.setBlock(bp, BlockTypes.FIRE);
            fire.add(bp);
        }
        es.close();
    }

    private void explodeWall(BlockVector3 block) {
        if (world.getBlock(block).getBlockType() != BlockTypes.HAY_BLOCK)
            return;
        world.setBlock(block, BlockTypes.FIRE.getDefaultState(), SideEffectSet.none());
        fire.add(block);
        int r = random.nextInt(5);
        ItemStack item = switch (r) {
            case 0 -> new PlayerData().getBombsItem();
            case 1 -> new PlayerData().getLivesItem();
            case 2 -> new PlayerData().getBlastDistanceItem();
            default -> null;
        };
        if (item == null)
            return;
        org.bukkit.World world = BukkitAdapter.adapt(this.world);
        Location loc = new Location(world, block.getX() + .5, block.getY() + .5, block.getZ() + .5);
        Bukkit.getScheduler().runTaskLater(EdenMinigames.instance(), () -> world.dropItem(loc, item), 20);
    }
}
