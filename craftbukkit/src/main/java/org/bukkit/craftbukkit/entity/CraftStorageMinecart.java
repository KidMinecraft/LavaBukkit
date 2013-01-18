package org.bukkit.craftbukkit.entity;

import immibis.lavabukkit.BukkitInventoryHelper;
import net.minecraft.entity.item.EntityMinecart;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.StorageMinecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class CraftStorageMinecart extends CraftMinecart implements StorageMinecart {
    private final CraftInventory inventory;

    public CraftStorageMinecart(CraftServer server, EntityMinecart entity) {
        super(server, entity);
        
        // LavaBukkit start
        Entity bent = entity.getBukkitEntity();
        if(bent instanceof InventoryHolder)
        	inventory = new CraftInventory(entity, (InventoryHolder)bent);
        else
        	inventory = BukkitInventoryHelper.createDummyHolderInventory(entity);
        // LavaBukkit end
    }

    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public String toString() {
        return "CraftStorageMinecart{" + "inventory=" + inventory + '}';
    }
}
