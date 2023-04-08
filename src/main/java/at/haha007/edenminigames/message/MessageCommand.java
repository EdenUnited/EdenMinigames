package at.haha007.edenminigames.message;

import at.haha007.edencommands.CommandContext;
import at.haha007.edencommands.CommandException;
import at.haha007.edencommands.annotations.AnnotatedCommandLoader;
import at.haha007.edencommands.annotations.Command;
import at.haha007.edencommands.annotations.EnumArgumentParser;
import at.haha007.edencommands.argument.Argument;
import at.haha007.edencommands.argument.ParsedArgument;
import at.haha007.edencommands.tree.CommandBuilder;
import at.haha007.edenminigames.EdenMinigames;
import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

@Command("message")
public class MessageCommand {
    private final String root;

    public MessageCommand(CommandBuilder<?> cmd, String root) {
        this.root = root;

        Function<CommandContext, List<AsyncTabCompleteEvent.Completion>> pageCompletionSupplier =
                c -> IntStream.range(1, EdenMinigames.messenger().keys().size() / 10 + 1)
                        .mapToObj(String::valueOf).map(AsyncTabCompleteEvent.Completion::completion).toList();
        Argument<Integer> pageArgument = new Argument<>(pageCompletionSupplier, true) {
            @NotNull
            public ParsedArgument<Integer> parse(CommandContext context) throws CommandException {
                int page = context.parameter("page");
                if (page < 1)
                    throw new CommandException(Component.text("Page must be greater than 0!"), context);
                if (page > EdenMinigames.messenger().keys().size() / 10 + 1)
                    throw new CommandException(Component.text("Page must be smaller than " + (EdenMinigames.messenger().keys().size() / 10 + 1) + "!"), context);
                return new ParsedArgument<>(page, 1);
            }
        };
        List<AsyncTabCompleteEvent.Completion> soundKeys = Arrays.stream(org.bukkit.Sound.values())
                .map(org.bukkit.Sound::getKey)
                .map(NamespacedKey::getKey)
                .map(AsyncTabCompleteEvent.Completion::completion)
                .toList();
        Argument<Key> soundKeyArgument = new Argument<>(c -> soundKeys, true) {
            @NotNull
            public ParsedArgument<Key> parse(CommandContext context) throws CommandException {
                @Subst("ambient.cave")
                String key = context.input()[context.pointer()];
                Key k = Key.key(key);
                return new ParsedArgument<>(k, 1);
            }
        };

        AnnotatedCommandLoader loader = new AnnotatedCommandLoader(EdenMinigames.instance());
        loader.addDefaultArgumentParsers();
        loader.addArgumentParser("message", map -> new MessageArgument());
        loader.addArgumentParser("page", map -> pageArgument);
        loader.addArgumentParser("sound", map -> soundKeyArgument);
        loader.addArgumentParser("sound_source", EnumArgumentParser.builder(Sound.Source.class).build());
        loader.addAnnotated(this);
        loader.getCommands().forEach(cmd::then);
    }

    @Command("info")
    @Command("message{type:message}")
    private void messageInfo(CommandContext context) {
        Message message = context.parameter("message");
        String key = context.input()[3];
        String chat = message.chat();
        context.sender().sendMessage(Component.text("Message: " + key, NamedTextColor.GOLD));
        if (chat != null)
            context.sender().sendMessage(Component.text("- Chat: " + chat, NamedTextColor.GOLD));
        else
            context.sender().sendMessage(Component.text("- No chat message", NamedTextColor.GOLD));

        String actionbar = message.actionbar();
        if (actionbar != null)
            context.sender().sendMessage(Component.text("- Actionbar: " + actionbar, NamedTextColor.GOLD));
        else
            context.sender().sendMessage(Component.text("- No actionbar message", NamedTextColor.GOLD));

        Sound sound = message.sound();
        if (sound != null) {
            context.sender().sendMessage(Component.text("- Sound: " + sound.name(), NamedTextColor.GOLD));
            context.sender().sendMessage(Component.text("-- Pitch: " + sound.pitch(), NamedTextColor.GOLD));
            context.sender().sendMessage(Component.text("-- Volume: " + sound.volume(), NamedTextColor.GOLD));
            context.sender().sendMessage(Component.text("-- Source: " + sound.source().name(), NamedTextColor.GOLD));
        } else {
            context.sender().sendMessage(Component.text("- No sound", NamedTextColor.GOLD));
        }

        Message.MessageTitle messageTitle = message.title();
        if (messageTitle != null) {
            String title = messageTitle.title();
            if (title != null)
                context.sender().sendMessage(Component.text("- Title: " + title, NamedTextColor.GOLD));
            else {
                context.sender().sendMessage(Component.text("- No big title", NamedTextColor.GOLD));
            }

            String subtitle = messageTitle.subtitle();
            if (subtitle != null)
                context.sender().sendMessage(Component.text("- Subtitle: " + subtitle, NamedTextColor.GOLD));
            else
                context.sender().sendMessage(Component.text("- No subtitle", NamedTextColor.GOLD));

            Title.Times times = messageTitle.times();
            if (times != null) {
                context.sender().sendMessage(Component.text("- Times:", NamedTextColor.GOLD));
                context.sender().sendMessage(Component.text("-- Fade in: " + times.fadeIn(), NamedTextColor.GOLD));
                context.sender().sendMessage(Component.text("-- Fade out: " + times.fadeOut(), NamedTextColor.GOLD));
                context.sender().sendMessage(Component.text("-- Stay: " + times.stay(), NamedTextColor.GOLD));
            } else {
                context.sender().sendMessage(Component.text("- No times", NamedTextColor.GOLD));
            }
        } else {
            context.sender().sendMessage(Component.text("- No title", NamedTextColor.GOLD));
        }
    }

    @Command("edit")
    @Command("message{type:message}")
    @Command("titletimes set")
    @Command("fadein{type:int,range:'0,100'}")
    @Command("stay{type:int,range:'0,100'}")
    @Command("fadeout{type:int,range:'0,100'}")
    private void setTimes(CommandContext context) {
        Message message = context.parameter("message");
        Message.MessageTitle title = message.title();
        if (title == null) {
            title = new Message.MessageTitle();
            message.title(title);
        }
        int fadeIn = context.parameter("fadein");
        int stay = context.parameter("stay");
        int fadeOut = context.parameter("fadeout");
        fadeIn *= 50;
        stay *= 50;
        fadeOut *= 50;
        Title.Times times = Title.Times.times(Duration.ofMillis(fadeIn), Duration.ofMillis(stay), Duration.ofMillis(fadeOut));
        title.times(times);
        EdenMinigames.messenger().save();
        context.sender().sendMessage(Component.text("Set times!"));
    }


    @Command("edit")
    @Command("message{type:message}")
    @Command("subtitle set")
    @Command("subtitle{type:string,parser:greedy}")
    private void setSubtitle(CommandContext context) {
        Message message = context.parameter("message");
        Message.MessageTitle title = message.title();
        if (title == null) {
            title = new Message.MessageTitle();
            message.title(title);
        }
        String text = context.parameter("subtitle");
        title.subtitle(text);
        EdenMinigames.messenger().save();
        context.sender().sendMessage(Component.text("Set subtitle!"));
    }

    @Command("edit")
    @Command("message{type:message}")
    @Command("subtitle unset")
    private void unsetSubtitle(CommandContext context) {
        Message message = context.parameter("message");
        Message.MessageTitle title = message.title();
        if (title == null) {
            context.sender().sendMessage(Component.text("No subtitle to unset!"));
            return;
        }
        title.subtitle(null);
        EdenMinigames.messenger().save();
        context.sender().sendMessage(Component.text("Unset subtitle!"));
    }


    @Command("edit")
    @Command("message{type:message}")
    @Command("title set")
    @Command("title{type:string,parser:greedy}")
    private void setTitle(CommandContext context) {
        Message message = context.parameter("message");
        Message.MessageTitle title = message.title();
        if (title == null) {
            title = new Message.MessageTitle();
            message.title(title);
        }
        String text = context.parameter("title");
        title.title(text);
        EdenMinigames.messenger().save();
        context.sender().sendMessage(Component.text("Set title to: " + title));
    }

    @Command("edit")
    @Command("message{type:message}")
    @Command("title unset")
    private void unsetTitle(CommandContext context) {
        Message message = context.parameter("message");
        Message.MessageTitle title = message.title();
        if (title == null) {
            context.sender().sendMessage(Component.text("No title to unset!"));
            return;
        }
        title.title(null);
        EdenMinigames.messenger().save();
        context.sender().sendMessage(Component.text("Unset title!"));
    }


    @Command("edit")
    @Command("message{type:message}")
    @Command("sound set")
    @Command("sound{type:sound}")
    private void setSound(CommandContext context) {
        Message message = context.parameter("message");
        Key key = context.parameter("sound");
        message.sound(Sound.sound(key, Sound.Source.MASTER, 1, 1));
        EdenMinigames.messenger().save();
        context.sender().sendMessage(Component.text("Set sound to: " + key));
    }

    @Command("edit")
    @Command("message{type:message}")
    @Command("sound set")
    @Command("sound{type:sound}")
    @Command("sound_source{type:sound_source}")
    @Command("sound_volume{type:float,range:'0.0,10.0',suggest:'0.5,1,2'}")
    @Command("sound_pitch{type:float,range:'0.0,2.0',suggest:'0,0.5,1,2'}")
    private void setSoundAll(CommandContext context) {
        Message message = context.parameter("message");
        Key key = context.parameter("sound");
        Sound.Source source = context.parameter("sound_source");
        float volume = context.parameter("sound_volume");
        float pitch = context.parameter("sound_pitch");
        message.sound(Sound.sound(key, source, volume, pitch));
        context.sender().playSound(message.sound());
        EdenMinigames.messenger().save();
        context.sender().sendMessage(Component.text("Set sound to: " + key));
    }

    @Command("edit")
    @Command("message{type:message}")
    @Command("sound unset")
    private void unsetSound(CommandContext context) {
        Message message = context.parameter("message");
        message.sound(null);
        EdenMinigames.messenger().save();
        context.sender().sendMessage(Component.text("Unset sound"));
    }

    @Command("edit")
    @Command("message{type:message}")
    @Command("chat set")
    @Command("chat{type:string,parser:greedy}")
    private void setChat(CommandContext context) {
        Message message = context.parameter("message");
        String chat = context.parameter("chat");
        message.chat(chat);
        EdenMinigames.messenger().save();
        context.sender().sendMessage(Component.text("Set chat message to: " + chat));
    }

    @Command("edit")
    @Command("message{type:message}")
    @Command("chat unset")
    private void unsetChat(CommandContext context) {
        Message message = context.parameter("message");
        message.chat(null);
        EdenMinigames.messenger().save();
        context.sender().sendMessage(Component.text("Unset chat message"));
    }

    @Command("edit")
    @Command("message{type:message}")
    @Command("actionbar set")
    @Command("chat{type:string,parser:greedy}")
    private void setActionbar(CommandContext context) {
        Message message = context.parameter("message");
        String actionbar = context.parameter("chat");
        message.actionbar(actionbar);
        EdenMinigames.messenger().save();
        context.sender().sendMessage(Component.text("Set actionbar message to: " + actionbar));
    }

    @Command("edit")
    @Command("message{type:message}")
    @Command("actionbar unset")
    private void unsetActionbar(CommandContext context) {
        Message message = context.parameter("message");
        message.actionbar(null);
        EdenMinigames.messenger().save();
        context.sender().sendMessage(Component.text("Unset actionbar message"));
    }


    @Command("test")
    @Command("message{type:message}")
    private void test(CommandContext context) {
        Message message = context.parameter("message");
        Player player = context.sender() instanceof Player ? (Player) context.sender() : null;
        message.sendMessage(context.sender(), player);
    }

    @Command("list")
    @Command("page{type:page}")
    private void listMessagesPage(CommandContext context) {
        int page = context.parameter("page");
        List<String> keys = EdenMinigames.messenger().keys().stream().sorted().toList();
        int from = (page - 1) * 10;
        int to = Math.min(keys.size(), page * 10);
        for (int i = from; i < to; i++) {
            String key = keys.get(i);
            Component msg = Component.text(key)
                    .hoverEvent(Component.text("/" + root + " message info " + key))
                    .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/" + root + " message info " + key));
            context.sender().sendMessage(msg);
        }
    }

    @Command("list")
    private void listMessages(CommandContext context) {
        List<String> keys = EdenMinigames.messenger().keys().stream().sorted().toList();
        int amount = Math.min(keys.size(), 10);
        for (int i = 0; i < amount; i++) {
            String key = keys.get(i);
            Component msg = Component.text(key)
                    .hoverEvent(Component.text("/" + root + " message info " + key))
                    .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/" + root + " message info " + key));
            context.sender().sendMessage(msg);
        }
    }

    private static class MessageArgument extends Argument<Message> {
        public MessageArgument() {
            super(c -> EdenMinigames.messenger().keys().stream().map(AsyncTabCompleteEvent.Completion::completion).toList(), true);
        }

        @Override
        public @NotNull ParsedArgument<Message> parse(CommandContext commandContext) throws CommandException {
            Message message = EdenMinigames.messenger().message(commandContext.input()[commandContext.pointer()]);
            if (message == null)
                throw new CommandException(Component.text("Message not found!"), commandContext);
            return new ParsedArgument<>(message, 1);
        }
    }
}
