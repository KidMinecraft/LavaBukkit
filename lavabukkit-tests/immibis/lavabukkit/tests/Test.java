package immibis.lavabukkit.tests;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet3Chat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.EnumGameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

import cpw.mods.fml.relauncher.ReflectionHelper;

public abstract class Test implements Runnable {
	// offset to testing area
	public int XOFF = 0;
	public int YOFF = 128;
	public int ZOFF = 0;
	
	// For all functions in here, coords are relative to test area (X/Y/ZOFF is added to them)
	
	public TestModPlugin plugin = TestMod.plugin;
	
	public <T> T onclient(Callable<T> x) {return TestMod.onclient(x);}
	public <T> T onserver(Callable<T> x) {return TestMod.onserver(x);}
	
	// List of all changed blocks so they can be reset to air after the test.
	private List<Coords> affectedCoords = new ArrayList<Coords>();
	
	public <T extends Event & Cancellable> void cancelNextEvent(Class<T> class1) {
		runOnNextEvent(class1, new EventCallback<T>() {
			@Override
			public void handle(T t) {
				t.setCancelled(true);
			}
		});
	}
	public <T extends Event> void runOnNextEvent(Class<T> class1, EventCallback<T> cb) {
		plugin.receivedEvents.runOnNextEvent(class1, cb);
	}
	
	public void discardEvents() {
		while(plugin.receivedEvents.poll() != null);
	}
	
	public String getPlayerUsername() {
		return onclient(new Callable<String>() {public String call() throws Exception {
			return Minecraft.getMinecraft().thePlayer.username;
		}});
	}
	
	public void teleportThePlayer(final double x, final double y, final double z) {
		final String playerName = getPlayerUsername();
		
		onserver(new Callable<Void>() {public Void call() throws Exception {
			EntityPlayer player = MinecraftServer.getServer().getConfigurationManager().getPlayerForUsername(playerName);
			player.setPositionAndUpdate(x+XOFF, y+YOFF, z+ZOFF);
			return null;
		}});
		
		final Ref<Boolean> done = new Ref<Boolean>(false);
		
		onclient(new Callable<Void>() {public Void call() throws Exception {
			EntityPlayer player = Minecraft.getMinecraft().thePlayer;
			
			if(player.getDistanceSq(x+XOFF, y+YOFF, z+ZOFF) < 4) {
				done.set(true);
			} else {
				onclient(this);
			}
			
			return null;
		}});
		
		try {
			while(!done.get())
				Thread.sleep(500);
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		
	}
	
	public void lookAt(final double x, final double y, final double z) {
		onclient(new Callable<Void>() {public Void call() throws Exception {
			EntityPlayer pl = Minecraft.getMinecraft().thePlayer;
			
			double dx = x+XOFF - pl.posX, dy = y+YOFF - pl.posY, dz = z+ZOFF - pl.posZ;
			double scale = Math.sqrt(dx*dx + dy*dy + dz*dz);
			dx /= scale;
			dy /= scale;
			dz /= scale;
			
			pl.rotationPitch = -(float)(Math.asin(dy) * 180 / Math.PI);
			scale = Math.sqrt(dx*dx + dz*dz);
			dx /= scale;
			dz /= scale;
			
			pl.rotationYaw = -(float)(Math.atan2(dx, dz) * 180 / Math.PI);
			
			pl.prevRotationPitch = pl.rotationPitch;
			pl.prevRotationYaw = pl.rotationYaw;
            
            return null;
		}});
	}
	
	public void setkey(final String fieldName, final boolean state) {
		onclient(new Callable<Void>() {public Void call() throws Exception {
			GameSettings gs = Minecraft.getMinecraft().gameSettings;
			KeyBinding kb = (KeyBinding)gs.getClass().getField(fieldName).get(gs);
			KeyBinding.setKeyBindState(kb.keyCode, state);
			if(state)
				KeyBinding.onTick(kb.keyCode);
            
            return null;
		}});
	}
	
	public void setGameMode(final int i) {
		if(onclient(new Callable<Boolean>() {public Boolean call() throws Exception {
			int curID = ReflectionHelper.<EnumGameType, PlayerControllerMP>getPrivateValue(PlayerControllerMP.class, Minecraft.getMinecraft().playerController, "currentGameType").getID();
			if(curID != i)
				Minecraft.getMinecraft().thePlayer.sendQueue.addToSendQueue(new Packet3Chat("/gamemode "+i));
            
            return curID == i;
		}}))
			// already had the right gamemode
			return;
		
		while(!onclient(new Callable<Boolean>() {public Boolean call() throws Exception {
			return ReflectionHelper.<EnumGameType, PlayerControllerMP>getPrivateValue(PlayerControllerMP.class, Minecraft.getMinecraft().playerController, "currentGameType").getID() == i;
		}}));
	}
	
	// Returns the first event of the given class that was received,
	// and clears the received event list.
	public <T> T assertReceivedEvent(Class<T> clazz) {
		Event evt;
		if(plugin.receivedEvents.unhandledException != null) {
			Throwable e = plugin.receivedEvents.unhandledException;
			plugin.receivedEvents.unhandledException = null;
			
			if(e instanceof Error)
				throw ((Error)e);
			if(e instanceof RuntimeException)
				throw ((RuntimeException)e);
			throw new RuntimeException(e);
		}
		while((evt = plugin.receivedEvents.poll()) != null) {
			if(clazz.isAssignableFrom(evt.getClass())) {
				while(plugin.receivedEvents.poll() != null); // clear remaining events
				return clazz.cast(evt);
			}
		}
		throw new AssertionError("No "+clazz.getSimpleName()+" received");
	}
	
	// Returns the first event of the given class that was received that matches the given filter,
	// and clears the received event list.
	public <T extends Event> T assertReceivedEvent(Class<T> clazz, EventMatcher<T> filter) throws Exception {
		Event evt;
		if(plugin.receivedEvents.unhandledException != null) {
			Throwable e = plugin.receivedEvents.unhandledException;
			plugin.receivedEvents.unhandledException = null;
			
			if(e instanceof Error)
				throw ((Error)e);
			if(e instanceof RuntimeException)
				throw ((RuntimeException)e);
			throw new RuntimeException(e);
		}
		while((evt = plugin.receivedEvents.poll()) != null) {
			if(clazz.isAssignableFrom(evt.getClass()) && filter.matches(clazz.cast(evt))) {
				while(plugin.receivedEvents.poll() != null); // clear remaining events
				return clazz.cast(evt);
			}
		}
		throw new AssertionError("No matching "+clazz.getSimpleName()+" received");
	}
	
	public <T extends Event> T waitForEvent(Class<T> clazz, EventMatcher<T> filter) throws Exception {
		Event evt;
		while(true) {
			if(plugin.receivedEvents.unhandledException != null) {
				Throwable e = plugin.receivedEvents.unhandledException;
				plugin.receivedEvents.unhandledException = null;
				
				if(e instanceof Error)
					throw ((Error)e);
				if(e instanceof RuntimeException)
					throw ((RuntimeException)e);
				throw new RuntimeException(e);
			}
			while((evt = plugin.receivedEvents.poll()) != null) {
				if(clazz.isAssignableFrom(evt.getClass()) && filter.matches(clazz.cast(evt))) {
					while(plugin.receivedEvents.poll() != null); // clear remaining events
					return clazz.cast(evt);
				}
			}
			// wait for next tick
			onserver(new Callable<Void>() {public Void call() {return null;}});
		}
	}
	
	// like assertReceivedEvent but waits if no such event was received yet
	public <T> T waitForEvent(Class<T> clazz) {
		Event evt;
		while(true) {
			if(plugin.receivedEvents.unhandledException != null) {
				Throwable e = plugin.receivedEvents.unhandledException;
				plugin.receivedEvents.unhandledException = null;
				
				if(e instanceof Error)
					throw ((Error)e);
				if(e instanceof RuntimeException)
					throw ((RuntimeException)e);
				throw new RuntimeException(e);
			}
			while((evt = plugin.receivedEvents.poll()) != null) {
				if(clazz.isAssignableFrom(evt.getClass())) {
					while(plugin.receivedEvents.poll() != null); // clear remaining events
					return clazz.cast(evt);
				}
			}
			// wait for next tick
			onserver(new Callable<Void>() {public Void call() {return null;}});
		}
	}
	
	public void setBlock(final int x, final int y, final int z, final Block block, final int meta) {
		//System.out.println("set "+x+" "+y+" "+z+" "+block+" "+meta);
		affectedCoords.add(new Coords(x, y, z));
		onserver(new Callable<Void>() {public Void call() throws Exception {
			DimensionManager.getWorld(0).setBlockAndMetadataWithNotify(x+XOFF, y+YOFF, z+ZOFF, block == null ? 0 : block.blockID, meta);
			return null;
		}});
	}
	
	public void setBlock(final int x, final int y, final int z, final int block, final int meta) {
		setBlock(x, y, z, Block.blocksList[block], meta);
	}
	
	public int getBlock(final int x, final int y, final int z) {
		return onserver(new Callable<Integer>() {public Integer call() throws Exception {
			return MinecraftServer.getServer().worldServerForDimension(0).getBlockId(x+XOFF, y+YOFF, z+ZOFF);
		}});
	}
	
	public int getBlockMeta(final int x, final int y, final int z) {
		return onserver(new Callable<Integer>() {public Integer call() throws Exception {
			return MinecraftServer.getServer().worldServerForDimension(0).getBlockMetadata(x+XOFF, y+YOFF, z+ZOFF);
		}});
	}
	
	public int getClientBlock(final int x, final int y, final int z) {
		return onclient(new Callable<Integer>() {public Integer call() throws Exception {
			return Minecraft.getMinecraft().theWorld.getBlockId(x+XOFF, y+YOFF, z+ZOFF);
		}});
	}
	
	public void setDifficulty(final int setting) {
		onserver(new Callable<Void>() {public Void call() {
			DimensionManager.getWorld(0).difficultySetting = setting;
			return null;
		}});
	}
	
	public void assertCoordsEqual(org.bukkit.block.Block b, int x, int y, int z) {
		if(b.getX() != x+XOFF || b.getY() != y+YOFF || b.getZ() != z+ZOFF)
			throw new AssertionError("wrong coordinates, expected "+x+","+y+","+z+", got "+(b.getX()-XOFF)+","+(b.getY()-YOFF)+","+(b.getZ()-ZOFF));
	}
	
	public void setTimeMultiplier(int f) {
		TestMod.timeFactor = f;
	}
	
	public void setRaining(boolean state) {
		TestMod.shouldRain = state;
	}
	
	public void setHeldItem(final ItemStack is) {
		final String name = getPlayerUsername();
		onserver(new Callable<Void>() {public Void call() {
			EntityPlayer pl = MinecraftServer.getServer().getConfigurationManager().getPlayerForUsername(name);
			pl.inventory.setInventorySlotContents(0, is);
			return null;
		}});
		while(!onclient(new Callable<Boolean>() {public Boolean call() {
			EntityPlayer pl = Minecraft.getMinecraft().thePlayer;
			pl.inventory.currentItem = 0;
			return ItemStack.areItemStacksEqual(pl.inventory.getCurrentItem(), is);
		}}));
	}
	
	public void setEventHandler(EventCallback<Event> cb) {
		plugin.receivedEvents.allHandler = cb;
	}
	
	
	
	
	
	public void resetState() {
		// undo block changes
		onserver(new Callable<Void>() {public Void call() throws Exception {
			World w = MinecraftServer.getServer().worldServerForDimension(0);
			for(Coords c : affectedCoords) {
				w.setBlockWithNotify(c.x, c.y, c.z, 0);
			}
			return null;
		}});
		affectedCoords.clear();
		
		// undo setting changes
		TestMod.timeFactor = 1;
		TestMod.shouldRain = false;
		setGameMode(1);
		
		// undo event handler
		plugin.receivedEvents.allHandler = null;
		discardEvents();
	}
	
	
	
	
	public abstract void test() throws Exception;
	public void run() {
		try {
			// clear area
			for(int x = -5; x <= 5; x++)
				for(int y = -5; y <= 5; y++)
					for(int z = -5; z <= 5; z++)
						affectedCoords.add(new Coords(XOFF+x, YOFF+y, ZOFF+z));
			resetState();
			
			test();
			resetState();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
}