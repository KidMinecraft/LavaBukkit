package org.bukkit.craftbukkit.entity;


import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

public class CraftItem extends CraftEntity implements Item {
    private final EntityItem item;

    public CraftItem(CraftServer server, Entity entity, EntityItem item) {
        super(server, entity);
        this.item = item;
    }

    public CraftItem(CraftServer server, EntityItem entity) {
        this(server, entity, entity);
    }

    public ItemStack getItemStack() {
        return CraftItemStack.asCraftMirror(item.getItem());
    }

    public void setItemStack(ItemStack stack) {
        item.setItem(CraftItemStack.asNMSCopy(stack));
    }

    public int getPickupDelay() {
        return item.delayBeforeCanPickup;
    }

    public void setPickupDelay(int delay) {
        item.delayBeforeCanPickup = delay;
    }

    @Override
    public String toString() {
        return "CraftItem";
    }

    public EntityType getType() {
        return EntityType.DROPPED_ITEM;
    }
}
