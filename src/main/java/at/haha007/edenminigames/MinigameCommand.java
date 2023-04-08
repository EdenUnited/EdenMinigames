package at.haha007.edenminigames;

import at.haha007.edencommands.CommandContext;
import at.haha007.edencommands.CommandRegistry;
import at.haha007.edencommands.annotations.AnnotatedCommandLoader;
import at.haha007.edencommands.annotations.Command;
import at.haha007.edencommands.annotations.SyncCommand;
import at.haha007.edencommands.tree.LiteralCommandNode;
import at.haha007.edencommands.tree.LiteralCommandNode.LiteralCommandBuilder;
import at.haha007.edenminigames.games.Game;
import at.haha007.edenminigames.games.utils.Path;
import at.haha007.edenminigames.games.utils.PathVisualizer;
import at.haha007.edenminigames.message.MessageCommand;
import at.haha007.edenminigames.utils.PathEditor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MinigameCommand {
    private final CommandRegistry registry;

    public MinigameCommand(String name) {
        registry = new CommandRegistry(EdenMinigames.instance());
        LiteralCommandBuilder cmd = LiteralCommandNode.builder(name.toLowerCase());
        cmd.requires(CommandRegistry.permission("minigames.command"));
        AnnotatedCommandLoader loader = new AnnotatedCommandLoader(EdenMinigames.instance());
        loader.addAnnotated(this);
        loader.getCommands().forEach(cmd::then);
        registerGameCommands(cmd);
        new MessageCommand(cmd, name);
        LiteralCommandBuilder pathCmd = LiteralCommandNode.builder("path");
        new PathEditor(pathCmd);
        cmd.then(pathCmd);
        registry.register(cmd.build());
    }

    private void registerGameCommands(LiteralCommandBuilder cmd) {
        EdenMinigames plugin = EdenMinigames.instance();
        List<Game> registeredGames = plugin.registeredGames();
        for (Game game : registeredGames) {
            var gameCommand = createGameCommand(game);
            cmd.then(gameCommand);
        }
    }

    private LiteralCommandBuilder createGameCommand(Game game) {
        String name = EdenMinigames.instance().allGames().entrySet()
                .stream().filter(e -> e.getValue().isInstance(game))
                .findAny().map(Map.Entry::getKey).orElse(null);
        if (name == null) throw new IllegalStateException(game.getClass().getSimpleName());
        LiteralCommandBuilder gameCommand = LiteralCommandNode.builder(name);
        gameCommand.requires(CommandRegistry.permission("minigames.command." + name));
        game.initCommand(gameCommand);
        return gameCommand;
    }

    @Command("path test")
    private void pathTestCommand(CommandContext context) {
        if (!(context.sender() instanceof Player player)) {
            context.sender().sendMessage(Component.text("Only players can use this command."));
            return;
        }
        World world = player.getWorld();
        Vector vector = player.getLocation().toVector();
        List<Vector> spiral = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            double x = Math.cos(i * 0.1) * i * 0.1;
            double z = Math.sin(i * 0.1) * i * 0.1;
            Vector v = new Vector(x, i / 10d, z).add(vector);
            spiral.add(v);
        }

        List<Vector> displayed = new ArrayList<>();
        displayed.add(vector);
        Path path = new Path(world, displayed);
        PathVisualizer visualizer = new PathVisualizer(path);
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(EdenMinigames.instance(), () -> {
            if (spiral.isEmpty()) return;
            Vector v = spiral.remove(0);
            path.addNode(v);
        }, 1, 1);
        Bukkit.getScheduler().scheduleSyncDelayedTask(EdenMinigames.instance(), () -> {
            Bukkit.getScheduler().cancelTask(taskId);
            visualizer.hide(player);
        }, 1050);

        visualizer.show(player);
    }


    @Command("reload")
    @SyncCommand
    private void reloadCommand(CommandContext context) {
        EdenMinigames plugin = EdenMinigames.instance();
        plugin.reload();
        context.sender().sendMessage(Component.text("Reloaded!"));
    }


    public void unregister() {
        registry.destroy();
    }
}
