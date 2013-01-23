package immibis.lavabukkit.tests;

import immibis.lavabukkit.tests.block.*;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.packet.Packet3Chat;
import net.minecraft.network.packet.Packet4UpdateTime;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumGameType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import cpw.mods.fml.common.BukkitPluginRef;
import cpw.mods.fml.common.BukkitProxy;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.Mod.ServerStarted;
import cpw.mods.fml.common.Mod.ServerStarting;
import cpw.mods.fml.common.Mod.ServerStopping;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.RelaunchClassLoader;
import cpw.mods.fml.relauncher.Side;

@Mod(
	modid = "LavaBukkit Test Mod",
	name = "LavaBukkit Test Mod",
	bukkitPlugin = "/immibis/lavabukkit/tests/TestModPlugin.yml",
	version = "1.0"
)
public class TestMod {
	@Instance("LavaBukkit Test Mod")
	public static TestMod instance;
	
	
	
	@Init
	public void init(FMLInitializationEvent evt) {
		ReflectionHelper.<Set<String>,RelaunchClassLoader>getPrivateValue(
				RelaunchClassLoader.class,
				(RelaunchClassLoader)getClass().getClassLoader(),
				"classLoaderExceptions")
			.add("immibis.lavabukkit.tests.TestMain");
		
		if(!TestMain.isTesting)
			return;
		
		TickRegistry.registerTickHandler(new ClientTickHandler(), Side.CLIENT);
		TickRegistry.registerTickHandler(new ServerTickHandler(), Side.SERVER);
	}
	
	private static void startTests() {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				long startTime = System.currentTimeMillis();
				try {
					new TestBlockBreakEvent().run();
					new TestBlockBurnEvent().run();
					new TestBlockCanBuildEvent().run();
					new TestBlockDispenseEvent().run();
					new TestBlockFadeEvent().run();
					new TestBlockFormEvent().run();
					new TestBlockFromToEvent().run();
					new TestBlockGrowEvent().run();
					new TestBlockIgniteEvent().run();
					new TestBlockPlaceEvent().run();
					
					/* Remaining block events:
					BlockPhysicsEvent
					BlockPistonExtendEvent
					BlockPistonRetractEvent 	 
					BlockRedstoneEvent
					BlockSpreadEvent
					EntityBlockFormEvent
					LeavesDecayEvent
					NotePlayEvent
					SignChangeEvent
					*/
					
					System.out.println("Testing complete.");
					
					System.out.println("Finished testing in "+(System.currentTimeMillis() - startTime)+" ms");
					
					System.exit(0);
				} catch(Throwable t) {
					t.printStackTrace();
					System.out.println("Test failed after "+(System.currentTimeMillis() - startTime)+" ms");
					onserver(new Callable<Void>() {public Void call() {
						MinecraftServer.getServer().getConfigurationManager().sendPacketToAllPlayers(new Packet3Chat("Testing failed. See console for details."));
						return null;
					}});
				} 
			}
		});
		t.setName("LavaBukkit test thread");
		t.setDaemon(true);
		t.start();
	}
	
	private static Queue<Runnable> clientTaskQueue = new LinkedList<Runnable>();
	private static Queue<Runnable> serverTaskQueue = new LinkedList<Runnable>();
	
	private static Thread clientThread, serverThread;
	
	public static <T> T onclient(Callable<T> task) {
		if(Thread.currentThread() == clientThread) {
			try {
				return task.call();
			} catch(RuntimeException e) {
				throw e;
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		}
		FutureTask<T> ft = new FutureTask<T>(task);
		synchronized(clientTaskQueue) {
			clientTaskQueue.add(ft);
		}
		try {
			return ft.get();
		} catch(RuntimeException e) {
			throw e;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static <T> T onserver(Callable<T> task) {
		if(Thread.currentThread() == serverThread) {
			try {
				return task.call();
			} catch(RuntimeException e) {
				throw e;
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		}
		FutureTask<T> ft = new FutureTask<T>(task);
		synchronized(serverTaskQueue) {
			serverTaskQueue.add(ft);
		}
		try {
			return ft.get();
		} catch(RuntimeException e) {
			throw e;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static <T> Iterable<T> copyQueue(Queue<T> queue) {
		LinkedList<T> rv = new LinkedList<T>();
		synchronized(queue) {
			T t;
			while((t = queue.poll()) != null)
				rv.add(t);
		}
		return rv;
	}
	
	public static int timeFactor;
	public static boolean shouldRain;
	
	public static class ServerTickHandler implements ITickHandler {

		@Override
		public void tickStart(EnumSet<TickType> type, Object... tickData) {
			serverThread = Thread.currentThread();
		}

		@Override
		public void tickEnd(EnumSet<TickType> type, Object... tickData) {
			WorldServer w = DimensionManager.getWorld(0);
			// do (timeFactor - 1) ticks
			for(int k = 1; k < timeFactor; k++)
				w.tick();
			
			if(timeFactor > 1)
				MinecraftServer.getServer().getConfigurationManager().sendPacketToAllPlayers(new Packet4UpdateTime(w.getTotalWorldTime(), w.getWorldTime()));
			
			w.getWorldInfo().setRaining(shouldRain);
			w.getWorldInfo().setThundering(false);
			
			for(Runnable task : copyQueue(serverTaskQueue))
				task.run();
		}

		@Override
		public EnumSet<TickType> ticks() {
			return EnumSet.of(TickType.SERVER);
		}

		@Override
		public String getLabel() {
			return "LavaBukkit Tester Server";
		}
		
	}
	
	public static class ClientTickHandler implements ITickHandler {

		@Override
		public void tickStart(EnumSet<TickType> type, Object... tickData) {
			clientThread = Thread.currentThread();
		}
		
		private void pressGuiButton(int id) {
			GuiScreen screen = Minecraft.getMinecraft().currentScreen;
			List<GuiButton> buttons = ReflectionHelper.getPrivateValue(GuiScreen.class, screen, "controlList");
			for(GuiButton b : buttons) {
				if(b.id == id) {
					try {
						Method m = screen.getClass().getDeclaredMethod("actionPerformed", GuiButton.class);
						m.setAccessible(true);
						m.invoke(screen, b);
					} catch(Exception e) {
						throw new RuntimeException(e);
					}
					return;
				}
			}
			throw new IllegalArgumentException("No button with ID "+id+" in "+screen);
		}
		
		boolean loadedWorld;
		boolean startedTests;
		
		@Override
		public void tickEnd(EnumSet<TickType> type, Object... tickData) {
			/*if(type.contains(TickType.CLIENT))*/ {
				Minecraft mc = Minecraft.getMinecraft();
				if(mc.currentScreen instanceof GuiMainMenu) {
					pressGuiButton(1);
				} else if(mc.currentScreen instanceof GuiSelectWorld && !loadedWorld) {
					loadedWorld = true;
					((GuiSelectWorld)mc.currentScreen).selectWorld(0);
				} else if(mc.currentScreen instanceof GuiIngameMenu) {
					mc.gameSettings.pauseOnLostFocus=false;
					pressGuiButton(4);
				} else if(!startedTests && mc.theWorld != null && mc.currentScreen == null) {
					startedTests = true;
					startTests();
				}
				
				if(startedTests) {
					mc.inGameHasFocus = true;
					for(Runnable task : copyQueue(clientTaskQueue))
						task.run();
				}
			}
		}

		@Override
		public EnumSet<TickType> ticks() {
			return EnumSet.of(TickType.CLIENT);
		}

		@Override
		public String getLabel() {
			return "LavaBukkit Tester Client";
		}
		
	}
	
	
	
	
	
	/* Test plugin/mod life cycle */
	
	@BukkitPluginRef("LavaBukkit Test Mod Plugin")
	public static TestModPlugin plugin;
	
	@BukkitPluginRef("LavaBukkit Test Mod Plugin@1.0")
	public static BukkitProxy plugin2;
	
	@BukkitPluginRef("LavaBukkit Test Mod Plugin@(3.0,)")
	public static BukkitProxy plugin3;
	
	public static void log(String s) {
		System.err.println(s);
	}
	
	{
		assert instance == null : "Instance should not have been set here!";
		assert plugin == null : "Plugin should not have been created here!";
	}
	
	@PreInit
	public void lctest_preinit(FMLPreInitializationEvent evt) {
		assert plugin == null : "Plugin should not exist in @PreInit";
	}
	
	@Init
	public void lctest_init(FMLInitializationEvent evt) {
		assert plugin == null : "Plugin should not exist in @Init";
	}
	
	@PostInit
	public void lctest_postinit(FMLPostInitializationEvent evt) {
		assert plugin == null : "Plugin should not exist in @PostInit";
	}
	
	@ServerStarting
	public void lctest_starting(FMLServerStartingEvent evt) {
		assert plugin == null : "Plugin should not exist in @ServerStarting";
	}
	
	@ServerStarted
	public void lctest_started(FMLServerStartedEvent evt) {
		assert plugin != null && plugin2 == plugin : "Plugin should exist in @ServerStarted";
		assert plugin.isEnabled() : "Plugin should have been enabled when server started";
		assert plugin3 == null : "Plugin with wrong version should not exist";
	}
	
	@ServerStopping
	public void lctest_stopping(FMLServerStoppingEvent evt) {
		//assert plugin != null && plugin2 == plugin : "Plugin should exist in @ServerStopping";
		//assert !plugin.isEnabled() : "Plugin should have been disabled when server stopped";
		// TODO reenable me (this blocks actual crashes from being displayed)
	}
}
