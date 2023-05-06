package at.haha007.edenminigames.games.tntfight;

import at.haha007.edenminigames.EdenMinigames;
import at.haha007.edenminigames.utils.ConfigUtils;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import net.kyori.adventure.audience.Audience;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.*;

public class TntFightArena implements Listener {
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") //this is not a map!
    private final CuboidRegion area;
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") //this is not a map!
    private final CuboidRegion outerArea;
    private final World world;
    private final Map<Player, TntFightPlayer> players = new HashMap<>();
    //air, tnt, range, lives
    private final int[] spawnChances = new int[]{10, 1, 1, 1};
    private final Location lobby;
    private final List<Location> spawns = new ArrayList<>();
    private final String key;

    public TntFightArena(String key, ConfigurationSection cfg, Location lobby) {
        BlockVector3 from = ConfigUtils.blockVector3(Objects.requireNonNull(cfg.getConfigurationSection("from")));
        BlockVector3 to = ConfigUtils.blockVector3(Objects.requireNonNull(cfg.getConfigurationSection("to")));
        area = new CuboidRegion(from, to);
        outerArea = new CuboidRegion(area.getMinimumPoint().subtract(3, 1, 3), area.getMaximumPoint().add(3, 1, 3));
        world = Bukkit.getWorld(Objects.requireNonNull(cfg.getString("world")));
        if (world == null) throw new IllegalArgumentException("World not found: " + cfg.getString("world"));
        Bukkit.getPluginManager().registerEvents(this, EdenMinigames.instance());
        this.lobby = lobby;
        this.key = key;
        ConfigurationSection spawnsSection = cfg.getConfigurationSection("spawns");
        if (spawnsSection == null) throw new IllegalArgumentException(key + ": No spawns found!");
        var keys = spawnsSection.getKeys(false);
        if (keys.size() < 2)
            throw new IllegalArgumentException(key + ": Not enough spawns found!");
        for (String spawnKey : keys) {
            double x = spawnsSection.getDouble(spawnKey + ".x");
            double y = spawnsSection.getDouble(spawnKey + ".y");
            double z = spawnsSection.getDouble(spawnKey + ".z");
            float yaw = (float) spawnsSection.getDouble(spawnKey + ".yaw", 0);
            float pitch = (float) spawnsSection.getDouble(spawnKey + ".pitch", 0);
            spawns.add(new Location(world, x, y, z, yaw, pitch));
        }
    }

    public void start(List<Player> players) {
        if (players.size() < 2) return;
        if (this.players.size() > 0) return;
        players = players.stream().limit(spawns.size()).toList();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            Location spawn = spawns.get(i);
            player.teleport(spawn);
            this.players.put(player, new TntFightPlayer());
            player.setGameMode(GameMode.SURVIVAL);
            updateInventory(player);
        }
        BoundingBox outerBox = new BoundingBox(
                outerArea.getMinimumPoint().getX(),
                outerArea.getMinimumPoint().getY(),
                outerArea.getMinimumPoint().getZ(),
                outerArea.getMaximumPoint().getX(),
                outerArea.getMaximumPoint().getY(),
                outerArea.getMaximumPoint().getZ()
        );
        world.getNearbyEntities(outerBox, e -> e instanceof Item).forEach(Entity::remove);
        for (BlockVector3 pos : area) {
            Block block = world.getBlockAt(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
            if (block.getType() != Material.AIR &&
                    block.getType() != Material.HAY_BLOCK &&
                    block.getType() != Material.TNT) continue;
            boolean hay = Math.random() < 0.5;
            block.setType(hay ? Material.HAY_BLOCK : Material.AIR);
        }
        spawns.stream().map(Location::getBlock)
                .map(b -> List.of(b, b.getRelative(BlockFace.WEST), b.getRelative(BlockFace.NORTH), b.getRelative(BlockFace.EAST), b.getRelative(BlockFace.SOUTH),
                        b.getRelative(BlockFace.NORTH_WEST), b.getRelative(BlockFace.NORTH_EAST), b.getRelative(BlockFace.SOUTH_WEST), b.getRelative(BlockFace.SOUTH_EAST)))
                .flatMap(Collection::stream)
                .filter(b -> b.getType() == Material.HAY_BLOCK)
                .forEach(b -> b.setType(Material.AIR));
    }

    public String getKey() {
        return key;
    }

    @EventHandler
    private void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (players.containsKey(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        TntFightPlayer tntFightPlayer = players.get(player);
        if (tntFightPlayer == null) return;
        event.setCancelled(true);
        if (!outerArea.contains(getPosition(player)) || world != player.getWorld()) {
            stop(player);
            return;
        }
        if (!area.contains(getPosition(event.getBlock()))) return;
        Block block = event.getBlock();
        if (block.getType() != Material.TNT) return;
        if (tntFightPlayer.tntAmount <= 0) return;
        placeTnt(getPosition(block), tntFightPlayer.tntDistance, tntFightPlayer);
    }

    @EventHandler
    private void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!players.containsKey(player)) return;
        if (!outerArea.contains(getPosition(player)) || world != player.getWorld()) {
            stop(player);
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    private void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!players.containsKey(player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    private void onPlayerDropItemEvent(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!players.containsKey(player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    private void onItemCollect(PlayerAttemptPickupItemEvent event) {
        TntFightPlayer player = players.get(event.getPlayer());
        if (player == null) return;
        if (!outerArea.contains(getPosition(event.getPlayer())) || world != event.getPlayer().getWorld()) {
            stop(event.getPlayer());
            return;
        }
        event.setCancelled(true);
        ItemStack itemStack = event.getItem().getItemStack();
        switch (itemStack.getType()) {
            case TNT -> {
                player.tntAmount += itemStack.getAmount();
                EdenMinigames.messenger().sendMessage("tntfight.collect_tnt", event.getPlayer(), String.valueOf(player.tntAmount));
            }
            case BLAZE_POWDER -> {
                player.tntDistance += itemStack.getAmount();
                EdenMinigames.messenger().sendMessage("tntfight.collect_range", event.getPlayer(), String.valueOf(player.tntDistance));
            }
            case GOLDEN_APPLE -> {
                player.lives += itemStack.getAmount();
                EdenMinigames.messenger().sendMessage("tntfight.collect_lives", event.getPlayer(), String.valueOf(player.lives));
            }
        }
        updateInventory(event.getPlayer());
        event.getItem().remove();
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        stop(event.getPlayer());
    }

    private void placeTnt(BlockVector3 position, int range, TntFightPlayer tntFightPlayer) {
        tntFightPlayer.tntAmount--;
        players.entrySet().stream().filter(e -> e.getValue() == tntFightPlayer).map(Map.Entry::getKey).forEach(this::updateInventory);
        Block block = world.getBlockAt(position.getBlockX(), position.getBlockY(), position.getBlockZ());
        Plugin plugin = EdenMinigames.instance();
        world.playSound(block.getLocation(), Sound.ENTITY_TNT_PRIMED, 1, 1);
        Bukkit.getScheduler().runTask(plugin, () -> block.setType(Material.TNT));
        Bukkit.getScheduler().runTaskLater(plugin, () -> block.setType(Material.RED_CONCRETE), 8);
        Bukkit.getScheduler().runTaskLater(plugin, () -> block.setType(Material.TNT), 16);
        Bukkit.getScheduler().runTaskLater(plugin, () -> block.setType(Material.RED_CONCRETE), 24);
        Bukkit.getScheduler().runTaskLater(plugin, () -> block.setType(Material.TNT), 32);
        Bukkit.getScheduler().runTaskLater(plugin, () -> block.setType(Material.RED_CONCRETE), 40);
        Bukkit.getScheduler().runTaskLater(plugin, () -> block.setType(Material.TNT), 48);
        Bukkit.getScheduler().runTaskLater(plugin, () -> block.setType(Material.RED_CONCRETE), 56);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            tntFightPlayer.tntAmount++;
            players.entrySet().stream().filter(e -> e.getValue() == tntFightPlayer).map(Map.Entry::getKey).forEach(this::updateInventory);
            block.setType(Material.AIR);
            world.playSound(block.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
            explode(block.getRelative(BlockFace.WEST), BlockFace.WEST, range, new HashSet<>());
            explode(block.getRelative(BlockFace.EAST), BlockFace.EAST, range, new HashSet<>());
            explode(block.getRelative(BlockFace.NORTH), BlockFace.NORTH, range, new HashSet<>());
            explode(block.getRelative(BlockFace.SOUTH), BlockFace.SOUTH, range, new HashSet<>());
        }, 64);
    }

    private void explode(Block block, BlockFace direction, int range, Set<Player> damagedPlayers) {
        if (range <= 0) return;
        if (block.getType() == Material.HAY_BLOCK) {
            block.setType(Material.AIR);
            int sum = Arrays.stream(spawnChances).sum();
            int random = new Random().nextInt(sum);
            int index = 0;
            for (int i = 0; i < spawnChances.length; i++) {
                if (random < spawnChances[i]) {
                    index = i;
                    break;
                }
                random -= spawnChances[i];
            }
            if (index == 0) return;
            Material material = switch (index - 1) {
                case 0 -> Material.TNT;
                case 1 -> Material.BLAZE_POWDER;
                case 2 -> Material.GOLDEN_APPLE;
                default -> throw new IllegalStateException("Unexpected value: " + index);
            };
            Location location = block.getLocation().toCenterLocation();
            Item item = location.getWorld().dropItem(location, new ItemStack(material));
            item.setVelocity(new Vector(0, 0.5, 0));
            item.setInvulnerable(true);
            return;
        }

        if (block.getType() != Material.AIR)
            return;
        block.setType(Material.FIRE);
        Bukkit.getScheduler().runTaskLater(EdenMinigames.instance(), () -> block.setType(Material.AIR), 20);
        BoundingBox box = new BoundingBox(block.getX(), block.getY(), block.getZ(), block.getX() + 1, block.getY() + 1, block.getZ() + 1);
        List<Player> hitPlayers = new ArrayList<>(players.keySet().stream().filter(player -> {
            try {
                player.getBoundingBox().intersection(box);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }).toList());
        hitPlayers.removeAll(damagedPlayers); // Remove players that were already damaged (to prevent double damage)
        damagedPlayers.addAll(hitPlayers);
        for (Player player : hitPlayers) {
            TntFightPlayer tntFightPlayer = players.get(player);
            if (tntFightPlayer == null) continue;
            tntFightPlayer.lives--;
            updateInventory(player);
            if (tntFightPlayer.lives <= 0) {
                stop(player);
                if (players.isEmpty()) {
                    return;
                }
            }
            updateInventory(player);
        }
        Bukkit.getScheduler().runTaskLater(EdenMinigames.instance(), () -> explode(block.getRelative(direction), direction, range - 1, damagedPlayers), 1);
    }


    private void updateInventory(Player player) {
        TntFightPlayer tntFightPlayer = players.get(player);
        if (tntFightPlayer == null) return;
        player.getInventory().clear();
        ItemStack tntStack = new ItemStack(Material.TNT, tntFightPlayer.tntAmount);
        ItemStack reachStack = new ItemStack(Material.BLAZE_POWDER, tntFightPlayer.tntDistance);
        ItemStack healthStack = new ItemStack(Material.GOLDEN_APPLE, tntFightPlayer.lives);
        player.getInventory().setItem(0, tntStack);
        player.getInventory().setItem(1, reachStack);
        player.getInventory().setItem(2, healthStack);
    }

    private BlockVector3 getPosition(Player player) {
        return BlockVector3.at(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
    }

    private BlockVector3 getPosition(Block block) {
        return BlockVector3.at(block.getX(), block.getY(), block.getZ());
    }


    public void stop(Player player) {
        if (!players.containsKey(player)) return;
        EdenMinigames.messenger().sendMessage("tntfight.lost", player);
        players.remove(player);
        broadcast("tntfight.stop", player);
        exit(player);

        if (players.size() > 1) return;
        if (players.isEmpty()) return;

        player = players.keySet().iterator().next();
        broadcast("tntfight.win", player);
        players.clear();
        exit(player);
    }

    private void exit(Player player) {
        player.getInventory().clear();
        player.teleport(lobby);
        player.setHealth(20);
        player.setFoodLevel(20);
    }

    private void broadcast(String key, Player player) {
        Audience audience = Audience.audience(players.keySet());
        EdenMinigames.messenger().sendMessage(key, audience, player);
    }

    public Set<Player> getPlayers() {
        return players.keySet();
    }

    private static final class TntFightPlayer {
        private int lives;
        private int tntDistance;
        private int tntAmount;

        private TntFightPlayer() {
            this.lives = 1;
            this.tntDistance = 1;
            this.tntAmount = 1;
        }
    }
}
