package immibis.lavabukkit;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.LazyPlayerSet;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.world.WorldSaveEvent;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.network.NetServerHandler;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.event.Event;
import net.minecraftforge.event.EventPriority;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;

public class ForgeEventListener {
	/*@ForgeSubscribe(priority=EventPriority.HIGHEST, receiveCanceled=true)
	public void onInteract(PlayerInteractEvent evt) {
		Entity bent = evt.entityPlayer.getBukkitEntity();
		if(!(bent instanceof Player))
			return;
		
		Action baction = translateAction(evt.action);
		if(baction == null) {
			System.err.println("FIXME: Unknown Forge action type "+evt.action);
			return;
		}
		
		org.bukkit.event.player.PlayerInteractEvent bevt = new org.bukkit.event.player.PlayerInteractEvent(
				(Player)bent,
				baction,
				new CraftItemStack(evt.entityPlayer.inventory.getCurrentItem()),
				bent.getWorld().getBlockAt(evt.x, evt.y, evt.z),
				CraftBlock.notchToBlockFace(evt.face));
	}
	
	private Action translateAction(net.minecraftforge.event.entity.player.PlayerInteractEvent.Action forge) {
		switch(forge) {
		case RIGHT_CLICK_AIR: return Action.RIGHT_CLICK_AIR;
		case RIGHT_CLICK_BLOCK: return Action.RIGHT_CLICK_BLOCK;
		case LEFT_CLICK_BLOCK: return Action.LEFT_CLICK_BLOCK;
		}
		return null;
	}*/
	
	@ForgeSubscribe(priority = EventPriority.HIGHEST, receiveCanceled = true)
	public void onLivingDrops(LivingDropsEvent evt) {
		
		ArrayList<org.bukkit.inventory.ItemStack> bukkitDrops = new ArrayList<org.bukkit.inventory.ItemStack>(evt.drops.size());
		
		for(EntityItem item : evt.drops)
			bukkitDrops.add(CraftItemStack.asBukkitCopy(item.getItem()));
		
		CraftLivingEntity entity = (CraftLivingEntity) evt.entity.getBukkitEntity();
        EntityDeathEvent event = new EntityDeathEvent(entity, bukkitDrops, ((EntityLiving)evt.entity).getExpReward());
        org.bukkit.World world = entity.getWorld();
        Bukkit.getServer().getPluginManager().callEvent(event);

        ((EntityLiving)evt.entity).expToDrop = event.getDroppedExp();

        evt.drops.clear();
        for (org.bukkit.inventory.ItemStack bukkitDrop : event.getDrops())
        	evt.drops.add(new EntityItem(evt.entity.worldObj, evt.entity.posX, evt.entity.posY, evt.entity.posZ, CraftItemStack.asNMSCopy(bukkitDrop)));
	}
	
	@ForgeSubscribe(priority = EventPriority.HIGHEST, receiveCanceled = false)
	public void onItemExpire(ItemExpireEvent evt) {
		int prevAge = evt.entityItem.age;
		org.bukkit.craftbukkit.event.CraftEventFactory.callItemDespawnEvent(evt.entityItem);
		int diff = prevAge - evt.entityItem.age;
		evt.entityItem.age += diff;
        evt.extraLife += diff;
        evt.setCanceled(evt.isCanceled() | diff > 0);
	}
	
	@ForgeSubscribe(priority = EventPriority.LOWEST, receiveCanceled = false)
	public void onPlayerPickupItem(EntityItemPickupEvent evt) {
		int canHold = evt.entityPlayer.inventory.canHold(evt.item.getItem());
		int remaining = evt.item.getItem().stackSize - canHold;
		
		if (canHold > 0) {
            evt.item.getItem().stackSize = canHold;
            PlayerPickupItemEvent event2 = new PlayerPickupItemEvent((org.bukkit.entity.Player) evt.entityPlayer.getBukkitEntity(), (org.bukkit.entity.Item) evt.item.getBukkitEntity(), remaining);
            //event2.setCancelled(!evt.entityPlayer.canPickUpLoot);
            evt.item.worldObj.getServer().getPluginManager().callEvent(event2);
            evt.item.getItem().stackSize = canHold + remaining;

            if (event2.isCancelled())
            	evt.setCanceled(true);
        }
	}
	
	@ForgeSubscribe(priority = EventPriority.LOWEST, receiveCanceled = true)
	public void onRightClickBlock(PlayerInteractEvent evt) {
		if(evt.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
			org.bukkit.event.player.PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(evt.entityPlayer, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, evt.x, evt.y, evt.z, evt.face, evt.entityPlayer.inventory.getCurrentItem());
			if(evt.useItem == Event.Result.DEFAULT)
				evt.useItem = Event.Result.valueOf(event.useItemInHand().name());
			if(evt.useBlock == Event.Result.DEFAULT)
				evt.useBlock = Event.Result.valueOf(event.useInteractedBlock().name());
			if(event.isCancelled())
				evt.setCanceled(true);
		}
	}
	
	@ForgeSubscribe(priority = EventPriority.LOWEST)
	public void onWorldLoad(WorldEvent.Load evt) {
		evt.world.getServer().getPluginManager().callEvent(new org.bukkit.event.world.WorldInitEvent(evt.world.getWorld()));
	}
	
	@ForgeSubscribe(priority = EventPriority.LOWEST)
	public void onWorldSave(WorldEvent.Save evt) {
		evt.world.getServer().getPluginManager().callEvent(new WorldSaveEvent(evt.world.getWorld()));
	}
	
	@ForgeSubscribe(priority = EventPriority.HIGHEST, receiveCanceled = true)
	public void onChatHighest(ServerChatEvent evt) {
		Player bplayer = evt.player.getBukkitEntity();
		
		if(bplayer.isConversing()) {
			bplayer.acceptConversationInput(evt.message);
			evt.setCanceled(true);
		}
	}
	
	@ForgeSubscribe(priority = EventPriority.LOWEST, receiveCanceled = false)
	public void onChatLowest(ServerChatEvent evt) {
		if(evt.player.isDead) {
			evt.setCanceled(true);
			return;
		}
		if(evt.message.length() == 0) {
            NetServerHandler.logger.warning(evt.player.username + " tried to send an empty message");
            evt.setCanceled(true);
            return;
        }
		Player bplayer = evt.player.getBukkitEntity();
		
		AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(!Bukkit.isPrimaryThread(), bplayer, evt.message, new LazyPlayerSet());
		String defaultFormat = event.getFormat();
		Bukkit.getPluginManager().callEvent(event);
		
		// note: doesn't send deprecated PlayerChatEvent
		
		if(event.isCancelled()) {
			evt.setCanceled(true);
			return;
		}
		
		if(!event.getFormat().equals(defaultFormat) || !event.getMessage().equals(evt.message)) {
			evt.line = String.format(event.getFormat(), event.getPlayer().getDisplayName(), event.getMessage());
		}
		
		if(((LazyPlayerSet)event.getRecipients()).isLazy()) {
			return;
		}
		
		evt.setCanceled(true);
		
		for(Player p : event.getRecipients()) {
			p.sendMessage(evt.line);
		}
	}
}
