package at.haha007.edenminigames.games.jnr;

import at.haha007.edencommands.CommandContext;
import at.haha007.edencommands.CommandException;
import at.haha007.edencommands.CommandRegistry;
import at.haha007.edencommands.annotations.AnnotatedCommandLoader;
import at.haha007.edencommands.annotations.ArgumentParser;
import at.haha007.edencommands.annotations.Command;
import at.haha007.edencommands.annotations.SyncCommand;
import at.haha007.edencommands.argument.Argument;
import at.haha007.edencommands.argument.ParsedArgument;
import at.haha007.edencommands.argument.StringArgument;
import at.haha007.edencommands.tree.ArgumentCommandNode;
import at.haha007.edencommands.tree.LiteralCommandNode;
import at.haha007.edencommands.tree.LiteralCommandNode.LiteralCommandBuilder;
import at.haha007.edenminigames.EdenMinigames;
import at.haha007.edenminigames.games.Game;
import at.haha007.edenminigames.message.MessageHandler;
import at.haha007.edenminigames.utils.ConfigUtils;
import at.haha007.edenminigames.utils.ScoreTracker;
import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent.Completion;
import com.sk89q.worldedit.math.BlockVector3;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class JumpAndRunGame implements Game, Listener {

    private final Map<String, JumpAndRun> jumpAndRuns = new HashMap<>();
    private final Map<Player, JumpAndRun> activeJumpAndRuns = new HashMap<>();
    private final Map<Player, Map<String, JnrRun>> playerCache = new HashMap<>();
    private final Location lobby;
    private final Map<String, ScoreTracker> scoreTracker = new HashMap<>();
    List<Player> blocked = new ArrayList<>();

    public JumpAndRunGame() {
        Bukkit.getPluginManager().registerEvents(this, EdenMinigames.instance());
        loadJumpAndRuns();
        jumpAndRuns.keySet().forEach(this::getScoreTracker);
        FileConfiguration config = EdenMinigames.instance().getConfig();
        ConfigurationSection jnrSection = config.getConfigurationSection("jnr");
        if (jnrSection == null) throw new IllegalArgumentException("jumpAndRun is null");
        ConfigurationSection lobbySection = jnrSection.getConfigurationSection("lobby");
        if (lobbySection == null) throw new IllegalArgumentException("lobby is null");
        lobby = ConfigUtils.location(lobbySection);
        new CommandRegistry(EdenMinigames.instance()).register(LiteralCommandNode.builder("resetjnr").then(ArgumentCommandNode.builder("jnr", StringArgument.builder().build()).executor(c -> {
            if (!(c.sender() instanceof Player player)) {
                c.sender().sendMessage(Component.text("Only players can use this command", NamedTextColor.RED));
                return;
            }
            String key = c.parameter("jnr");
            JumpAndRun jnr = jumpAndRuns.get(key);
            if (jnr == null) {
                EdenMinigames.messenger().sendMessage("jumpAndRun.notFound", player, key);
                return;
            }
            JnrRun run = getRun(player, jnr.getKey());
            EdenMinigames.messenger().sendMessage("jumpAndRun.reset",
                    player,
                    String.valueOf(run.checkpoint),
                    formatDuration(System.currentTimeMillis() - run.startedAt),
                    String.valueOf(run.fails));
            Bukkit.getScheduler().runTask(EdenMinigames.instance(), () -> reset(player, jnr));
        })).build());
        Bukkit.getScheduler().scheduleSyncRepeatingTask(EdenMinigames.instance(), () -> {
            for (Player player : activeJumpAndRuns.keySet()) {
                sendActionBar(player);
            }
        }, 0, 20);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(EdenMinigames.instance(), () -> blocked.clear(), 0, 1);
    }

    private void sendActionBar(Player player) {
        JumpAndRun jnr = activeJumpAndRuns.get(player);
        if (jnr == null) return;
        JnrRun run = getRun(player, jnr.getKey());
        String duration = formatDuration(System.currentTimeMillis() - run.startedAt);
        String fails = String.valueOf(run.fails);
        String actionBar = "<gold>%s <green>| <gold>%s Fails".formatted(duration, fails);
        player.sendActionBar(MiniMessage.miniMessage().deserialize(actionBar));
    }

    private void loadJumpAndRuns() {
        jumpAndRuns.clear();
        List<JumpAndRun> list = JumpAndRun.loadAll();
        for (JumpAndRun jnr : list) {
            jumpAndRuns.put(jnr.getKey(), jnr);
        }
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        stop(event.getPlayer());
        Map<String, JnrRun> cache = playerCache.get(event.getPlayer());
        playerCache.remove(event.getPlayer());
        if (cache == null) return;
        for (JnrRun run : cache.values()) {
            run.save(event.getPlayer());
        }
    }

    @EventHandler
    private void onPlayerDropItem(PlayerDropItemEvent event){
        if(!activeJumpAndRuns.containsKey(event.getPlayer())) return;
        event.setCancelled(true);
    }

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!activeJumpAndRuns.containsKey(player)) return;
        event.setCancelled(true);
        if (!event.getAction().isLeftClick() && !event.getAction().isRightClick()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if(blocked.contains(player)) return;
        blocked.add(player);
        int slot = player.getInventory().getHeldItemSlot();
        if (slot == 1) {
            JnrRun run = getRun(player, activeJumpAndRuns.get(player).getKey());
            run.fails++;
            respawn(player, true);
        }
        if (slot == 5) resetRequest(player);
        if (slot == 7) stop(player);
    }

    @EventHandler
    private void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!activeJumpAndRuns.containsKey(player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    private void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!activeJumpAndRuns.containsKey(player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    private void checkpointReachedListener(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.PHYSICAL) return;
        JumpAndRun jnr = activeJumpAndRuns.get(player);
        if (jnr == null) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        JnrRun run = getRun(player, jnr.getKey());
        int cpIndex = run.checkpoint;
        int nextCpIndex = cpIndex + 1;
        List<BlockVector3> cps = jnr.getCheckPoints().stream().map(CheckPoint::getPosition).toList();
        if (nextCpIndex >= cps.size()) {
            long time = System.currentTimeMillis() - run.startedAt;
            ScoreTracker scoreTracker = getScoreTracker(jnr.getKey());
            scoreTracker.addScore(player, -time);
            scoreTracker.saveAsync();
            int place = scoreTracker.getPlace(player);
            String duration = formatDuration(time);
            EdenMinigames.messenger().sendMessage("jumpAndRun.time",
                    player,
                    jnr.getKey(),
                    duration,
                    String.valueOf(run.fails),
                    String.valueOf(place + 1)
            );
            reset(player, jnr);
            stop(player);
            return;
        }
        BlockVector3 cp = cps.get(nextCpIndex);
        if (cp == null) return;
        if (cp.getBlockX() != block.getX() || cp.getBlockY() != block.getY() || cp.getBlockZ() != block.getZ()) return;
        run.checkpoint++;
        long time = System.currentTimeMillis() - run.startedAt;
        String duration = formatDuration(time);
        EdenMinigames.messenger().sendMessage("jumpAndRun.checkpointReached",
                player,
                String.valueOf(nextCpIndex),
                duration,
                String.valueOf(run.fails),
                String.valueOf(cps.size() - 1));
    }

    private static PersistentDataContainer getJnrContainer(Player player, String jnrKey) {
        PersistentDataContainer jnrpdc = getJnrContainer(player);
        NamespacedKey key = new NamespacedKey(EdenMinigames.instance(), jnrKey);
        PersistentDataContainer pdc = jnrpdc.getOrDefault(key, PersistentDataType.TAG_CONTAINER, jnrpdc.getAdapterContext().newPersistentDataContainer());
        jnrpdc.set(key, PersistentDataType.TAG_CONTAINER, pdc);
        return pdc;
    }

    private static PersistentDataContainer getJnrContainer(Player player) {
        PersistentDataContainer playerpdc = player.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(EdenMinigames.instance(), "jnr");
        PersistentDataContainer pdc = playerpdc.getOrDefault(key, PersistentDataType.TAG_CONTAINER, playerpdc.getAdapterContext().newPersistentDataContainer());
        playerpdc.set(key, PersistentDataType.TAG_CONTAINER, pdc);
        return pdc;
    }

    private ScoreTracker getScoreTracker(String key) {
        return scoreTracker.computeIfAbsent(key, k -> new ScoreTracker("jnr_" + key, 100));
    }

    public void start(Player player, String key) {
        JumpAndRun jumpAndRun = jumpAndRuns.get(key);
        if (jumpAndRun == null) {
            EdenMinigames.messenger().sendMessage("jumpAndRun.notFound", player, key);
            return;
        }
        if (!player.hasPermission("edenminigames.jnr.play." + key)) {
            EdenMinigames.messenger().sendMessage("jumpAndRun.noPermission", player, key);
            return;
        }
        activeJumpAndRuns.put(player, jumpAndRun);
        EdenMinigames.messenger().sendMessage("jumpAndRun.start", player, key);
        respawn(player, false);
    }

    public void stop(Player player, boolean sendMessage) {
        if (activeJumpAndRuns.remove(player) == null) return;
        player.teleport(lobby);
        player.getInventory().clear();
        if (sendMessage)
            EdenMinigames.messenger().sendMessage("jumpAndRun.stop", player);
    }

    public void stop(Player player) {
        stop(player, true);
    }

    private void resetRequest(Player player) {
        JumpAndRun jnr = activeJumpAndRuns.get(player);
        if (jnr == null) return;
        Component message = Component.text("Reset", NamedTextColor.RED, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Click to reset")))
                .clickEvent(ClickEvent.runCommand("/resetjnr " + jnr.getKey()));
        player.sendMessage(message);
    }

    private void reset(Player player, JumpAndRun jnr) {
        JnrRun run = getRun(player, jnr.getKey());
        run.reset();
        respawn(player, false);
    }

    private void respawn(Player player, boolean sendMessage) {
        JumpAndRun jnr = activeJumpAndRuns.get(player);
        if (jnr == null) return;
        sendActionBar(player);
        JnrRun run = getRun(player, jnr.getKey());
        jnr.tpToCheckpoint(player, run.checkpoint);
        long time = System.currentTimeMillis() - run.startedAt;
        ItemStack respawnItem = new ItemStack(Material.REDSTONE);
        ItemStack resetItem = new ItemStack(Material.STRUCTURE_VOID);
        ItemStack stopItem = new ItemStack(Material.BARRIER);
        respawnItem.editMeta(meta -> meta.displayName(Component.text("Respawn", NamedTextColor.GREEN)));
        resetItem.editMeta(meta -> meta.displayName(Component.text("Reset", NamedTextColor.GREEN)));
        stopItem.editMeta(meta -> meta.displayName(Component.text("Exit", NamedTextColor.GREEN)));
        player.getInventory().clear();
        player.getInventory().setItem(1, respawnItem);
        player.getInventory().setItem(5, resetItem);
        player.getInventory().setItem(7, stopItem);

        player.setVelocity(new Vector());
        player.setGameMode(GameMode.ADVENTURE);
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.getInventory().setHeldItemSlot(1);
        if (sendMessage)
            EdenMinigames.messenger().sendMessage("jumpAndRun.respawn", player, String.valueOf(run.checkpoint), formatDuration(time), String.valueOf(run.fails));
    }

    @Override
    public List<Player> activePlayers() {
        return activeJumpAndRuns.keySet().stream().toList();
    }

    @Override
    public void initCommand(LiteralCommandBuilder cmd) {
        AnnotatedCommandLoader loader = new AnnotatedCommandLoader(EdenMinigames.instance());
        loader.addDefaultArgumentParsers();

        class JnrArgument extends Argument<JumpAndRun> {
            public JnrArgument() {
                super(s -> jumpAndRuns.keySet().stream().map(Completion::completion).toList(), true);
            }

            @Override
            public @NotNull ParsedArgument<JumpAndRun> parse(CommandContext commandContext) throws CommandException {
                String key = commandContext.input()[commandContext.pointer()];
                JumpAndRun jnr = jumpAndRuns.get(key);
                if (jnr == null)
                    throw new CommandException(Component.text("Jump and Run not found"), commandContext);
                return new ParsedArgument<>(jnr, 1);
            }
        }

        class JnrArgumentParser implements ArgumentParser<JnrArgument> {
            @Override
            public JnrArgument parse(Map<String, String> map) {
                return new JnrArgument();
            }
        }

        loader.addArgumentParser("jnr", new JnrArgumentParser());
        loader.addAnnotated(this);
        loader.getCommands().forEach(cmd::then);
    }

    @Command("start player{type:player} key{type:jnr}")
    @SyncCommand
    private void startCommand(CommandContext context) {
        Player player = context.parameter("player");
        JumpAndRun jnr = context.parameter("key");
        start(player, jnr.getKey());
        context.sender().sendMessage(Component.text("Started " + jnr.getKey() + " for " + player.getName()));
    }

    @Command("stop player{type:player}")
    @SyncCommand
    private void stopCommand(CommandContext context) {
        Player player = context.parameter("player");
        stop(player);
        context.sender().sendMessage(Component.text("Stopped for " + player.getName()));
    }

    @Command("delete key{type:jnr}")
    private void deleteCommand(CommandContext context) throws ExecutionException, InterruptedException, TimeoutException {
        JumpAndRun jnr = context.parameter("key");
        Future<?> future = Bukkit.getScheduler().callSyncMethod(EdenMinigames.instance(), () -> {
            activeJumpAndRuns.entrySet().stream()
                    .filter(e -> e.getValue() == jnr)
                    .map(Map.Entry::getKey)
                    .toList()
                    .forEach(this::stop);
            return null;
        });
        jnr.hidePath();
        future.get(10, TimeUnit.SECONDS);
        jumpAndRuns.remove(jnr.getKey());
        jnr.getFile().delete();
        context.sender().sendMessage(Component.text("Deleted " + jnr.getKey()));
    }

    @Command("create key{type:string}")
    private void createCommand(CommandContext context) throws IOException {
        String key = context.parameter("key");
        if (jumpAndRuns.containsKey(key)) {
            context.sender().sendMessage(Component.text("Already exists " + key));
            return;
        }
        if (!(context.sender() instanceof Player player)) {
            context.sender().sendMessage(Component.text("Only players can use this command"));
            return;
        }
        JumpAndRun jnr = new JumpAndRun(key, player.getLocation());
        jnr.save();
        jumpAndRuns.put(key, jnr);
        context.sender().sendMessage(Component.text("Created " + key));
    }

    @Command("list")
    private void listCommand(CommandContext context) {
        context.sender().sendMessage(Component.text("JumpAndRuns: " + jumpAndRuns.keySet()));
    }

    @Command("cp teleport key{type:jnr} index{type:int,suggestions:'0'}")
    @SyncCommand
    private void cpTeleportCommand(CommandContext context) {
        if (!(context.sender() instanceof Player player)) {
            context.sender().sendMessage(Component.text("Only players can use this command"));
            return;
        }
        JumpAndRun jnr = context.parameter("key");
        int index = context.parameter("index");
        jnr.tpToCheckpoint(player, index);
        player.sendMessage(Component.text("Teleported to checkpoint " + index + " for " + jnr.getKey()));
    }

    @Command("cp add key{type:jnr}")
    private void cpAddCommand(CommandContext context) throws IOException {
        if (!(context.sender() instanceof Player player)) {
            context.sender().sendMessage(Component.text("Only players can use this command"));
            return;
        }
        JumpAndRun jnr = context.parameter("key");
        jnr.addCheckpoint(player.getLocation());
        jnr.updateVisualizer();
        jnr.save();
        context.sender().sendMessage(Component.text("Added checkpoint " + jnr.getCheckPoints().size() + " for " + jnr.getKey()));
    }

    @Command("cp add key{type:jnr} index{type:int,suggestions:'0'}")
    private void cpAddAtIndexCommand(CommandContext context) throws IOException {
        if (!(context.sender() instanceof Player player)) {
            context.sender().sendMessage(Component.text("Only players can use this command"));
            return;
        }
        JumpAndRun jnr = context.parameter("key");
        int index = context.parameter("index");
        if (index < 0 || index > jnr.getCheckPoints().size()) {
            context.sender().sendMessage(Component.text("Invalid index " + index));
            return;
        }
        Location location = player.getLocation();
        BlockVector3 blockVector3 = BlockVector3.at(location.getX(), location.getY(), location.getZ());
        CheckPoint checkPoint = new CheckPoint(blockVector3, location.getYaw(), location.getPitch());
        jnr.getCheckPoints().add(index, checkPoint);
        jnr.updateVisualizer();
        jnr.save();
        context.sender().sendMessage(Component.text("Added checkpoint " + index + " for " + jnr.getKey()));
    }

    @Command("cp set key{type:jnr} index{type:int,suggestions:'0'}")
    private void cpSetCommand(CommandContext context) throws IOException {
        if (!(context.sender() instanceof Player player)) {
            context.sender().sendMessage(Component.text("Only players can use this command"));
            return;
        }
        JumpAndRun jnr = context.parameter("key");
        int index = context.parameter("index");
        if (index < 0 || index >= jnr.getCheckPoints().size()) {
            context.sender().sendMessage(Component.text("Invalid index " + index));
            return;
        }
        Location location = player.getLocation();
        BlockVector3 blockVector3 = BlockVector3.at(location.getX(), location.getY(), location.getZ());
        CheckPoint checkPoint = new CheckPoint(blockVector3, location.getYaw(), location.getPitch());
        jnr.getCheckPoints().set(index, checkPoint);
        jnr.updateVisualizer();
        jnr.save();
        context.sender().sendMessage(Component.text("Set checkpoint " + index + " for " + jnr.getKey()));
    }

    @Command("cp nearest key{type:jnr}")
    private void cpNearestCommand(CommandContext context) {
        if (!(context.sender() instanceof Player player)) {
            context.sender().sendMessage(Component.text("Only players can use this command"));
            return;
        }
        JumpAndRun jnr = context.parameter("key");
        int index = jnr.getNearestCheckpointIndex(player.getLocation());
        context.sender().sendMessage(Component.text("Nearest checkpoint " + index + " for " + jnr.getKey()));
    }

    @Command("cp remove key{type:jnr} index{type:int}")
    private void cpRemoveCommand(CommandContext context) throws IOException {
        JumpAndRun jnr = context.parameter("key");
        int index = context.parameter("index");
        if (index < 0 || index >= jnr.getCheckPoints().size()) {
            context.sender().sendMessage(Component.text("Invalid index " + index));
            return;
        }
        jnr.getCheckPoints().remove(index);
        jnr.updateVisualizer();
        jnr.save();
        context.sender().sendMessage(Component.text("Removed checkpoint " + index + " for " + jnr.getKey()));
    }

    @Command("show key{type:jnr}")
    private void showCommand(CommandContext context) {
        if (!(context.sender() instanceof Player player)) {
            context.sender().sendMessage(Component.text("Only players can use this command"));
            return;
        }
        JumpAndRun jnr = context.parameter("key");
        jnr.showPath(player);
        jnr.updateVisualizer();
        context.sender().sendMessage(Component.text("Showing " + jnr.getKey()));
    }

    @Command("hide key{type:jnr}")
    private void hideCommand(CommandContext context) {
        if (!(context.sender() instanceof Player player)) {
            context.sender().sendMessage(Component.text("Only players can use this command"));
            return;
        }
        JumpAndRun jnr = context.parameter("key");
        jnr.hidePath(player);
        jnr.updateVisualizer();
        context.sender().sendMessage(Component.text("Hiding " + jnr.getKey()));
    }

    @Command("score key{type:jnr} player{type:player}")
    private void scoreCommand(CommandContext context) {
        JumpAndRun jnr = context.parameter("key");
        Player player = context.parameter("player");
        ScoreTracker scoreTracker = getScoreTracker(jnr.getKey());
        ScoreTracker.PlayerScore playerScore = scoreTracker.getPlayerScore(player);
        MessageHandler messenger = EdenMinigames.messenger();
        if (playerScore == null) {
            messenger.sendMessage("jumpAndRun.no_score", player);
        } else {
            int place = scoreTracker.getPlace(player);
            String time = formatDuration(-playerScore.score().score());
            messenger.sendMessage("jumpAndRun.score", player, time, String.valueOf(place));
        }
    }

    @Command("top key{type:jnr} player{type:player} page{type:int,suggestions:'1'}")
    private void scoreTopCommand(CommandContext context) {
        JumpAndRun jnr = context.parameter("key");
        Player player = context.parameter("player");
        ScoreTracker scoreTracker = getScoreTracker(jnr.getKey());
        MessageHandler messenger = EdenMinigames.messenger();
        int page = context.parameter("page");
        if (page < 1) {
            context.sender().sendMessage(Component.text("Invalid page " + page));
            return;
        }
        List<ScoreTracker.PlayerScore> scores = scoreTracker.asList();
        int from = (page - 1) * 10;
        int to = Math.min(from + 10, scores.size());
        messenger.sendMessage("jumpAndRun.top_header", player, String.valueOf(page), String.valueOf((scores.size() + 9) / 10), String.valueOf(scores.size()));
        for (int i = from; i < to; i++) {
            ScoreTracker.PlayerScore score = scores.get(i);
            String time = DurationFormatUtils.formatDuration(-score.score().score(), "mm:ss");
            Component displayName = MiniMessage.miniMessage().deserialize(score.displayName());
            String legacy = LegacyComponentSerializer.legacySection().serialize(displayName);
            messenger.sendMessage("jumpAndRun.top_entry", player, String.valueOf(i + 1), legacy, time);
        }
    }

    private JnrRun getRun(Player player, String key) {
        Map<String, JnrRun> cache = playerCache.computeIfAbsent(player, p -> new HashMap<>());
        return cache.computeIfAbsent(key, k -> JnrRun.getOrCreate(player, k));
    }


    private static final class JnrRun {
        private final String key;
        private int checkpoint;
        private int fails;
        private long startedAt;

        private JnrRun(String key, int checkpoint, int fails, long startedAt) {
            this.key = key;
            this.checkpoint = checkpoint;
            this.startedAt = startedAt;
            this.fails = fails;
        }

        public void reset() {
            checkpoint = 0;
            fails = 0;
            startedAt = System.currentTimeMillis();
        }

        public static JnrRun getOrCreate(Player player, String key) {
            PersistentDataContainer pdc = getJnrContainer(player, key);
            int checkpoint = pdc.getOrDefault(new NamespacedKey(EdenMinigames.instance(), "checkpoint"), PersistentDataType.INTEGER, 0);
            long startedAt = pdc.getOrDefault(new NamespacedKey(EdenMinigames.instance(), "startedAt"), PersistentDataType.LONG, System.currentTimeMillis());
            int fails = pdc.getOrDefault(new NamespacedKey(EdenMinigames.instance(), "fails"), PersistentDataType.INTEGER, 0);
            return new JnrRun(key, checkpoint, fails, startedAt);
        }

        public void save(Player player) {
            PersistentDataContainer pdc = getJnrContainer(player, key);
            pdc.set(new NamespacedKey(EdenMinigames.instance(), "checkpoint"), PersistentDataType.INTEGER, checkpoint);
            pdc.set(new NamespacedKey(EdenMinigames.instance(), "startedAt"), PersistentDataType.LONG, startedAt);
            pdc.set(new NamespacedKey(EdenMinigames.instance(), "fails"), PersistentDataType.INTEGER, fails);
            PersistentDataContainer jnrContainer = getJnrContainer(player);
            jnrContainer.set(new NamespacedKey(EdenMinigames.instance(), key), PersistentDataType.TAG_CONTAINER, pdc);
            PersistentDataContainer playerContainer = player.getPersistentDataContainer();
            playerContainer.set(new NamespacedKey(EdenMinigames.instance(), "jnr"), PersistentDataType.TAG_CONTAINER, jnrContainer);
        }
    }

    private String formatDuration(long time) {
        if (time < 0) time = -time;
        long seconds = time / 1000;
        long minutes = seconds / 60;
        seconds %= 60;
        long hours = minutes / 60;
        minutes %= 60;

        String timeString = "";
        if (hours > 0) {
            timeString += hours + "h:";
        }
        if (minutes > 0) {
            timeString += "%02dm:".formatted(minutes);
        }
        timeString += "%02ds".formatted(seconds);

        return timeString;
    }
}
