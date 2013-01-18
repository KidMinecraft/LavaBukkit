package org.bukkit.craftbukkit.block;

import net.minecraft.tileentity.TileEntityBrewingStand;

import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.inventory.CraftInventoryBrewer;
import org.bukkit.inventory.BrewerInventory;

public class CraftBrewingStand extends CraftBlockState implements BrewingStand {
    private final CraftWorld world;
    private final TileEntityBrewingStand brewingStand;

    public CraftBrewingStand(Block block) {
        super(block);

        world = (CraftWorld) block.getWorld();
        brewingStand = (TileEntityBrewingStand) world.getTileEntityAt(getX(), getY(), getZ());
    }

    public BrewerInventory getInventory() {
        return new CraftInventoryBrewer(brewingStand);
    }

    @Override
    public boolean update(boolean force) {
        boolean result = super.update(force);

        if (result) {
            brewingStand.onInventoryChanged();
        }

        return result;
    }

    public int getBrewingTime() {
        return brewingStand.getBrewTime();
    }

    public void setBrewingTime(int brewTime) {
        brewingStand.setBrewTime(brewTime);
    }
}
