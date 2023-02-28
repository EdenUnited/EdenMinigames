package at.haha007.edenminigames.placeholder;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Accessors(fluent = true)
public abstract class PlaceholderNode<T extends PlaceholderNode<T>> {
    @NotNull
    @Setter
    private Function<PlaceholderContext, String> executor = c -> null;
    private final List<PlaceholderNode<?>> children = new ArrayList<>();

    public T then(PlaceholderNode<?> child) {
        children.add(child);
        return getThis();
    }

    String execute(PlaceholderContext context, int pointer) {
        if (pointer >= context.input().length) return null;
        String arg = context.input()[pointer];
        if (!matches(arg, context)) return null;
        for (PlaceholderNode<?> child : children) {
            String s = child.execute(context, pointer + 1);
            if (s != null) return s;
        }
        return executor.apply(context);
    }

    protected abstract boolean matches(String s, PlaceholderContext context);

    protected abstract T getThis();
}
