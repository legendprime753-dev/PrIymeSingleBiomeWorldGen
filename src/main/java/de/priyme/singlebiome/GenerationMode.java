package de.priyme.singlebiome;

import java.util.Locale;
import java.util.Optional;

public enum GenerationMode {
    FLAT,
    NOISE;

    public static Optional<GenerationMode> parse(String input) {
        if (input == null) return Optional.empty();
        String s = input.trim().toUpperCase(Locale.ROOT);
        if (s.equals("HILLS")) return Optional.of(NOISE);
        try {
            return Optional.of(GenerationMode.valueOf(s));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
