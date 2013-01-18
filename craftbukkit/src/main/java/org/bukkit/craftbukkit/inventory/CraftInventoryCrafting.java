package org.bukkit.craftbukkit.inventory;

import immibis.lavabukkit.BukkitInventoryHelper;
import immibis.lavabukkit.BukkitRecipeHelper;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;

import org.apache.commons.lang.Validate;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.util.Java15Compat;

public class CraftInventoryCrafting extends CraftInventory implements CraftingInventory {
    private final IInventory resultInventory;

    public CraftInventoryCrafting(InventoryCrafting inventory, IInventory resultInventory, InventoryHolder holder) {
        super(inventory, holder);
        this.resultInventory = resultInventory;
        
        Validate.notNull(resultInventory);
    }

    public IInventory getResultInventory() {
        return resultInventory;
    }

    public IInventory getMatrixInventory() {
        return inventory;
    }

    @Override
    public int getSize() {
        return getResultInventory().getSizeInventory() + getMatrixInventory().getSizeInventory();
    }

    @Override
    public void setContents(ItemStack[] items) {
        int resultLen = getResultInventory().getSizeInventory(); // LavaBukkit
        int len = getMatrixInventory().getSizeInventory() + resultLen; // LavaBukkit
        if (len > items.length) {
            throw new IllegalArgumentException("Invalid inventory size; expected " + len + " or less");
        }
        setContents(items[0], Java15Compat.Arrays_copyOfRange(items, 1, items.length));
    }

    @Override
    public ItemStack[] getContents() {
        ItemStack[] items = new ItemStack[getSize()];
        net.minecraft.item.ItemStack[] mcResultItems = BukkitInventoryHelper.getContents(getResultInventory()); // LavaBukkit

        int i = 0;
        for (i = 0; i < mcResultItems.length; i++ ) {
            items[i] = CraftItemStack.asCraftMirror(mcResultItems[i]);
        }

        net.minecraft.item.ItemStack[] mcItems = BukkitInventoryHelper.getContents(getMatrixInventory()); // LavaBukkit

        for (int j = 0; j < mcItems.length; j++) {
            items[i + j] = CraftItemStack.asCraftMirror(mcItems[j]);
        }

        return items;
    }

    public void setContents(ItemStack result, ItemStack[] contents) {
        setResult(result);
        setMatrix(contents);
    }

    @Override
    public CraftItemStack getItem(int index) {
        if (index < getResultInventory().getSizeInventory()) {
            net.minecraft.item.ItemStack item = getResultInventory().getStackInSlot(index);
            return item == null ? null : CraftItemStack.asCraftMirror(item);
        } else {
            net.minecraft.item.ItemStack item = getMatrixInventory().getStackInSlot(index - getResultInventory().getSizeInventory());
            return item == null ? null : CraftItemStack.asCraftMirror(item);
        }
    }

    @Override
    public void setItem(int index, ItemStack item) {
        if (index < getResultInventory().getSizeInventory()) {
            getResultInventory().setInventorySlotContents(index, (item == null ? null : CraftItemStack.asNMSCopy(item)));
        } else {
            getMatrixInventory().setInventorySlotContents((index - getResultInventory().getSizeInventory()), (item == null ? null : CraftItemStack.asNMSCopy(item)));
        }
    }

    public ItemStack[] getMatrix() {
        ItemStack[] items = new ItemStack[getSize()];
        net.minecraft.item.ItemStack[] matrix = BukkitInventoryHelper.getContents(getMatrixInventory()); // LavaBukkit

        for (int i = 0; i < matrix.length; i++ ) {
            items[i] = CraftItemStack.asCraftMirror(matrix[i]);
        }

        return items;
    }

    public ItemStack getResult() {
        net.minecraft.item.ItemStack item = getResultInventory().getStackInSlot(0);
        if(item != null) return CraftItemStack.asCraftMirror(item);
        return null;
    }

    public void setMatrix(ItemStack[] contents) {
    	// LavaBukkit start
        if (getMatrixInventory().getSizeInventory() > contents.length)
            throw new IllegalArgumentException("Invalid inventory size; expected " + getMatrixInventory().getSizeInventory() + " or less");

        for (int i = 0; i < inventory.getSizeInventory(); i++ ) {
            if (i < contents.length) {
                ItemStack item = contents[i];
                if (item == null || item.getTypeId() <= 0) {
                    inventory.setInventorySlotContents(i, null);
                } else {
                    inventory.setInventorySlotContents(i, CraftItemStack.asNMSCopy(item));
                }
            } else {
                inventory.setInventorySlotContents(i, null);
            }
        }
        // LavaBukkit end
    }

    public void setResult(ItemStack item) {
        if (item == null || item.getTypeId() <= 0) {
            resultInventory.setInventorySlotContents(0, null);
        } else {
            resultInventory.setInventorySlotContents(0, CraftItemStack.asNMSCopy(item));
        }
    }

    public Recipe getRecipe() {
    	try {
    		IRecipe recipe = ((InventoryCrafting)getInventory()).currentRecipe;
    		return recipe == null ? null : BukkitRecipeHelper.toBukkitRecipe(recipe);
    		
    	} catch(AbstractMethodError e) {
    		// Catches mod-specific recipes that don't implement toBukkitRecipe
    		return null;
    	}
    }
}
