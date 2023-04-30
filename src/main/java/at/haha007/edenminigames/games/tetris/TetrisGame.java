package at.haha007.edenminigames.games.tetris;

import at.haha007.edencommands.CommandContext;
import at.haha007.edencommands.annotations.AnnotatedCommandLoader;
import at.haha007.edencommands.annotations.Command;
import at.haha007.edencommands.annotations.SyncCommand;
import at.haha007.edencommands.tree.LiteralCommandNode;
import at.haha007.edenminigames.EdenMinigames;
import at.haha007.edenminigames.games.Game;
import at.haha007.edenminigames.utils.ConfigUtils;
import at.haha007.edenminigames.utils.ScoreTracker;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;

public class TetrisGame implements Game, Listener {
    private static final BlockState[] placedPieceColors = {
            Objects.requireNonNull(BlockTypes.AIR).getDefaultState(),
            Objects.requireNonNull(BlockTypes.RED_WOOL).getDefaultState(),
            Objects.requireNonNull(BlockTypes.ORANGE_WOOL).getDefaultState(),
            Objects.requireNonNull(BlockTypes.YELLOW_WOOL).getDefaultState(),
            Objects.requireNonNull(BlockTypes.LIME_WOOL).getDefaultState(),
            Objects.requireNonNull(BlockTypes.GREEN_WOOL).getDefaultState(),
            Objects.requireNonNull(BlockTypes.CYAN_WOOL).getDefaultState(),
            Objects.requireNonNull(BlockTypes.BLUE_WOOL).getDefaultState(),
            Objects.requireNonNull(BlockTypes.PURPLE_WOOL).getDefaultState(),
    };

    private static final BlockState[] fallingPieceColors = {
            Objects.requireNonNull(BlockTypes.AIR).getDefaultState(),
            Objects.requireNonNull(BlockTypes.RED_CONCRETE).getDefaultState(),
            Objects.requireNonNull(BlockTypes.ORANGE_CONCRETE).getDefaultState(),
            Objects.requireNonNull(BlockTypes.YELLOW_CONCRETE).getDefaultState(),
            Objects.requireNonNull(BlockTypes.LIME_CONCRETE).getDefaultState(),
            Objects.requireNonNull(BlockTypes.GREEN_CONCRETE).getDefaultState(),
            Objects.requireNonNull(BlockTypes.CYAN_CONCRETE).getDefaultState(),
            Objects.requireNonNull(BlockTypes.BLUE_CONCRETE).getDefaultState(),
            Objects.requireNonNull(BlockTypes.PURPLE_CONCRETE).getDefaultState(),
    };

    private static final int[][][][] pieces = {
            {
                    {
                            {0, 0, 0, 0},
                            {1, 1, 1, 1},
                            {0, 0, 0, 0},
                            {0, 0, 0, 0},
                    },
                    {
                            {0, 0, 1, 0},
                            {0, 0, 1, 0},
                            {0, 0, 1, 0},
                            {0, 0, 1, 0},
                    },
            },
            {
                    {
                            {0, 0, 0, 0},
                            {0, 2, 2, 0},
                            {0, 2, 2, 0},
                            {0, 0, 0, 0},
                    },
            },
            {
                    {
                            {0, 0, 0},
                            {3, 3, 3},
                            {0, 0, 3},
                    },
                    {
                            {0, 3, 0},
                            {0, 3, 0},
                            {3, 3, 0},
                    },
                    {
                            {0, 0, 0},
                            {3, 0, 0},
                            {3, 3, 3},
                    },
                    {
                            {0, 3, 3},
                            {0, 3, 0},
                            {0, 3, 0},
                    },
            },
            {
                    {
                            {0, 0, 0},
                            {4, 4, 4},
                            {4, 0, 0},
                    },
                    {
                            {4, 4, 0},
                            {0, 4, 0},
                            {0, 4, 0},
                    },
                    {
                            {0, 0, 0},
                            {0, 0, 4},
                            {4, 4, 4},
                    },
                    {
                            {0, 4, 0},
                            {0, 4, 0},
                            {0, 4, 4},
                    },
            },
            {
                    {
                            {0, 0, 0},
                            {0, 5, 5},
                            {5, 5, 0},
                    },
                    {
                            {5, 0, 0},
                            {5, 5, 0},
                            {0, 5, 0},
                    },
            },
            {
                    {
                            {0, 0, 0},
                            {6, 6, 6},
                            {0, 6, 0},
                    },
                    {
                            {0, 6, 0},
                            {6, 6, 0},
                            {0, 6, 0},
                    },
                    {
                            {0, 0, 0},
                            {0, 6, 0},
                            {6, 6, 6},
                    },
                    {
                            {0, 6, 0},
                            {0, 6, 6},
                            {0, 6, 0},
                    },
            },
            {
                    {
                            {0, 0, 0},
                            {7, 7, 0},
                            {0, 7, 7},
                    },
                    {
                            {0, 0, 7},
                            {0, 7, 7},
                            {0, 7, 0},
                    }
            },
    };

    private final Location location;
    private final Location lobbyCenter;
    private final double lobbyRadius;
    private final ScoreTracker scoreTracker = new ScoreTracker("Tetris", 100);

    private int[][] board = new int[20][10];
    private int piece = 0;
    private int nextPiece = 0;
    private int pieceX = 0;
    private int pieceY = 0;
    private int pieceRotation = 0;
    private int score = 0;
    private int linesCleared = 0;
    private Player player;
    private int tick = 0;
    private int tickDelay = 20;

    public TetrisGame() {
        ConfigurationSection cfg = EdenMinigames.config().getConfigurationSection("tetris");
        if (cfg == null) throw new RuntimeException("Tetris config is null!");
        ConfigurationSection locationSection = cfg.getConfigurationSection("location");
        if (locationSection == null) throw new RuntimeException("Tetris location is null!");
        ConfigurationSection lobbySection = cfg.getConfigurationSection("lobby");
        if (lobbySection == null) throw new RuntimeException("Tetris lobby is null!");

        location = ConfigUtils.location(locationSection);
        BlockVector3 lobbyCenter = ConfigUtils.blockVector3(lobbySection);
        this.lobbyCenter = new Location(location.getWorld(), lobbyCenter.getX(), lobbyCenter.getY(), lobbyCenter.getZ());
        lobbyRadius = lobbySection.getDouble("radius");
        Bukkit.getPluginManager().registerEvents(this, EdenMinigames.instance());
    }

    private void start(Player player) {
        this.player = player;
        player.getInventory().clear();
        player.getInventory().setItem(4, new ItemStack(Material.STICK, 1));
        EdenMinigames.messenger().sendMessage("tetris.start", player);
        player.getInventory().setHeldItemSlot(4);
        board = new int[20][10];
        nextPiece = (int) (Math.random() * pieces.length);
        nextPiece();
        score = 0;
        linesCleared = 0;
        tick = 0;
        tickDelay = 20;
        updateDisplay();
        Bukkit.getScheduler().runTask(EdenMinigames.instance(), this::tick);
    }

    private void nextPiece() {
        piece = nextPiece;
        pieceX = 3;
        pieceY = 0;
        pieceRotation = 0;
        nextPiece = (int) (Math.random() * pieces.length);
    }

    private void clearLines() {
        int linesCleared = 0;
        for (int y = 0; y < board.length; y++) {
            boolean line = true;
            for (int x = 0; x < board[y].length; x++) {
                if (board[y][x] == 0) {
                    line = false;
                    break;
                }
            }
            if (line) {
                linesCleared++;
                for (int yy = y; yy > 0; yy--) {
                    board[yy] = board[yy - 1];
                }
                board[0] = new int[10];
            }
        }
        this.linesCleared += linesCleared;
        score += switch (linesCleared) {
            case 1 -> 40;
            case 2 -> 100;
            case 3 -> 300;
            case 4 -> 1200;
            default -> 0;
        };
        if (linesCleared > 0) {
            EdenMinigames.messenger().sendMessage("tetris.lines_cleared", player, String.valueOf(score), String.valueOf(linesCleared));
        }
    }

    private void updateDisplay() {
        World world = BukkitAdapter.adapt(location.getWorld());
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
            BlockVector3 delta = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            CuboidRegion field = new CuboidRegion(BlockVector3.at(0, 0, 0).add(delta), BlockVector3.at(9, 19, 0).add(delta));
            CuboidRegion next = new CuboidRegion(BlockVector3.at(3, 21, 0).add(delta), BlockVector3.at(6, 24, 0).add(delta));
            editSession.setBlocks((Region) field, Objects.requireNonNull(BlockTypes.AIR).getDefaultState());
            editSession.setBlocks((Region) next, Objects.requireNonNull(BlockTypes.AIR).getDefaultState());
            for (int y = 0; y < board.length; y++) {
                for (int x = 0; x < board[y].length; x++) {
                    editSession.smartSetBlock(delta.add(x, 19 - y, 0), placedPieceColors[board[y][x]]);
                }
            }
            int[][] piece = piece(this.piece, pieceRotation);
            for (int y = 0; y < piece.length; y++) {
                for (int x = 0; x < piece[y].length; x++) {
                    if (piece[y][x] != 0) {
                        editSession.smartSetBlock(delta.add(x + pieceX, 19 - (y + pieceY), 0), fallingPieceColors[piece[y][x]]);
                    }
                }
            }

            int[][] nextPiece = piece(this.nextPiece, 0);
            for (int y = 0; y < nextPiece.length; y++) {
                for (int x = 0; x < nextPiece[y].length; x++) {
                    editSession.smartSetBlock(delta.add(x + 3, 24 - y, 0), fallingPieceColors[nextPiece[y][x]]);
                }
            }
        }
    }

    private int[][] piece(int pieceIndex, int rotation) {
        int[][][] allRotations = pieces[pieceIndex];
        return allRotations[rotation(allRotations.length, rotation)];
    }

    private int rotation(int size, int rotation) {
        return (rotation % size + size) % size;
    }

    private boolean canPlace(int[][] piece, int x, int y) {
        for (int py = 0; py < piece.length; py++) {
            for (int px = 0; px < piece[py].length; px++) {
                if (piece[py][px] == 0) continue;
                if (x + px < 0 || x + px >= board[0].length) return false;
                if (y + py < 0 || y + py >= board.length) return false;
                if (board[y + py][x + px] != 0) return false;
            }
        }
        return true;
    }

    private void place(int[][] piece, int x, int y) {
        for (int py = 0; py < piece.length; py++) {
            for (int px = 0; px < piece[py].length; px++) {
                if (piece[py][px] == 0) continue;
                board[y + py][x + px] = piece[py][px];
            }
        }
        clearLines();
        updateDisplay();
    }

    private void moveLeft() {
        if (canPlace(piece(this.piece, this.pieceRotation), pieceX - 1, pieceY)) pieceX--;
        updateDisplay();
    }

    private void moveRight() {
        if (canPlace(piece(this.piece, this.pieceRotation), pieceX + 1, pieceY)) pieceX++;
        updateDisplay();
    }

    private void drop() {
        while (canPlace(piece(this.piece, this.pieceRotation), pieceX, pieceY + 1)) {
            pieceY++;
        }
        place(piece(this.piece, this.pieceRotation), pieceX, pieceY);
        clearLines();
        if (!canPlace(piece(this.piece, this.pieceRotation), 0, 0)) {
            stop();
            return;
        }
        nextPiece();
        updateDisplay();
    }

    private void tick() {
        tick++;
        if (tick > 100 / tickDelay * 3) {
            tickDelay = Math.max(3, tickDelay - 1);
            tick = 0;
        }

        if (canPlace(piece(this.piece, this.pieceRotation), pieceX, pieceY + 1)) {
            pieceY++;
        } else {
            place(piece(this.piece, this.pieceRotation), pieceX, pieceY);
            clearLines();
            if (!canPlace(piece(nextPiece, 0), 3, 0)) {
                stop();
                return;
            }
            nextPiece();
        }
        updateDisplay();
        if (player == null) return;
        Bukkit.getScheduler().runTaskLater(EdenMinigames.instance(), this::tick, tickDelay);
    }

    private void rotateRight() {
        if (canPlace(piece(this.piece, this.pieceRotation + 1), pieceX, pieceY)) {
            pieceRotation++;
        }
        updateDisplay();
    }

    private void rotateLeft() {
        if (canPlace(piece(this.piece, this.pieceRotation - 1), pieceX, pieceY)) {
            pieceRotation--;
        }
        updateDisplay();
    }

    @EventHandler
    private void onSlotChange(PlayerItemHeldEvent event) {
        if (event.getPlayer() != player) return;
        int slot = event.getNewSlot();
        if (slot < 4) moveLeft();
        if (slot > 4) moveRight();
        event.setCancelled(true);
    }

    @EventHandler
    private void onInteract(PlayerInteractEvent event) {
        if (event.getPlayer() != player) return;
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) rotateRight();
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) rotateLeft();
        event.setCancelled(true);
    }

    @EventHandler
    private void onDrop(PlayerDropItemEvent event) {
        if (event.getPlayer() != player) return;
        drop();
        event.setCancelled(true);
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        if (event.getPlayer() != player) return;
        stop();
    }

    private void stop() {
        EdenMinigames.messenger().sendMessage("tetris.end", player, String.valueOf(score), String.valueOf(linesCleared));
        player.getInventory().clear();
        scoreTracker.addScore(player, score);
        scoreTracker.saveAsync();
        player = null;
    }

    @EventHandler
    private void onTeleport(PlayerTeleportEvent event) {
        if (event.getPlayer() != player) return;
        stop();
    }

    @Override
    public void stop(Player player) {
        if (player != this.player) return;
        stop();
    }


    @Override
    public List<Player> activePlayers() {
        return player == null ? List.of() : List.of(player);
    }

    @Override
    public void initCommand(LiteralCommandNode.LiteralCommandBuilder cmd) {
        AnnotatedCommandLoader annotatedCommandLoader = new AnnotatedCommandLoader(EdenMinigames.instance());
        annotatedCommandLoader.addDefaultArgumentParsers();
        annotatedCommandLoader.addAnnotated(this);
        annotatedCommandLoader.getCommands().forEach(cmd::then);
    }

    @Command("top page{type:int} target{type:player}")
    private void topCommand(CommandContext context) {
        int page = context.parameter("page");
        Player target = context.parameter("target");
        List<ScoreTracker.PlayerScore> scores = scoreTracker.asList();
        if (scores.size() == 0) {
            EdenMinigames.messenger().sendMessage("tetris.no-scores", target);
            return;
        }
        int from = (page - 1) * 10;
        int to = Math.min(from + 10, scores.size());
        EdenMinigames.messenger().sendMessage("tetris.top_header", target, String.valueOf(page), String.valueOf((scores.size() - 1) / 10 + 1));
        for (int i = from; i < to; i++) {
            ScoreTracker.PlayerScore score = scores.get(i);
            String displayName = LegacyComponentSerializer.legacySection().serialize(MiniMessage.miniMessage().deserialize(score.displayName()));
            EdenMinigames.messenger().sendMessage("tetris.top_entry", target, String.valueOf(i + 1), displayName, String.valueOf(score.score()));
        }
    }

    @Command("score player{type:player}")
    private void scoreCommand(CommandContext context) {
        Player player = context.parameter("player");
        long score;
        try {
            score = scoreTracker.getScore(player);
        } catch (IllegalArgumentException e) {
            EdenMinigames.messenger().sendMessage("tetris.no-score", player);
            return;
        }
        EdenMinigames.messenger().sendMessage("tetris.score", player, String.valueOf(score));
    }

    @Command("start")
    @SyncCommand
    private void startCommand(CommandContext context) {
        List<Player> players = List.copyOf(lobbyCenter.getNearbyPlayers(lobbyRadius));
        if (players.size() == 0) {
            context.sender().sendMessage(Component.text("No players found"));
            return;
        }
        Player player = players.get((int) (Math.random() * players.size()));
        start(player);
        context.sender().sendMessage(Component.text("Started game for " + player.getName()));
    }
}
