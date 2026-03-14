package de.priyme.singlebiome;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class WorldManager {

    private final JavaPlugin plugin;
    private final BiomeSelector biomeSelector;
    private final WorldConfigStore store;

    public WorldManager(JavaPlugin plugin, BiomeSelector biomeSelector, WorldConfigStore store) {
        this.plugin = plugin;
        this.biomeSelector = biomeSelector;
        this.store = store;
    }

    public boolean isManagedWorld(World world) {
        return world != null && store.hasConfig(world.getName());
    }

    public boolean isManagedWorld(String worldName) {
        return worldName != null && store.hasConfig(worldName);
    }

    public List<String> managedWorldNames() {
        return store.loadAll().stream().map(WorldConfig::worldName).sorted(String::compareToIgnoreCase).toList();
    }

    public Optional<World> createWorld(WorldConfig config, CommandSender sender) {
        String name = config.worldName();

        if (!biomeSelector.isAllowed(config.biome())) {
            sender.sendMessage(Text.error("This biome is not allowed by the whitelist."));
            return Optional.empty();
        }

        if (Bukkit.getWorld(name) != null) {
            sender.sendMessage(Text.error("This world is already loaded: " + name));
            return Optional.empty();
        }

        File worldFolder = new File(Bukkit.getWorldContainer(), name);
        if (worldFolder.exists() && !store.hasConfig(name)) {
            sender.sendMessage(Text.error("World folder already exists. Choose a different name or delete the folder."));
            return Optional.empty();
        }

        WorldCreator creator = new WorldCreator(name);
        creator.type(WorldType.NORMAL);
        creator.generateStructures(false);
        long seed = config.seed() != null ? config.seed() : new Random().nextLong();
        creator.seed(seed);
        creator.generator(new SingleBiomeChunkGenerator(config));
        creator.biomeProvider(new SingleBiomeBiomeProvider(config.biome()));

        World world = Bukkit.createWorld(creator);
        if (world == null) {
            sender.sendMessage(Text.error("World could not be created."));
            return Optional.empty();
        }

        applyGamerules(world);

        WorldConfig toSave = config.seed() == null
                ? new WorldConfig(config.worldName(), config.biome(), config.mode(), config.flat(), config.noise(), config.trees(), seed)
                : config;

        try {
            store.save(toSave);
        } catch (IOException ex) {
            sender.sendMessage(Text.error("World config could not be saved."));
        }

        sender.sendMessage(Text.ok("World created: " + name + " (" + BiomeKeys.id(config.biome()) + ", " + config.mode().name()
                + ", trees=" + config.trees().perChunk() + "/chunk, seed=" + seed + ")"));
        return Optional.of(world);
    }

    public Optional<World> loadWorld(String name, CommandSender sender) {
        World already = Bukkit.getWorld(name);
        if (already != null) return Optional.of(already);

        Optional<WorldConfig> cfg = store.load(name);
        if (cfg.isEmpty()) {
            sender.sendMessage(Text.error("No saved config found for this world."));
            return Optional.empty();
        }

        File worldFolder = new File(Bukkit.getWorldContainer(), name);
        if (!worldFolder.exists()) {
            sender.sendMessage(Text.error("World folder does not exist."));
            return Optional.empty();
        }

        WorldCreator creator = new WorldCreator(name);
        creator.type(WorldType.NORMAL);
        creator.generateStructures(false);
        creator.generator(new SingleBiomeChunkGenerator(cfg.get()));
        creator.biomeProvider(new SingleBiomeBiomeProvider(cfg.get().biome()));

        World world = Bukkit.createWorld(creator);
        if (world == null) {
            sender.sendMessage(Text.error("World could not be loaded."));
            return Optional.empty();
        }

        applyGamerules(world);
        sender.sendMessage(Text.ok("World loaded: " + name));
        return Optional.of(world);
    }

    public boolean deleteWorld(String name, CommandSender sender) {
        if (!store.hasConfig(name)) {
            sender.sendMessage(Text.error("This world is not registered as a plugin world."));
            return false;
        }

        World world = Bukkit.getWorld(name);
        if (world != null) {
            boolean unloaded = Bukkit.unloadWorld(world, true);
            if (!unloaded) {
                sender.sendMessage(Text.error("World could not be unloaded."));
                return false;
            }
        }

        File worldFolder = new File(Bukkit.getWorldContainer(), name);
        boolean deleted = deleteRecursively(worldFolder);
        if (!deleted) {
            sender.sendMessage(Text.error("World folder could not be deleted."));
            return false;
        }

        store.deleteConfig(name);
        sender.sendMessage(Text.ok("World deleted: " + name));
        return true;
    }

    public void loadManagedWorldsOnStartup() {
        for (WorldConfig cfg : store.loadAll()) {
            String name = cfg.worldName();
            if (Bukkit.getWorld(name) != null) continue;

            File worldFolder = new File(Bukkit.getWorldContainer(), name);
            if (!worldFolder.exists()) continue;

            WorldCreator creator = new WorldCreator(name);
            creator.type(WorldType.NORMAL);
            creator.generateStructures(false);
            creator.generator(new SingleBiomeChunkGenerator(cfg));
            creator.biomeProvider(new SingleBiomeBiomeProvider(cfg.biome()));
            World world = Bukkit.createWorld(creator);
            if (world != null) {
                applyGamerules(world);
            }
        }
    }

    private void applyGamerules(World world) {
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
        world.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
    }

    private boolean deleteRecursively(File file) {
        if (file == null || !file.exists()) return true;

        File[] children = file.listFiles();
        if (children != null) {
            for (File c : children) {
                if (!deleteRecursively(c)) return false;
            }
        }
        return file.delete();
    }
}
