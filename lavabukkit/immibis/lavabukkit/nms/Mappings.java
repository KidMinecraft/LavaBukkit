package immibis.lavabukkit.nms;

import immibis.lavabukkit.asm.Field;
import immibis.lavabukkit.asm.HookInsertionTransformer;
import immibis.lavabukkit.asm.TransformingURLClassLoader;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Multimap;

import cpw.mods.fml.relauncher.RelaunchClassLoader;

public class Mappings {
	public static Mapping CB_to_obf;
	public static Mapping MCP_to_obf;
	public static Mapping obf_to_current;
	public static Mapping CB_to_current;
	public static Mapping MCP_to_current;
	public static Mapping current_to_MCP;
	
	public static boolean RUNNING_IN_MCP;
	
	public static final String CURRENT_VERSION_CB = "1.4.6";
	public static final String CURRENT_VERSION_MCP = "1.4.7";
	public static final String CB_VERSIONED_PACKAGE_NAME = "v1_4_R1";
	
	static {
		try {
			RelaunchClassLoader cl = (RelaunchClassLoader)Mappings.class.getClassLoader();
			RUNNING_IN_MCP = cl.getSources().get(0).toString().endsWith("/");
			
			CB_to_obf = TableMapping.fromResource("/immibis/lavabukkit/nms/" + CURRENT_VERSION_CB + "-cb.txt");
			MCP_to_obf = TableMapping.fromResource("/immibis/lavabukkit/nms/" + CURRENT_VERSION_MCP + "-mcp.txt");
			
			CB_to_obf = join(new StripVersionMapping(CB_VERSIONED_PACKAGE_NAME), CB_to_obf);
			
			if(RUNNING_IN_MCP)
				obf_to_current = MCP_to_obf.reverse();
			else
				obf_to_current = identity();
			
			CB_to_current = join(CB_to_obf, obf_to_current);
			MCP_to_current = join(MCP_to_obf, obf_to_current);
			
			current_to_MCP = MCP_to_current.reverse();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String mapTypeDescriptor(Mapping m, String desc) {
		if(desc.startsWith("["))
			return "[" + mapTypeDescriptor(m, desc.substring(1));
		if(desc.startsWith("L") && desc.endsWith(";"))
			return "L" + m.mapClass(desc.substring(1, desc.length() - 1)) + ";";
		if(desc.length() == 1 && "BSIJFDCZV".contains(desc))
			return desc;
		return m.mapClass(desc);
	}
	
	public static String mapMethodDescriptor(Mapping m, String desc) {
		StringBuilder rv = new StringBuilder();
		int pos = 0;
		if(!desc.startsWith("("))
			throw new IllegalArgumentException("Invalid method descriptor: "+desc);
		else {
			pos++;
			rv.append("(");
		}
		
		while(pos < desc.length()) {
			switch(desc.charAt(pos++)) {
			case 'L':
				int i = desc.indexOf(';', pos);
				if(i < 0)
					throw new IllegalArgumentException("Invalid method descriptor: "+desc);
				rv.append('L');
				rv.append(m.mapClass(desc.substring(pos, i)));
				rv.append(';');
				pos = i+1;
				break;
				
			default:
				rv.append(desc.charAt(pos - 1));
			}
		}
		return rv.toString();
	}
	
	public static Mapping join(Mapping a, Mapping b) {
		return new ChainedMapping(a, b);
	}
	
	public static Mapping identity() {
		return new IdentityMapping();
	}

	
	
	
	
	
	private static class SuperclassCache {
		public Multimap<String, String> supers = ArrayListMultimap.create();
		public Set<String> unknownSupers = new HashSet<String>();
	}
	
	private static WeakHashMap<Mapping, SuperclassCache> cachedSuperclasses = new WeakHashMap<Mapping, SuperclassCache>();
	
	/**
	 * Finds the classes a class inherits from.
	 * @param c The class name in internal form (with /)
	 * @param cl The classloader to use.
	 * @param to_current A mapping from the class names used in f, to the current class names.
	 * @param from_current The reverse of to_current.
	 * @return A collection of the superclass of c (if known) and all interfaces it implements (if known), which could be empty.
	 */
	public static Collection<String> getDirectSuperclasses(String c, IClassAccessor cl, Mapping to_current, Mapping from_current) {
		SuperclassCache cache = cachedSuperclasses.get(to_current);
		if(cache == null)
			cachedSuperclasses.put(to_current, cache = new SuperclassCache());
		
		if(cache.supers.containsKey(c))
			return cache.supers.get(c);
		
		if(cache.unknownSupers.contains(c))
			return Collections.emptyList();
		
		byte[] bytes = null;
		
		try {
			bytes = cl.getUntransformedClassBytes(to_current.mapClass(c).replace('/', '.'));
		} catch(ClassNotFoundException e) {
		}
		
		if(bytes == null) {
			cache.unknownSupers.add(c);
			return Collections.emptyList();
		}
		ClassNode cn = new ClassNode();
		new ClassReader(bytes).accept(cn, ClassReader.SKIP_CODE);
		
		if(cn.superName != null)
			cache.supers.put(c, from_current.mapClass(cn.superName));
		if(cn.interfaces != null)
			for(String i : (List<String>)cn.interfaces)
				cache.supers.put(c, from_current.mapClass(i));
		return cache.supers.get(c);
	}
	
	/**
	 * Resolves a field by finding it in the superclasses of the given owner class.
	 * @param f The field.
	 * @param cl The classloader to use to find superclasses.
	 * @param to_current A mapping from the class names used in f, to the current class names.
	 * @param from_current The reverse of to_current.
	 * @return The resolved field, or f if the field could not be resolved.
	 */
	public static Field resolveField(Field f, IClassAccessor cl, Mapping to_current, Mapping from_current) {
		Field rv = resolveField2(f, cl, to_current, from_current);
		return rv == null ? f : rv;
	}
	
	// This should really check descriptor information too (to match the JVM exactly),
	// but that information isn't present in MCP's SRG or CSV files.
	// TODO: extract it from minecraft.jar and craftbukkit.jar
	private static Field resolveField2(Field f, IClassAccessor cl, Mapping to_current, Mapping from_current) {
		if(fieldExists(to_current.mapField(f), cl))
			return f;
		
		for(String s : getDirectSuperclasses(f.owner, cl, to_current, from_current)) {
			Field sf = resolveField2(new Field(s, f.name), cl, to_current, from_current);
			if(sf != null)
				return sf;
		}
		return null;
	}
	
	public static boolean fieldExists(Field f, IClassAccessor cl) {
		try {
			byte[] bytes = cl.getUntransformedClassBytes(f.owner.replace('/','.'));
			ClassNode cn = new ClassNode();
			new ClassReader(bytes).accept(cn, ClassReader.SKIP_CODE);
			for(FieldNode fn : (List<FieldNode>)cn.fields)
				if(fn.name.equals(f.name))
					return true;
			return false;
		} catch(ClassNotFoundException e) {
			return false;
		}
	}
}