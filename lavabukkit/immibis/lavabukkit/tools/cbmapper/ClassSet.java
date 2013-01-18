package immibis.lavabukkit.tools.cbmapper;

import java.util.HashMap;
import java.util.Map;

public class ClassSet {
	// maps class names (with /) to classes 
	public Map<String, ClassObject> classes = new HashMap<String, ClassObject>();
	
	public ClassSet(ZipFile zf) {
		for(Map.Entry<String, byte[]> file : zf.files.entrySet()) {
			String fn = file.getKey();
			if(!fn.endsWith(".class"))
				continue;
			
			byte[] data = file.getValue();
			classes.put(fn.substring(0, fn.length()-6), new ClassObject(this, data));
		}
	}
}
