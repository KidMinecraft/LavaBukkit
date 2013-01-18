package immibis.lavabukkit.tools;

import immibis.lavabukkit.asm.Field;
import immibis.lavabukkit.asm.Method;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class MCPMapperTool {
	public static void main(String[] args) {
		if(args.length < 3) {
			System.err.println("Need MCP conf dir, MC version and output dir on command line.");
			return;
		}
		
		File confDir = new File(args[0]);
		String mcVer = args[1];
		File outDir = new File(args[2]);
		
		try {
			Map<String, String> fieldsCSV = readCSV(new File(confDir, "fields.csv"));
			Map<String, String> methodsCSV = readCSV(new File(confDir, "methods.csv"));
			
			Scanner srg = new Scanner(new File(confDir, "packaged.srg"));
			while(srg.hasNextLine()) {
				String[] parts = srg.nextLine().split(" ");
				if(parts[0].equals("PK:"))
					;
				else if(parts[0].equals("CL:")) {
					String mcp = parts[2], obf = parts[1];
					classes.put(mcp, obf);
				} else if(parts[0].equals("FD:")) {
					String mcp = replaceLastSegment(parts[2], fieldsCSV);
					Field obf = parseFieldSRG(parts[1]);
					fields.put(mcp, obf);
				} else if(parts[0].equals("MD:")) {
					String mcp = replaceLastSegment(parts[3], methodsCSV) + parts[4];
					Method obf = parseMethodSRG(parts[1], parts[2]);
					methods.put(mcp, obf);
				} else {
					srg.close();
					throw new Exception("Unknown SRG line type: "+parts[0]);
				}
			}
			srg.close();
			
			PrintWriter out = new PrintWriter(new File(outDir, mcVer+"-mcp.txt"));
			for(Map.Entry<String, String> ce : classes.entrySet()) {
				String clazz = ce.getKey();
				out.println(ce.getKey()+" "+ce.getValue());
				
				for(Map.Entry<String, Method> me : methods.entrySet())
					if(me.getKey().startsWith(clazz + "/"))
						out.println("  " + stripClass(me.getKey())+" "+me.getValue().name+me.getValue().desc);
				
				for(Map.Entry<String, Field> me : fields.entrySet())
					if(me.getKey().startsWith(clazz + "/"))
						out.println("  " + stripClass(me.getKey())+" "+me.getValue().name);
				
				out.println();
			}
			out.close();
			
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String stripClass(String s) {
		int j;
		if(s.contains("("))
			j = s.lastIndexOf('/', s.indexOf('('));
		else
			j = s.lastIndexOf('/');
		if(j <= 0)
			return s;
		else
			return s.substring(j+1);
	}
	
	public static Map<String, String> classes = new HashMap<String, String>();
	public static Map<String, Field> fields = new HashMap<String, Field>();
	public static Map<String, Method> methods = new HashMap<String, Method>();
	
	private static Field parseFieldSRG(String s) {
		int i = s.lastIndexOf('/');
		return new Field(s.substring(0, i), s.substring(i + 1));
	}
	
	private static Method parseMethodSRG(String path, String desc) {
		int i = path.lastIndexOf('/');
		return new Method(path.substring(0, i), path.substring(i + 1), desc);
	}
	
	private static String replaceLastSegment(String s, Map<String, String> csv) {
		int i = s.lastIndexOf('/');
		String last = csv.get(s.substring(i + 1));
		if(last != null)
			return s.substring(0, i) + '/' + last;
		else
			return s;
	}
	
	private static Map<String, String> readCSV(File file) throws IOException {
		Scanner in = new Scanner(file);
		Map<String, String> rv = new HashMap<String, String>();
		in.nextLine();
		while(in.hasNextLine()) {
			String parts[] = in.nextLine().split(",");
			if(parts.length < 2)
				continue;
			rv.put(parts[0], parts[1]);
		}
		in.close();
		return rv;
	}

	public static String mapclass(String name) {
		String s = classes.get(name);
		return s == null ? name : s;
	}
	public static Field mapfield(String owner, String name) {
		Field f = fields.get(owner+"/"+name);
		return f == null ? new Field(owner, name) : f;
	}
	public static Method mapmethod(String owner, String name, String desc) {
		Method m = methods.get(owner+"/"+name+desc);
		return m == null ? new Method(owner, name, desc) : m;
	}
	
	public static String mapmdesc(String desc) {
		StringBuilder rv = new StringBuilder();
		if(desc.charAt(0) != '(') throw new IllegalArgumentException("Invalid method descriptor: "+desc);
		int pos = 1;
		rv.append('(');
		while(pos < desc.length()) {
			switch(desc.charAt(pos)) {
			case 'B': case 'I': case 'J': case 'C': case 'S': case 'F': case 'D': case 'Z': case '[': case ')': case 'V':
				rv.append(desc.charAt(pos));
				pos++;
				break;
			case 'L':
				int i = desc.indexOf(';', pos);
				if(i < 0) throw new IllegalArgumentException("Invalid method descriptor: "+desc);
				String type = desc.substring(pos + 1, i);
				pos = i + 1;
				rv.append('L');
				rv.append(mapclass(type));
				rv.append(';');
				break;
			default: throw new IllegalArgumentException("Invalid method descriptor: "+desc+" (at pos "+pos+")");
			}
		}
		return rv.toString();
	}

	public static Method reobfuscate(String to) {
		if(methods.containsKey(to))
			return methods.get(to);
		
		String desc = to.substring(to.indexOf('('));
		String owner = to.substring(0, to.lastIndexOf('/', to.indexOf('(')));
		String name = to.substring(owner.length() + 1, to.indexOf('('));
		
		return new Method(mapclass(owner), name, mapmdesc(desc));
	}
}
