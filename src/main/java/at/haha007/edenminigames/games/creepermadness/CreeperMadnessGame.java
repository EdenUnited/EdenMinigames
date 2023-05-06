package at.haha007.edenminigames.games.creepermadness;

import at.haha007.edencommands.CommandContext;
import at.haha007.edencommands.annotations.AnnotatedCommandLoader;
import at.haha007.edencommands.annotations.Command;
import at.haha007.edencommands.annotations.SyncCommand;
import at.haha007.edencommands.tree.LiteralCommandNode;
import at.haha007.edenminigames.EdenMinigames;
import at.haha007.edenminigames.games.Game;
import at.haha007.edenminigames.utils.ConfigUtils;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class CreeperMadnessGame implements Game, Listener {
    private final List<Player> players = new ArrayList<>();
    private final BoundingBox box;
    private final World world;
    private final double broadcastRadius;
    private final int startTicks;
    private final int startWeightAmount;
    private final Location lobby;
    private final double lobbyDistance;
    private final double explosionRadius;
    private final ItemStack feather = new ItemStack(Material.FEATHER, 1);
    private int tick;


    public CreeperMadnessGame() {
        ConfigurationSection globalConfig = EdenMinigames.config();
        ConfigurationSection config = globalConfig.getConfigurationSection("creeper_madness");
        if (config == null) throw new RuntimeException("CreeperMadnessGame config is null");
        BlockVector3 from = ConfigUtils.blockVector3(Objects.requireNonNull(config.getConfigurationSection("from")));
        BlockVector3 to = ConfigUtils.blockVector3(Objects.requireNonNull(config.getConfigurationSection("to")));
        box = BoundingBox.of(new Vector(from.getX(), from.getY(), from.getZ()), new Vector(to.getX(), to.getY(), to.getZ()));
        Component weightName = MiniMessage.miniMessage().deserialize(config.getString("weight_name", "<green>Weight"));
        feather.editMeta(meta -> meta.displayName(weightName));
        startWeightAmount = config.getInt("weight_amount", 5);
        lobby = ConfigUtils.location(Objects.requireNonNull(config.getConfigurationSection("lobby")));
        world = lobby.getWorld();
        lobbyDistance = config.getDouble("lobby.radius");
        broadcastRadius = config.getDouble("broadcast_radius");
        explosionRadius = config.getDouble("explosion_radius");
        startTicks = config.getInt("start_ticks");
        Bukkit.getScheduler().scheduleSyncRepeatingTask(EdenMinigames.instance(), this::tick, 0, 1);
        Bukkit.getPluginManager().registerEvents(this, EdenMinigames.instance());
    }


    private void scheduleStart(Collection<Player> players) {
        if (players.size() < 2) return;
        Random random = new Random();
        //get random schematic
        File[] schematics = new File(EdenMinigames.instance().getDataFolder(), "schematics/creeper_madness").listFiles();
        if (schematics == null) throw new RuntimeException("No schematics found");
        File schematicFile = schematics[random.nextInt(schematics.length)];
        try (Clipboard clipboard = BuiltInClipboardFormat.FAST.load(schematicFile)) {
            Vector min = box.getMin();
            BlockVector3 at = BlockVector3.at(min.getX(), min.getY(), min.getZ());
            EditSession es = clipboard.paste(BukkitAdapter.adapt(world), at);
            es.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(EdenMinigames.instance(), () -> start(players), 10);
    }

    private void start(Collection<Player> players) {
        Random random = new Random();
        this.players.addAll(players);
        this.players.removeIf(Objects::isNull);
        this.players.removeIf(p -> !p.isOnline());
        checkWin();
        if (this.players.isEmpty()) return;
        tick = 0;
        players.forEach(player -> {
            Block target = null;
            for (int i = 0; i < 100; i++) {
                int x = random.nextInt((int) box.getMinX(), (int) box.getMaxX());
                int z = random.nextInt((int) box.getMinZ(), (int) box.getMaxZ());
                int y = world.getHighestBlockYAt(x, z);
                if (y < box.getMinY()) continue;
                target = world.getBlockAt(x, y, z);
                break;
            }
            if (target == null) target = box.getCenter().toLocation(world).getBlock();
            Location tpTarget = target.getLocation().add(0.5, 1, 0.5);
            tpTarget.setYaw(random.nextInt(360));
            player.teleport(tpTarget);
            ItemStack weight = this.feather.clone();
            weight.setAmount(startWeightAmount);
            player.setGameMode(GameMode.ADVENTURE);
            player.setFlying(false);
            player.setHealth(20);
            player.setAllowFlight(false);
            player.getInventory().clear();
            player.getInventory().addItem(weight);
            player.getInventory().setHeldItemSlot(0);
        });
        broadcast("creeper_madness.start", this.players.get(0));
    }

    private void tick() {
        if (players.isEmpty()) return;
        tick++;
        players.stream().filter(player -> player.getLocation().getY() < box.getMinY()).toList().forEach(this::stop);
        if (tick < startTicks) return;
        double tickRate = (10000 / (tick - 300d) + 1);
        if (tick % tickRate < 1) {
            for (int i = 0; i < 50; i++) {
                if (tryToSpawnCreeper())
                    break;
            }
        }
        world.getNearbyEntities(box, entity -> entity instanceof Creeper).forEach(entity -> {
            if (!entity.isOnGround() && entity.getLocation().getBlock().getType().isAir()) return;
            explode(entity.getLocation());
            entity.remove();
        });
    }

    @EventHandler
    private void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!players.contains(player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    private void onItemCollect(PlayerAttemptPickupItemEvent event) {
        if (!players.contains(event.getPlayer())) return;
        event.setCancelled(true);
    }

    @EventHandler
    void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!players.contains(event.getPlayer())) return;
        event.setCancelled(true);
    }

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event) {
        if (!players.contains(event.getPlayer())) return;
        if (event.getItem() == null) return;
        if (!event.getItem().isSimilar(feather)) return;
        event.getPlayer().setVelocity(new Vector(0, 1, 0));
        event.getPlayer().setFallDistance(0);
        event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 1);
        event.getItem().setAmount(event.getItem().getAmount() - 1);
        event.setCancelled(true);
    }

    @EventHandler
    private void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!players.contains(player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        if (!players.contains(event.getPlayer())) return;
        stop(event.getPlayer());
    }


    @Override
    public void stop(Player player) {
        if (!players.contains(player)) return;
        broadcast("creeper_madness.stop", player, String.valueOf(players.size()), String.valueOf(players.size() - 1));
        players.remove(player);
        player.teleport(lobby);
        player.setVelocity(new Vector());
        player.setFallDistance(0);
        player.setHealth(20);
        player.setGameMode(GameMode.ADVENTURE);
        player.setFlying(false);
        player.setAllowFlight(false);
        player.getInventory().clear();
        BoundingBox box = this.box.clone();
        box.expand(BlockFace.UP, 50);
        box.expand(BlockFace.DOWN, 50);
        world.getNearbyEntities(box, e -> e instanceof Creeper).forEach(Entity::remove);
        checkWin();
    }

    private void checkWin() {
        if (players.size() != 1) return;
        Player winner = players.get(0);
        broadcast("creeper_madness.win", winner);
        stop(winner);
        players.clear();
    }

    @Override
    public List<Player> activePlayers() {
        return players;
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
    private void start(CommandContext context) {
        if (!activePlayers().isEmpty()) {
            context.sender().sendMessage(Component.text("Game is already running!"));
            return;
        }
        var players = lobby.getNearbyPlayers(lobbyDistance);
        if (players.size() < 2) {
            context.sender().sendMessage(Component.text("Not enough players!"));
            return;
        }
        context.sender().sendMessage(Component.text("Started game with " + activePlayers().size() + " players"));
        scheduleStart(players);
    }

    @Command("list")
    private void list(CommandContext context) {
        File schemFolder = new File(EdenMinigames.instance().getDataFolder(), "schematics/creeper_madness");
        if (!schemFolder.exists()) {
            context.sender().sendMessage(Component.text("No schematics found!"));
            return;
        }
        File[] files = schemFolder.listFiles();
        if (files == null) {
            context.sender().sendMessage(Component.text("No schematics found!"));
            return;
        }
        context.sender().sendMessage(Component.text("Schematics:", NamedTextColor.GOLD));
        for (File file : files) {
            context.sender().sendMessage(Component.text("- " + file.getName().replace(".schem", ""), NamedTextColor.GREEN));
        }
    }

    @Command("delete name{type:string,parser:word}")
    private void delete(CommandContext context) {
        String name = context.parameter("name");
        File file = schematicFile(name);
        if (!file.exists()) {
            context.sender().sendMessage(Component.text("Schematic does not exist!"));
            return;
        }
        if (!file.delete()) {
            context.sender().sendMessage(Component.text("Failed to delete schematic!"));
            return;
        }
        context.sender().sendMessage(Component.text("Deleted schematic " + name));
    }

    @Command("load name{type:string,parser:word}")
    private void load(CommandContext context) {
        String name = context.parameter("name");
        File file = schematicFile(name);
        if (!file.exists()) {
            context.sender().sendMessage(Component.text("Schematic does not exist!"));
            return;
        }
        try (Clipboard clipboard = BuiltInClipboardFormat.FAST.load(file)) {
            BlockVector3 from = BlockVector3.at(box.getMinX(), box.getMinY(), box.getMinZ());
            clipboard.paste(BukkitAdapter.adapt(world), from);
            context.sender().sendMessage(Component.text("Loaded schematic from " + name));
        } catch (IOException e) {
            context.sender().sendMessage(Component.text("Failed to load schematic: " + e.getMessage()));
        }
    }

    @Command("save name{type:string,parser:word}")
    private void save(CommandContext context) {
        String name = context.parameter("name");
        BlockVector3 from = BlockVector3.at(box.getMinX(), box.getMinY(), box.getMinZ());
        BlockVector3 to = BlockVector3.at(box.getMaxX(), box.getMaxY(), box.getMaxZ());
        CuboidRegion region = new CuboidRegion(from, to);
        try (Clipboard clipboard = new BlockArrayClipboard(region)) {
            ForwardExtentCopy copy = new ForwardExtentCopy(BukkitAdapter.adapt(world), region, clipboard, from);
            Operations.complete(copy);
            clipboard.save(schematicFile(name), BuiltInClipboardFormat.FAST);
            context.sender().sendMessage(Component.text("Saved schematic to " + name));
        } catch (IOException e) {
            context.sender().sendMessage(Component.text("Failed to save schematic: " + e.getMessage()));
        }
    }

    @Command("explode")
    @SyncCommand
    private void explode(CommandContext context) {
        if (!(context.sender() instanceof Player)) {
            context.sender().sendMessage(Component.text("Only players can use this command!"));
            return;
        }
        Block target = ((Player) context.sender()).getTargetBlockExact(200);
        if (target == null) {
            context.sender().sendMessage(Component.text("No block in sight!"));
            return;
        }
        explode(target.getLocation().toCenterLocation());
    }

    private boolean tryToSpawnCreeper() {
        int x = (int) (Math.random() * box.getWidthX()) + (int) box.getMinX();
        int z = (int) (Math.random() * box.getWidthZ()) + (int) box.getMinZ();
        int y = (int) (box.getMaxY() + 20);
        Block heighest = world.getHighestBlockAt(x, z, HeightMap.WORLD_SURFACE);
        if (heighest.getY() < box.getMinY()) return false;
        Location loc = heighest.getLocation().toCenterLocation();
        loc.setY(y);
        Creeper creeper = world.spawn(loc, Creeper.class);
        creeper.setInvulnerable(true);
        creeper.getPathfinder().stopPathfinding();
        return true;
    }

    private void broadcast(String key, Player player, String... args) {
        Location center = box.getCenter().toLocation(world);
        Collection<Player> players = center.getNearbyPlayers(broadcastRadius);
        Audience audience = Audience.audience(players);
        EdenMinigames.messenger().sendMessage(key, audience, player, args);
    }


    private void explode(Location location) {
        Random random = new Random();
        world.createExplosion(location, 4, false, false);
        players.stream().filter(player -> player.getLocation().distance(location) < explosionRadius).forEach(player -> {
            double x = random.nextDouble(-0.8, 0.8);
            double y = random.nextDouble(0.8, 1.0);
            double z = random.nextDouble(-0.8, 0.8);
            Vector v = new Vector(x, y, z);
            player.setVelocity(v);
        });
        getBlocksInRadius(location, explosionRadius).filter(b -> box.contains(b.getLocation().toVector())).forEach(block -> {
            if (block.getType() == Material.AIR) return;
            BlockData blockData = block.getBlockData();
            block.setType(Material.AIR);
            Location loc = block.getLocation().toCenterLocation();
            FallingBlock fallingBlock = world.spawnFallingBlock(loc, blockData);
            double x = random.nextDouble(-0.8, 0.8);
            double y = random.nextDouble(0.8, 1.0);
            double z = random.nextDouble(-0.8, 0.8);
            Vector v = new Vector(x, y, z);
            fallingBlock.setVelocity(v);
            fallingBlock.setDropItem(false);
        });
    }

    private Stream<Block> getBlocksInRadius(Location location, double radius) {
        List<Block> blocks = new ArrayList<>();
        Block center = location.getBlock();
        int min = (int) -radius - 1;
        int max = (int) radius + 1;
        for (int x = min; x <= max; x++) {
            for (int y = min; y <= max; y++) {
                for (int z = min; z <= max; z++) {
                    blocks.add(center.getRelative(x, y, z));
                }
            }
        }
        double radiusSquared = radius * radius;
        return blocks.stream().filter(block -> block.getLocation().toCenterLocation().distanceSquared(location) <= radiusSquared);
    }

    private File schematicFile(String name) {
        return new File(EdenMinigames.instance().getDataFolder(), "schematics/creeper_madness/" + name + ".schem");
    }

    @Override
    public String toString() {
        return "CreeperMadnessGame{" +
                "players=" + players +
                ", box=" + box +
                ", world=" + world +
                ", broadcastRadius=" + broadcastRadius +
                ", startTicks=" + startTicks +
                ", startWeightAmount=" + startWeightAmount +
                ", lobby=" + lobby +
                ", lobbyDistance=" + lobbyDistance +
                ", explosionRadius=" + explosionRadius +
                ", weight=" + feather +
                ", tick=" + tick +
                '}';
    }
}
