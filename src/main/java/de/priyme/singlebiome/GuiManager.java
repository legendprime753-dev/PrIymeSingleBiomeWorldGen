package de.priyme.singlebiome;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.regex.Pattern;

public final class GuiManager implements Listener {

    private static final int CREATOR_SIZE = 54;
    private static final int LIST_SIZE = 54;

    private final JavaPlugin plugin;
    private final BiomeSelector biomeSelector;
    private final WorldManager worldManager;
    private final WorldConfigStore worldConfigStore;

    private final Map<UUID, GuiSession> sessions = new HashMap<>();
    private final Pattern worldNamePattern = Pattern.compile("^[A-Za-z0-9_-]{3,32}$");

    public GuiManager(JavaPlugin plugin, BiomeSelector biomeSelector, WorldManager worldManager, WorldConfigStore worldConfigStore) {
        this.plugin = plugin;
        this.biomeSelector = biomeSelector;
        this.worldManager = worldManager;
        this.worldConfigStore = worldConfigStore;
    }

    public void openCreator(Player player) {
        GuiSession session = session(player);

        String title = plugin.getConfig().getString("gui.title", "Single Biome World Creator");
        Inventory inv = Bukkit.createInventory(new Holder(GuiType.CREATOR, player.getUniqueId()), CREATOR_SIZE, title);

        inv.setItem(10, item(Material.GRASS_BLOCK, ChatColor.AQUA + "Biome", List.of(
                ChatColor.GRAY + BiomeKeys.id(session.biome()),
                ChatColor.DARK_GRAY + "Click to change"
        )));

        inv.setItem(12, item(Material.COMPASS, ChatColor.AQUA + "Mode", List.of(
                ChatColor.GRAY + session.mode().name(),
                ChatColor.DARK_GRAY + "Click to toggle"
        )));

        inv.setItem(14, item(Material.NAME_TAG, ChatColor.AQUA + "World Name", List.of(
                ChatColor.GRAY + (session.worldName() == null ? "-" : session.worldName()),
                ChatColor.DARK_GRAY + "Click to enter via chat"
        )));

        inv.setItem(16, item(Material.OAK_SAPLING, ChatColor.AQUA + "Trees", List.of(
                ChatColor.GRAY + (session.treesEnabled() ? "Enabled" : "Disabled"),
                ChatColor.GRAY + "Per chunk: " + session.treesPerChunk(),
                ChatColor.DARK_GRAY + "Left click: toggle",
                ChatColor.DARK_GRAY + "Right click: set amount"
        )));

        inv.setItem(18, item(Material.OAK_LOG, ChatColor.AQUA + "Tree Style", List.of(
                ChatColor.GRAY + session.treeStyle().name(),
                ChatColor.DARK_GRAY + "Click to toggle"
        )));

        inv.setItem(20, item(Material.REPEATER, ChatColor.AQUA + "Seed", List.of(
                ChatColor.GRAY + (session.seed() == null ? "Random" : String.valueOf(session.seed())),
                ChatColor.DARK_GRAY + "Click to set via chat"
        )));

        inv.setItem(29, item(Material.ENDER_PEARL, ChatColor.AQUA + "Switch World", List.of(
                ChatColor.GRAY + "Teleport to a world",
                ChatColor.DARK_GRAY + "Click to open the world list"
        )));

        inv.setItem(31, item(Material.LIME_CONCRETE, ChatColor.GREEN + "Create World", List.of(
                ChatColor.GRAY + "Create a world with the current settings"
        )));

        inv.setItem(49, item(Material.BARRIER, ChatColor.RED + "Close", List.of()));

        fillBackground(inv);

        player.openInventory(inv);
    }

    private void openBiomeSelect(Player player) {
        GuiSession session = session(player);

        Inventory inv = Bukkit.createInventory(new Holder(GuiType.BIOME_SELECT, player.getUniqueId()), LIST_SIZE, ChatColor.DARK_GREEN + "Select Biome");

        List<Biome> all = biomeSelector.allowedBiomesSorted();
        String search = session.biomeSearch().trim().toLowerCase(Locale.ROOT);

        List<Biome> filtered = all;
        if (!search.isEmpty()) {
            filtered = all.stream()
                    .filter(b -> BiomeKeys.id(b).toLowerCase(Locale.ROOT).contains(search))
                    .toList();
        }

        int perPage = 45;
        int pages = Math.max(1, (int) Math.ceil(filtered.size() / (double) perPage));
        int page = Math.min(session.biomePage(), pages - 1);
        session.biomePage(page);

        int start = page * perPage;
        int end = Math.min(filtered.size(), start + perPage);

        int slot = 0;
        for (int i = start; i < end; i++) {
            Biome biome = filtered.get(i);
            boolean selected = biome.equals(session.biome());
            Material mat = selected ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
            inv.setItem(slot, item(mat, (selected ? ChatColor.GREEN : ChatColor.WHITE) + BiomeKeys.id(biome), List.of()));
            slot++;
        }

        inv.setItem(45, item(Material.ARROW, ChatColor.YELLOW + "Previous", List.of(ChatColor.GRAY + "Page " + (page + 1) + "/" + pages)));
        inv.setItem(53, item(Material.ARROW, ChatColor.YELLOW + "Next", List.of(ChatColor.GRAY + "Page " + (page + 1) + "/" + pages)));

        inv.setItem(48, item(Material.PAPER, ChatColor.AQUA + "Clear Search", List.of(ChatColor.GRAY + "Current: " + (search.isEmpty() ? "-" : search))));
        inv.setItem(49, item(Material.OAK_SIGN, ChatColor.AQUA + "Search", List.of(ChatColor.GRAY + "Type a search term in chat")));
        inv.setItem(50, item(Material.BARRIER, ChatColor.RED + "Back", List.of()));

        fillBackground(inv);

        player.openInventory(inv);
    }

    private void openWorldSelect(Player player) {
        GuiSession session = session(player);

        Inventory inv = Bukkit.createInventory(new Holder(GuiType.WORLD_SELECT, player.getUniqueId()), LIST_SIZE, ChatColor.DARK_AQUA + "Select World");

        Set<String> names = new HashSet<>(worldManager.managedWorldNames());
        for (World w : Bukkit.getWorlds()) {
            names.add(w.getName());
        }

        List<String> list = new ArrayList<>(names);
        list.sort(String::compareToIgnoreCase);

        int perPage = 45;
        int pages = Math.max(1, (int) Math.ceil(list.size() / (double) perPage));
        int page = Math.min(session.worldPage(), pages - 1);
        session.worldPage(page);

        int start = page * perPage;
        int end = Math.min(list.size(), start + perPage);

        int slot = 0;
        for (int i = start; i < end; i++) {
            String worldName = list.get(i);
            boolean loaded = Bukkit.getWorld(worldName) != null;
            boolean managed = worldManager.isManagedWorld(worldName);

            Material mat = loaded ? Material.ENDER_PEARL : (managed ? Material.CHEST : Material.BARRIER);
            String color = loaded ? ChatColor.GREEN.toString() : (managed ? ChatColor.YELLOW.toString() : ChatColor.RED.toString());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Loaded: " + (loaded ? "Yes" : "No"));
            lore.add(ChatColor.GRAY + "Managed: " + (managed ? "Yes" : "No"));
            lore.add(ChatColor.DARK_GRAY + (loaded ? "Click to teleport" : managed ? "Click to load and teleport" : "Not available"));

            inv.setItem(slot, item(mat, color + worldName, lore));
            slot++;
        }

        inv.setItem(45, item(Material.ARROW, ChatColor.YELLOW + "Previous", List.of(ChatColor.GRAY + "Page " + (page + 1) + "/" + pages)));
        inv.setItem(53, item(Material.ARROW, ChatColor.YELLOW + "Next", List.of(ChatColor.GRAY + "Page " + (page + 1) + "/" + pages)));

        inv.setItem(49, item(Material.BARRIER, ChatColor.RED + "Back", List.of()));

        fillBackground(inv);

        player.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof Holder h)) return;

        if (!player.getUniqueId().equals(h.playerId())) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;

        GuiSession session = session(player);

        if (h.type() == GuiType.CREATOR) {
            handleCreatorClick(player, session, event);
            return;
        }

        if (h.type() == GuiType.BIOME_SELECT) {
            handleBiomeClick(player, session, event.getRawSlot(), event.getCurrentItem());
            return;
        }

        if (h.type() == GuiType.WORLD_SELECT) {
            handleWorldClick(player, session, event.getRawSlot(), event.getCurrentItem());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof Holder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof Holder h)) return;
        GuiSession session = sessions.get(player.getUniqueId());
        if (session != null && session.inputState() == GuiSession.InputState.NONE && h.type() == GuiType.CREATOR) {
            sessions.put(player.getUniqueId(), session);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        GuiSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        if (session.inputState() == GuiSession.InputState.NONE) return;

        event.setCancelled(true);

        String msg = event.getMessage().trim();
        Bukkit.getScheduler().runTask(plugin, () -> handleChatInput(player, session, msg));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    private void handleCreatorClick(Player player, GuiSession session, InventoryClickEvent event) {
        int rawSlot = event.getRawSlot();

        switch (rawSlot) {
            case 10 -> openBiomeSelect(player);
            case 12 -> {
                session.mode(session.mode() == GenerationMode.FLAT ? GenerationMode.NOISE : GenerationMode.FLAT);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                openCreator(player);
            }
            case 14 -> {
                session.inputState(GuiSession.InputState.WORLD_NAME);
                player.closeInventory();
                player.sendMessage(Text.info("Enter the world name in chat (3-32 chars, A-Z 0-9 _ -). Type 'cancel' to abort."));
            }
            case 16 -> {
                if (event.isRightClick()) {
                    session.inputState(GuiSession.InputState.TREES_PER_CHUNK);
                    player.closeInventory();
                    player.sendMessage(Text.info("Enter trees per chunk (0-64) in chat. 0 disables trees. Type 'cancel' to abort."));
                } else {
                    session.treesEnabled(!session.treesEnabled());
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    openCreator(player);
                }
            }
            case 18 -> {
                TreeStyle current = session.treeStyle();
                TreeStyle next = current == TreeStyle.SIMPLE ? TreeStyle.VANILLA : TreeStyle.SIMPLE;
                session.treeStyle(next);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                openCreator(player);
            }
            case 20 -> {
                session.inputState(GuiSession.InputState.SEED);
                player.closeInventory();
                player.sendMessage(Text.info("Enter a numeric seed in chat. Type 'random' to clear or 'cancel' to abort."));
            }
            case 29 -> openWorldSelect(player);
            case 31 -> {
                if (!player.hasPermission("priyme.sbw.create")) {
                    player.sendMessage(Text.error("You do not have permission."));
                    return;
                }
                if (session.worldName() == null || session.worldName().isBlank()) {
                    player.sendMessage(Text.error("Please set a world name first."));
                    return;
                }
                if (!worldNamePattern.matcher(session.worldName()).matches()) {
                    player.sendMessage(Text.error("Invalid world name."));
                    return;
                }

                SingleBiomeWorldGenPlugin pl = (SingleBiomeWorldGenPlugin) plugin;

                WorldConfig cfg = new WorldConfig(
                        session.worldName(),
                        session.biome(),
                        session.mode(),
                        pl.defaultFlatSettings(),
                        pl.defaultNoiseSettings(),
                        session.trees(),
                        session.seed()
                );

                worldManager.createWorld(cfg, player).ifPresent(w -> player.teleport(w.getSpawnLocation()));
            }
            case 49 -> player.closeInventory();
            default -> {
            }
        }
    }

    private void handleBiomeClick(Player player, GuiSession session, int rawSlot, ItemStack clicked) {
        if (rawSlot >= 0 && rawSlot < 45) {
            String id = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            biomeSelector.parseBiome(id).ifPresent(b -> {
                session.biome(b);
                openCreator(player);
            });
            return;
        }

        switch (rawSlot) {
            case 45 -> {
                session.biomePage(session.biomePage() - 1);
                openBiomeSelect(player);
            }
            case 53 -> {
                session.biomePage(session.biomePage() + 1);
                openBiomeSelect(player);
            }
            case 48 -> {
                session.biomeSearch("");
                session.biomePage(0);
                openBiomeSelect(player);
            }
            case 49 -> {
                session.inputState(GuiSession.InputState.BIOME_SEARCH);
                player.closeInventory();
                player.sendMessage(Text.info("Enter a search term in chat. Type 'clear' to reset or 'cancel' to abort."));
            }
            case 50 -> openCreator(player);
            default -> {
            }
        }
    }

    private void handleWorldClick(Player player, GuiSession session, int rawSlot, ItemStack clicked) {
        if (rawSlot >= 0 && rawSlot < 45) {
            String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            World world = Bukkit.getWorld(name);
            if (world != null) {
                player.teleport(world.getSpawnLocation());
                player.closeInventory();
                return;
            }

            if (!worldManager.isManagedWorld(name)) {
                player.sendMessage(Text.error("World is not loaded and has no saved plugin config."));
                return;
            }

            worldManager.loadWorld(name, player).ifPresent(w -> {
                player.teleport(w.getSpawnLocation());
                player.closeInventory();
            });
            return;
        }

        switch (rawSlot) {
            case 45 -> {
                session.worldPage(session.worldPage() - 1);
                openWorldSelect(player);
            }
            case 53 -> {
                session.worldPage(session.worldPage() + 1);
                openWorldSelect(player);
            }
            case 49 -> openCreator(player);
            default -> {
            }
        }
    }

    private void handleChatInput(Player player, GuiSession session, String msg) {
        if (msg.equalsIgnoreCase("cancel")) {
            session.inputState(GuiSession.InputState.NONE);
            player.sendMessage(Text.info("Cancelled."));
            openCreator(player);
            return;
        }

        if (session.inputState() == GuiSession.InputState.WORLD_NAME) {
            if (msg.isBlank()) {
                player.sendMessage(Text.error("Name must not be empty."));
                return;
            }
            if (!worldNamePattern.matcher(msg).matches()) {
                player.sendMessage(Text.error("Invalid world name."));
                return;
            }
            session.worldName(msg);
            session.inputState(GuiSession.InputState.NONE);
            openCreator(player);
            return;
        }

        if (session.inputState() == GuiSession.InputState.BIOME_SEARCH) {
            if (msg.equalsIgnoreCase("clear")) {
                session.biomeSearch("");
            } else {
                session.biomeSearch(msg);
            }
            session.biomePage(0);
            session.inputState(GuiSession.InputState.NONE);
            openBiomeSelect(player);
            return;
        }

        if (session.inputState() == GuiSession.InputState.TREES_PER_CHUNK) {
            Integer parsed = tryParseInt(msg);
            if (parsed == null) {
                player.sendMessage(Text.error("Please enter a number between 0 and 64."));
                return;
            }
            session.treesPerChunk(parsed);
            session.inputState(GuiSession.InputState.NONE);
            openCreator(player);
            return;
        }

        if (session.inputState() == GuiSession.InputState.SEED) {
            if (msg.equalsIgnoreCase("random") || msg.equalsIgnoreCase("clear") || msg.equalsIgnoreCase("none")) {
                session.seed(null);
                session.inputState(GuiSession.InputState.NONE);
                openCreator(player);
                return;
            }
            Long parsed = tryParseLong(msg);
            if (parsed == null) {
                player.sendMessage(Text.error("Please enter a valid 64-bit number, or 'random'."));
                return;
            }
            session.seed(parsed);
            session.inputState(GuiSession.InputState.NONE);
            openCreator(player);
        }
    }

    private Integer tryParseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long tryParseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private GuiSession session(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), uuid -> {
            String defaultBiome = plugin.getConfig().getString("defaults.biome", "minecraft:plains");
            Biome biome = biomeSelector.parseBiome(defaultBiome).orElse(Biome.PLAINS);

            if (!biomeSelector.isAllowed(biome)) {
                List<Biome> allowed = biomeSelector.allowedBiomesSorted();
                biome = allowed.isEmpty() ? Biome.PLAINS : allowed.get(0);
            }

            String name = "sbw_" + player.getName().toLowerCase(Locale.ROOT);
            SingleBiomeWorldGenPlugin pl = (SingleBiomeWorldGenPlugin) plugin;
            GenerationMode mode = pl.defaultMode();
            TreesSettings trees = pl.defaultTreesSettings();
            return new GuiSession(name, biome, mode, trees, null);
        });
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private void fillBackground(Inventory inv) {
        ItemStack glass = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }
    }

    private record Holder(GuiType type, UUID playerId) implements InventoryHolder {

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
