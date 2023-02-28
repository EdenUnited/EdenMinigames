package at.haha007.edenminigames.placeholder;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LiteralPlaceholderNode extends PlaceholderNode<LiteralPlaceholderNode>{
    private final String key;

    @Override
    protected boolean matches(String s, PlaceholderContext context) {
        return key.equals(s);
    }

    protected LiteralPlaceholderNode getThis() {
        return this;
    }
}
