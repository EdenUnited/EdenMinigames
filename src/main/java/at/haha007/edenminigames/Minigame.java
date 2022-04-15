package at.haha007.edenminigames;

import at.haha007.edencommands.tree.node.LiteralCommandNode;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public abstract class Minigame {
    @NotNull
    private CuboidRegion lobby;
    @NotNull
    Location lobbySpawn;
    private List<? extends Player> playing;
    protected ConfigurationSection cfg;
    protected final LiteralCommandNode command;

    protected Minigame(String configurationKey) {
        command = LiteralCommandNode.literal(configurationKey);
        command.then(LiteralCommandNode.literal("lobby").executes(c -> {
            if (!(c.getSender() instanceof Player player)) {
                c.getSender().sendMessage("This command can only be executed by players!");
                return;
            }

            Region region;
            try {
                region = BukkitAdapter.adapt(player).getSelection();
            } catch (IncompleteRegionException e) {
                player.sendMessage("Make a WorldEdit selection!");
                return;
            }
            CuboidRegion cuboid = CuboidRegion.makeCuboid(region);
            cuboid.setWorld(BukkitAdapter.adapt(player.getWorld()));

            setLobby(cuboid, player.getLocation());

            EdenMinigames.instance().saveConfig();
            player.sendMessage("Lobby updated.");
        }));

        cfg = EdenMinigames.config().getConfigurationSection(configurationKey);

        if (cfg == null) {
            throw new NullPointerException(String.format("The config entry for %s does not exist!", configurationKey));
        }
        ConfigurationSection lobbySection = Objects.requireNonNull(cfg.getConfigurationSection("lobby"));
        BlockVector3 pos1 = Utils.blockVector3(Objects.requireNonNull(lobbySection.getString("pos1")));
        BlockVector3 pos2 = Utils.blockVector3(Objects.requireNonNull(lobbySection.getString("pos2")));
        World world = BukkitAdapter.adapt(Bukkit.getWorld(Objects.requireNonNull(lobbySection.getString("world"))));

        lobby = new CuboidRegion(world, pos1, pos2);

        String s = lobbySection.getString("spawn");
        double[] vals = Arrays.stream(Objects.requireNonNull(s).split(",")).mapToDouble(Double::parseDouble).toArray();
        lobbySpawn = new Location(BukkitAdapter.adapt(world), vals[0], vals[1], vals[2], (float) vals[3], (float) vals[4]);
    }

    public void setLobby(@NotNull CuboidRegion lobby, @NotNull Location location) {
        this.lobby = lobby;
        this.lobbySpawn = location;

        ConfigurationSection cfg = this.cfg.getConfigurationSection("lobby");
        if (cfg == null)
            cfg = this.cfg.createSection("lobby");

        BlockVector3 pos1 = lobby.getPos1();
        BlockVector3 pos2 = lobby.getPos2();

        cfg.set("world", location.getWorld().getName());
        cfg.set("pos1", "%d,%d,%d".formatted(pos1.getX(), pos1.getY(), pos1.getZ()));
        cfg.set("pos2", "%d,%d,%d".formatted(pos2.getX(), pos2.getY(), pos2.getZ()));

        cfg.set("spawn", "%s,%s,%s,%s,%s".formatted(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch()));
    }

    @Nullable
    public List<? extends Player> start() {
        if (playing != null) return null;

        List<? extends Player> players = playersInLobbyArea().stream().filter(Predicate.not(EdenMinigames::isBlocked)).toList();
        if (players.isEmpty()) return null;


        players = start(players);

        this.playing = players;
        return players;
    }

    public void stop() {
        for (Player player : playing) {
            player.teleport(lobbySpawn);
        }
        playing = null;
    }

    public void stop(Player player) {
        player.teleport(lobbySpawn);
        if (playing == null)
            return;
        playing.removeIf(p -> p == player);
        if (playing.isEmpty())
            stop();
    }

    public boolean isPlaying(Player player) {
        return playing != null && playing.contains(player);
    }

    public abstract void unregister();

    protected abstract List<? extends Player> start(List<? extends Player> players);

    public List<? extends Player> playersInLobbyArea() {
        return Bukkit.getOnlinePlayers().stream().filter(player -> lobby.contains(blockVector(player))).toList();
    }

    private BlockVector3 blockVector(Player player) {
        Location l = player.getLocation();
        return BlockVector3.at(l.getX(), l.getY(), l.getZ());
    }

    protected org.bukkit.World world() {
        return lobbySpawn.getWorld();
    }

    public String name() {
        return cfg.getName();
    }

    public LiteralCommandNode command() {
        return command;
    }
}
