package de.priyme.singlebiome;

import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.Locale;
import java.util.Random;

public final class SimpleTreePopulator extends BlockPopulator {

    private final TreesSettings trees;
    private final TreePalette palette;

    public SimpleTreePopulator(TreesSettings trees, Biome biome) {
        this.trees = trees;
        this.palette = TreePalette.fromBiome(biome);
    }

    @Override
    public void populate(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, LimitedRegion region) {
        if (!trees.enabled() || trees.perChunk() <= 0) return;

        int target = trees.perChunk();
        int attempts = Math.min(Math.max(target * 3, target), 256);

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int buffer = region.getBuffer();
        int margin = buffer <= 0 ? 2 : 0;
        int range = 16 - (margin * 2);
        if (range <= 0) return;

        int placed = 0;
        for (int i = 0; i < attempts && placed < target; i++) {
            int x = baseX + margin + random.nextInt(range);
            int z = baseZ + margin + random.nextInt(range);
            int y = region.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE);
            if (!region.isInRegion(x, y, z)) continue;

            Material ground = region.getType(x, y, z);
            if (!isValidGround(ground)) continue;

            if (placeSimpleTree(region, random, worldInfo.getMaxHeight(), x, y, z)) {
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
        int margin = 2;
        int range = 16 - (margin * 2);
        if (range <= 0) return;

        int placed = 0;
        for (int i = 0; i < attempts && placed < target; i++) {
            int x = baseX + margin + random.nextInt(range);
            int z = baseZ + margin + random.nextInt(range);
            int y = world.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE);
            Material ground = world.getBlockAt(x, y, z).getType();
            if (!isValidGround(ground)) continue;

            if (placeSimpleTree(world, random, world.getMaxHeight(), x, y, z)) {
                placed++;
            }
        }
    }

    private boolean placeSimpleTree(LimitedRegion region, Random random, int maxY, int x, int groundY, int z) {
        int trunkHeight = 4 + random.nextInt(3);
        int topY = groundY + trunkHeight;
        if (topY >= maxY - 2) return false;

        for (int y = groundY + 1; y <= topY; y++) {
            if (!region.isInRegion(x, y, z)) return false;
            if (!canReplace(region.getType(x, y, z), false)) return false;
            region.setType(x, y, z, palette.log());
        }

        int canopyBase = topY - 2;
        for (int y = canopyBase; y <= topY; y++) {
            int r = (y == topY) ? 1 : 2;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    int lx = x + dx;
                    int lz = z + dz;
                    if (!region.isInRegion(lx, y, lz)) continue;
                    if (lx == x && lz == z) continue;
                    if (Math.abs(dx) == r && Math.abs(dz) == r && y != topY) continue;
                    Material existing = region.getType(lx, y, lz);
                    if (!canReplace(existing, true)) continue;
                    region.setType(lx, y, lz, palette.leaves());
                }
            }
        }

        int capY = topY + 1;
        if (region.isInRegion(x, capY, z) && canReplace(region.getType(x, capY, z), true)) {
            region.setType(x, capY, z, palette.leaves());
        }
        return true;
    }

    private boolean placeSimpleTree(World world, Random random, int maxY, int x, int groundY, int z) {
        int trunkHeight = 4 + random.nextInt(3);
        int topY = groundY + trunkHeight;
        if (topY >= maxY - 2) return false;

        for (int y = groundY + 1; y <= topY; y++) {
            Material existing = world.getBlockAt(x, y, z).getType();
            if (!canReplace(existing, false)) return false;
            world.getBlockAt(x, y, z).setType(palette.log(), false);
        }

        int canopyBase = topY - 2;
        for (int y = canopyBase; y <= topY; y++) {
            int r = (y == topY) ? 1 : 2;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    int lx = x + dx;
                    int lz = z + dz;
                    if (lx == x && lz == z) continue;
                    if (Math.abs(dx) == r && Math.abs(dz) == r && y != topY) continue;
                    Material existing = world.getBlockAt(lx, y, lz).getType();
                    if (!canReplace(existing, true)) continue;
                    world.getBlockAt(lx, y, lz).setType(palette.leaves(), false);
                }
            }
        }

        Material capExisting = world.getBlockAt(x, topY + 1, z).getType();
        if (canReplace(capExisting, true)) {
            world.getBlockAt(x, topY + 1, z).setType(palette.leaves(), false);
        }
        return true;
    }

    private boolean isValidGround(Material material) {
        if (material.isAir()) return false;
        if (material == Material.WATER || material == Material.LAVA) return false;
        if (!material.isSolid()) return false;
        if (Tag.LEAVES.isTagged(material)) return false;
        if (Tag.LOGS.isTagged(material)) return false;
        return material != Material.BEDROCK;
    }

    private boolean canReplace(Material material, boolean allowLeaves) {
        if (material.isAir()) return true;
        if (allowLeaves && Tag.LEAVES.isTagged(material)) return true;
        if (material == Material.WATER || material == Material.LAVA) return false;
        return !material.isSolid();
    }

    private record TreePalette(Material log, Material leaves) {

        static TreePalette fromBiome(Biome biome) {
            String n = BiomeKeys.id(biome).toLowerCase(Locale.ROOT);

            if (n.contains("crimson")) return new TreePalette(Material.CRIMSON_STEM, Material.NETHER_WART_BLOCK);
            if (n.contains("warped")) return new TreePalette(Material.WARPED_STEM, Material.WARPED_WART_BLOCK);

            if (n.contains("cherry")) return new TreePalette(Material.CHERRY_LOG, Material.CHERRY_LEAVES);
            if (n.contains("mangrove")) return new TreePalette(Material.MANGROVE_LOG, Material.MANGROVE_LEAVES);
            if (n.contains("dark_forest")) return new TreePalette(Material.DARK_OAK_LOG, Material.DARK_OAK_LEAVES);

            if (n.contains("birch")) return new TreePalette(Material.BIRCH_LOG, Material.BIRCH_LEAVES);
            if (n.contains("jungle")) return new TreePalette(Material.JUNGLE_LOG, Material.JUNGLE_LEAVES);
            if (n.contains("savanna") || n.contains("acacia")) return new TreePalette(Material.ACACIA_LOG, Material.ACACIA_LEAVES);
            if (n.contains("taiga") || n.contains("spruce") || n.contains("snowy_taiga") || n.contains("old_growth_pine") || n.contains("old_growth_spruce")) {
                return new TreePalette(Material.SPRUCE_LOG, Material.SPRUCE_LEAVES);
            }

            return new TreePalette(Material.OAK_LOG, Material.OAK_LEAVES);
        }
    }
}
