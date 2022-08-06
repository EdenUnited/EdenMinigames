package at.haha007.edenminigames;

import at.haha007.edencommands.eden.*;
import at.haha007.edencommands.eden.argument.Argument;
import at.haha007.edencommands.eden.argument.ParsedArgument;
import at.haha007.edenminigames.games.Minigame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class MinigameCommand {
    private final Argument<Minigame> argumentParser;
    private final CommandRegistry registry = new CommandRegistry(EdenMinigames.instance());

    public MinigameCommand(String name) {
        argumentParser = new Argument<>() {
            public @NotNull ParsedArgument<Minigame> parse(CommandContext context) throws CommandException {
                Minigame game = EdenMinigames.getGame(context.input()[context.pointer()].toLowerCase());
                if (game == null)
                    throw new CommandException(Component.text("This game doesn't exist!", NamedTextColor.RED), context);
                return new ParsedArgument<>(game, 1);
            }
        };
        argumentParser.tabCompleter(c -> EdenMinigames.registeredGames().stream().map(Minigame::name).collect(Collectors.toList()));
        LiteralCommandNode root = new LiteralCommandNode(name);
        root.then(startCommand().requires(CommandRegistry.permission("minigames.command.start")));
        root.then(reloadCommand().requires(CommandRegistry.permission("minigames.command.reload")));
        root.then(editCommand().requires(CommandRegistry.permission("minigames.command.edit")));
        root.requires(CommandRegistry.permission("minigames.command"));
        registry.register(root);
    }

    private LiteralCommandNode editCommand() {
        LiteralCommandNode node = new LiteralCommandNode("edit");
        for (Minigame minigame : EdenMinigames.registeredGames()) {
            LiteralCommandNode child = minigame.command();
            if (child == null) continue;
            node.then(child);
        }
        return node;
    }

    private LiteralCommandNode reloadCommand() {
        LiteralCommandNode node = new LiteralCommandNode("reload");
        node.executor(c -> Bukkit.getScheduler().runTask(EdenMinigames.instance(), () -> {
            c.sender().sendMessage("Reloading EdenMinigames...");
            EdenMinigames.reload();
            c.sender().sendMessage("EdenMinigames reload complete.");
        }));
        return node;
    }

    private LiteralCommandNode startCommand() {
        LiteralCommandNode node = new LiteralCommandNode("start");
        node.executor(c -> c.sender().sendMessage("This game is not loaded or does not exist!"));

        ArgumentCommandNode<Minigame> arg = new ArgumentCommandNode<>("game", argumentParser);
        arg.executor(c -> {
            Minigame game = c.parameter("game");
            Bukkit.getScheduler().runTask(EdenMinigames.instance(), () -> {
                List<? extends Player> started = game.start();
                if (started == null) {
                    c.sender().sendMessage("Couldn't start game.");
                    return;
                }
                c.sender().sendMessage(String.format("Game started for %d players.", started.size()));
            });
        });
        return node.then(arg);
    }

    public void unregister() {
        registry.destroy();
    }
}
