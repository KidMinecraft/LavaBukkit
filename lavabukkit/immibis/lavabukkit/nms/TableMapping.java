package immibis.lavabukkit.nms;

import immibis.lavabukkit.asm.Field;
import immibis.lavabukkit.asm.Method;

import java.io.IOException;
import java.util.Scanner;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class TableMapping implements Mapping {
	private BiMap<Field, Field> fields = HashBiMap.create();
	private BiMap<Method, Method> methods = HashBiMap.create();
	private BiMap<String, String> classes = HashBiMap.create();
	
	
	
	@Override
	public Field mapField(Field f) {
		Field f2 = fields.get(f);
		return f2 == null ? f : f2;
	}
	
	@Override
	public Method mapMethod(Method m) {
		Method m2 = methods.get(m);
		return m2 == null ? m : m2;
	}
	
	@Override
	public String mapClass(String c) {
		if(c.startsWith("[")) {
			return "[" + mapClass(c.substring(1));
		}
		if(c.startsWith("L") && c.endsWith(";")) {
			return "L" + mapClass(c.substring(1, c.length() - 1)) + ";";
		}
		String c2 = classes.get(c);
		return c2 == null ? c : c2;
	}
	
	
	
	
	public static Mapping fromResource(String respath) throws IOException {
		Scanner s = new Scanner(Mapping.class.getResourceAsStream(respath));
		
		String curClassFrom = null, curClassTo = null;
		
		TableMapping m = new TableMapping();
		
		while(s.hasNext()) {
			String from = s.next();
			String to = s.next();
			
			if(to.equals("@"))
				to = from;
			
			if(from.contains("/") && !from.contains("(")) {
				// it's a class
				m.classes.put(from, to);
				curClassFrom = from;
				curClassTo = to;
			} else if(from.contains("(")) {
				// it's a method
				// shortcut: use @ for the descriptor if the descriptor does not change
				if(to.endsWith("@"))
					to = to.substring(0, to.length() - 1) + from.substring(from.indexOf('('));
				
				to = curClassTo + "/" + to;
				from = curClassFrom + "/" + from;
				
				m.methods.put(Method.parse(from), Method.parse(to));
			} else {
				// it's a field
				
				to = curClassTo + "/" + to;
				from = curClassFrom + "/" + from;
				
				m.fields.put(Field.parse(from), Field.parse(to));
			}
		}
		
		s.close();
		
		return m;
	}
	
	
	public Iterable<Method> getMethods() {
		return methods.keySet();
	}

	@Override
	public Mapping reverse() {
		TableMapping m2 = new TableMapping();
		m2.fields = HashBiMap.create(fields.inverse());
		m2.methods = HashBiMap.create(methods.inverse());
		m2.classes = HashBiMap.create(classes.inverse());
		return m2;
	}
}
