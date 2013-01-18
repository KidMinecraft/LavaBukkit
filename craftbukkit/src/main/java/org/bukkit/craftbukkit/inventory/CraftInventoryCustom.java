package org.bukkit.craftbukkit.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryHolder;

public class CraftInventoryCustom extends CraftInventory {
    public CraftInventoryCustom(InventoryHolder owner, InventoryType type) {
        super(new MinecraftInventory(owner, type), owner);
    }

    public CraftInventoryCustom(InventoryHolder owner, int size) {
        super(new MinecraftInventory(owner, size), owner);
    }

    public CraftInventoryCustom(InventoryHolder owner, int size, String title) {
        super(new MinecraftInventory(owner, size, title), owner);
    }

    static class MinecraftInventory implements IInventory {
        private final ItemStack[] items;
        private final String title;
        private InventoryType type;
        private final InventoryHolder owner;

        public MinecraftInventory(InventoryHolder owner, InventoryType type) {
            this(owner, type.getDefaultSize(), type.getDefaultTitle());
            this.type = type;
        }

        public MinecraftInventory(InventoryHolder owner, int size) {
            this(owner, size, "Chest");
        }

        public MinecraftInventory(InventoryHolder owner, int size, String title) {
            this.items = new ItemStack[size];
            this.title = title;
            this.owner = owner;
            this.type = InventoryType.CHEST;
        }

        public int getSizeInventory() {
            return items.length;
        }

        public ItemStack getStackInSlot(int i) {
            return items[i];
        }

        public ItemStack decrStackSize(int i, int j) {
            ItemStack stack = this.getStackInSlot(i);
            ItemStack result;
            if (stack == null) return null;
            if (stack.stackSize <= j) {
                this.setInventorySlotContents(i, null);
                result = stack;
            } else {
                result = CraftItemStack.copyNMSStack(stack, j);
                stack.stackSize -= j;
            }
            this.onInventoryChanged();
            return result;
        }

        public ItemStack getStackInSlotOnClosing(int i) {
            ItemStack stack = this.getStackInSlot(i);
            ItemStack result;
            if (stack == null) return null;
            if (stack.stackSize <= 1) {
                this.setInventorySlotContents(i, null);
                result = stack;
            } else {
                result = CraftItemStack.copyNMSStack(stack, 1);
                stack.stackSize -= 1;
            }
            return result;
        }

        public void setInventorySlotContents(int i, ItemStack itemstack) {
            items[i] = itemstack;
            if (itemstack != null && this.getInventoryStackLimit() > 0 && itemstack.stackSize > this.getInventoryStackLimit()) {
                itemstack.stackSize = this.getInventoryStackLimit();
            }
        }

        public String getInvName() {
            return title;
        }

        public int getInventoryStackLimit() {
            return 64;
        }

        public void onInventoryChanged() {}

        public boolean isUseableByPlayer(EntityPlayer entityhuman) {
            return true;
        }

        public ItemStack[] getContents() {
            return items;
        }
        
        public InventoryType getType() {
            return type;
        }

        public void openChest() {}

        public void closeChest() {}

        public InventoryHolder getOwner() {
            return owner;
        }

        public void startOpen() {}
    }
}
