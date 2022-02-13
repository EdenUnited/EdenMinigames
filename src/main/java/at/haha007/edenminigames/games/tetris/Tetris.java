package at.haha007.edenminigames.games.tetris;

import at.haha007.edenminigames.EdenMinigames;
import at.haha007.edenminigames.Minigame;
import at.haha007.edenminigames.Utils;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Tetris extends Minigame implements Listener {
    //scroll up   -> move left
    //scroll down -> move right
    //left click  -> rotate left
    //right click -> rotate right
    //drop item   -> instant drop tetromino

    private Player player;
    private final int taskId;
    private Tetromino[][] field;
    private final Display display;
    private Tetromino falling;
    private BlockVector2 fallingPos;
    private int rotation;
    private final Random rand = new Random();
    private int score;
    private int tick;
    private int totalTicks = 1;

    protected Tetris() {
        super("tetris");
        JavaPlugin plugin = EdenMinigames.instance();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 0, 1);
        display = new Display(Utils.blockVector3(cfg.getString("location")), BukkitAdapter.adapt(world()));
    }

    @EventHandler
    void onDrop(PlayerDropItemEvent e) {
        if (e.getPlayer() != player) return;
        e.setCancelled(true);
        for (int i = 0; i < 20; i++) {
            if (collides(falling, fallingPos.add(0, -1), rotation)) {
                hitGround();
                checkForClearedLines();
                spawnRandomTetrimino();
                if (collides(falling, fallingPos, rotation)) {
                    stop();
                    updateDisplay();
                    return;
                }
                break;
            } else {
                fallingPos = fallingPos.add(0, -1);
            }
        }
        updateDisplay();
    }

    @EventHandler
    void onScroll(PlayerItemHeldEvent e) {
        if (e.getPlayer() != player)
            return;
        int slot = e.getNewSlot();
        if (slot < 4) {
            moveLeft();
            e.setCancelled(true);
            e.getPlayer().getInventory().setHeldItemSlot(4);
        } else if (slot > 4) {
            moveRight();
            e.setCancelled(true);
            e.getPlayer().getInventory().setHeldItemSlot(4);
        }
    }

    @EventHandler
    void onClick(PlayerInteractEvent e) {
        if (e.getPlayer() != player)
            return;
        Action action = e.getAction();
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            rotateLeft();
            e.setCancelled(true);
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            rotateRight();
            e.setCancelled(true);
        }
    }

    private void rotateRight() {
        if (collides(falling, fallingPos, rotation + 1))
            return;
        rotation++;
        updateDisplay();
    }

    private void rotateLeft() {
        if (collides(falling, fallingPos, rotation - 1))
            return;
        rotation--;
        updateDisplay();
    }

    private void moveRight() {
        if (collides(falling, BlockVector2.at(fallingPos.getX() + 1, fallingPos.getZ()), rotation))
            return;
        fallingPos = fallingPos.add(1, 0);
        updateDisplay();
    }

    private void moveLeft() {
        if (collides(falling, BlockVector2.at(fallingPos.getX() - 1, fallingPos.getZ()), rotation))
            return;
        fallingPos = fallingPos.add(-1, 0);
        updateDisplay();
    }

    private void tick() {
        if (player == null)
            return;
        tick++;
        totalTicks++;
        if (!player.isOnline()) {
            stop();
            updateDisplay();
            return;
        }

        player.sendActionBar(Component.text(score));
        if (tick >= Math.max(5, 30 - Math.sqrt(totalTicks))) {
            tick = 0;
            if (collides(falling, fallingPos.add(0, -1), rotation)) {
                hitGround();
                checkForClearedLines();
                spawnRandomTetrimino();
                if (collides(falling, fallingPos, rotation)) {
                    stop();
                    updateDisplay();
                    return;
                }
                updateDisplay();
            } else {
                fallingPos = fallingPos.add(0, -1);
                updateDisplay();
            }
        }
    }

    private void checkForClearedLines() {
        List<Integer> clearedLines = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            if (isLineCleared(i)) clearedLines.add(i);
        }
        switch (clearedLines.size()) {
            case 1 -> score(40);
            case 2 -> score += 100;
            case 3 -> score += 300;
            case 4 -> score += 1200;
        }

        for (int i = clearedLines.size() - 1; i >= 0; i--) {
            Integer line = clearedLines.get(i);
            clearLine(line);
        }
    }

    private void score(int i) {
        score += i;
    }

    private void hitGround() {
        Tetromino[][] f = falling.getRotation(rotation);
        BlockVector2 offset = fallingPos.subtract(falling.centerOffset(rotation));

        for (int y = 19; y >= 0; y--) {
            for (int x = 0; x < 10; x++) {
                boolean d = false;
                try {
                    if (f[x - offset.getX()][y - offset.getZ()] != null)
                        d = true;
                } catch (ArrayIndexOutOfBoundsException ignored) {
                }
                if (d) {
                    field[x][y] = falling;
                }
            }
        }
    }

    private void updateDisplay() {
        display.update(field, falling, fallingPos, rotation);
    }

    private boolean collides(Tetromino tetromino, BlockVector2 pos, int rotation) {
        Tetromino[][] f = tetromino.getRotation(rotation);
        BlockVector2 offset = BlockVector2.at(pos.getX(), pos.getZ()).subtract(tetromino.centerOffset(rotation));
        int inCount = 0;

        for (int y = 19; y >= 0; y--) {
            for (int x = 0; x < 10; x++) {
                boolean d = false;
                try {
                    if (f[x - offset.getX()][y - offset.getZ()] != null) {
                        d = true;
                        inCount++;
                    }
                } catch (ArrayIndexOutOfBoundsException ignored) {
                }
                if (field[x][y] != null && d) {
                    return true;
                }
            }
        }
        return inCount != 4;
    }

    private boolean isLineCleared(int index) {
        for (int i = 0; i < 10; i++) {
            if (field[i][index] == null) return false;
        }
        return true;
    }

    private void clearLine(int line) {
        for (int x = 0; x < 10; x++) {
            if (20 - (line + 1) >= 0) System.arraycopy(field[x], line + 1, field[x], line + 1 - 1, 20 - (line + 1));
        }
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTask(taskId);
    }

    private void spawnRandomTetrimino() {
        Tetromino[] v = Tetromino.values();
        falling = v[rand.nextInt(v.length)];
        rotation = 0;
        fallingPos = BlockVector2.at(5, 19 - falling.getRotation(0)[0].length);
    }

    public void stop() {
        field = new Tetromino[10][20];
        player.sendMessage("Your score is %d.".formatted(score));
        player = null;
        falling = null;
        super.stop();
    }

    protected List<? extends Player> start(List<? extends Player> players) {
        field = new Tetromino[10][20];
        spawnRandomTetrimino();
        score = 0;
        tick = 0;
        totalTicks = 0;
        updateDisplay();
        return List.of(player = players.get(0));
    }
}