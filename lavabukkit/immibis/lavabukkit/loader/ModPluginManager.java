package immibis.lavabukkit.loader;

import immibis.lavabukkit.loader.ModPluginManager.PluginInfo;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minecraftforge.common.MinecraftForge;

import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import cpw.mods.fml.common.BukkitPluginRef;
import cpw.mods.fml.common.FMLModContainer;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;
import cpw.mods.fml.common.versioning.VersionParser;
import cpw.mods.fml.relauncher.FMLInjectionData;

public class ModPluginManager {
	
	private static Logger logger = Logger.getLogger("ModPluginLoader");
	
	public static class PluginInfo {
		public String fakeFilename;
		public Object mod;
		public FMLModContainer modContainer;
		public String ymlPath;
	}
	
	/** Refers to a mod class field that will be populated with a plugin reference */
	public static class PluginRef {
		public Class<?> clazz;
		public Field field;
		public Object instance;
		public String pluginName;
		public ArtifactVersion versionBounds;
		public FMLModContainer modContainer;
	}
	
	// maps fake filenames to plugin info
	static Map<String, PluginInfo> plugins = new HashMap<String, PluginInfo>();
	
	private static List<PluginRef> pluginRefs = new ArrayList<PluginRef>();
	
	public static void loadModPlugins(PluginManager pluginManager) throws InvalidDescriptionException, InvalidPluginException {
		pluginManager.registerInterface(ModPluginLoader.class);
		
		pluginRefs.clear();
		
		logger.fine("Finding mod plugins and plugin references");
		
		for(ModContainer container : Loader.instance().getActiveModList()) {
			if(container instanceof FMLModContainer) {
				logger.finer("Checking "+container.getName());
				
				Object mod = ((FMLModContainer)container).getMod();
				
				String ymlPath = mod.getClass().getAnnotation(Mod.class).bukkitPlugin();
				if(!ymlPath.equals("")) {
					String fakeFN = container.getModId().replace('/','_').replace('\\','_') + ".mod-plugin";
					
					logger.info("Loading mod plugin for "+container.getName());
					logger.finer("Mod plugin YML path: "+ymlPath);
					logger.finer("Mod plugin fake filename: "+fakeFN);
					
					PluginInfo info = new PluginInfo();
					info.fakeFilename = fakeFN;
					info.modContainer = (FMLModContainer)container;
					info.mod = mod;
					info.ymlPath = ymlPath;
					
					plugins.put(fakeFN, info);
					
					pluginManager.loadPlugin(new File(fakeFN));
				}
				
				for(Field f : mod.getClass().getFields()) {
					if(f.isAnnotationPresent(BukkitPluginRef.class)) {
						String ref = f.getAnnotation(BukkitPluginRef.class).value();
						
						PluginRef r = new PluginRef();
						r.clazz = mod.getClass();
						r.field = f;
						r.instance = mod;
						r.versionBounds = VersionParser.parseVersionReference(ref);
						r.pluginName = r.versionBounds.getLabel();
						r.modContainer = (FMLModContainer)container;
						pluginRefs.add(r);
					}
				}
			}
		}
	}

	public static File getDataFolder(PluginInfo pluginInfo) {
		return new File((File)FMLInjectionData.data()[6], "config");
	}

	public static void onServerStopping() {
		logger.finest("Clearing mod plugin references");
		for(PluginRef ref : pluginRefs) {
			try {
				ref.field.set(ref.instance, null);
			} catch(Exception t) {
				throw new RuntimeException("Failed to clear BukkitPluginRef in "+ref.modContainer.getName(), t);
			}
		}
		logger.fine("Cleared mod plugin references");
	}
	
	public static void onServerStarted() {
		for(PluginRef ref : pluginRefs) {
			try {
				ref.field.set(ref.instance, getPluginRef(ref.pluginName, ref.versionBounds));
			} catch(Exception t) {
				throw new RuntimeException("Failed to set BukkitPluginRef in "+ref.modContainer.getName(), t);
			}
		}
	}
	
	private static Object getPluginRef(String name, ArtifactVersion versionBounds) {
		Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
		if(plugin == null) {
			logger.info("Referenced plugin not found: "+name+" "+versionBounds.getVersionString());
			return null;
		}
		String pluginName = plugin.getName();
		String pluginVersion = plugin.getDescription().getVersion();
		if(!VersionParser.satisfies(versionBounds, new DefaultArtifactVersion(pluginName, pluginVersion))) {
			logger.info("Referenced plugin has unacceptable version: need "+pluginName+" "+versionBounds.getRangeString()+", got "+pluginVersion);
			return null;
		}
		logger.info("Referenced plugin: "+pluginName+" "+versionBounds.getRangeString()+" matched: "+pluginName+" "+pluginVersion);
		try {
			return plugin.getClass().getMethod("getModProxy").invoke(plugin);
		} catch(NoSuchMethodException e) {
			throw new RuntimeException("Referenced plugin has no getModProxy() method: "+pluginName+" "+pluginVersion, e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Referenced plugin has invalid getModProxy() method: "+pluginName+" "+pluginVersion, e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException("getModProxy threw exception. Plugin: "+pluginName+" "+pluginVersion, e);
		}
	}
}
