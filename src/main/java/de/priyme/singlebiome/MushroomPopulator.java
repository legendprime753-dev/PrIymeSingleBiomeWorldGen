package de.priyme.singlebiome;

import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

public final class MushroomPopulator extends BlockPopulator {

    private static final Material[] MUSHROOMS = new Material[]{Material.BROWN_MUSHROOM, Material.RED_MUSHROOM};
    private final TreesSettings trees;

    public MushroomPopulator(TreesSettings trees) {
        this.trees = trees;
    }

    @Override
    public void populate(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, LimitedRegion region) {
        if (!trees.enabled() || trees.perChunk() <= 0) return;

        int target = trees.perChunk();
        int attempts = Math.min(Math.max(target * 4, target), 256);

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int buffer = region.getBuffer();
        int margin = buffer <= 0 ? 1 : 0;
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

            int placeY = y + 1;
            if (!region.isInRegion(x, placeY, z)) continue;
            if (!region.getType(x, placeY, z).isAir()) continue;

            region.setType(x, placeY, z, MUSHROOMS[random.nextInt(MUSHROOMS.length)]);
            placed++;
        }
    }

    @Override
    public void populate(World world, Random random, Chunk chunk) {
        if (!trees.enabled() || trees.perChunk() <= 0) return;

        int target = trees.perChunk();
        int attempts = Math.min(Math.max(target * 4, target), 256);

        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;
        int margin = 1;
        int range = 16 - (margin * 2);
        if (range <= 0) return;

        int placed = 0;
        for (int i = 0; i < attempts && placed < target; i++) {
            int x = baseX + margin + random.nextInt(range);
            int z = baseZ + margin + random.nextInt(range);
            int y = world.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE);
            Material ground = world.getBlockAt(x, y, z).getType();
            if (!isValidGround(ground)) continue;

            if (!world.getBlockAt(x, y + 1, z).getType().isAir()) continue;
            world.getBlockAt(x, y + 1, z).setType(MUSHROOMS[random.nextInt(MUSHROOMS.length)], false);
            placed++;
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
}
