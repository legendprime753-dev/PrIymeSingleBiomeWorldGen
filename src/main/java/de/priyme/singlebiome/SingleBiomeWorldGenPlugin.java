package de.priyme.singlebiome;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SingleBiomeWorldGenPlugin extends JavaPlugin {

    private BiomeSelector biomeSelector;
    private WorldConfigStore worldConfigStore;
    private PresetStore presetStore;
    private WorldManager worldManager;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.biomeSelector = new BiomeSelector(getConfig());
        this.worldConfigStore = new WorldConfigStore(this);
        this.presetStore = new PresetStore(this);
        this.worldManager = new WorldManager(this, biomeSelector, worldConfigStore);
        this.guiManager = new GuiManager(this, biomeSelector, worldManager, worldConfigStore);

        Bukkit.getPluginManager().registerEvents(guiManager, this);

        if (getConfig().getBoolean("mobs.suppress-natural-spawning", true)) {
            Bukkit.getPluginManager().registerEvents(new MobSuppressListener(worldManager), this);
        }

        SbwCommand command = new SbwCommand(this, biomeSelector, worldManager, presetStore, worldConfigStore, guiManager);
        PluginCommand sbw = getCommand("sbw");
        if (sbw != null) {
            sbw.setExecutor(command);
            sbw.setTabCompleter(command);
        }

        if (getConfig().getBoolean("worlds.auto-load-on-startup", true)) {
            worldManager.loadManagedWorldsOnStartup();
        }
    }

    public FlatSettings defaultFlatSettings() {
        int surfaceY = getConfig().getInt("defaults.flat.surface-y", 64);
        int fillerDepth = getConfig().getInt("defaults.flat.filler-depth", 3);
        return new FlatSettings(surfaceY, fillerDepth);
    }

    public NoiseSettings defaultNoiseSettings() {
        int baseY = getConfig().getInt("defaults.noise.base-y", 64);
        int amplitude = getConfig().getInt("defaults.noise.amplitude", 12);
        double scale = getConfig().getDouble("defaults.noise.scale", 0.008D);
        int octaves = getConfig().getInt("defaults.noise.octaves", 4);
        double persistence = getConfig().getDouble("defaults.noise.persistence", 0.5D);
        int fillerDepth = getConfig().getInt("defaults.noise.filler-depth", 3);
        return new NoiseSettings(baseY, amplitude, scale, octaves, persistence, fillerDepth);
    }

    public TreesSettings defaultTreesSettings() {
        boolean enabled = getConfig().getBoolean("defaults.trees.enabled", false);
        int perChunk = getConfig().getInt("defaults.trees.per-chunk", 0);
        if (!enabled) perChunk = 0;
        TreeStyle style = TreeStyle.parse(getConfig().getString("defaults.trees.style", "VANILLA"));
        if (style == null) style = TreeStyle.VANILLA;
        return new TreesSettings(enabled, perChunk, style);
    }

    public GenerationMode defaultMode() {
        String raw = getConfig().getString("defaults.mode", "NOISE");
        return GenerationMode.parse(raw).orElse(GenerationMode.NOISE);
    }
}
