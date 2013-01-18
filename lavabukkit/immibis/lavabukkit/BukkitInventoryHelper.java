package immibis.lavabukkit;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.inventory.InventoryMerchant;
import net.minecraft.inventory.InventoryRepair;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.inventory.SlotEnchantmentTable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.tileentity.TileEntityFurnace;

import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.craftbukkit.inventory.CraftInventoryAnvil;
import org.bukkit.craftbukkit.inventory.CraftInventoryBeacon;
import org.bukkit.craftbukkit.inventory.CraftInventoryCrafting;
import org.bukkit.craftbukkit.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.inventory.CraftInventoryDoubleChest;
import org.bukkit.craftbukkit.inventory.CraftInventoryEnchanting;
import org.bukkit.craftbukkit.inventory.CraftInventoryFurnace;
import org.bukkit.craftbukkit.inventory.CraftInventoryMerchant;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import cpw.mods.fml.relauncher.ReflectionHelper;

public class BukkitInventoryHelper {
	
	private static class DummyInventoryHolder implements InventoryHolder {
		public Inventory inventory = new CraftInventoryCustom(this, 0);
		@Override public Inventory getInventory() {return inventory;}
	}
	
	private static IInventory findCraftResult(Container container, InventoryCrafting matrix) {
		for(Slot s : (List<Slot>)container.inventorySlots) {
			if(s instanceof SlotCrafting && ((SlotCrafting) s).craftMatrix == matrix) {
				matrix.resultInventory = s.inventory;
				return s.inventory;
			}
		}
		return matrix.resultInventory;
	}
	
	private static InventoryCrafting findCraftMatrix(Container container, IInventory result) {
		for(Slot s : (List<Slot>)container.inventorySlots) {
			if(s instanceof SlotCrafting && s.inventory == result && ((SlotCrafting)s).craftMatrix instanceof InventoryCrafting)
				return (InventoryCrafting)((SlotCrafting)s).craftMatrix;
		}
		return null;
	}
	
	public static Inventory toBukkitInventory(IInventory inventory, Container container, EntityPlayer player) {
		if(inventory instanceof TileEntityBeacon)
			return new CraftInventoryBeacon((TileEntityBeacon)inventory);
		if(inventory instanceof InventoryLargeChest)
			return new CraftInventoryDoubleChest((InventoryLargeChest)inventory);
		if(inventory instanceof SlotEnchantmentTable)
			return new CraftInventoryEnchanting((SlotEnchantmentTable)inventory, player.getBukkitEntity());
		if(inventory instanceof TileEntityFurnace)
			return new CraftInventoryFurnace((TileEntityFurnace)inventory);
		if(inventory instanceof InventoryMerchant)
			return new CraftInventoryMerchant((InventoryMerchant)inventory, player.getBukkitEntity());
		if(inventory instanceof InventoryCrafting) {
			if(player != null)
				return new CraftInventoryCrafting((InventoryCrafting)inventory, findCraftResult(container, (InventoryCrafting)inventory), player.getBukkitEntity());
			else {
				DummyInventoryHolder h = new DummyInventoryHolder();
				h.inventory = new CraftInventoryCrafting((InventoryCrafting)inventory, findCraftResult(container, (InventoryCrafting)inventory), h);
				return h.inventory;
			}
		}
		if(inventory instanceof InventoryCraftResult) {
			InventoryCrafting matrix = findCraftMatrix(container, inventory);
			if(matrix == null || player == null)
				return null;
			else
				return new CraftInventoryCrafting(matrix, inventory, player.getBukkitEntity());
		}
		if(inventory instanceof InventoryRepair)
			return new CraftInventoryAnvil((InventoryRepair)inventory, player.getBukkitEntity());
		if(inventory instanceof TileEntity)
			return new CraftInventory(inventory, ((TileEntity)inventory).getBlockStateCB());
		return createDummyHolderInventory(inventory);
	}

	private static WeakHashMap<Container, InventoryView> bukkitContainers = new WeakHashMap<Container, InventoryView>();
	public static InventoryView getBukkitView(Container container) {
		
		InventoryView cached = bukkitContainers.get(container);
		if(cached != null)
			return cached;
		
		EntityPlayer player = null;
		for(Object o : container.crafters) {
    		if(o instanceof EntityPlayer) {
    			player = (EntityPlayer)o;
    			break;
    		}
    	}
		
		Inventory inventory = null;
		for(Slot  s: (List<Slot>)container.inventorySlots) {
			boolean isPlayer = s.inventory instanceof InventoryPlayer;
			if(player == null && isPlayer)
				player = ((InventoryPlayer)s.inventory).player;
			else if(player != null && s.inventory == player.inventory)
				/* do nothing */;
			else if(inventory == null)
				inventory = toBukkitInventory(s.inventory, container, player);
				
			if(player != null && inventory != null)
				break;
		}
		
		if(player == null)
			System.err.println("could not detect player who is using container, this will probably crash");
		if(inventory == null) {
			inventory = new DummyInventoryHolder().getInventory();
		}
    	InventoryView bukkitView = new CraftInventoryView(player == null ? null : player.getBukkitEntity(), inventory, container);
    	bukkitContainers.put(container, bukkitView);
    	return bukkitView;
	}

	public static void setMaxStackSize(IInventory inventory, int size) {
		// could happen if a crafting inventory doesn't know its result slot
		if(inventory == null) return;
		
        try {
        	// this field is added by IInventoryTransformer
        	Field f = inventory.getClass().getField("__lbMaxStackSize");
        	f.set(inventory, size);
        } catch(Exception e) {
        	e.printStackTrace();
        }
        if(inventory instanceof InventoryLargeChest) {
        	IInventory lowerChest = ReflectionHelper.getPrivateValue(InventoryLargeChest.class, (InventoryLargeChest)inventory, 1);
        	IInventory upperChest = ReflectionHelper.getPrivateValue(InventoryLargeChest.class, (InventoryLargeChest)inventory, 2);
        	setMaxStackSize(lowerChest, size);
        	setMaxStackSize(upperChest, size);
        }
        if(inventory instanceof InventoryCrafting) {
        	setMaxStackSize(((InventoryCrafting)inventory).resultInventory, size);
        }
	}
	
	private static boolean containerViewsInventory(Container c, IInventory inv) {
		for(Slot s : (List<Slot>)c.inventorySlots) {
			if(s.inventory == inv)
				return true;
			if(s.inventory instanceof InventoryLargeChest) {
				InventoryLargeChest ilc = (InventoryLargeChest)s.inventory;

				IInventory lowerChest = ReflectionHelper.getPrivateValue(InventoryLargeChest.class, ilc, 1);
				IInventory upperChest = ReflectionHelper.getPrivateValue(InventoryLargeChest.class, ilc, 2);
        		if(lowerChest == inv || upperChest == inv)
					return true;
			}
		}
		return false;
	}

	public static List<HumanEntity> getViewers(IInventory inv) {
		List<HumanEntity> rv = new ArrayList<HumanEntity>();
		for(EntityPlayer ply : (List<EntityPlayer>)MinecraftServer.getServer().getConfigurationManager().playerEntityList)
			if(containerViewsInventory(ply.openContainer, inv))
				rv.add(ply.getBukkitEntity());
		return rv;
	}

	public static net.minecraft.item.ItemStack[] getContents(IInventory inv) {
		int size = inv.getSizeInventory();
		net.minecraft.item.ItemStack[] rv = new net.minecraft.item.ItemStack[size];
		for(int k = 0; k < size; k++)
			rv[k] = inv.getStackInSlot(k);
		return rv;
	}

	public static InventoryHolder getHolder(IInventory inv) {
		if(inv instanceof TileEntity) {
			InventoryHolder h = ((TileEntity)inv).getBlockStateCB();
			if(h != null)
				return h;
		}
		throw new UnsupportedOperationException("cannot determine InventoryHolder of "+inv+"!");
	}

	public static CraftInventory createDummyHolderInventory(IInventory inv) {
		DummyInventoryHolder h = new DummyInventoryHolder();
		h.inventory = new CraftInventory(inv, h);
		return (CraftInventory)h.inventory;
	}
	
	public static ArrayList<ItemStack> toBukkitList(List<net.minecraft.item.ItemStack> in) {
		ArrayList<ItemStack> rv = new ArrayList<ItemStack>(in.size());
		for(net.minecraft.item.ItemStack a : in)
			rv.add(CraftItemStack.asBukkitCopy(a));
		return rv;
	}
	
	public static ArrayList<net.minecraft.item.ItemStack> fromBukkitList(List<ItemStack> in) {
		ArrayList<net.minecraft.item.ItemStack> rv = new ArrayList<net.minecraft.item.ItemStack>(in.size());
		for(ItemStack a : in)
			rv.add(CraftItemStack.asNMSCopy(a));
		return rv;
	}
}
