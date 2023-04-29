package at.haha007.edenminigames.games.tntfight;

import at.haha007.edencommands.CommandContext;
import at.haha007.edencommands.CommandException;
import at.haha007.edencommands.annotations.AnnotatedCommandLoader;
import at.haha007.edencommands.annotations.Command;
import at.haha007.edencommands.annotations.SyncCommand;
import at.haha007.edencommands.argument.Argument;
import at.haha007.edencommands.argument.ParsedArgument;
import at.haha007.edencommands.tree.LiteralCommandNode;
import at.haha007.edenminigames.EdenMinigames;
import at.haha007.edenminigames.games.Game;
import at.haha007.edenminigames.utils.ConfigUtils;
import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class TntFightGame implements Game {

    private final List<TntFightArena> arenas = new ArrayList<>();
    private final Location lobbyLocation;
    private final double lobbyDistance;


    public TntFightGame() {
        ConfigurationSection globalCfg = EdenMinigames.instance().getConfig();
        ConfigurationSection cfg = globalCfg.getConfigurationSection("tnt_fight");
        if (cfg == null) throw new RuntimeException("tnt_fight section not found in config");
        lobbyLocation = ConfigUtils.location(Objects.requireNonNull(cfg.getConfigurationSection("lobby")));
        lobbyDistance = cfg.getDouble("lobby.radius");
        ConfigurationSection arenasCfg = cfg.getConfigurationSection("arenas");
        if (arenasCfg == null) throw new RuntimeException("arenas section not found in config");
        for (String key : arenasCfg.getKeys(false)) {
            ConfigurationSection arenaCfg = arenasCfg.getConfigurationSection(key);
            if (arenaCfg == null) continue;
            arenas.add(new TntFightArena(key, arenaCfg, lobbyLocation));
        }
    }

    @Override
    public void stop(Player player) {
        arenas.forEach(arena -> arena.stop(player));
    }

    @Override
    public List<Player> activePlayers() {
        return arenas.stream().flatMap(arena -> arena.getPlayers().stream()).toList();
    }

    @Override
    public void initCommand(LiteralCommandNode.LiteralCommandBuilder cmd) {
        AnnotatedCommandLoader loader = new AnnotatedCommandLoader(EdenMinigames.instance());
        loader.addDefaultArgumentParsers();
        loader.addAnnotated(this);
        Argument<TntFightArena> argument = new Argument<>(c -> arenas.stream().map(TntFightArena::getKey)
                .map(AsyncTabCompleteEvent.Completion::completion).toList(), true) {
            public @NotNull ParsedArgument<TntFightArena> parse(CommandContext context) throws CommandException {
                String key = context.input()[context.pointer()];
                for (TntFightArena arena : arenas) {
                    if (arena.getKey().equalsIgnoreCase(key)) {
                        return new ParsedArgument<>(arena, 1);
                    }
                }
                throw new CommandException(Component.text("Arena not found!"), context);
            }
        };
        loader.addArgumentParser("arena", ctx -> argument);
        loader.getCommands().forEach(cmd::then);
    }

    @Command("start arena{type:arena}")
    @SyncCommand
    private void start(CommandContext ctx) throws CommandException {
        if (!activePlayers().isEmpty()) {
            throw new CommandException(Component.text("There is already a game running!"), ctx);
        }
        TntFightArena arena = ctx.parameter("arena");
        Collection<Player> players = lobbyLocation.getNearbyPlayers(lobbyDistance);
        ctx.sender().sendMessage(Component.text("Starting game in arena " + arena.getKey() + " with " + players.size() + " players!"));
        arena.start(List.copyOf(players));
        if (activePlayers().isEmpty())
            ctx.sender().sendMessage(Component.text("Game could not be started!"));
        else
            ctx.sender().sendMessage(Component.text("Game started!"));
    }
}
