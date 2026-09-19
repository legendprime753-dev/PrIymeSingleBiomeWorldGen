package de.priyme.singlebiome;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public final class SbwCommand implements CommandExecutor, TabCompleter {

    private final SingleBiomeWorldGenPlugin plugin;
    private final BiomeSelector biomeSelector;
    private final WorldManager worldManager;
    private final PresetStore presetStore;
    private final WorldConfigStore worldConfigStore;
    private final GuiManager guiManager;

    public SbwCommand(SingleBiomeWorldGenPlugin plugin,
                      BiomeSelector biomeSelector,
                      WorldManager worldManager,
                      PresetStore presetStore,
                      WorldConfigStore worldConfigStore,
                      GuiManager guiManager) {
        this.plugin = plugin;
        this.biomeSelector = biomeSelector;
        this.worldManager = worldManager;
        this.presetStore = presetStore;
        this.worldConfigStore = worldConfigStore;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                guiManager.openCreator(player);
                return true;
            }
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "gui" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Text.error("Players only."));
                    return true;
                }
                guiManager.openCreator(player);
                return true;
            }
            case "list" -> {
                List<String> names = worldManager.managedWorldNames();
                if (names.isEmpty()) {
                    sender.sendMessage(Text.info("No plugin worlds saved."));
                    return true;
                }
                sender.sendMessage(Text.info("Plugin worlds: " + String.join(", ", names)));
                return true;
            }
            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage(Text.error("Usage: /sbw info <world>"));
                    return true;
                }
                String world = args[1];
                worldConfigStore.load(world).ifPresentOrElse(cfg -> {
                    sender.sendMessage(Text.info("World: " + cfg.worldName()));
                    sender.sendMessage(Text.info("Biome: " + BiomeKeys.id(cfg.biome())));
                    sender.sendMessage(Text.info("Mode: " + cfg.mode().name()));
                    sender.sendMessage(Text.info("Trees per chunk: " + cfg.trees().perChunk()));
                    sender.sendMessage(Text.info("Tree style: " + cfg.trees().style().name()));
                    sender.sendMessage(Text.info("Seed: " + (cfg.seed() == null ? "random" : cfg.seed())));
                }, () -> sender.sendMessage(Text.error("No saved config found.")));
                return true;
            }
            case "create" -> {
                if (!sender.hasPermission("priyme.sbw.create")) {
                    sender.sendMessage(Text.error("You do not have permission."));
                    return true;
                }
                return handleCreate(sender, args);
            }
            case "load" -> {
                if (!sender.hasPermission("priyme.sbw.create")) {
                    sender.sendMessage(Text.error("You do not have permission."));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Text.error("Usage: /sbw load <world>"));
                    return true;
                }
                worldManager.loadWorld(args[1], sender);
                return true;
            }
            case "delete" -> {
                if (!sender.hasPermission("priyme.sbw.delete")) {
                    sender.sendMessage(Text.error("You do not have permission."));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Text.error("Usage: /sbw delete <world>"));
                    return true;
                }
                worldManager.deleteWorld(args[1], sender);
                return true;
            }
            case "preset" -> {
                if (!sender.hasPermission("priyme.sbw.preset")) {
                    sender.sendMessage(Text.error("You do not have permission."));
                    return true;
                }
                return handlePreset(sender, args);
            }
            case "go", "tp", "world" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Text.error("Players only."));
                    return true;
                }
                if (!sender.hasPermission("priyme.sbw.tp")) {
                    sender.sendMessage(Text.error("You do not have permission."));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Text.error("Usage: /sbw go <world>"));
                    return true;
                }
                teleportToWorld(player, args[1]);
                return true;
            }
            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage(Text.ok("Config reloaded."));
                return true;
            }
            default -> {
                sendHelp(sender);
                return true;
            }
        }
    }

    private void teleportToWorld(Player player, String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            player.teleport(world.getSpawnLocation());
            player.sendMessage(Text.ok("Teleported to: " + world.getName()));
            return;
        }

        if (worldManager.isManagedWorld(worldName)) {
            worldManager.loadWorld(worldName, player).ifPresent(w -> {
                player.teleport(w.getSpawnLocation());
                player.sendMessage(Text.ok("Teleported to: " + w.getName()));
            });
            return;
        }

        player.sendMessage(Text.error("World not loaded and no saved plugin config found: " + worldName));
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Text.error("Usage: /sbw create <world> <biome|preset> <flat|noise|presetName> [treesPerChunk|on|off] [seed]"));
            sender.sendMessage(Text.info("Example: /sbw create test minecraft:plains flat 0"));
            sender.sendMessage(Text.info("Example: /sbw create test minecraft:plains noise 8 12345"));
            sender.sendMessage(Text.info("Example: /sbw create test preset myPreset 98765"));
            return true;
        }

        String worldName = args[1];

        if (args[2].equalsIgnoreCase("preset")) {
            if (args.length < 4) {
                sender.sendMessage(Text.error("Usage: /sbw create <world> preset <presetName>"));
                return true;
            }
            String presetName = args[3];
            SeedParse seedParse = parseSeedArg(args.length >= 5 ? args[4] : null);
            if (seedParse.invalid()) {
                sender.sendMessage(Text.error("Invalid seed. Use a number or 'random'."));
                return true;
            }
            Long seed = seedParse.seed();
            Optional<WorldConfig> preset = presetStore.loadPreset(presetName);
            if (preset.isEmpty()) {
                sender.sendMessage(Text.error("Preset not found."));
                return true;
            }

            WorldConfig p = preset.get();
            WorldConfig cfg = new WorldConfig(worldName, p.biome(), p.mode(), p.flat(), p.noise(), p.trees(), seed != null ? seed : p.seed());
            worldManager.createWorld(cfg, sender).ifPresent(w -> {
                if (sender instanceof Player player && player.hasPermission("priyme.sbw.tp")) {
                    player.teleport(w.getSpawnLocation());
                }
            });
            return true;
        }

        Optional<Biome> biome = biomeSelector.parseBiome(args[2]);
        if (biome.isEmpty()) {
            sender.sendMessage(Text.error("Unknown biome."));
            return true;
        }
        if (!biomeSelector.isAllowed(biome.get())) {
            sender.sendMessage(Text.error("This biome is not allowed by the whitelist."));
            return true;
        }

        Optional<GenerationMode> mode = GenerationMode.parse(args[3]);
        if (mode.isEmpty()) {
            sender.sendMessage(Text.error("Unknown mode. Use flat or noise."));
            return true;
        }

        TreesSettings trees = parseTreesArg(args.length >= 5 ? args[4] : null, plugin.defaultTreesSettings());
        SeedParse seedParse = parseSeedArg(args.length >= 6 ? args[5] : null);
        if (seedParse.invalid()) {
            sender.sendMessage(Text.error("Invalid seed. Use a number or 'random'."));
            return true;
        }
        Long seed = seedParse.seed();

        WorldConfig cfg = new WorldConfig(
                worldName,
                biome.get(),
                mode.get(),
                plugin.defaultFlatSettings(),
                plugin.defaultNoiseSettings(),
                trees,
                seed
        );

        worldManager.createWorld(cfg, sender).ifPresent(w -> {
            if (sender instanceof Player player && player.hasPermission("priyme.sbw.tp")) {
                player.teleport(w.getSpawnLocation());
            }
        });
        return true;
    }

    private TreesSettings parseTreesArg(String raw, TreesSettings fallback) {
        if (raw == null || raw.isBlank()) return fallback;

        String s = raw.trim().toLowerCase(Locale.ROOT);

        if (s.equals("off") || s.equals("false") || s.equals("none")) {
            return TreesSettings.fromCount(0, fallback.style());
        }

        if (s.equals("on") || s.equals("true") || s.equals("yes")) {
            int n = Math.max(1, fallback.perChunk());
            return TreesSettings.fromCount(n, fallback.style());
        }

        Integer parsed = tryParseInt(s);
        if (parsed == null) return fallback;

        return TreesSettings.fromCount(parsed, fallback.style());
    }

    private SeedParse parseSeedArg(String raw) {
        if (raw == null || raw.isBlank()) return new SeedParse(null, false);
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.equals("random") || s.equals("none") || s.equals("clear")) return new SeedParse(null, false);
        try {
            return new SeedParse(Long.parseLong(s), false);
        } catch (Exception ignored) {
            return new SeedParse(null, true);
        }
    }

    private Integer tryParseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private record SeedParse(Long seed, boolean invalid) {
    }

    private boolean handlePreset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Text.error("Usage: /sbw preset <list|save|savefrom|load|delete>"));
            return true;
        }

        String action = args[1].toLowerCase(Locale.ROOT);

        switch (action) {
            case "list" -> {
                List<String> names = presetStore.listPresetNames();
                if (names.isEmpty()) {
                    sender.sendMessage(Text.info("No presets."));
                    return true;
                }
                sender.sendMessage(Text.info("Presets: " + String.join(", ", names)));
                return true;
            }
            case "save" -> {
                if (args.length < 5) {
                    sender.sendMessage(Text.error("Usage: /sbw preset save <presetName> <biome> <flat|noise> [treesPerChunk|on|off] [seed]"));
                    return true;
                }
                String presetName = args[2];
                Optional<Biome> biome = biomeSelector.parseBiome(args[3]);
                Optional<GenerationMode> mode = GenerationMode.parse(args[4]);

                if (biome.isEmpty() || mode.isEmpty()) {
                    sender.sendMessage(Text.error("Invalid values."));
                    return true;
                }
                if (!biomeSelector.isAllowed(biome.get())) {
                    sender.sendMessage(Text.error("This biome is not allowed by the whitelist."));
                    return true;
                }

                TreesSettings trees = parseTreesArg(args.length >= 6 ? args[5] : null, plugin.defaultTreesSettings());
                SeedParse seedParse = parseSeedArg(args.length >= 7 ? args[6] : null);
                if (seedParse.invalid()) {
                    sender.sendMessage(Text.error("Invalid seed. Use a number or 'random'."));
                    return true;
                }
                Long seed = seedParse.seed();

                WorldConfig cfg = new WorldConfig(
                        "PRESET",
                        biome.get(),
                        mode.get(),
                        plugin.defaultFlatSettings(),
                        plugin.defaultNoiseSettings(),
                        trees,
                        seed
                );

                try {
                    presetStore.savePreset(presetName, cfg);
                    sender.sendMessage(Text.ok("Preset saved: " + presetName));
                } catch (IOException ex) {
                    sender.sendMessage(Text.error("Preset could not be saved."));
                }
                return true;
            }
            case "savefrom" -> {
                if (args.length < 4) {
                    sender.sendMessage(Text.error("Usage: /sbw preset savefrom <presetName> <world>"));
                    return true;
                }
                String presetName = args[2];
                String world = args[3];
                Optional<WorldConfig> cfg = worldConfigStore.load(world);
                if (cfg.isEmpty()) {
                    sender.sendMessage(Text.error("No saved config found."));
                    return true;
                }
                try {
                    presetStore.savePreset(presetName, cfg.get());
                    sender.sendMessage(Text.ok("Preset saved: " + presetName));
                } catch (IOException ex) {
                    sender.sendMessage(Text.error("Preset could not be saved."));
                }
                return true;
            }
            case "load" -> {
                if (args.length < 4) {
                    sender.sendMessage(Text.error("Usage: /sbw preset load <presetName> <worldName>"));
                    return true;
                }
                String presetName = args[2];
                String worldName = args[3];

                Optional<WorldConfig> preset = presetStore.loadPreset(presetName);
                if (preset.isEmpty()) {
                    sender.sendMessage(Text.error("Preset not found."));
                    return true;
                }
                WorldConfig p = preset.get();
                WorldConfig cfg = new WorldConfig(worldName, p.biome(), p.mode(), p.flat(), p.noise(), p.trees(), p.seed());
                worldManager.createWorld(cfg, sender).ifPresent(w -> {
                    if (sender instanceof Player player && player.hasPermission("priyme.sbw.tp")) {
                        player.teleport(w.getSpawnLocation());
                    }
                });
                return true;
            }
            case "delete" -> {
                if (args.length < 3) {
                    sender.sendMessage(Text.error("Usage: /sbw preset delete <presetName>"));
                    return true;
                }
                try {
                    boolean ok = presetStore.deletePreset(args[2]);
                    sender.sendMessage(ok ? Text.ok("Preset deleted.") : Text.error("Preset not found."));
                } catch (IOException ex) {
                    sender.sendMessage(Text.error("Preset could not be deleted."));
                }
                return true;
            }
            default -> {
                sender.sendMessage(Text.error("Unknown. Usage: /sbw preset <list|save|savefrom|load|delete>"));
                return true;
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Text.info("/sbw gui"));
        sender.sendMessage(Text.info("/sbw create <world> <biome> <flat|noise> [treesPerChunk|on|off] [seed]"));
        sender.sendMessage(Text.info("/sbw create <world> preset <presetName> [seed]"));
        sender.sendMessage(Text.info("/sbw load <world>"));
        sender.sendMessage(Text.info("/sbw go <world>"));
        sender.sendMessage(Text.info("/sbw info <world>"));
        sender.sendMessage(Text.info("/sbw list"));
        sender.sendMessage(Text.info("/sbw delete <world>"));
        sender.sendMessage(Text.info("/sbw preset list"));
        sender.sendMessage(Text.info("/sbw preset save <presetName> <biome> <flat|noise> [treesPerChunk|on|off] [seed]"));
        sender.sendMessage(Text.info("/sbw preset savefrom <presetName> <world>"));
        sender.sendMessage(Text.info("/sbw preset load <presetName> <worldName>"));
        sender.sendMessage(Text.info("/sbw preset delete <presetName>"));
        sender.sendMessage(Text.info("/sbw reload"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) return List.of();

        if (args.length == 1) {
            return filterPrefix(List.of("gui", "create", "load", "list", "info", "delete", "preset", "go", "reload"), args[0]);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "create" -> {
                if (args.length == 3) {
                    List<String> biomes = new ArrayList<>(biomeSelector.allowedBiomeIdsSorted());
                    biomes.add("preset");
                    return filterPrefix(biomes, args[2]);
                }
                if (args.length == 4) {
                    if (args[2].equalsIgnoreCase("preset")) {
                        return filterPrefix(presetStore.listPresetNames(), args[3]);
                    }
                    return filterPrefix(List.of("flat", "noise"), args[3]);
                }
                if (args.length == 5) {
                    if (args[2].equalsIgnoreCase("preset")) return filterPrefix(List.of("random"), args[4]);
                    return filterPrefix(List.of("0", "1", "2", "4", "8", "16", "on", "off"), args[4]);
                }
                if (args.length == 6) {
                    if (args[2].equalsIgnoreCase("preset")) return List.of();
                    return filterPrefix(List.of("random"), args[5]);
                }
                return List.of();
            }
            case "load", "info", "delete", "go", "tp", "world" -> {
                if (args.length == 2) {
                    return filterPrefix(worldNameSuggestions(), args[1]);
                }
                return List.of();
            }
            case "preset" -> {
                if (args.length == 2) {
                    return filterPrefix(List.of("list", "save", "savefrom", "load", "delete"), args[1]);
                }
                if (args.length == 3) {
                    String action = args[1].toLowerCase(Locale.ROOT);
                    if (action.equals("load") || action.equals("delete")) {
                        return filterPrefix(presetStore.listPresetNames(), args[2]);
                    }
                    return List.of();
                }
                if (args.length == 4) {
                    String action = args[1].toLowerCase(Locale.ROOT);
                    if (action.equals("save")) {
                        return filterPrefix(biomeSelector.allowedBiomeIdsSorted(), args[3]);
                    }
                    if (action.equals("savefrom")) {
                        return filterPrefix(worldNameSuggestions(), args[3]);
                    }
                    if (action.equals("load")) {
                        return List.of();
                    }
                }
                if (args.length == 5) {
                    String action = args[1].toLowerCase(Locale.ROOT);
                    if (action.equals("save")) {
                        return filterPrefix(List.of("flat", "noise"), args[4]);
                    }
                }
                if (args.length == 6) {
                    String action = args[1].toLowerCase(Locale.ROOT);
                    if (action.equals("save")) {
                        return filterPrefix(List.of("0", "1", "2", "4", "8", "16", "on", "off"), args[5]);
                    }
                }
                if (args.length == 7) {
                    String action = args[1].toLowerCase(Locale.ROOT);
                    if (action.equals("save")) {
                        return filterPrefix(List.of("random"), args[6]);
                    }
                }
                return List.of();
            }
            default -> {
                return List.of();
            }
        }
    }

    private List<String> worldNameSuggestions() {
        Set<String> set = new HashSet<>(worldManager.managedWorldNames());
        for (World w : Bukkit.getWorlds()) {
            set.add(w.getName());
        }
        List<String> list = new ArrayList<>(set);
        list.sort(String::compareToIgnoreCase);
        return list;
    }

    private List<String> filterPrefix(List<String> options, String prefix) {
        if (prefix == null || prefix.isEmpty()) return options;
        String p = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(p)).collect(Collectors.toList());
    }
}
