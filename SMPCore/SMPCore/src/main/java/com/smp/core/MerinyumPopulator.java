package com.smp.core;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.Random;
import java.util.Set;

/** Yeni olusan chunk'lara Y <= max-y arasinda Merinyum damarlari koyar. */
public class MerinyumPopulator extends BlockPopulator {
    private static final Set<Material> REPLACEABLE = Set.of(Material.STONE, Material.DEEPSLATE, Material.TUFF);
    private final SMPCore plugin;

    public MerinyumPopulator(SMPCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void populate(WorldInfo world, Random r, int chunkX, int chunkZ, LimitedRegion region) {
        if (world.getEnvironment() != World.Environment.NORMAL) return;

        Material ore = Material.matchMaterial(plugin.getConfig().getString("merinyum.ore-block", "NETHER_GOLD_ORE"));
        if (ore == null) return;
        int maxY = plugin.getConfig().getInt("merinyum.max-y", -30);
        int minY = world.getMinHeight();
        double perChunk = plugin.getConfig().getDouble("merinyum.veins-per-chunk", 1.5);
        int size = plugin.getConfig().getInt("merinyum.vein-size", 4);

        int veins = (int) perChunk + (r.nextDouble() < (perChunk - (int) perChunk) ? 1 : 0);
        for (int v = 0; v < veins; v++) {
            int x = chunkX * 16 + r.nextInt(16);
            int z = chunkZ * 16 + r.nextInt(16);
            int y = minY + r.nextInt(maxY - minY + 1);

            for (int i = 0; i < size; i++) {
                if (y >= minY && y <= maxY && region.isInRegion(x, y, z)
                        && REPLACEABLE.contains(region.getType(x, y, z))) {
                    region.setType(x, y, z, ore);
                }
                x += r.nextInt(3) - 1;
                y += r.nextInt(3) - 1;
                z += r.nextInt(3) - 1;
            }
        }
    }
}
