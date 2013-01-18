package org.bukkit.craftbukkit.inventory;

import net.minecraft.inventory.InventoryMerchant;

import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.MerchantInventory;

public class CraftInventoryMerchant extends CraftInventory implements MerchantInventory {
    public CraftInventoryMerchant(InventoryMerchant merchant, InventoryHolder holder) {
        super(merchant, holder);
    }
}
