package org.bukkit.craftbukkit.inventory;

import net.minecraft.inventory.SlotEnchantmentTable;

import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class CraftInventoryEnchanting extends CraftInventory implements EnchantingInventory {
    public CraftInventoryEnchanting(SlotEnchantmentTable inventory, InventoryHolder holder) {
        super(inventory, holder);
    }

    public void setItem(ItemStack item) {
        setItem(0,item);
    }

    public ItemStack getItem() {
        return getItem(0);
    }

    @Override
    public SlotEnchantmentTable getInventory() {
        return (SlotEnchantmentTable)inventory;
    }
}
