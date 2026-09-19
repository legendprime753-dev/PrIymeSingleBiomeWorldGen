package de.priyme.singlebiome;

import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PresetStore {

    private final JavaPlugin plugin;
    private final File file;

    public PresetStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "presets.yml");
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException ignored) {
            }
        }
    }

    public List<String> listPresetNames() {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yml.getConfigurationSection("presets");
        if (section == null) return List.of();
        List<String> names = new ArrayList<>(section.getKeys(false));
        names.sort(String::compareToIgnoreCase);
        return names;
    }

    public Optional<WorldConfig> loadPreset(String presetName) {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yml.getConfigurationSection("presets." + presetName);
        if (section == null) return Optional.empty();

        String biomeName = section.getString("biome", "minecraft:plains");
        String modeName = section.getString("mode", "NOISE");

        Biome biome = BiomeKeys.parse(biomeName).orElse(Biome.PLAINS);
        GenerationMode mode = GenerationMode.parse(modeName).orElse(GenerationMode.NOISE);

        int flatSurfaceY = section.getInt("flat.surface-y", 64);
        int flatFiller = section.getInt("flat.filler-depth", 3);

        int noiseBaseY = section.getInt("noise.base-y", 64);
        int noiseAmplitude = section.getInt("noise.amplitude", 12);
        double noiseScale = section.getDouble("noise.scale", 0.008D);
        int noiseOctaves = section.getInt("noise.octaves", 4);
        double noisePersistence = section.getDouble("noise.persistence", 0.5D);
        int noiseFillerDepth = section.getInt("noise.filler-depth", plugin.getConfig().getInt("defaults.noise.filler-depth", 3));

        int treesPerChunk = section.getInt("trees.per-chunk", 0);
        boolean treesEnabled = section.contains("trees.enabled") ? section.getBoolean("trees.enabled", treesPerChunk > 0) : treesPerChunk > 0;
        TreeStyle treeStyle = TreeStyle.parse(section.getString("trees.style", plugin.getConfig().getString("defaults.trees.style", "VANILLA")));
        if (treeStyle == null) treeStyle = TreeStyle.VANILLA;

        Long seed = section.contains("seed") ? section.getLong("seed") : null;

        WorldConfig cfg = new WorldConfig(
                "PRESET",
                biome,
                mode,
                new FlatSettings(flatSurfaceY, flatFiller),
                new NoiseSettings(noiseBaseY, noiseAmplitude, noiseScale, noiseOctaves, noisePersistence, noiseFillerDepth),
                new TreesSettings(treesEnabled, treesPerChunk, treeStyle),
                seed
        );
        return Optional.of(cfg);
    }

    public void savePreset(String presetName, WorldConfig config) throws IOException {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);

        String path = "presets." + presetName;

        yml.set(path + ".biome", BiomeKeys.id(config.biome()));
        yml.set(path + ".mode", config.mode().name());

        yml.set(path + ".flat.surface-y", config.flat().surfaceY());
        yml.set(path + ".flat.filler-depth", config.flat().fillerDepth());

        yml.set(path + ".noise.base-y", config.noise().baseY());
        yml.set(path + ".noise.amplitude", config.noise().amplitude());
        yml.set(path + ".noise.scale", config.noise().scale());
        yml.set(path + ".noise.octaves", config.noise().octaves());
        yml.set(path + ".noise.persistence", config.noise().persistence());
        yml.set(path + ".noise.filler-depth", config.noise().fillerDepth());

        yml.set(path + ".trees.enabled", config.trees().enabled());
        yml.set(path + ".trees.per-chunk", config.trees().perChunk());
        yml.set(path + ".trees.style", config.trees().style().name());

        yml.set(path + ".seed", config.seed());

        plugin.getDataFolder().mkdirs();
        yml.save(file);
    }

    public boolean deletePreset(String presetName) throws IOException {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        if (!yml.contains("presets." + presetName)) return false;
        yml.set("presets." + presetName, null);
        yml.save(file);
        return true;
    }
}
