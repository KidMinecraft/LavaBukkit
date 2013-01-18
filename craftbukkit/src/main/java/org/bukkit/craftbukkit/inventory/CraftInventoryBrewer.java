package org.bukkit.craftbukkit.inventory;

import net.minecraft.tileentity.TileEntityBrewingStand;

import org.bukkit.block.BrewingStand;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;

public class CraftInventoryBrewer extends CraftInventory implements BrewerInventory {
	// LavaBukkit - changed signature
    public CraftInventoryBrewer(TileEntityBrewingStand inventory) {
        super(inventory, inventory.getBlockStateCB());
    }

    public ItemStack getIngredient() {
        return getItem(3);
    }

    public void setIngredient(ItemStack ingredient) {
        setItem(3, ingredient);
    }

    @Override
    public BrewingStand getHolder() {
        return (BrewingStand)((TileEntityBrewingStand)inventory).getBlockStateCB();
    }
}
