package de.priyme.singlebiome;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;

import java.util.Locale;
import java.util.Optional;

public final class BiomeKeys {

    private BiomeKeys() {
    }

    public static String id(Biome biome) {
        return biome.getKey().toString();
    }

    public static Optional<Biome> parse(String input) {
        if (input == null) return Optional.empty();

        String raw = input.trim();
        if (raw.isEmpty()) return Optional.empty();

        String normalized = raw.toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        if (normalized.equals("void") || normalized.equals("minecraft:void")) {
            normalized = "minecraft:the_void";
        }

        NamespacedKey key = NamespacedKey.fromString(normalized.contains(":") ? normalized : "minecraft:" + normalized);
        if (key == null) return Optional.empty();

        Biome biome = Registry.BIOME.get(key);
        return Optional.ofNullable(biome);
    }
}
