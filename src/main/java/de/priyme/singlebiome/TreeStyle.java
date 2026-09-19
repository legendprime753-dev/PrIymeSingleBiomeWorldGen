package de.priyme.singlebiome;

import java.util.Locale;

public enum TreeStyle {
    VANILLA,
    SIMPLE,
    VANILLA_UNSAFE;

    public static TreeStyle parse(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toUpperCase(Locale.ROOT);
        if (s.isEmpty()) return null;
        try {
            return TreeStyle.valueOf(s);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
