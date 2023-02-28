package at.haha007.edenminigames.placeholder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Accessors(fluent = true)
@Getter
public class PlaceholderContext {
    private final OfflinePlayer target;
    private final String[] input;
    private final List<Object> args = new ArrayList<>();

    public void addArgument(Object o) {
        args.add(o);
    }

    @SuppressWarnings("unchecked")
    public <T> T argument(int index) {
        return (T) args.get(index);
    }
}
