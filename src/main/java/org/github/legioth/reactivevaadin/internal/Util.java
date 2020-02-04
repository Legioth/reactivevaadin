package org.github.legioth.reactivevaadin.internal;

public class Util {
    private Util() {
        // Only static helpers
    }

    private static final Object missingValueToken = new Object();

    @SuppressWarnings({ "unchecked" })
    public static <T> T missingValueToken() {
        return (T) missingValueToken;
    }
}
