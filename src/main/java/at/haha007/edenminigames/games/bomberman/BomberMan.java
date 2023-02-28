package at.haha007.edenminigames.games.bomberman;

import at.haha007.edencommands.eden.LiteralCommandNode;
import at.haha007.edenminigames.EdenMinigames;
import at.haha007.edenminigames.Utils;
import at.haha007.edenminigames.games.BiStateMinigame;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

public class BomberMan extends BiStateMinigame implements Listener {
    private final int taskId;
    private CuboidRegion wallArea;
    private CuboidRegion bounds;
    private final int wallTries;
    private final List<Location> spawns;
    private Map<Player, PlayerData> playerData = new HashMap<>();
    private final Random rand = new Random();
    private final BaseBlock fire = BukkitAdapter.adapt(Material.FIRE.createBlockData()).toBaseBlock();
    private final BaseBlock hay_block = BukkitAdapter.adapt(Material.HAY_BLOCK.createBlockData()).toBaseBlock();

    protected BomberMan() {
        super("bomberman");
        wallArea = area("wallArea");
        bounds = area("bounds");
        wallTries = cfg.getInt("wallTries");
        spawns = spawns();
        registerCommand();
        Bukkit.getPluginManager().registerEvents(this, EdenMinigames.instance());
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(EdenMinigames.instance(), this::tick, 1, 1);
    }

    private void tick() {
        if (playerData == null) return;
        for (Player player : playerData.keySet()) {
            Location loc = player.getLocation();
            double x = loc.getX();
            double y = loc.getY();
            double z = loc.getZ();
            if (bounds.contains(BlockVector3.at(x, y, z)))
                continue;
            stop(player);
        }
    }

    private List<Location> spawns() {
        return cfg.getStringList("spawns").stream().map(this::location).toList();
    }

    private Location location(String s) {
        String[] args = s.split(",");
        return new Location(world(),
                Double.parseDouble(args[0]),
                Double.parseDouble(args[1]),
                Double.parseDouble(args[2]),
                Float.parseFloat(args[3]),
                Float.parseFloat(args[4]));
    }

    private void registerCommand() {
        command.then(new LiteralCommandNode("wallarea").executor(c -> {
            if (!(c.sender() instanceof Player player)) {
                c.sender().sendMessage("This command can only be executed by players.");
                return;
            }
            CuboidRegion region;
            try {
                region = CuboidRegion.makeCuboid(BukkitAdapter.adapt(player).getSelection());
            } catch (IncompleteRegionException e) {
                player.sendMessage("Incomplete region!");
                return;
            }
            area("wallArea", region);
            player.sendMessage("Area updated!");
            EdenMinigames.instance().saveConfig();
            wallArea = region;
        }));

        command.then(new LiteralCommandNode("bounds").executor(c -> {
            if (!(c.sender() instanceof Player player)) {
                c.sender().sendMessage("This command can only be executed by players.");
                return;
            }
            CuboidRegion region;
            try {
                region = CuboidRegion.makeCuboid(BukkitAdapter.adapt(player).getSelection());
            } catch (IncompleteRegionException e) {
                player.sendMessage("Incomplete region!");
                return;
            }
            area("bounds", region);
            player.sendMessage("Bounds updated!");
            EdenMinigames.instance().saveConfig();
            bounds = region;
        }));
    }

    public void stop(Player player) {
        EdenMinigames.messageHandler().sendMessage("bomberman_end", player, "" + playerData.size());
        playerData.remove(player);
        if (playerData.size() < 2) {
            for (Player winner : playerData.keySet()) {
                EdenMinigames.messageHandler().sendMessage("bomberman_win", winner);
            }
            stop();
        }
        super.stop(player);
    }

    public void stop() {
        playerData.clear();
        EditSession es = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world()));
        es.replaceBlocks(wallArea,
                Set.of(fire, hay_block),
                BlockTypes.AIR);
        es.close();
        super.stop();
    }

    private void area(String key, CuboidRegion region) {
        ConfigurationSection cfg = this.cfg.getConfigurationSection(key);
        if (cfg == null)
            cfg = this.cfg.createSection(key);
        BlockVector3 pos1 = region.getPos1();
        BlockVector3 pos2 = region.getPos2();
        cfg.set("pos1", "%s,%s,%s".formatted(pos1.getX(), pos1.getY(), pos1.getZ()));
        cfg.set("pos2", "%s,%s,%s".formatted(pos2.getX(), pos2.getY(), pos2.getZ()));
    }

    private CuboidRegion area(String key) {
        ConfigurationSection cfg = this.cfg.getConfigurationSection(key);
        if (cfg == null)
            return null;
        BlockVector3 pos1 = Utils.blockVector3(Objects.requireNonNull(cfg.getString("pos1")));
        BlockVector3 pos2 = Utils.blockVector3(Objects.requireNonNull(cfg.getString("pos2")));
        return new CuboidRegion(BukkitAdapter.adapt(world()), pos1, pos2);
    }

    @Override
    public void unregister() {
        if (!playerData.isEmpty())
            stop();
        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTask(taskId);
    }

    @Override
    protected List<? extends Player> start(List<? extends Player> players) {
        players = new ArrayList<>(players);
        if (players.size() < 2)
            return null;
        if (players.size() > spawns.size())
            players = players.subList(0, 4);
        placeWalls();
        playerData = new HashMap<>();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            player.teleport(spawns.get(i));
            playerData.put(player, new PlayerData());
        }
        updateInventories();
        return players;
    }

    private void updateInventories() {
        playerData.forEach(this::updateInventory);
    }

    private void updateInventory(Player player, PlayerData playerData) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        ItemStack bombs = playerData.getBombsItem();
        ItemStack lives = playerData.getLivesItem();
        ItemStack radius = playerData.getBlastDistanceItem();
        inventory.setItem(0, bombs);
        inventory.setItem(1, lives);
        inventory.setItem(2, radius);
    }

    @EventHandler
    void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;
        if (!playerData.containsKey(player))
            return;
        event.setDamage(0);
        if (event.getCause() != EntityDamageEvent.DamageCause.FIRE)
            return;
        PlayerData data = playerData.get(player);
        data.damage();
        player.setFireTicks(0);
        updateInventory(player, data);
        if (!data.isAlive())
            stop(player);
    }

    @EventHandler
    void onBlockBreak(BlockBreakEvent event) {
        if (!playerData.containsKey(event.getPlayer())) return;
        event.setCancelled(true);
    }

    @EventHandler
    void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!playerData.containsKey(player))
            return;
        Block block = event.getBlock();
        if (!bounds.contains(BlockVector3.at(block.getX(), block.getY(), block.getZ())))
            return;
        if (block.getType() != Material.TNT)
            return;
        PlayerData playerData = this.playerData.get(player);
        playerData.placedBomb(() -> updateInventory(player, playerData));
        new Explosion(BukkitAdapter.adapt(world()),
                BlockVector3.at(block.getX(), block.getY(), block.getZ()),
                playerData.getBlastDistance());
    }

    @EventHandler
    void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!playerData.containsKey(event.getPlayer()))
            return;
        event.setCancelled(true);
    }

    @EventHandler
    void onPlayerCollectItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;
        if (!playerData.containsKey(player))
            return;
        PlayerData data = playerData.get(player);
        ItemStack item = event.getItem().getItemStack();
        if (item.getType() == data.getBombsItem().getType()) {
            data.setBombs(data.getBombs() + item.getAmount());
        } else if (item.getType() == data.getLivesItem().getType()) {
            data.setLives(data.getLives() + item.getAmount());
        } else if (item.getType() == data.getBlastDistanceItem().getType()) {
            data.setBlastDistance(data.getBlastDistance() + item.getAmount());
        } else {
            return;
        }
        Bukkit.getScheduler().runTask(EdenMinigames.instance(), this::updateInventories);
    }

    @EventHandler
    void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (!playerData.containsKey(player))
            return;
        Bukkit.getScheduler().runTask(EdenMinigames.instance(), this::updateInventories);
        event.setCancelled(true);
    }

    private void placeWalls() {
        try (EditSession es = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world()))) {
            int bombsPlaced = 0;
            for (int i = 0; i < 100000; i++) {
                int x = rand.nextInt(wallArea.getMaximumX() - wallArea.getMinimumX()) + wallArea.getMinimumX();
                int y = wallArea.getMinimumY();
                int z = rand.nextInt(wallArea.getMaximumZ() - wallArea.getMinimumZ()) + wallArea.getMinimumZ();

                if (!es.getBlock(x, y, z).isAir())
                    continue;

                es.setBlock(x, y, z, BlockTypes.HAY_BLOCK);
                bombsPlaced++;

                if (bombsPlaced >= wallTries)
                    break;
            }
        } catch (WorldEditException e) {
            e.printStackTrace();
        }
    }
}
