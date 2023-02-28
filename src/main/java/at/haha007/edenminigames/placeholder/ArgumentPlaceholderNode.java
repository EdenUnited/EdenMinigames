package at.haha007.edenminigames.placeholder;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ArgumentPlaceholderNode<T> extends PlaceholderNode<ArgumentPlaceholderNode<T>> {
    private final PlaceholderArgument<T> argument;

    String execute(PlaceholderContext context, int pointer) {
        return super.execute(context, pointer);
    }

    protected boolean matches(String s, PlaceholderContext context) {
        T t = argument.parse(s);
        if (t == null) return false;
        context.addArgument(t);
        return true;
    }

    protected ArgumentPlaceholderNode<T> getThis() {
        return this;
    }
}
