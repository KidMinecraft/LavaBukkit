package immibis.lavabukkit.tools.cbmapper;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class MapWriter {

	public static void write(File outDir, String mcVer, Map<MappableObject, MappableObject> mappedObjects) throws Exception {
		PrintWriter out = new PrintWriter(new File(outDir, mcVer + "-cb.txt"));
		
		// maps mcdev class names -> vanilla class names
		Map<String, String> classes = new HashMap<String, String>();
		
		// maps mcdev class names -> (mcdev method/field -> vanilla method/field)
		Map<String, Map<MappableObject, MappableObject>> objects = new HashMap<String, Map<MappableObject, MappableObject>>();
		
		for(Map.Entry<MappableObject, MappableObject> e : mappedObjects.entrySet()) {
			MappableObject m = e.getKey();
			MappableObject v = e.getValue();
			if(!m.is_mcdev) continue;
			
			if(m instanceof ClassObject) {
				classes.put(((ClassObject)m).cn.name, ((ClassObject)v).cn.name);
				String cn = ((ClassObject)m).cn.name;
				if(!objects.containsKey(cn))
					objects.put(cn, new HashMap<MappableObject, MappableObject>());
			} else {
				ClassObject cl = null;
				if(m instanceof FieldObject)
					cl = ((FieldObject)m).cl;
				else if(m instanceof MethodObject)
					cl = ((MethodObject)m).cl;
				
				Map<MappableObject, MappableObject> map = objects.get(cl.cn.name);
				if(map == null)
					objects.put(cl.cn.name, map = new HashMap<MappableObject, MappableObject>());
				
				map.put(m, v);
			}
		}
		
		for(Map.Entry<String, String> e : classes.entrySet()) {
			out.println(e.getKey()+" "+e.getValue());
			for(Map.Entry<MappableObject, MappableObject> e2 : objects.get(e.getKey()).entrySet()) {
				MappableObject left = e2.getKey();
				MappableObject right = e2.getValue();
				
				String ls = null, rs = null;
				
				if(left instanceof FieldObject) {
					ls = ((FieldObject)left).fn.name;
					rs = ((FieldObject)right).fn.name;
				} else if(left instanceof MethodObject) {
					ls = ((MethodObject)left).mn.name + ((MethodObject)left).mn.desc;
					rs = ((MethodObject)right).mn.name + ((MethodObject)right).mn.desc;
				}
				out.println("  " + ls + " " + rs);
			}
			out.println();
		}
		
		out.close();
	}

}
