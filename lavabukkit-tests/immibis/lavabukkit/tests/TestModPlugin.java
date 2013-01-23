package immibis.lavabukkit.tests;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.bukkit.Location;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import cpw.mods.fml.common.BukkitProxy;

import org.bukkit.event.block.*;

public class TestModPlugin extends JavaPlugin implements BukkitProxy {
	@Override
	public void onEnable() {
		getLogger().info("Plugin enabled");
		getServer().getPluginManager().registerEvents(new TMPListener(), this);
	}
	
	@Override
	public void onDisable() {
		getLogger().info("Plugin disabled");
	}
	
	public BukkitProxy getModProxy() {
		return this;
	}
	
	// synchronized wrapper of LinkedList
	public static class EventQueue {
		public Queue<Event> w = new LinkedList<Event>();
		
		public Throwable unhandledException;
		
		private Class handleType = null;
		private EventCallback handler = null;

		public EventCallback allHandler; 
		
		public synchronized void add(Event e) {
			try {
				if(allHandler != null)
					allHandler.handle(e);
				if(handleType != null && handleType.isAssignableFrom(e.getClass())) {
					handleType = null;
					handler.handle(e);
				}
			} catch(Throwable ex) {
				unhandledException = ex;
			}
			w.add(e);
		}
		public synchronized Event poll() {
			return w.poll();
		}
		public synchronized void runOnNextEvent(Class<? extends Event> class1, EventCallback<? extends Event> handler) {
			if(handleType != null)
				throw new IllegalStateException("Already going to handle the next event specially");
			handleType = class1;
			this.handler = handler;
		}
	}
	public EventQueue receivedEvents = new EventQueue();
	
	public class TMPListener implements Listener {
		@EventHandler public void log(BlockBreakEvent evt) {receivedEvents.add(evt);}
		@EventHandler public void log(BlockBurnEvent evt) {receivedEvents.add(evt);}
		@EventHandler public void log(BlockCanBuildEvent evt) {receivedEvents.add(evt);}
		@EventHandler public void log(BlockDamageEvent evt) {receivedEvents.add(evt);}
		@EventHandler public void log(BlockDispenseEvent evt) {receivedEvents.add(evt);}
		@EventHandler public void log(BlockExpEvent evt) {receivedEvents.add(evt);}
		@EventHandler public void log(BlockFadeEvent evt) {receivedEvents.add(evt);}
		@EventHandler public void log(BlockFormEvent evt) {receivedEvents.add(evt);}
		@EventHandler public void log(BlockFromToEvent evt) {receivedEvents.add(evt);}
		@EventHandler public void log(BlockGrowEvent evt) {receivedEvents.add(evt);}
		@EventHandler public void log(BlockIgniteEvent evt) {receivedEvents.add(evt);}
		@EventHandler public void log(BlockPhysicsEvent evt) {receivedEvents.add(evt);}
		@EventHandler public void log(BlockPistonExtendEvent evt) {receivedEvents.add(evt);}
		@EventHandler public void log(BlockPistonRetractEvent evt) {receivedEvents.add(evt);}
		@EventHandler public void log(BlockRedstoneEvent evt) {receivedEvents.add(evt);}
		@EventHandler public void log(BlockSpreadEvent evt) {receivedEvents.add(evt);}
		@EventHandler public void log(EntityBlockFormEvent evt) {receivedEvents.add(evt);}
		@EventHandler public void log(LeavesDecayEvent evt) {receivedEvents.add(evt);}
		@EventHandler public void log(NotePlayEvent evt) {receivedEvents.add(evt);}
		@EventHandler public void log(SignChangeEvent evt) {receivedEvents.add(evt);}
		@EventHandler public void log(BlockPlaceEvent evt) {receivedEvents.add(evt);}
	}
}
