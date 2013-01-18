package org.bukkit.craftbukkit.entity;

import immibis.lavabukkit.BukkitInventoryHelper;

import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.network.packet.Packet100OpenWindow;
import net.minecraft.network.packet.Packet101CloseWindow;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityFurnace;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftContainer;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.craftbukkit.inventory.CraftInventoryPlayer;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

public class CraftHumanEntity extends CraftLivingEntity implements HumanEntity {
    private CraftInventoryPlayer inventory;
    private final CraftInventory enderChest;
    protected final PermissibleBase perm = new PermissibleBase(this);
    private boolean op;
    private GameMode mode;

    public CraftHumanEntity(final CraftServer server, final EntityPlayer entity) {
        super(server, entity);
        mode = server.getDefaultGameMode();
        this.inventory = new CraftInventoryPlayer(entity.inventory);
        enderChest = new CraftInventory(entity.getInventoryEnderChest(), this);
    }

    public String getName() {
        return getHandle().getEntityName();
    }

    public PlayerInventory getInventory() {
        return inventory;
    }

    public EntityEquipment getEquipment() {
        return inventory;
    }

    public Inventory getEnderChest() {
        return enderChest;
    }

    public ItemStack getItemInHand() {
        return getInventory().getItemInHand();
    }

    public void setItemInHand(ItemStack item) {
        getInventory().setItemInHand(item);
    }

    public ItemStack getItemOnCursor() {
        return CraftItemStack.asCraftMirror(getHandle().inventory.getItemStack());
    }

    public void setItemOnCursor(ItemStack item) {
        net.minecraft.item.ItemStack stack = CraftItemStack.asNMSCopy(item);
        getHandle().inventory.setItemStack(stack);
        if (this instanceof CraftPlayer) {
            ((EntityPlayerMP) getHandle()).updateHeldItem(); // Send set slot for cursor
        }
    }

    public boolean isSleeping() {
        return getHandle().isPlayerSleeping();
    }

    public int getSleepTicks() {
        return getHandle().getSleepTimer();
    }

    public boolean isOp() {
        return op;
    }

    public boolean isPermissionSet(String name) {
        return perm.isPermissionSet(name);
    }

    public boolean isPermissionSet(Permission perm) {
        return this.perm.isPermissionSet(perm);
    }

    public boolean hasPermission(String name) {
        return perm.hasPermission(name);
    }

    public boolean hasPermission(Permission perm) {
        return this.perm.hasPermission(perm);
    }

    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
        return perm.addAttachment(plugin, name, value);
    }

    public PermissionAttachment addAttachment(Plugin plugin) {
        return perm.addAttachment(plugin);
    }

    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
        return perm.addAttachment(plugin, name, value, ticks);
    }

    public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
        return perm.addAttachment(plugin, ticks);
    }

    public void removeAttachment(PermissionAttachment attachment) {
        perm.removeAttachment(attachment);
    }

    public void recalculatePermissions() {
        perm.recalculatePermissions();
    }

    public void setOp(boolean value) {
        this.op = value;
        perm.recalculatePermissions();
    }

    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return perm.getEffectivePermissions();
    }

    public GameMode getGameMode() {
        return mode;
    }

    public void setGameMode(GameMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("Mode cannot be null");
        }

        this.mode = mode;
    }

    @Override
    public EntityPlayer getHandle() {
        return (EntityPlayer) entity;
    }

    public void setHandle(final EntityPlayer entity) {
        super.setHandle(entity);
        this.inventory = new CraftInventoryPlayer(entity.inventory);
    }

    @Override
    public String toString() {
        return "CraftHumanEntity{" + "id=" + getEntityId() + "name=" + getName() + '}';
    }

    public InventoryView getOpenInventory() {
        return BukkitInventoryHelper.getBukkitView(getHandle().openContainer);
    }

    public InventoryView openInventory(Inventory inventory) {
        if(!(getHandle() instanceof EntityPlayerMP)) return null;
        EntityPlayerMP player = (EntityPlayerMP) getHandle();
        InventoryType type = inventory.getType();
        Container formerContainer = getHandle().openContainer;
        // TODO: Should we check that it really IS a CraftInventory first?
        CraftInventory craftinv = (CraftInventory) inventory;
        switch(type) {
        case PLAYER:
        case CHEST:
        case ENDER_CHEST:
            getHandle().displayGUIChest(craftinv.getInventory());
            break;
        case DISPENSER:
            if (craftinv.getInventory() instanceof TileEntityDispenser) {
                getHandle().displayGUIDispenser((TileEntityDispenser)craftinv.getInventory());
            } else {
                openCustomInventory(inventory, player, 3);
            }
            break;
        case FURNACE:
            if (craftinv.getInventory() instanceof TileEntityFurnace) {
                getHandle().displayGUIFurnace((TileEntityFurnace)craftinv.getInventory());
            } else {
                openCustomInventory(inventory, player, 2);
            }
            break;
        case WORKBENCH:
            openCustomInventory(inventory, player, 1);
            break;
        case BREWING:
            if (craftinv.getInventory() instanceof TileEntityBrewingStand) {
                getHandle().displayGUIBrewingStand((TileEntityBrewingStand)craftinv.getInventory());
            } else {
                openCustomInventory(inventory, player, 5);
            }
            break;
        case ENCHANTING:
                openCustomInventory(inventory, player, 4);
            break;
        case CREATIVE:
        case CRAFTING:
            throw new IllegalArgumentException("Can't open a " + type + " inventory!");
        }
        if (getHandle().openContainer == formerContainer) {
            return null;
        }
        getHandle().openContainer.checkReachable = false;
        return getOpenInventory();
    }

    private void openCustomInventory(Inventory inventory, EntityPlayerMP player, int windowType) {
        if (player.playerNetServerHandler == null) return;
        player.incrementWindowID();
        Container container = new CraftContainer(inventory, this, player.currentWindowId);

        container = CraftEventFactory.callInventoryOpenEvent(player, container);
        if(container == null) return;

        String title = BukkitInventoryHelper.getBukkitView(container).getTitle();
        int size = BukkitInventoryHelper.getBukkitView(container).getTopInventory().getSize();

        player.playerNetServerHandler.sendPacketToPlayer(new Packet100OpenWindow(container.windowId, windowType, title, size));
        getHandle().openContainer = container;
        getHandle().openContainer.addCraftingToCrafters(player);
    }

    public InventoryView openWorkbench(Location location, boolean force) {
        if (!force) {
            Block block = location.getBlock();
            if (block.getType() != Material.WORKBENCH) {
                return null;
            }
        }
        if (location == null) {
            location = getLocation();
        }
        getHandle().displayGUIWorkbench(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (force) {
            getHandle().openContainer.checkReachable = false;
        }
        return getOpenInventory();
    }

    public InventoryView openEnchanting(Location location, boolean force) {
        if (!force) {
            Block block = location.getBlock();
            if (block.getType() != Material.ENCHANTMENT_TABLE) {
                return null;
            }
        }
        if (location == null) {
            location = getLocation();
        }
        getHandle().displayGUIEnchantment(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (force) {
            getHandle().openContainer.checkReachable = false;
        }
        return getOpenInventory();
    }

    public void openInventory(InventoryView inventory) {
        if (!(getHandle() instanceof EntityPlayerMP)) return; // TODO: NPC support?
        if (((EntityPlayerMP) getHandle()).playerNetServerHandler == null) return;
        if (getHandle().openContainer != getHandle().inventoryContainer) {
            // fire INVENTORY_CLOSE if one already open
            ((EntityPlayerMP)getHandle()).playerNetServerHandler.handleCloseWindow(new Packet101CloseWindow(getHandle().openContainer.windowId));
        }
        EntityPlayerMP player = (EntityPlayerMP) getHandle();
        Container container;
        if (inventory instanceof CraftInventoryView) {
            container = ((CraftInventoryView) inventory).getHandle();
        } else {
        	player.incrementWindowID();
            container = new CraftContainer(inventory, player.currentWindowId);
        }

        // Trigger an INVENTORY_OPEN event
        container = CraftEventFactory.callInventoryOpenEvent(player, container);
        if (container == null) {
            return;
        }

        // Now open the window
        InventoryType type = inventory.getType();
        int windowType = CraftContainer.getNotchInventoryType(type);
        String title = inventory.getTitle();
        int size = inventory.getTopInventory().getSize();
        player.playerNetServerHandler.sendPacketToPlayer(new Packet100OpenWindow(container.windowId, windowType, title, size));
        player.openContainer = container;
        player.openContainer.addCraftingToCrafters(player);
    }

    public void closeInventory() {
        getHandle().closeScreen();
    }

    public boolean isBlocking() {
        return getHandle().isBlocking();
    }

    public boolean setWindowProperty(InventoryView.Property prop, int value) {
        return false;
    }

    public int getExpToLevel() {
        return getHandle().xpBarCap();
    }
}
