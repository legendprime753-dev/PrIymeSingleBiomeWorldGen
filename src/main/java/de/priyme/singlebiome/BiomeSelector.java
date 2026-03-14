package de.priyme.singlebiome;

import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class BiomeSelector {

    private final boolean whitelistEnabled;
    private final Set<Biome> allowed;

    public BiomeSelector(FileConfiguration config) {
        this.whitelistEnabled = config.getBoolean("biome-whitelist.enabled", false);
        if (!whitelistEnabled) {
            this.allowed = Collections.unmodifiableSet(allBiomes());
            return;
        }

        List<String> raw = config.getStringList("biome-whitelist.biomes");
        Set<Biome> set = new HashSet<>();
        for (String s : raw) {
            parseBiome(s).ifPresent(set::add);
        }
        if (set.isEmpty()) {
            set = allBiomes();
        }
        this.allowed = Collections.unmodifiableSet(set);
    }

    public boolean isAllowed(Biome biome) {
        return allowed.contains(biome);
    }

    public List<Biome> allowedBiomesSorted() {
        List<Biome> list = new ArrayList<>(allowed);
        list.sort((a, b) -> BiomeKeys.id(a).compareToIgnoreCase(BiomeKeys.id(b)));
        return list;
    }

    public List<String> allowedBiomeIdsSorted() {
        return allowedBiomesSorted().stream().map(BiomeKeys::id).collect(Collectors.toList());
    }

    public java.util.Optional<Biome> parseBiome(String input) {
        return BiomeKeys.parse(input);
    }

    private Set<Biome> allBiomes() {
        Set<Biome> set = new HashSet<>();
        for (Biome biome : Registry.BIOME) {
            set.add(biome);
        }
        return set;
    }
}
