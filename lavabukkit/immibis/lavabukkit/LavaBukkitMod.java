package immibis.lavabukkit;

import immibis.lavabukkit.asm.HookInsertionTransformer;
import immibis.lavabukkit.asm.PluginClassLoaderTransformer;
import immibis.lavabukkit.dispenser.DispenseBoat;
import immibis.lavabukkit.dispenser.DispenseBucketEmpty;
import immibis.lavabukkit.dispenser.DispenseBucketFull;
import immibis.lavabukkit.dispenser.DispenseFireball;
import immibis.lavabukkit.dispenser.DispenseFirework;
import immibis.lavabukkit.dispenser.DispenseMinecart;
import immibis.lavabukkit.dispenser.DispenseSpawnEgg;
import immibis.lavabukkit.world.BukkitWorldProvider;
import immibis.lavabukkit.world.WorldSaveHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.EnumSet;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Logger;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDispenseFirework;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemColored;
import net.minecraft.potion.Potion;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.CraftCrashReport;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.potion.CraftPotionEffectType;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffectType;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.Mod.ServerStarting;
import cpw.mods.fml.common.Mod.ServerStopping;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.ReflectionHelper;

@Mod(modid = LavaBukkitMod.MODID, name = LavaBukkitMod.NAME, version = LavaBukkitMod.VERSION)
public class LavaBukkitMod implements ITickHandler {
	
	// All suggested names:
	// * iBukkit
	// * LavaBukkit
	// * Better Than Porting
	// * Fukkit
	// * DireBukkit (?)
	// * ForgeLiquidContainerMadeOfIron
	// * Liminality
	// * ForgeFlowerPot
	// * ForgeCanister
	// * ForgeWaxCapsule
	// * EmptyKapsule
	
	public static final String NAME = "LavaBukkit";
	public static final String MODID = "LavaBukkit";
	public static final String VERSION = "dev";
	
	public static final boolean DEBUG_MODE = Block.class.getName().equals("net.minecraft.src.Block");
	
	public static final int LB_PROVIDER_TYPE_ID = "LavaBukkit".hashCode();

	public void fixLogging() {
		try {
			Class<? extends Formatter> FMLLogFormatter = Class.forName("cpw.mods.fml.relauncher.FMLLogFormatter").asSubclass(Formatter.class);
			Constructor<? extends Formatter> constructor = FMLLogFormatter.getDeclaredConstructor();
			constructor.setAccessible(true);
			Formatter formatter = constructor.newInstance();
			
	        Logger globalLogger = Logger.getLogger("");
	        for(Handler h : globalLogger.getHandlers())
	        	globalLogger.removeHandler(h);
	        
	        
	        Class<?> ConsoleLogThread = Class.forName("cpw.mods.fml.relauncher.FMLRelaunchLog$ConsoleLogThread");
	        Field wrappedHandler = ConsoleLogThread.getDeclaredField("wrappedHandler");
	        wrappedHandler.setAccessible(true);
	        ConsoleHandler h = (ConsoleHandler)wrappedHandler.get(null);
	        
	        //h.setFormatter(formatter);
	        globalLogger.addHandler(h);
	        
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@PreInit
	public void preInit(FMLPreInitializationEvent evt) {
		fixLogging();
		fixItemBlocks();
		
		try {
			PluginClassLoaderTransformer.guavaLoader = new URLClassLoader(new URL[] {
				getPluginGuavaFile().toURI().toURL()
			}, null);
		} catch(MalformedURLException e) {
			throw new RuntimeException(e);
		}
		
		HookInsertionTransformer.loadAllHookedClasses();
		
		
	}
	
	@Init
	public void init(FMLInitializationEvent evt) {
		DimensionManager.registerProviderType(LB_PROVIDER_TYPE_ID, BukkitWorldProvider.class, true);
		
		MinecraftForge.EVENT_BUS.register(new WorldSaveHandler());
		
		FMLCommonHandler.instance().registerCrashCallable(new CraftCrashReport());
	}
	
	@PostInit
	public void postInit(FMLPostInitializationEvent evt) {
		addMaterials();
		registerPotions();
		registerEnchantments();
		
		TickRegistry.registerTickHandler(this, Side.SERVER);
	}
	
	private void registerEnchantments() {
		for(Enchantment e : Enchantment.enchantmentsList)
			if(e != null)
				org.bukkit.enchantments.Enchantment.registerEnchantment(new CraftEnchantment(e)); // CraftBukkit
		
        org.bukkit.enchantments.Enchantment.stopAcceptingRegistrations();
	}
	
	private void registerPotions() {
		// fix wrong array length in Bukkit API - access transformer makes this non-final
		ReflectionHelper.setPrivateValue(PotionEffectType.class, null, new PotionEffectType[Potion.potionTypes.length], "byId");
		
		for(Potion p : Potion.potionTypes)
			if(p != null)
				PotionEffectType.registerPotionEffectType(new CraftPotionEffectType(p)); // CraftBukkit
		
        PotionEffectType.stopAcceptingRegistrations();
	}
	
	private void fixItemBlocks() {
		Item.itemsList[Block.mushroomCapBrown.blockID] = null;
        Item.itemsList[Block.mushroomCapBrown.blockID] = new ItemColored(Block.mushroomCapBrown.blockID - 256, true);
        Item.itemsList[Block.mushroomCapRed.blockID] = null;
        Item.itemsList[Block.mushroomCapRed.blockID] = new ItemColored(Block.mushroomCapRed.blockID - 256, true);
        Item.itemsList[Block.mobSpawner.blockID] = null;
        Item.itemsList[Block.mobSpawner.blockID] = new ItemColored(Block.mobSpawner.blockID - 256, true);
	}
	
	private void addMaterials() {
		try {
			Method constructor = Material.class.getDeclaredMethod("__fbCreateInstance", String.class, int.class, int.class, int.class, int.class, Class.class);
			
			int nextOrdinal = Material.values().length;
			int firstNew = nextOrdinal;
			
			for(Item i : Item.itemsList) {
				if(i == null)
					continue;
				if(Material.getMaterial(i.itemID) != null)
					continue;
				
				String name = "X" + i.itemID;
				
				constructor.invoke(null, name, nextOrdinal++, i.itemID, i.getItemStackLimit(), i.getMaxDamage(), MaterialData.class); 
			}
			
			Material[] values = Material.values();
			Material[] byID = ReflectionHelper.getPrivateValue(Material.class, null, "byId");
			Map<String, Material> BY_NAME = ReflectionHelper.getPrivateValue(Material.class, null, "BY_NAME");
			for(int i = firstNew; i < nextOrdinal; i++) {
				Material m = values[i];
				byID[m.getId()] = m;
				BY_NAME.put(m.name().toUpperCase(), m);
			}
			
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private int currentTick;
	
	private boolean isFirstServerStart = true;
	@ServerStarting
	public void serverStarting(FMLServerStartingEvent evt) {
		if(isFirstServerStart) {
			isFirstServerStart = false;
		}
		
		currentTick = 0;
	}
	
	@ServerStopping
	public void serverStopping(FMLServerStoppingEvent evt) {
		
	}
	
	
	

	@Override
	public void tickStart(EnumSet<TickType> type, Object... tickData) {
	}

	@Override
	public void tickEnd(EnumSet<TickType> type, Object... tickData) {
		currentTick++;
		((CraftServer)Bukkit.getServer()).getScheduler().mainThreadHeartbeat(currentTick);
	}

	@Override
	public EnumSet<TickType> ticks() {
		return EnumSet.of(TickType.SERVER);
	}

	@Override
	public String getLabel() {
		return NAME;
	}
	
	private static void extract(String respath, File file) {
		InputStream in = LavaBukkitMod.class.getResourceAsStream(respath);
		if(in == null)
			throw new RuntimeException(respath+" is missing from classpath, cannot extract");
		
		try {
			FileOutputStream out = new FileOutputStream(file);
			
			byte[] buffer = new byte[131072];
			int read;
			while(true) {
				read = in.read(buffer);
				if(read < 0)
					break;
				out.write(buffer, 0, read);
			}
			
			in.close();
			out.close();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static File getPluginGuavaFile() {
		File f = new File(FMLInjectionData.minecraftHome, "lib/guava-10.0.1.jar");
		if(f.isFile())
			return f;
		
		extract("/libs/guava-10.0.1.jar", f);
		
		return f;
	}

	public static void registerDispenseBehaviours() {
		DispenseBucketFull bucketFullDB = new DispenseBucketFull();
		DispenseMinecart minecartDB = new DispenseMinecart();
		
		BlockDispenser.dispenseBehaviorRegistry.putObject(Item.boat, new DispenseBoat());
		BlockDispenser.dispenseBehaviorRegistry.putObject(Item.bucketEmpty, new DispenseBucketEmpty());
		BlockDispenser.dispenseBehaviorRegistry.putObject(Item.bucketWater, bucketFullDB);
		BlockDispenser.dispenseBehaviorRegistry.putObject(Item.bucketLava, bucketFullDB);
		BlockDispenser.dispenseBehaviorRegistry.putObject(Item.fireballCharge, new DispenseFireball());
        BlockDispenser.dispenseBehaviorRegistry.putObject(Item.minecartEmpty, minecartDB);
		BlockDispenser.dispenseBehaviorRegistry.putObject(Item.minecartCrate, minecartDB);
		BlockDispenser.dispenseBehaviorRegistry.putObject(Item.minecartPowered, minecartDB);
		BlockDispenser.dispenseBehaviorRegistry.putObject(Item.monsterPlacer, new DispenseSpawnEgg());
		BlockDispenser.dispenseBehaviorRegistry.putObject(Item.field_92052_bU, new DispenseFirework());
	}
}
