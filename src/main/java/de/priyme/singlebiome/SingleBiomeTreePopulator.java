package de.priyme.singlebiome;

import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.Locale;
import java.util.Random;

public final class SingleBiomeTreePopulator extends BlockPopulator {

    private final TreesSettings trees;
    private final TreeType[] treeTypes;

    public SingleBiomeTreePopulator(TreesSettings trees, Biome biome) {
        this.trees = trees;
        this.treeTypes = treeTypesForBiome(biome);
    }

    @Override
    public void populate(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, LimitedRegion region) {
        if (!trees.enabled() || trees.perChunk() <= 0) return;

        int target = trees.perChunk();
        int attempts = Math.min(Math.max(target * 3, target), 256);

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int buffer = Math.max(0, region.getBuffer());
        int maxY = worldInfo.getMaxHeight();

        int placed = 0;
        for (int i = 0; i < attempts && placed < target; i++) {
            TreeType type = resolveTreeTypeForSpace(pickTreeType(random), buffer);
            int margin = treeMargin(type, buffer);
            int range = 16 - (margin * 2);
            if (range <= 0) continue;

            int x = baseX + margin + random.nextInt(range);
            int z = baseZ + margin + random.nextInt(range);
            int y = region.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE);
            if (!region.isInRegion(x, y, z)) continue;
            if (y + treeHeight(type) >= maxY - 2) continue;

            if (requires2x2(type)) {
                if (!isValidGround2x2(region, x, z, y)) continue;
            } else {
                Material ground = region.getType(x, y, z);
                if (!isValidGround(ground)) continue;
            }

            Location loc = new Location(region.getWorld(), x, y + 1, z);
            if (region.generateTree(loc, random, type)) {
                placed++;
            }
        }
    }

    @Override
    public void populate(World world, Random random, Chunk chunk) {
        if (!trees.enabled() || trees.perChunk() <= 0) return;

        int target = trees.perChunk();
        int attempts = Math.min(Math.max(target * 3, target), 256);
        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;
        int maxY = world.getMaxHeight();

        int placed = 0;
        for (int i = 0; i < attempts && placed < target; i++) {
            TreeType type = resolveTreeTypeForSpace(pickTreeType(random), 0);
            int margin = treeMargin(type, 0);
            int range = 16 - (margin * 2);
            if (range <= 0) continue;

            int x = baseX + margin + random.nextInt(range);
            int z = baseZ + margin + random.nextInt(range);
            int y = world.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE);
            if (y + treeHeight(type) >= maxY - 2) continue;

            if (requires2x2(type)) {
                if (!isValidGround2x2(world, x, z, y)) continue;
            } else {
                Material ground = world.getBlockAt(x, y, z).getType();
                if (!isValidGround(ground)) continue;
            }

            Location loc = new Location(world, x, y + 1, z);
            if (world.generateTree(loc, random, type)) {
                placed++;
            }
        }
    }

    private boolean isValidGround(Material material) {
        if (material.isAir()) return false;
        if (material == Material.WATER || material == Material.LAVA) return false;
        if (!material.isSolid()) return false;
        if (Tag.LEAVES.isTagged(material)) return false;
        if (Tag.LOGS.isTagged(material)) return false;
        return material != Material.BEDROCK;
    }

    private TreeType pickTreeType(Random random) {
        if (treeTypes.length == 1) return treeTypes[0];
        return treeTypes[random.nextInt(treeTypes.length)];
    }

    private TreeType resolveTreeTypeForSpace(TreeType type, int buffer) {
        TreeType current = type;
        for (int i = 0; i < 3; i++) {
            int margin = treeMargin(current, buffer);
            int range = 16 - (margin * 2);
            if (range > 0) return current;
            TreeType fallback = fallbackTreeType(current);
            if (fallback == current) return current;
            current = fallback;
        }
        return current;
    }

    private TreeType fallbackTreeType(TreeType type) {
        return switch (type) {
            case MEGA_PINE, MEGA_REDWOOD -> TreeType.TALL_REDWOOD;
            case TALL_REDWOOD -> TreeType.REDWOOD;
            case JUNGLE, COCOA_TREE -> TreeType.SMALL_JUNGLE;
            case DARK_OAK, MANGROVE, TALL_MANGROVE, CHERRY, PALE_OAK, PALE_OAK_CREAKING -> TreeType.TREE;
            case CRIMSON_FUNGUS -> TreeType.WARPED_FUNGUS;
            case WARPED_FUNGUS -> TreeType.TREE;
            default -> type;
        };
    }

    private int treeRadius(TreeType type) {
        return switch (type) {
            case BIG_TREE -> 5;
            case DARK_OAK -> 4;
            case JUNGLE, COCOA_TREE -> 5;
            case SMALL_JUNGLE, JUNGLE_BUSH -> 3;
            case MEGA_REDWOOD, MEGA_PINE -> 6;
            case TALL_REDWOOD, REDWOOD -> 4;
            case BIRCH, TALL_BIRCH -> 3;
            case ACACIA, SWAMP -> 4;
            case MANGROVE, TALL_MANGROVE -> 5;
            case CHERRY, PALE_OAK, PALE_OAK_CREAKING -> 5;
            case CRIMSON_FUNGUS, WARPED_FUNGUS -> 5;
            case AZALEA -> 3;
            case RED_MUSHROOM, BROWN_MUSHROOM -> 4;
            case CHORUS_PLANT -> 3;
            default -> 3;
        };
    }

    private int treeMargin(TreeType type, int buffer) {
        int radius = treeRadius(type);
        return Math.max(1, radius + 2 - buffer);
    }

    private int treeHeight(TreeType type) {
        return switch (type) {
            case BIG_TREE -> 10;
            case DARK_OAK -> 9;
            case JUNGLE, COCOA_TREE -> 16;
            case SMALL_JUNGLE, JUNGLE_BUSH -> 8;
            case MEGA_REDWOOD, MEGA_PINE -> 20;
            case TALL_REDWOOD, REDWOOD -> 14;
            case BIRCH -> 8;
            case TALL_BIRCH -> 12;
            case ACACIA, SWAMP -> 10;
            case MANGROVE, TALL_MANGROVE -> 14;
            case CHERRY, PALE_OAK, PALE_OAK_CREAKING -> 12;
            case CRIMSON_FUNGUS, WARPED_FUNGUS -> 10;
            case AZALEA -> 7;
            case RED_MUSHROOM, BROWN_MUSHROOM -> 7;
            case CHORUS_PLANT -> 8;
            default -> 9;
        };
    }

    private boolean requires2x2(TreeType type) {
        return switch (type) {
            case JUNGLE, COCOA_TREE, DARK_OAK, MEGA_REDWOOD, MEGA_PINE, MANGROVE, TALL_MANGROVE -> true;
            default -> false;
        };
    }

    private boolean isValidGround2x2(LimitedRegion region, int x, int z, int y) {
        int y1 = region.getHighestBlockYAt(x + 1, z, HeightMap.WORLD_SURFACE);
        int y2 = region.getHighestBlockYAt(x, z + 1, HeightMap.WORLD_SURFACE);
        int y3 = region.getHighestBlockYAt(x + 1, z + 1, HeightMap.WORLD_SURFACE);
        if (y1 != y || y2 != y || y3 != y) return false;
        return isValidGround(region.getType(x, y, z))
                && isValidGround(region.getType(x + 1, y, z))
                && isValidGround(region.getType(x, y, z + 1))
                && isValidGround(region.getType(x + 1, y, z + 1));
    }

    private boolean isValidGround2x2(World world, int x, int z, int y) {
        int y1 = world.getHighestBlockYAt(x + 1, z, HeightMap.WORLD_SURFACE);
        int y2 = world.getHighestBlockYAt(x, z + 1, HeightMap.WORLD_SURFACE);
        int y3 = world.getHighestBlockYAt(x + 1, z + 1, HeightMap.WORLD_SURFACE);
        if (y1 != y || y2 != y || y3 != y) return false;
        return isValidGround(world.getBlockAt(x, y, z).getType())
                && isValidGround(world.getBlockAt(x + 1, y, z).getType())
                && isValidGround(world.getBlockAt(x, y, z + 1).getType())
                && isValidGround(world.getBlockAt(x + 1, y, z + 1).getType());
    }

    private static TreeType[] treeTypesForBiome(Biome biome) {
        String n = BiomeKeys.id(biome).toLowerCase(Locale.ROOT);

        if (n.contains("crimson")) return new TreeType[]{TreeType.CRIMSON_FUNGUS};
        if (n.contains("warped")) return new TreeType[]{TreeType.WARPED_FUNGUS};

        if (n.contains("cherry")) return new TreeType[]{TreeType.CHERRY};
        if (n.contains("mangrove")) return new TreeType[]{TreeType.MANGROVE, TreeType.TALL_MANGROVE};
        if (n.contains("dark_forest")) return new TreeType[]{TreeType.DARK_OAK};
        if (n.contains("swamp")) return new TreeType[]{TreeType.SWAMP};

        if (n.contains("birch")) return new TreeType[]{TreeType.BIRCH, TreeType.TALL_BIRCH};
        if (n.contains("jungle")) return new TreeType[]{TreeType.JUNGLE, TreeType.SMALL_JUNGLE, TreeType.COCOA_TREE, TreeType.JUNGLE_BUSH};
        if (n.contains("savanna") || n.contains("acacia")) return new TreeType[]{TreeType.ACACIA};

        if (n.contains("old_growth_pine")) return new TreeType[]{TreeType.MEGA_PINE, TreeType.REDWOOD, TreeType.TALL_REDWOOD};
        if (n.contains("old_growth_spruce")) return new TreeType[]{TreeType.MEGA_REDWOOD, TreeType.REDWOOD, TreeType.TALL_REDWOOD};
        if (n.contains("taiga") || n.contains("spruce")) return new TreeType[]{TreeType.REDWOOD, TreeType.TALL_REDWOOD};

        if (n.contains("mushroom")) return new TreeType[]{TreeType.RED_MUSHROOM, TreeType.BROWN_MUSHROOM};
        if (n.contains("pale_garden") || n.contains("pale")) return new TreeType[]{TreeType.PALE_OAK};
        if (n.contains("end")) return new TreeType[]{TreeType.CHORUS_PLANT};

        return new TreeType[]{TreeType.TREE, TreeType.BIG_TREE};
    }
}
