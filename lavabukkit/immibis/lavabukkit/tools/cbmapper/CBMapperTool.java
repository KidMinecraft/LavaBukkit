package immibis.lavabukkit.tools.cbmapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.objectweb.asm.Type;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

/**
 * This code is a mess, but apparently it works.
 * 
 * Some hints:
 * <ul>
 * 
 * <li> An object is a method, class or field from either mcdev or vanilla.
 * 
 * <li> The mapper works by matching properties of objects that do not change under obfuscation.
 * This only works because mcdev has not been altered except for its deobfuscation. If it had
 * even been recompiled, this would probably fail.
 * 
 * <li> In each iteration, objects get signatures. Signatures encode said properties,
 * in some arbitrary form (they're basically just a bunch of things concatenated together
 * with delimiters). This is the selfSignature part of the signature.
 * 
 * <li> To handle the case when two similar objects are used in different places, there are
 * references. This iteration's fullSignature of all references are added to the selfSignature
 * to form the fullSignature used for that object in the next iteration. A reference is just
 * any object - its only purpose is to uniquify things based on where they're used. They're also
 * very ad-hoc like the selfSignatures.
 * 
 * <li> After iterating normally results in no extra mappings being found, we do one iteration
 * where the names are part of the signatures, then one iteration where the indices in the file
 * are part of the signatures. Names don't always match, and file position could be unreliable,
 * which is why these are a last resort.
 * 
 * <li> The result is a Map which maps vanilla to mcdev objects and vice versa.
 * This is passed to MapWriter which <s>does sanity checks and</s> creates the final output file. 
 * 
 * </ul>
 * 
 * 
 */

public class CBMapperTool {
	
	public static File downloadOrCacheFile(String name, String url) throws Exception {
		File file = new File(name);
		if(file.exists())
			return file;
		
		System.out.println("downloading "+url);
		
		File tempfile = new File(name + ".temp");
		
		FileOutputStream out = new FileOutputStream(tempfile);
		InputStream in = new URL(url).openStream();
		byte[] buffer = new byte[65536];
		while(true) {
			int read = in.read(buffer);
			if(read < 0)
				break;
			out.write(buffer, 0, read);
		}
		
		in.close();
		out.close();
		
		if(!tempfile.renameTo(file))
			throw new Exception("failed to rename "+tempfile+" to "+file);
		return file;
	}
	
	public static List<MappableObject> getObjects(File f, boolean is_mcdev) throws Exception {
		List<MappableObject> rv = new ArrayList<MappableObject>();
		for(ClassObject ci : new ClassSet(new ZipFile(new FileInputStream(f))).classes.values()) {
			rv.add(ci);
			rv.addAll(ci.methods);
			rv.addAll(ci.fields);
		}
		for(MappableObject o : rv)
			o.is_mcdev = is_mcdev;
		return rv;
	}
	
	public static Map<MappableObject, MappableObject> mappedObjects = new HashMap<MappableObject, MappableObject>();
	
	public static Set<MappableObject> unmappedObjects = new HashSet<MappableObject>();
	public static Set<MappableObject> allObjects = new HashSet<MappableObject>();
	
	public static void main(String[] args) throws Exception {
		
		if(args.length < 2) {
			System.err.println("Requires Minecraft version and output directory on command line.");
			return;
		}
		
		String mcVer = args[0];
		File outDir = new File(args[1]);
		
		File mcdev = downloadOrCacheFile("mcdev-"+mcVer+".jar", "http://repo.bukkit.org/content/repositories/releases/org/bukkit/minecraft-server/"+mcVer+"/minecraft-server-"+mcVer+".jar");
		File vanilla = downloadOrCacheFile("minecraft_server-"+mcVer+".jar", "http://assets.minecraft.net/"+mcVer.replace('.','_')+"/minecraft_server.jar");
		
		unmappedObjects.addAll(getObjects(mcdev, true));
		unmappedObjects.addAll(getObjects(vanilla, false));
		allObjects.addAll(unmappedObjects);
		
		int numPairs = unmappedObjects.size() / 2;
		
		
		
		long startTime, endTime;
		
		long totalStartTime = System.currentTimeMillis();
		
		int numResolved, totalResolved = 0;
		int iterNum = 0;
		do {
			startTime = System.currentTimeMillis();
			
			numResolved = doMappingStep(true);
			totalResolved += numResolved;
			
			endTime = System.currentTimeMillis();
			iterNum++;
			System.out.println("Iteration "+iterNum+", time "+(endTime - startTime)+" ms, resolved "+numResolved);
		} while(numResolved != 0);
		
		// try matching names, catches a lot of CB undeobfuscated names that otherwise aren't mapped
		MethodObject.includeNames = true;
		FieldObject.includeNames = true;
		{
			startTime = System.currentTimeMillis();
			
			numResolved = doMappingStep(true);
			totalResolved += numResolved;
			
			endTime = System.currentTimeMillis();
			iterNum++;
			System.out.println("Name iteration, time "+(endTime - startTime)+" ms, resolved "+numResolved);
		}
		
		MethodObject.includeNames = false;
		MethodObject.includeIndices = true;
		{
			startTime = System.currentTimeMillis();
			
			numResolved = doMappingStep(false);
			totalResolved += numResolved;
			
			endTime = System.currentTimeMillis();
			iterNum++;
			System.out.println("Index iteration, time "+(endTime - startTime)+" ms, resolved "+numResolved);
		}
		
		long totalEndTime = System.currentTimeMillis();
		System.out.println("Finished in "+(totalEndTime-totalStartTime)+" ms");
		System.out.println("Mapped "+totalResolved+" of "+numPairs+" pairs");
		
		if(totalResolved < numPairs)
		{
			System.out.println("Dumping unmapped objects to unmapped.log");
			
			PrintWriter pw = new PrintWriter(new FileWriter("unmapped.log"));
			
			ListMultimap<String, MappableObject> objectsBySignature = ArrayListMultimap.create();
			for(MappableObject mr : unmappedObjects)
				objectsBySignature.put(mr.toString(), mr);
			
			for(String s : objectsBySignature.keySet()) {
				List<MappableObject> objects = objectsBySignature.get(s);
				pw.println("Final signature: "+s);
				for(MappableObject m : objects)
					pw.println("  " + m.toHumanString());
				pw.println();
			}
			
			pw.close();
		}
		
		MapWriter.write(outDir, mcVer, mappedObjects);
	}
	
	private static int nextCompactedSig = 0;
	private static Map<String, String> compactedSigs = new HashMap<String, String>();
	private static String compactSig(String sig) {
		if(sig.length() < 1000)
			return sig;
		
		String rv = compactedSigs.get(sig);
		if(rv != null)
			return rv;
		
		rv = "CS" + (nextCompactedSig++);
		compactedSigs.put(sig, rv);
		return rv;
	}
	
	public static int doMappingStep(boolean useRefs) {
		ListMultimap<String, MappableObject> objectsBySignature = ArrayListMultimap.create();
		int numResolved = 0;
		
		for(MappableObject mr : mappedObjects.values()) {
			MappableObject base = mr.is_mcdev ? mr : mappedObjects.get(mr);
			mr.selfSignature = mr.fullSignature = base.toHumanString()+" ("+mappedObjects.get(base).toHumanString()+")";
		}
		
		for(MappableObject mr : unmappedObjects) {
			mr.updateSelfSignature();
			mr.fullSignature = null;
		}
		
		for(MappableObject mr : allObjects)
			mr.referenceSet.clear();
		
		if(useRefs)
			for(MappableObject mr : allObjects)
				mr.addReferences();
		
		for(MappableObject mr : unmappedObjects)
			mr.updateNextSignature();
		
		for(MappableObject mr : unmappedObjects) {
			mr.lastSignature = compactSig(mr.nextSignature);
			mr.fullSignature = null;
		}
		
		for(MappableObject mr : unmappedObjects) {
			
			//if(mr instanceof ClassObject)
			//	System.out.println(mr.toHumanString()+": "+mr);
			objectsBySignature.put(mr.toString(), mr);
		}
		
		for(String s : objectsBySignature.keySet()) {
			List<MappableObject> objects = objectsBySignature.get(s);
			if(objects.size() == 2) {
				numResolved++;
				MappableObject a = objects.get(0);
				MappableObject b = objects.get(1);
				if(a.is_mcdev == b.is_mcdev) {
					System.err.println("Sig: "+s);
					System.err.println("Both from "+(a.is_mcdev ? "mcdev" : "vanilla"));
					System.err.println("A: "+a.toHumanString());
					System.err.println("B: "+b.toHumanString());
					throw new RuntimeException("mapping error; two objects match but are from the same source");
				}
				unmappedObjects.remove(a);
				unmappedObjects.remove(b);
				//System.out.println("mapped "+a.toHumanString()+", "+b.toHumanString());
				mappedObjects.put(a, b);
				mappedObjects.put(b, a);
			}
		}
		
		return numResolved;
	}
}
