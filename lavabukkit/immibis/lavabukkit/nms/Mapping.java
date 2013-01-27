package immibis.lavabukkit.nms;

import immibis.lavabukkit.asm.Field;
import immibis.lavabukkit.asm.Method;
import immibis.lavabukkit.asm.NaughtyPluginTransformer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Scanner;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * A Mapping is a map that translates field, method and class names from one
 * name-set to another - for example, from CraftBukkit names to MCP names.
 * 
 * Methods of Mapping will <b>not</b> translate objects that are not specifically
 * included in their data. For example, Forge methods have the same names in every
 * name-set, but can be included in vanilla classes which do not. In this case,
 * mapMethod would not translate the class name. Owner names and method descriptors
 * returned from mapField and mapMethod should not be used.
 *
 */
public interface Mapping {

	String mapClass(String c);
	Field mapField(Field f);
	Method mapMethod(Method m);
	
	Mapping reverse();
	
	
	
}
