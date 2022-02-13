package at.haha007.edenminigames;

import at.haha007.edencommands.CommandRegistry;
import at.haha007.edencommands.tree.CommandContext;
import at.haha007.edencommands.tree.node.ArgumentCommandNode;
import at.haha007.edencommands.tree.node.CommandNode;
import at.haha007.edencommands.tree.node.LiteralCommandNode;
import at.haha007.edencommands.tree.node.argument.ArgumentParser;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Function;

public class MinigameCommand {
    private final LiteralCommandNode root;
    private final ArgumentParser<Minigame> argumentParser = s -> EdenMinigames.getGame(s.toLowerCase());
    private final Function<CommandContext, List<String>> tabCompleter = c -> EdenMinigames.registeredGames().stream().map(Minigame::name).toList();

    public MinigameCommand(String name) {
        root = LiteralCommandNode.literal(name);
        root.then(startCommand());
        root.then(setLobbyCommand());
        root.then(reloadCommand());
        CommandRegistry.register(root);
    }

    private CommandNode reloadCommand() {
        LiteralCommandNode node = LiteralCommandNode.literal("reload");
        node.executes(c -> {
            c.getSender().sendMessage("Reloading EdenMinigames...");
            EdenMinigames.reload();
            c.getSender().sendMessage("EdenMinigames reload complete.");
        });
        return node;
    }

    private CommandNode setLobbyCommand() {
        LiteralCommandNode node = LiteralCommandNode.literal("lobby");
        node.executes(c -> c.getSender().sendMessage(String.format("/%s lobby <game>", root.getLiteral())));

        CommandNode arg = ArgumentCommandNode.argument("game", argumentParser);
        node.then(arg);
        arg.tabCompletes(tabCompleter);
        arg.executes(c -> {
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

            Minigame game = c.getParameter("game", Minigame.class);

            game.setLobby(cuboid, player.getLocation());

            EdenMinigames.instance().saveConfig();
            player.sendMessage("Lobby updated.");
        });

        return node;
    }

    private CommandNode startCommand() {
        LiteralCommandNode node = LiteralCommandNode.literal("start");
        node.executes(c -> c.getSender().sendMessage("This game is not loaded or does not exist!"));

        CommandNode arg = ArgumentCommandNode.argument("game", argumentParser);
        node.then(arg);
        arg.tabCompletes(tabCompleter);
        arg.executes(c -> {
            Minigame game = c.getParameter("game", Minigame.class);
            List<? extends Player> started = game.start();
            if (started == null) {
                c.getSender().sendMessage("Couldn't start game.");
                return;
            }
            c.getSender().sendMessage(String.format("Game started for %d players.", started.size()));
        });

        return node;
    }

    public void unregister() {
        CommandMap commandMap = Bukkit.getCommandMap();
        Command bukkitCommand = commandMap.getCommand(root.getLiteral());
        if (bukkitCommand == null) {
            EdenMinigames.logger().warning("Command was registered but cannot be found: " + root.getLiteral());
            return;
        }
        bukkitCommand.unregister(commandMap);
    }
}
