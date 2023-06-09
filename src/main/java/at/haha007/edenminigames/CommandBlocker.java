package at.haha007.edenminigames;

import at.haha007.edencommands.CommandRegistry;
import at.haha007.edencommands.tree.LiteralCommandNode;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CommandBlocker implements Listener {
    private final List<String> allowedCommands = new ArrayList<>();

    public CommandBlocker() {
        CommandRegistry registry = EdenMinigames.instance().commandRegistry();
        registry.register(LiteralCommandNode.builder("commandblocker").executor(c -> {
            loadCommands();
        }).build());
        loadCommands();
        Bukkit.getPluginManager().registerEvents(this, EdenMinigames.instance());
    }

    private void loadCommands() {
        File allowedCommandsFile = new File(EdenMinigames.instance().getDataFolder(), "allowed_commands.txt");
        if (!allowedCommandsFile.exists())
            EdenMinigames.instance().saveResource("allowed_commands.txt", false);
        allowedCommands.clear();
        //read text from file
        try (FileReader fileReader = new FileReader(allowedCommandsFile);
             BufferedReader reader = new BufferedReader(fileReader)) {
            String line;
            while ((line = reader.readLine()) != null) {
                allowedCommands.add(line.toLowerCase());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    private void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.getPlayer().hasPermission("minigames.command_blocker.bypass")) return;
        if (!EdenMinigames.isInGame(event.getPlayer())) return;
        String command = event.getMessage().split(" ")[0].substring(1).toLowerCase();
        if (allowedCommands.contains(command)) return;
        EdenMinigames.messenger().sendMessage("command_blocker.blocked", event.getPlayer());
        event.setCancelled(true);
    }
}
