package de.priyme.singlebiome;

import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class WorldConfigStore {

    private final JavaPlugin plugin;

    public WorldConfigStore(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hasConfig(String worldName) {
        return worldFile(worldName).exists();
    }

    public Optional<WorldConfig> load(String worldName) {
        File file = worldFile(worldName);
        if (!file.exists()) return Optional.empty();

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);

        String biomeName = yml.getString("biome", null);
        String modeName = yml.getString("mode", null);

        Biome biome = BiomeKeys.parse(biomeName).orElse(Biome.PLAINS);
        GenerationMode mode = GenerationMode.parse(modeName).orElse(GenerationMode.NOISE);

        int flatSurfaceY = yml.getInt("flat.surface-y", 64);
        int flatFiller = yml.getInt("flat.filler-depth", 3);

        int noiseBaseY = yml.getInt("noise.base-y", 64);
        int noiseAmplitude = yml.getInt("noise.amplitude", 12);
        double noiseScale = yml.getDouble("noise.scale", 0.008D);
        int noiseOctaves = yml.getInt("noise.octaves", 4);
        double noisePersistence = yml.getDouble("noise.persistence", 0.5D);
        int noiseFillerDepth = yml.getInt("noise.filler-depth", plugin.getConfig().getInt("defaults.noise.filler-depth", 3));

        int treesPerChunk = yml.getInt("trees.per-chunk", 0);
        boolean treesEnabled = yml.contains("trees.enabled") ? yml.getBoolean("trees.enabled", treesPerChunk > 0) : treesPerChunk > 0;
        TreeStyle treeStyle = TreeStyle.parse(yml.getString("trees.style", plugin.getConfig().getString("defaults.trees.style", "VANILLA")));
        if (treeStyle == null) treeStyle = TreeStyle.VANILLA;

        Long seed = yml.contains("seed") ? yml.getLong("seed") : null;

        WorldConfig cfg = new WorldConfig(
                worldName,
                biome,
                mode,
                new FlatSettings(flatSurfaceY, flatFiller),
                new NoiseSettings(noiseBaseY, noiseAmplitude, noiseScale, noiseOctaves, noisePersistence, noiseFillerDepth),
                new TreesSettings(treesEnabled, treesPerChunk, treeStyle),
                seed
        );
        return Optional.of(cfg);
    }

    public void save(WorldConfig config) throws IOException {
        File file = worldFile(config.worldName());
        ensureDir(file.getParentFile());

        YamlConfiguration yml = new YamlConfiguration();
        yml.set("biome", BiomeKeys.id(config.biome()));
        yml.set("mode", config.mode().name());

        yml.set("flat.surface-y", config.flat().surfaceY());
        yml.set("flat.filler-depth", config.flat().fillerDepth());

        yml.set("noise.base-y", config.noise().baseY());
        yml.set("noise.amplitude", config.noise().amplitude());
        yml.set("noise.scale", config.noise().scale());
        yml.set("noise.octaves", config.noise().octaves());
        yml.set("noise.persistence", config.noise().persistence());
        yml.set("noise.filler-depth", config.noise().fillerDepth());

        yml.set("trees.enabled", config.trees().enabled());
        yml.set("trees.per-chunk", config.trees().perChunk());
        yml.set("trees.style", config.trees().style().name());

        yml.set("seed", config.seed());

        yml.save(file);
    }

    public List<WorldConfig> loadAll() {
        File dir = worldsDir();
        if (!dir.exists() || !dir.isDirectory()) return List.of();

        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) return List.of();

        List<WorldConfig> list = new ArrayList<>();
        for (File f : files) {
            String name = f.getName();
            if (name.endsWith(".yml")) {
                String worldName = name.substring(0, name.length() - 4);
                load(worldName).ifPresent(list::add);
            }
        }
        return list;
    }

    public boolean deleteConfig(String worldName) {
        File file = worldFile(worldName);
        return file.exists() && file.delete();
    }

    private File worldsDir() {
        File dir = new File(plugin.getDataFolder(), "worlds");
        ensureDir(dir);
        return dir;
    }

    private File worldFile(String worldName) {
        return new File(worldsDir(), worldName + ".yml");
    }

    private void ensureDir(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}
