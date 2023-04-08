package at.haha007.edenminigames.games.mensch;

import at.haha007.edenminigames.EdenMinigames;
import at.haha007.edenminigames.games.utils.BoardCoordinateCalculator;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.checkerframework.common.value.qual.ArrayLen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.function.Consumer;

@NotNull
public class BlockGameWorldHandler implements MenschDisplay, MenschInput, Listener {


    @NotNull
    private final BoardCoordinateCalculator coordinateCalculator;
    private final World world;

    private Player activePlayer = null;
    private int[] pieces = new int[]{0, 1, 2, 3};
    @NotNull
    private Consumer<Integer> clickCallback = i -> {
    };
    @NotNull
    private Runnable diceCallback = () -> {
    };
    @Range(from = 0, to = 3)
    private int playerIndex = 0;

    public BlockGameWorldHandler(@NotNull BoardCoordinateCalculator coordinateCalculator, World world) {
        Bukkit.getPluginManager().registerEvents(this, EdenMinigames.instance());
        this.coordinateCalculator = coordinateCalculator;
        this.world = world;
        for (int i = 0; i < 5; i++) {
            saveResource(pieceSchematicName(i));
        }
        for (int i = 1; i <= 6; i++) {
            saveResource(diceSchematicName(i));
        }
    }

    private void saveResource(String name) {
        //just to avoid warnings :(
        if (new File(EdenMinigames.instance().getDataFolder(), name).exists()) return;
        EdenMinigames.instance().saveResource(name, false);
    }

    @Override
    public void clear(int playerCount) {
        new Thread(() -> {
            for (int i = 0; i < MenschGame.BOARD_COORDINATES.size(); i++) {
                Coordinate boardCoordinate = MenschGame.BOARD_COORDINATES.get(i);
                int player = i >= playerCount * 4 ? -1 : i / 4;
                placePiece(boardCoordinate, player);
            }
        }).start();
    }

    @Override
    public void savePiece(int number) {
        BlockVector3 from = coordinateCalculator.getWorldCoordinate(BlockVector2.at(5, 5));
        int fieldSize = coordinateCalculator.scale().fieldSize();
        BlockVector3 to = from.add(fieldSize, coordinateCalculator.height(), fieldSize);
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
        CuboidRegion region = new CuboidRegion(weWorld, from, to);
        try (Clipboard clipboard = Clipboard.create(region);
             FileOutputStream fos = new FileOutputStream(pieceSchematicFile(number));
             ClipboardWriter writer = BuiltInClipboardFormat.FAST.getWriter(fos)) {
            writer.write(clipboard);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveDice(int number) {
        BlockVector3 from = coordinateCalculator.getWorldCoordinate(BlockVector2.at(5, 5));
        int fieldSize = coordinateCalculator.scale().fieldSize();
        BlockVector3 to = from.add(fieldSize, coordinateCalculator.height(), fieldSize);
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
        CuboidRegion region = new CuboidRegion(weWorld, from, to);
        try (Clipboard clipboard = Clipboard.create(region);
             FileOutputStream fos = new FileOutputStream(diceSchematicFile(number));
             ClipboardWriter writer = BuiltInClipboardFormat.FAST.getWriter(fos)) {
            writer.write(clipboard);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void movePiece(int player, int from, int to) {
        playerIndex = player;
        from = MenschGame.worldPosition(from, playerIndex);
        to = MenschGame.worldPosition(to, playerIndex);

        if (from > 0)
            placePiece(MenschGame.BOARD_COORDINATES.get(from), -1);
        if (to > 0)
            placePiece(MenschGame.BOARD_COORDINATES.get(to), player);
    }

    private void placePiece(Coordinate coordinate, int player) {
        if (coordinate == null) return;
        File file = pieceSchematicFile(player);
        var format = ClipboardFormats.findByFile(file);
        if (format == null) {
            try {
                throw new IOException("format not found");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Clipboard clipboard;
        try {
            clipboard = format.load(file);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        BlockVector3 blockVector3 = coordinateCalculator.getWorldCoordinate(coordinate.xy());
        blockVector3.subtract(clipboard.getOrigin());
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
            Operation operation = new ClipboardHolder(clipboard).createPaste(editSession).to(blockVector3).build();
            Operations.complete(operation);
        }
    }

    private void placeDice(int number) {
        File file = diceSchematicFile(number);
        var format = ClipboardFormats.findByFile(file);
        if (format == null) {
            try {
                throw new IOException("format not found");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Clipboard clipboard;
        try {
            clipboard = format.load(file);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        BlockVector3 blockVector3 = coordinateCalculator.getWorldCoordinate(BlockVector2.at(5, 5));
        blockVector3.subtract(clipboard.getOrigin());
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
            Operation operation = new ClipboardHolder(clipboard).createPaste(editSession).to(blockVector3).build();
            Operations.complete(operation);
        }
    }

    private File diceSchematicFile(int number) {
        return new File(EdenMinigames.instance().getDataFolder(), diceSchematicName(number));
    }

    private String diceSchematicName(int number) {
        return "schematics/mensch/" + switch (number) {
            case 1 -> "dice_one";
            case 2 -> "dice_two";
            case 3 -> "dice_three";
            case 4 -> "dice_four";
            case 5 -> "dice_five";
            case 6 -> "dice_six";
            default -> throw new IllegalArgumentException("number");
        } + ".schem";
    }

    private File pieceSchematicFile(int player) {
        return new File(EdenMinigames.instance().getDataFolder(), pieceSchematicName(player));
    }

    private String pieceSchematicName(int player) {
        return "schematics/mensch/" + switch (player) {
            case 0 -> "one";
            case 1 -> "two";
            case 2 -> "three";
            case 3 -> "four";
            default -> "empty";
        } + ".schem";
    }

    @Override
    public void displayDice(int number) {
        placeDice(number);
    }


    @Override
    public void setActivePlayer(Player player, @ArrayLen(4) int[] pieces, @Range(from = 0, to = 3) int color) {
        this.pieces = pieces;
        this.activePlayer = player;
        this.playerIndex = color;
    }

    @Override
    public void clickPieceCallback(@NotNull Consumer<Integer> clickCallback) {
        this.clickCallback = clickCallback;
    }

    @Override
    public void throwDiceCallback(@NotNull Runnable callback) {
        this.diceCallback = callback;
    }

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player != this.activePlayer) return;
        event.setCancelled(true);
        Block block = event.getClickedBlock();
        if (block == null) {
            int distance = (int) (coordinateCalculator.boardSize() * 1.5 + 10);
            block = player.getTargetBlockExact(distance, FluidCollisionMode.NEVER);
        }
        if (block == null)
            return;
        BlockVector2 xy = boardXY(block.getRelative(BlockFace.UP));
        if (xy == null)
            return;
        if (xy.equals(BlockVector2.at(5, 5))) {
            diceCallback.run();
            return;
        }
        int worldPosition = worldPosition(xy);
        int playerPosition = MenschGame.playerPosition(worldPosition, playerIndex);
        int[] ints = this.pieces;
        for (int i = 0; i < ints.length; i++) {
            int piece = ints[i];
            if (piece != playerPosition)
                continue;
            clickCallback.accept(i);
        }
    }

    @Nullable
    private BlockVector2 boardXY(Block block) {
        BlockVector3 bv3 = BukkitAdapter.adapt(block.getLocation()).toVector().toBlockPoint();
        return coordinateCalculator.getBoardCoordinate(bv3).orElse(null);
    }

    private int worldPosition(BlockVector2 xy) {
        if (xy == null) return -1;
        for (Coordinate coordinate : MenschGame.BOARD_COORDINATES) {
            if (coordinate.x != xy.getBlockX() || coordinate.y != xy.getBlockZ()) continue;
            return coordinate.index;
        }
        return -1;
    }

    public record Coordinate(int x, int y, int index) {
        BlockVector2 xy() {
            return BlockVector2.at(x, y);
        }
    }
}
