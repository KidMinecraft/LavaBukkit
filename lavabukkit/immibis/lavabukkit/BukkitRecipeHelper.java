package immibis.lavabukkit;

import java.util.List;

import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftShapedRecipe;
import org.bukkit.craftbukkit.inventory.CraftShapelessRecipe;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;

public class BukkitRecipeHelper {
	public static ShapedRecipe shapedRecipeToBukkit(ShapedRecipes sr) {
        CraftItemStack result = CraftItemStack.asCraftMirror(sr.getRecipeOutput());
        CraftShapedRecipe recipe = new CraftShapedRecipe(result, sr);
        switch (sr.recipeHeight) {
        case 1:
            switch (sr.recipeWidth) {
            case 1:
                recipe.shape("a");
                break;
            case 2:
                recipe.shape("ab");
                break;
            case 3:
                recipe.shape("abc");
                break;
            }
            break;
        case 2:
            switch (sr.recipeWidth) {
            case 1:
                recipe.shape("a","b");
                break;
            case 2:
                recipe.shape("ab","cd");
                break;
            case 3:
                recipe.shape("abc","def");
                break;
            }
            break;
        case 3:
            switch (sr.recipeWidth) {
            case 1:
                recipe.shape("a","b","c");
                break;
            case 2:
                recipe.shape("ab","cd","ef");
                break;
            case 3:
                recipe.shape("abc","def","ghi");
                break;
            }
            break;
        }
        char c = 'a';
        for (ItemStack stack : sr.recipeItems) {
            if (stack != null) {
                recipe.setIngredient(c, org.bukkit.Material.getMaterial(stack.itemID), stack.getItemDamage());
            }
            c++;
        }
        return recipe;
    }
	
	public static ShapelessRecipe shapelessRecipeToBukkit(ShapelessRecipes sr) {
	    CraftItemStack result = CraftItemStack.asCraftMirror(sr.getRecipeOutput());
        CraftShapelessRecipe recipe = new CraftShapelessRecipe(result, sr);
        for (ItemStack stack : (List<ItemStack>) sr.recipeItems) {
            if (stack != null) {
                recipe.addIngredient(org.bukkit.Material.getMaterial(stack.itemID), stack.getItemDamage());
            }
        }
        return recipe;
	}

	public static Recipe toBukkitRecipe(IRecipe r) {
		if(r instanceof ShapedRecipes)
			return shapedRecipeToBukkit((ShapedRecipes)r);
		else if(r instanceof ShapelessRecipes)
			return shapelessRecipeToBukkit((ShapelessRecipes)r);
		// TODO: is it possible to do ore-dict recipes? what about IC2?
		else
			return null;
	}
}
