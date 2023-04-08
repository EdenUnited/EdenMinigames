package at.haha007.edenminigames.games.mensch;

import at.haha007.edencommands.CommandContext;
import at.haha007.edencommands.CommandException;
import at.haha007.edencommands.annotations.AnnotatedCommandLoader;
import at.haha007.edencommands.annotations.Command;
import at.haha007.edencommands.tree.LiteralCommandNode.LiteralCommandBuilder;
import at.haha007.edenminigames.EdenMinigames;
import at.haha007.edenminigames.games.Game;
import at.haha007.edenminigames.games.utils.BoardCoordinateCalculator;
import at.haha007.edenminigames.utils.ConfigUtils;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import lombok.AccessLevel;
import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

public class MenschGame implements Game, Listener {
    @Getter(AccessLevel.MODULE)
    static final List<BlockGameWorldHandler.Coordinate> BOARD_COORDINATES = generateCoordinates();


    private static List<BlockGameWorldHandler.Coordinate> startCoordinatesAt(int x, int y, int startIndex) {
        return List.of(
                new BlockGameWorldHandler.Coordinate(x, y, startIndex),
                new BlockGameWorldHandler.Coordinate(x, y + 1, startIndex + 1),
                new BlockGameWorldHandler.Coordinate(x + 1, y, startIndex + 2),
                new BlockGameWorldHandler.Coordinate(x + 1, y + 1, startIndex + 3)
        );
    }

    private static void addCoordinateLine(int count, List<BlockGameWorldHandler.Coordinate> coordinates,
                                          Function<Integer, Integer> xMod,
                                          Function<Integer, Integer> yMod) {
        for (int i = 0; i < count; i++) {
            int x = coordinates.get(coordinates.size() - 1).x();
            int y = coordinates.get(coordinates.size() - 1).y();
            x = xMod.apply(x);
            y = yMod.apply(y);
            BlockGameWorldHandler.Coordinate c = new BlockGameWorldHandler.Coordinate(x, y, coordinates.size());
            coordinates.add(c);
        }
    }

    private static List<BlockGameWorldHandler.Coordinate> generateCoordinates() {
        List<BlockGameWorldHandler.Coordinate> list = new ArrayList<>();
        list.addAll(startCoordinatesAt(0, 0, 0));
        list.addAll(startCoordinatesAt(9, 9, 4));
        list.addAll(startCoordinatesAt(0, 9, 8));
        list.addAll(startCoordinatesAt(9, 0, 12));

        list.add(new BlockGameWorldHandler.Coordinate(4, 0, 16));
        addCoordinateLine(4, list, x -> x, y -> y + 1);
        addCoordinateLine(4, list, x -> x - 1, y -> y);
        addCoordinateLine(2, list, x -> x, y -> y + 1);
        addCoordinateLine(4, list, x -> x + 1, y -> y);
        addCoordinateLine(4, list, x -> x, y -> y + 1);
        addCoordinateLine(2, list, x -> x + 1, y -> y);
        addCoordinateLine(4, list, x -> x, y -> y - 1);
        addCoordinateLine(4, list, x -> x + 1, y -> y);
        addCoordinateLine(2, list, x -> x, y -> y - 1);
        addCoordinateLine(4, list, x -> x - 1, y -> y);
        addCoordinateLine(4, list, x -> x, y -> y - 1);
        addCoordinateLine(1, list, x -> x - 1, y -> y);

        addCoordinateLine(4, list, x -> x, y -> y + 1);
        list.add(new BlockGameWorldHandler.Coordinate(5, 9, 60));
        addCoordinateLine(3, list, x -> x, y -> y - 1);
        list.add(new BlockGameWorldHandler.Coordinate(1, 5, 64));
        addCoordinateLine(3, list, x -> x + 1, y -> y);
        list.add(new BlockGameWorldHandler.Coordinate(9, 5, 68));
        addCoordinateLine(3, list, x -> x - 1, y -> y);
        return List.copyOf(list);
    }


    private final Player[] players = new Player[4];
    private final PlayerGameState[] gameStates = new PlayerGameState[4];
    private final MenschDisplay display;
    private final MenschInput input;
    private int activePlayer = 0;
    private final BoundingBox box;
    private final Location lobby;
    private final Location[] spawnLocations = new Location[4];

    public MenschGame() {
        Bukkit.getPluginManager().registerEvents(this, EdenMinigames.instance());
        ConfigurationSection config = EdenMinigames.instance().getConfig().getConfigurationSection("mensch");
        if (config == null)
            throw new IllegalStateException("mensch config not found");
        ConfigurationSection lobbySection = config.getConfigurationSection("lobby");
        if (lobbySection == null)
            throw new IllegalStateException("mensch lobby not found");
        ConfigurationSection locationSection = config.getConfigurationSection("location");
        if (locationSection == null)
            throw new IllegalStateException("mensch location not found");
        lobby = ConfigUtils.location(lobbySection);
        World world = lobby.getWorld();
        BoardCoordinateCalculator.Scale scale = new BoardCoordinateCalculator.Scale(config.getInt("scale"), config.getInt("field_size"));
        BlockVector3 pos = ConfigUtils.blockVector3(locationSection);
        int height = config.getInt("height");
        BlockVector2 size = BlockVector2.at(11, 11);
        BoardCoordinateCalculator calc = new BoardCoordinateCalculator(pos, size, scale, height);
        BlockGameWorldHandler io = new BlockGameWorldHandler(calc, world);
        display = io;
        input = io;
        var center = calc.getWorldCoordinate(BlockVector2.at(5, 5)).add(scale.fieldSize() / 2, height, scale.fieldSize() / 2);
        box = BoundingBox.of(new Vector(center.getX(), center.getY(), center.getZ()), scale.scale() * 6 + 3, height + 5, scale.scale() * 6 + 3);
        spawnLocations[0] = toLocation(calc.getWorldCoordinate(BlockVector2.at(1, 1)).add(0, height + 2, 0));
        spawnLocations[1] = toLocation(calc.getWorldCoordinate(BlockVector2.at(11, 11)).add(0, height + 2, 0));
        spawnLocations[2] = toLocation(calc.getWorldCoordinate(BlockVector2.at(1, 11)).add(0, height + 2, 0));
        spawnLocations[3] = toLocation(calc.getWorldCoordinate(BlockVector2.at(11, 1)).add(0, height + 2, 0));
    }

    private ItemStack getWand() {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Mensch stab", NamedTextColor.GOLD));
        meta.lore(List.of(Component.text("Klicke auf eine Spielfigur um sie zu bewegen.", Style.style().color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false).build())));
        item.setItemMeta(meta);
        return item;
    }

    private Location toLocation(BlockVector3 vec) {
        return new Location(lobby.getWorld(), vec.getX(), vec.getY(), vec.getZ());
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        if (activePlayers().contains(event.getPlayer())) {
            broadcast("mensch.player_quit", event.getPlayer());
            stop(event.getPlayer());
        }
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        int playerIndex = -1;
        Player player = event.getPlayer();
        for (int i = 0; i < players.length; i++) {
            Player p = players[i];
            if (p == player) {
                playerIndex = i;
                break;
            }
        }
        if (playerIndex == -1) return;
        Location to = event.getTo();
        if (box.contains(to.toVector()) && player.getWorld() == lobby.getWorld()) return;
        tpToRespawn(playerIndex);
    }

    private void tpToLobby(Player player) {
        player.teleport(lobby);
    }

    private void tpToRespawn(int playerIndex) {
        Player player = players[playerIndex];
        if (player == null) return;
        player.getInventory().clear();
        player.getInventory().setItem(0, getWand());
        player.teleport(spawnLocations[playerIndex]);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
    }


    public void start(Collection<Player> players) {
        if (activePlayers().size() > 0)
            throw new IllegalStateException("already active");
        List<Player> list = players.stream().distinct().limit(4).toList();
        if (list.size() < 2)
            throw new IllegalArgumentException("Has to have at least 2 players!");
        for (int i = 0; i < list.size(); i++) {
            this.players[i] = list.get(i);
            gameStates[i] = new PlayerGameState(i);
        }
        broadcast("mensch.start_game", this.players[activePlayer]);
        display.clear(list.size());
        setActivePlayer(0);
    }

    private void setActivePlayer(int playerIndex) {
        if (playerIndex < 0) return;
        playerIndex %= 4;
        activePlayer = playerIndex;
        input.setActivePlayer(players[activePlayer], gameStates[activePlayer].getPositions(), activePlayer);
        input.clickPieceCallback((click) -> {
        });
        broadcast("mensch.set_active_player", players[activePlayer]);
        input.throwDiceCallback(() -> throwDice(0));
    }

    private void resetBoard() {
        for (int i = 0; i < players.length; i++) {
            players[i] = null;
            gameStates[i] = new PlayerGameState(i);
        }
        display.clear(4);
    }

    private void setNextPlayerActive() {
        for (int i = 0; i < 10; i++) {
            activePlayer = (activePlayer + 1) % 4;
            if (players[activePlayer] == null) {
                continue;
            }
            setActivePlayer(activePlayer);
            return;
        }
        resetBoard();
    }

    private void broadcast(String key, Player papiPlayer, String... args) {
        Audience audience = Audience.audience(activePlayers());
        EdenMinigames.messenger().sendMessage(key, audience, papiPlayer, args);
    }

    private void throwDice(int diceThrownBefore) {
        Random rand = new Random();
        EdenMinigames plugin = EdenMinigames.instance();
        int n;
        if (players[activePlayer].getInventory().getItemInMainHand().getType() == Material.BEDROCK) {
            n = players[activePlayer].getInventory().getItemInMainHand().getAmount() % 6 + 1;
        } else {
            n = rand.nextInt(1, 7);
        }
        Consumer<Integer> pieceCallback = piece -> tryToMovePiece(n, piece);

        input.setActivePlayer(players[activePlayer], gameStates[activePlayer].getPositions(), activePlayer);
        input.throwDiceCallback(() -> {
        });
        input.clickPieceCallback(piece -> {
        });
        for (int i = 0; i <= 8; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> display.displayDice(rand.nextInt(1, 7)), 2 * i);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            display.displayDice(n);
            if (isBlocked(activePlayer, n)) {
                if (diceThrownBefore < 2) {
                    broadcast("mensch.no_move_again", players[activePlayer], String.valueOf(n));
                    input.setActivePlayer(players[activePlayer], gameStates[activePlayer].getPositions(), activePlayer);
                    input.clickPieceCallback((click) -> {
                    });
                    input.throwDiceCallback(() -> throwDice(diceThrownBefore + 1));
                } else {
                    EdenMinigames.messenger().sendMessage("mensch.no_move_next_player", players[activePlayer], String.valueOf(n));
                    setNextPlayerActive();
                }
                return;
            }
            broadcast("mensch.dice_thrown", players[activePlayer], String.valueOf(n));
            input.setActivePlayer(players[activePlayer], gameStates[activePlayer].getPositions(), activePlayer);
            input.throwDiceCallback(() -> {
            });
            input.clickPieceCallback(pieceCallback);
        }, 20);
    }

    private void tryToMovePiece(int n, int piece) {
        List<Integer> pieces = new ArrayList<>();
        for (int p : gameStates[activePlayer].getPositions()) {
            pieces.add(p);
        }
        if (canMovePiece(n, piece, activePlayer) != MoveResult.MOVE) return;
        int from = pieces.get(piece);
        int to = from < 4 ? 4 : from + n;

        broadcast("mensch.move_piece", players[activePlayer], String.valueOf(n));
        BlockVector2 collision = getCollision(worldPosition(to, activePlayer));
        if (collision != null) {
            gameStates[collision.getX()].returnToStart(collision.getZ());
            display.movePiece(collision.getX(), -1, collision.getZ());
        }

        gameStates[activePlayer].getPositions()[piece] = to;
        display.movePiece(activePlayer, from, to);
        checkWin();
        if (activePlayers().isEmpty()) return;
        if (n < 6)
            setNextPlayerActive();
        else
            setActivePlayer(activePlayer);
    }

    private void checkWin() {
        long piecesInEnd = Arrays.stream(gameStates[activePlayer].getPositions()).filter(i -> i >= 44).count();
        if (piecesInEnd < 4) return;
        broadcast("mensch.game_win", players[activePlayer]);
        display.clear(4);
        input.clickPieceCallback((i) -> {
        });
        input.throwDiceCallback(() -> {
        });
        input.setActivePlayer(null, new int[4], 0);
        for (int i = 0; i < 4; i++) {
            gameStates[i] = new PlayerGameState(i);
            players[i] = null;
        }
    }

    //player, piece
    private BlockVector2 getCollision(int worldPosition) {
        for (int i = 0; i < gameStates.length; i++) {
            PlayerGameState gameState = gameStates[i];
            if (gameState == null) continue;
            int[] positions = gameState.getPositions();
            for (int j = 0; j < positions.length; j++) {
                int position = positions[j];
                int worldPos = worldPosition(position, i);
                if (worldPos == worldPosition)
                    return BlockVector2.at(i, j);
            }
        }
        return null;
    }

    private boolean isBlocked(int player, int n) {
        return IntStream.range(0, 4).noneMatch(i -> canMovePiece(n, i, player) == MoveResult.MOVE);
    }


    private MoveResult canMovePiece(int n, int piece, int player) {
        List<Integer> pieces = new ArrayList<>();
        for (int p : gameStates[player].getPositions()) {
            pieces.add(p);
        }
        int from = pieces.get(piece);
        int to = from < 4 ? 4 : from + n;

        //not possible
        if (to >= 48) return MoveResult.END_EXCEEDED;
        if (from < 4 && n != 6) return MoveResult.START_NOT_SIX;
        if (pieces.contains(to)) return MoveResult.BLOCKED;
        return MoveResult.MOVE;
    }

    private enum MoveResult {
        MOVE, BLOCKED, END_EXCEEDED, START_NOT_SIX
    }

    static int worldPosition(int playerPosition, int playerIndex) {
        if (playerPosition < 0) return playerPosition;
        if (playerPosition < 4) {
            return playerPosition + (playerIndex * 4);
        }
        if (playerPosition >= 4 + 40) {
            return playerPosition % 4 + (playerIndex * 4) + 40 + 16;
        }
        int[] offset = new int[]{0, 20, 10, 30};
        return ((playerPosition - 4 + offset[playerIndex]) % 40) + 16;
    }

    static int playerPosition(int worldPosition, int playerIndex) {
        int[] offset = new int[]{0, 2, 1, 3};
        if (worldPosition < 0) return worldPosition;
        if (worldPosition < 16) {
            int i = worldPosition - (playerIndex * 4);
            if (i < 0 || i > 3)
                return -1;
            return i;
        }
        if (worldPosition >= 16 + 40) {
            int i = worldPosition - 40 - 16 - (playerIndex * 4);
            if (i < 0 || i > 3)
                return -1;
            return i + 44;
        }
        return ((worldPosition - 16 + 40 - offset[playerIndex] * 10) % 40) + 4;
    }

    private void checkGameEnd() {
        List<Player> activePlayers = activePlayers();
        if (activePlayers.size() != 1) return;
        tpToLobby(activePlayers.get(0));
        broadcast("mensch.game_end", activePlayers.get(0));
        for (int i = 0; i < 4; i++) {
            players[i] = null;
            gameStates[i] = new PlayerGameState(i);
        }
    }

    public void stop(Player stopPlayer) {
        if (stopPlayer == null) return;
        for (int i = 0; i < players.length; i++) {
            Player player = players[i];
            if (player != stopPlayer) continue;
            players[i] = null;
            broadcast("mensch.stop_kick", players[activePlayer]);
            if (activePlayer == i)
                setNextPlayerActive();
            checkGameEnd();
            break;
        }
    }

    public List<Player> activePlayers() {
        return Arrays.stream(players).filter(Objects::nonNull).toList();
    }

    @Override
    public void initCommand(LiteralCommandBuilder gameCommand) {
        gameCommand.usageText(Component.text("/<cmd> <game> [start]", NamedTextColor.GOLD));
        AnnotatedCommandLoader loader = new AnnotatedCommandLoader(EdenMinigames.instance());
        loader.addDefaultArgumentParsers();
        loader.addAnnotated(this);
        loader.getCommands().forEach(gameCommand::then);
    }

    @Command("save dice")
    @Command("number{type:int,range:'1,6',suggest:'1,2,3,4,5,6'}")
    private void saveDiceCommand(CommandContext context) {
        display.saveDice(context.parameter("number"));
        context.sender().sendMessage(Component.text("Saved dice", NamedTextColor.GOLD));
    }

    @Command("save piece")
    @Command("number{type:int,range:'1,4',suggest:'1,2,3,4'}")
    private void savePieceCommand(CommandContext context) {
        int piece = context.parameter("piece");
        display.savePiece(piece);
        context.sender().sendMessage(Component.text("Saved piece", NamedTextColor.GOLD));
    }

    @Command("start")
    @Command("distance{type:double,range:'1,200',suggest:'1,5,10'}")
    private void start(CommandContext context) throws CommandException {
        CommandSender sender = context.sender();
        Location location;
        if (sender instanceof BlockCommandSender blockCommandSender) {
            location = blockCommandSender.getBlock().getLocation().toCenterLocation();
        } else if (sender instanceof Player player) {
            location = player.getLocation().toCenterLocation();
        } else {
            sender.sendMessage(Component.text("This command can only be executed by Players and CommandBlocks", NamedTextColor.GOLD));
            return;
        }
        double distance = context.parameter("distance");
        Collection<Player> players;
        try {
            players = Bukkit.getScheduler().callSyncMethod(EdenMinigames.instance(), () -> location.getNearbyPlayers(distance)).get(1, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            throw new CommandException(Component.text("Timeout while getting players!"), context);
        }
        if (players.size() < 2) {
            sender.sendMessage(Component.text("Not enough players in area!", NamedTextColor.GOLD));
            return;
        }
        start(players);
        sender.sendMessage(Component.text("Starting Mensch", NamedTextColor.GOLD));
    }
}
