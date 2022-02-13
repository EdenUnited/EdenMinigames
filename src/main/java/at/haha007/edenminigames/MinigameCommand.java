package at.haha007.edenminigames;

import at.haha007.edencommands.CommandRegistry;
import at.haha007.edencommands.tree.CommandContext;
import at.haha007.edencommands.tree.node.ArgumentCommandNode;
import at.haha007.edencommands.tree.node.CommandNode;
import at.haha007.edencommands.tree.node.LiteralCommandNode;
import at.haha007.edencommands.tree.node.argument.ArgumentParser;
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
        root.then(reloadCommand());
        root.then(editCommand());
        CommandRegistry.register(root);
    }

    private CommandNode editCommand() {
        LiteralCommandNode node = LiteralCommandNode.literal("edit");
        for (Minigame minigame : EdenMinigames.registeredGames()) {
            LiteralCommandNode child = minigame.command();
            if (child == null) continue;
            node.then(child);
        }
        return node;
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
