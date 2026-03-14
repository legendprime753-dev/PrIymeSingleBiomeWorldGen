package de.priyme.singlebiome;

import org.bukkit.block.Biome;

public record WorldConfig(String worldName,
                          Biome biome,
                          GenerationMode mode,
                          FlatSettings flat,
                          NoiseSettings noise,
                          TreesSettings trees,
                          Long seed) {
}
