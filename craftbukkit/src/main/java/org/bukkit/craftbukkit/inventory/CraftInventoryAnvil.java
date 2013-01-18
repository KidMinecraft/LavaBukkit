package org.bukkit.craftbukkit.inventory;

import net.minecraft.inventory.InventoryRepair;

import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.InventoryHolder;

public class CraftInventoryAnvil extends CraftInventory implements AnvilInventory {
    public CraftInventoryAnvil(InventoryRepair anvil, InventoryHolder holder) {
        super(anvil, holder);
    }
}
