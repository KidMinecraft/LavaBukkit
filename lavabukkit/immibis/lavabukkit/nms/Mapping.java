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

public class Mapping {
	private BiMap<Field, Field> fields = HashBiMap.create();
	private BiMap<Method, Method> methods = HashBiMap.create();
	private BiMap<String, String> classes = HashBiMap.create();
	
	
	
	public Field mapField(Field f) {
		Field f2 = fields.get(f);
		return f2 == null ? f : f2;
	}
	
	public Method mapMethod(Method m) {
		Method m2 = methods.get(m);
		return m2 == null ? m : m2;
	}
	
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
		
		Mapping m = new Mapping();
		
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

	
	
	
	public static Mapping reverse(Mapping m) {
		Mapping m2 = new Mapping();
		m2.fields = HashBiMap.create(m.fields.inverse());
		m2.methods = HashBiMap.create(m.methods.inverse());
		m2.classes = HashBiMap.create(m.classes.inverse());
		return m2;
	}
	
	private static <T> BiMap<T,T> join(BiMap<T,T> a, BiMap<T,T> b) {
		BiMap<T,T> r = HashBiMap.create();
		
		for(Map.Entry<T,T> e : a.entrySet()) {
			if(b.containsKey(e.getValue()))
				r.put(e.getKey(), b.get(e.getValue()));
			else
				r.put(e.getKey(), e.getValue());
		}
		
		return r;
	}
	
	public static Mapping join(final Mapping a, final Mapping b) {
		Mapping r = new Mapping();
		r.classes = join(a.classes, b.classes);
		r.fields = join(a.fields, b.fields);
		r.methods = join(a.methods, b.methods);
		return r;
	}
	
	public static Mapping identity() {
		return new Mapping();
	}
	
	
	public Iterable<Method> getMethods() {
		return methods.keySet();
	}

	
	
	
	public String mapMethodDescriptor(String desc) {
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
				rv.append(mapClass(desc.substring(pos, i)));
				rv.append(';');
				pos = i+1;
				break;
				
			default:
				rv.append(desc.charAt(pos - 1));
			}
		}
		return rv.toString();
	}
	
	
}
