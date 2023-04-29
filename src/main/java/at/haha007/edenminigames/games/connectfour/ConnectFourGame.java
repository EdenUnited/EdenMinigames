package at.haha007.edenminigames.games.connectfour;

import at.haha007.edencommands.CommandContext;
import at.haha007.edencommands.annotations.AnnotatedCommandLoader;
import at.haha007.edencommands.annotations.Command;
import at.haha007.edencommands.annotations.SyncCommand;
import at.haha007.edencommands.tree.LiteralCommandNode;
import at.haha007.edenminigames.EdenMinigames;
import at.haha007.edenminigames.games.Game;
import at.haha007.edenminigames.utils.ConfigUtils;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConnectFourGame implements Game, Listener {

    private Player playerA;
    private Player playerB;
    private final int[][] board = new int[7][6];
    private boolean activePlayer = true;
    private final Location boardLocation;
    private final Location lobbyLocation;
    private final double startDistance;

    public ConnectFourGame() {
        Bukkit.getPluginManager().registerEvents(this, EdenMinigames.instance());
        ConfigurationSection cfg = EdenMinigames.instance().getConfig().getConfigurationSection("connect4");
        if (cfg == null) throw new RuntimeException("connect4 section not found in config");
        ConfigurationSection lobbySection = cfg.getConfigurationSection("lobby");
        ConfigurationSection boardSection = cfg.getConfigurationSection("location");
        if (lobbySection == null) throw new RuntimeException("lobby section not found in config");
        if(boardSection == null) throw new RuntimeException("location section not found in config");
        boardLocation = ConfigUtils.location(boardSection);
        lobbyLocation = ConfigUtils.location(lobbySection);
        startDistance = lobbySection.getDouble("distance");
    }

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event) {
        Player player = activePlayer ? playerA : playerB;
        if (player == null) return;
        if (player != event.getPlayer()) return;
        Block block = event.getClickedBlock();
        if (block == null) block = player.getTargetBlockExact(50);
        if (block == null) return;
        int x = getXCoordinate(block);
        if (x == -1) x = getXCoordinate(block.getRelative(BlockFace.DOWN));
        if (x == -1) x = getXCoordinate(block.getRelative(event.getBlockFace()));
        if (x == -1) x = getXCoordinate(block.getRelative(event.getBlockFace()).getRelative(BlockFace.DOWN));
        if (x == -1) return;
        int[] column = board[x];
        for (int i = 0; i < column.length; i++) {
            if (column[i] == 0) {
                column[i] = activePlayer ? 1 : 2;
                activePlayer = !activePlayer;
                drawBoard();
                if (checkWin())
                    return;
                player = activePlayer ? playerA : playerB;
                broadcastMessage("connect4.next_player", player);
                return;
            }
        }
    }

    private void drawBoard() {
        try (EditSession es = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(boardLocation.getWorld()))) {
            for (int i = 0; i < board.length; i++) {
                for (int j = 0; j < board[i].length; j++) {
                    BlockVector3 pos = getBlockPosition(i, j);
                    BlockType blockType = switch (board[i][j]) {
                        case 1 -> BlockTypes.RED_CONCRETE;
                        case 2 -> BlockTypes.YELLOW_CONCRETE;
                        case 3 -> BlockTypes.RED_CONCRETE_POWDER;
                        case 4 -> BlockTypes.YELLOW_CONCRETE_POWDER;
                        default -> BlockTypes.AIR;
                    };
                    es.setBlock(pos, blockType);
                }
            }
            es.flushQueue();
        }
    }

    private BlockVector3 getBlockPosition(int x, int y) {
        return BlockVector3.at(boardLocation.getBlockX() + x * 2, boardLocation.getBlockY() + y * 2, boardLocation.getBlockZ());
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player == playerA || player == playerB) {
            stop(player);
        }
    }

    private void broadcastMessage(String key, Player player, String... args) {
        Audience audience = Audience.audience(playerA, playerB);
        EdenMinigames.messenger().sendMessage(key, audience, player, args);
    }

    private boolean checkWin() {
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 6; y++) {
                int player = board[x][y];
                if (player == 0) continue;
                List<BlockVector2> list = checkWin(x, y, player);
                if (!list.isEmpty()) {
                    for (BlockVector2 blockVector2 : list) {
                        board[blockVector2.getBlockX()][blockVector2.getBlockZ()] = activePlayer ? 3 : 4;
                    }
                    stop(activePlayer ? playerB : playerA);
                    return true;
                }
            }
        }
        return false;
    }

    private List<BlockVector2> checkWin(int x, int y, int player) {
        List<BlockVector2> list = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (x + i >= 7) break;
            if (board[x + i][y] != player) break;
            list.add(BlockVector2.at(x + i, y));
        }
        if (list.size() == 4)
            return list;
        list.clear();
        for (int i = 0; i < 4; i++) {
            if (y + i >= 6) break;
            if (board[x][y + i] != player) break;
            list.add(BlockVector2.at(x, y + i));
        }
        if (list.size() == 4)
            return list;
        list.clear();
        for (int i = 0; i < 4; i++) {
            if (x + i >= 7 || y + i >= 6) break;
            if (board[x + i][y + i] != player) break;
            list.add(BlockVector2.at(x + i, y + i));
        }
        if (list.size() == 4)
            return list;
        list.clear();
        for (int i = 0; i < 4; i++) {
            if (x - i < 0 || y + i >= 6) break;
            if (board[x - i][y + i] != player) break;
            list.add(BlockVector2.at(x - i, y + i));
        }
        if (list.size() == 4)
            return list;
        list.clear();
        return list;
    }

    private int getXCoordinate(Block block) {
        int y = block.getY() - boardLocation.getBlockY();
        if (y < 0 || y >= 15)
            return -1;

        int x = block.getX() - boardLocation.getBlockX();
        if (x % 2 != 0)
            return -1;
        x /= 2;
        if (x < 0 || x >= 7)
            return -1;

        return x;
    }

    private void start(Player playerA, Player playerB) {
        this.playerA = playerA;
        this.playerB = playerB;
        for (int[] ints : board) {
            Arrays.fill(ints, 0);
        }
        activePlayer = true;
        drawBoard();
        EdenMinigames.messenger().sendMessage("connect4.player_a", playerA, playerA);
        EdenMinigames.messenger().sendMessage("connect4.player_b", playerB, playerB);
        broadcastMessage("connect4.next_player", playerA);
    }

    @Override
    public void stop(Player loser) {
        if (loser == null) return;
        if (playerA == null || playerB == null) return;
        if (loser != playerA && loser != playerB) return;
        Player winner = loser == playerA ? playerB : playerA;
        broadcastMessage("connect4.game_over", winner, LegacyComponentSerializer.legacySection().serialize(loser.displayName()));
        playerA = null;
        playerB = null;
        drawBoard();
    }

    @Override
    public List<Player> activePlayers() {
        if (playerA == null || playerB == null) return List.of();
        return List.of(playerA, playerB);
    }

    @Override
    public void initCommand(LiteralCommandNode.LiteralCommandBuilder cmd) {
        AnnotatedCommandLoader loader = new AnnotatedCommandLoader(EdenMinigames.instance());
        loader.addDefaultArgumentParsers();
        loader.addAnnotated(this);
        loader.getCommands().forEach(cmd::then);
    }

    @Command("start")
    @SyncCommand
    private void startCommand(CommandContext context) {
        if (playerA != null || playerB != null) {
            context.sender().sendMessage(Component.text("Game already started!"));
            return;
        }
        List<Player> playersInRange = new ArrayList<>(lobbyLocation.getNearbyPlayers(startDistance));
        if (playersInRange.size() < 2) {
            context.sender().sendMessage(Component.text("Not enough players in range!"));
            return;
        }
        Collections.shuffle(playersInRange);
        start(playersInRange.get(0), playersInRange.get(1));
        context.sender().sendMessage(Component.text("Game started!"));
    }

}
