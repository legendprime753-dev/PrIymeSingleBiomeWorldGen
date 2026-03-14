package de.priyme.singlebiome;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;

import java.util.List;

public final class SingleBiomeBiomeProvider extends BiomeProvider {

    private final Biome biome;

    public SingleBiomeBiomeProvider(Biome biome) {
        this.biome = biome;
    }

    @Override
    public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
        return biome;
    }

    @Override
    public List<Biome> getBiomes(WorldInfo worldInfo) {
        return List.of(biome);
    }
}
