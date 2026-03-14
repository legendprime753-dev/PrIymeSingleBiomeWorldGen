package de.priyme.singlebiome;

import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class SingleBiomeChunkGenerator extends ChunkGenerator {

    private final WorldConfig config;
    private final SurfacePalette surfacePalette;
    private final List<BlockPopulator> populators;
    private volatile PerlinNoise perlin;
    private volatile long perlinSeed = Long.MIN_VALUE;

    public SingleBiomeChunkGenerator(WorldConfig config) {
        this.config = config;
        this.surfacePalette = SurfacePalette.fromBiome(config.biome());
        TreesSettings trees = config.trees();
        if (!isVoidBiome(config.biome()) && trees.enabled() && trees.perChunk() > 0) {
            if (isMushroomBiome(config.biome())) {
                this.populators = List.of(new MushroomPopulator(trees));
            } else if (trees.style() == TreeStyle.SIMPLE) {
                this.populators = List.of(new SimpleTreePopulator(trees, config.biome()));
            } else {
                this.populators = List.of(new SingleBiomeTreePopulator(trees, config.biome()));
            }
        } else {
            this.populators = List.of();
        }
    }

    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
        ChunkData data = createChunkData(world);

        if (!isVoidBiome(config.biome())) {
            generateBaseTerrain(world.getMinHeight(), world.getMaxHeight(), world.getSeed(), chunkX, chunkZ, data);
            generateSurfaceLayers(world.getMinHeight(), world.getMaxHeight(), world.getSeed(), chunkX, chunkZ, data);
        }

        applyBiomeGrid(world.getMinHeight(), world.getMaxHeight(), biome);

        return data;
    }

    @Override
    public void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData data) {
        if (isVoidBiome(config.biome())) return;
        generateBaseTerrain(worldInfo.getMinHeight(), worldInfo.getMaxHeight(), worldInfo.getSeed(), chunkX, chunkZ, data);
    }

    @Override
    public void generateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData data) {
        if (isVoidBiome(config.biome())) return;
        int minY = worldInfo.getMinHeight();
        int maxY = worldInfo.getMaxHeight();
        if (data.getType(0, minY, 0) != Material.BEDROCK) {
            generateBaseTerrain(minY, maxY, worldInfo.getSeed(), chunkX, chunkZ, data);
        }
        generateSurfaceLayers(minY, maxY, worldInfo.getSeed(), chunkX, chunkZ, data);
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
        return new SingleBiomeBiomeProvider(config.biome());
    }

    @Override
    public int getBaseHeight(WorldInfo worldInfo, Random random, int x, int z, HeightMap heightMap) {
        if (isVoidBiome(config.biome())) return worldInfo.getMinHeight();
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        int localX = x & 15;
        int localZ = z & 15;

        int height = surfaceY(worldInfo.getSeed(), chunkX, chunkZ, localX, localZ);
        return clamp(height, worldInfo.getMinHeight() + 1, worldInfo.getMaxHeight() - 2);
    }

    private void generateBaseTerrain(int minY, int maxY, long seed, int chunkX, int chunkZ, ChunkData data) {
        int filler = Math.max(0, config.mode() == GenerationMode.FLAT ? config.flat().fillerDepth() : config.noise().fillerDepth());

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int height = surfaceY(seed, chunkX, chunkZ, x, z);
                height = clamp(height, minY + 1, maxY - 2);

                data.setBlock(x, minY, z, Material.BEDROCK);

                int stoneTop = Math.max(minY + 1, height - filler - 1);
                if (stoneTop >= minY + 1 && stoneTop < height) {
                    data.setRegion(x, minY + 1, z, x + 1, stoneTop + 1, z + 1, surfacePalette.stone());
                }
            }
        }
    }

    private void generateSurfaceLayers(int minY, int maxY, long seed, int chunkX, int chunkZ, ChunkData data) {
        int filler = Math.max(0, config.mode() == GenerationMode.FLAT ? config.flat().fillerDepth() : config.noise().fillerDepth());

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int height = surfaceY(seed, chunkX, chunkZ, x, z);
                height = clamp(height, minY + 1, maxY - 2);

                int fillerFrom = Math.max(minY + 1, height - filler);
                if (height > fillerFrom) {
                    data.setRegion(x, fillerFrom, z, x + 1, height, z + 1, surfacePalette.filler());
                }

                data.setBlock(x, height, z, surfacePalette.top());

            }
        }
    }

    private void applyBiomeGrid(int minY, int maxY, BiomeGrid biome) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                biome.setBiome(x, z, config.biome());
            }
        }
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return populators;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        return false;
    }

    @Override
    public boolean shouldGenerateNoise() {
        return true;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return true;
    }

    @Override
    public boolean shouldGenerateBedrock() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        return false;
    }

    @Override
    public boolean isParallelCapable() {
        return true;
    }

    private int surfaceY(long seed, int chunkX, int chunkZ, int x, int z) {
        if (config.mode() == GenerationMode.FLAT) {
            return config.flat().surfaceY();
        }

        NoiseSettings s = config.noise();
        double scale = Math.max(1.0E-6D, s.scale());

        int wx = (chunkX << 4) + x;
        int wz = (chunkZ << 4) + z;

        double n = perlin(seed).octaveNoise(wx * scale, wz * scale, s.octaves(), s.persistence());
        double h = s.baseY() + (n * s.amplitude());
        return (int) Math.round(h);
    }

    private PerlinNoise perlin(long seed) {
        PerlinNoise local = perlin;
        if (local != null && perlinSeed == seed) return local;
        synchronized (this) {
            if (perlin == null || perlinSeed != seed) {
                perlin = new PerlinNoise(seed);
                perlinSeed = seed;
            }
            return perlin;
        }
    }

    private int clamp(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    private record SurfacePalette(Material top, Material filler, Material stone) {

        static SurfacePalette fromBiome(Biome biome) {
            String n = BiomeKeys.id(biome).toUpperCase(Locale.ROOT);

            if (n.contains("NETHER") || n.contains("CRIMSON") || n.contains("WARPED") || n.contains("BASALT") || n.contains("SOUL_SAND")) {
                if (n.contains("CRIMSON")) return new SurfacePalette(Material.CRIMSON_NYLIUM, Material.NETHERRACK, Material.NETHERRACK);
                if (n.contains("WARPED")) return new SurfacePalette(Material.WARPED_NYLIUM, Material.NETHERRACK, Material.NETHERRACK);
                if (n.contains("SOUL_SAND")) return new SurfacePalette(Material.SOUL_SAND, Material.SOUL_SOIL, Material.NETHERRACK);
                if (n.contains("BASALT")) return new SurfacePalette(Material.BASALT, Material.BASALT, Material.BASALT);
                return new SurfacePalette(Material.NETHERRACK, Material.NETHERRACK, Material.NETHERRACK);
            }

            if (n.contains("END")) {
                return new SurfacePalette(Material.END_STONE, Material.END_STONE, Material.END_STONE);
            }

            if (biome == Biome.MUSHROOM_FIELDS) {
                return new SurfacePalette(Material.MYCELIUM, Material.DIRT, Material.STONE);
            }
            if (n.contains("MUSHROOM")) {
                return new SurfacePalette(Material.MYCELIUM, Material.DIRT, Material.STONE);
            }

            if (n.contains("DESERT") || n.contains("BADLANDS")) {
                return new SurfacePalette(Material.SAND, Material.SANDSTONE, Material.STONE);
            }

            if (n.contains("BEACH") || n.contains("OCEAN") || n.contains("RIVER")) {
                return new SurfacePalette(Material.SAND, Material.SANDSTONE, Material.STONE);
            }

            if (n.contains("SNOW") || n.contains("FROZEN") || n.contains("ICE")) {
                return new SurfacePalette(Material.SNOW_BLOCK, Material.DIRT, Material.STONE);
            }

            if (n.contains("SWAMP") || n.contains("MANGROVE")) {
                return new SurfacePalette(Material.GRASS_BLOCK, Material.DIRT, Material.STONE);
            }

            if (n.contains("SAVANNA")) {
                return new SurfacePalette(Material.GRASS_BLOCK, Material.DIRT, Material.STONE);
            }

            if (n.contains("STONY") || n.contains("MOUNTAIN") || n.contains("PEAK") || n.contains("HILLS")) {
                return new SurfacePalette(Material.STONE, Material.STONE, Material.STONE);
            }

            return new SurfacePalette(Material.GRASS_BLOCK, Material.DIRT, Material.STONE);
        }
    }

    private boolean isMushroomBiome(Biome biome) {
        String n = BiomeKeys.id(biome).toLowerCase(Locale.ROOT);
        return n.contains("mushroom");
    }

    private boolean isVoidBiome(Biome biome) {
        String n = BiomeKeys.id(biome).toLowerCase(Locale.ROOT);
        return n.contains("the_void");
    }
}
