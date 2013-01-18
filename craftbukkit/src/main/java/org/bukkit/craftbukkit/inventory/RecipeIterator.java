package org.bukkit.craftbukkit.inventory;

import immibis.lavabukkit.BukkitRecipeHelper;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public class RecipeIterator implements Iterator<Recipe> {
    private Iterator<IRecipe> recipes;
    private Iterator<Map.Entry<Integer, net.minecraft.item.ItemStack>> smelting;
    private Iterator<Map.Entry<List<Integer>, net.minecraft.item.ItemStack>> metaSmelting;
    private Iterator<?> removeFrom = null;

    public RecipeIterator() {
        this.recipes = CraftingManager.getInstance().getRecipeList().iterator();
        this.smelting = FurnaceRecipes.smelting().smeltingList.entrySet().iterator();
        this.metaSmelting = FurnaceRecipes.smelting().metaSmeltingList.entrySet().iterator();
    }

    public boolean hasNext() {
    	return recipes.hasNext() || smelting.hasNext() || metaSmelting.hasNext();
    }

    public Recipe next() {
        if (recipes.hasNext()) {
            removeFrom = recipes;
            while(recipes.hasNext()) {
            	Recipe recipe = BukkitRecipeHelper.toBukkitRecipe(recipes.next());
            	if(recipe != null)
            		return recipe;
            }
            return next();
        } else if(smelting.hasNext()) {
            removeFrom = smelting;
            Map.Entry<Integer, net.minecraft.item.ItemStack> id = smelting.next();
            CraftItemStack stack = CraftItemStack.asCraftMirror(id.getValue());
            CraftFurnaceRecipe recipe = new CraftFurnaceRecipe(stack, new ItemStack(id.getKey(), 1, (short) -1));
            return recipe;
        } else {
        	removeFrom = metaSmelting;
        	Map.Entry<List<Integer>, net.minecraft.item.ItemStack> id = metaSmelting.next();
        	CraftItemStack stack = CraftItemStack.asCraftMirror(id.getValue());
            CraftFurnaceRecipe recipe = new CraftFurnaceRecipe(stack, new ItemStack(id.getKey().get(0), 1, (short)(int)id.getKey().get(1)));
            return recipe;
        }
    }

    public void remove() {
        if (removeFrom == null) {
            throw new IllegalStateException();
        }
        removeFrom.remove();
    }
}
