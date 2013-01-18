package immibis.lavabukkit.asm;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import cpw.mods.fml.common.asm.transformers.AccessTransformer;

public class LBAccessTransformer extends ASMTransformerBase {
	
	private static class Modification {
		public int set = 0, reset = 0;
	}
	
	private Map<String, Map<String, Modification>> mods = new HashMap<String, Map<String, Modification>>();

	public LBAccessTransformer() throws IOException {
		Scanner s = new Scanner(LBAccessTransformer.class.getResourceAsStream("/immibis/lavabukkit/asm/lb_access.txt"));
		
		try {
			Map<String, Modification> curClass = null;
			while(s.hasNext()) {
				String name = s.next();
				String op = s.next();
				if(name.equals("class:")) {
					curClass = mods.get(op);
					if(curClass == null)
						mods.put(op, curClass = new HashMap<String, Modification>());
					continue;
				}
				
				if(curClass == null)
					throw new RuntimeException("Must start with class: line");
				
				boolean setting;
				if(op.startsWith("+"))
					setting = true;
				else if(op.startsWith("-"))
					setting = false;
				else
					throw new RuntimeException("Must start with + or -: "+op);
				
				op = op.substring(1);
				
				int modifier = parseModifier(op);
				if(modifier == 0)
					throw new RuntimeException("Unknown modifier: "+modifier);
				
				Modification mod = curClass.get(name);
				if(mod == null)
					curClass.put(name, mod = new Modification());
				
				if(setting)
					mod.set |= modifier;
				else
					mod.reset |= modifier;
				
				if((mod.set & mod.reset) != 0)
					throw new RuntimeException("Cannot set and reset the same modifier.");
			}
		} finally {
			s.close();
		}
	}
	
	private int parseModifier(String s) {
		s = s.toLowerCase();
		if(s.equals("final")) return Modifier.FINAL;
		if(s.equals("private")) return Modifier.PRIVATE;
		if(s.equals("protected")) return Modifier.PROTECTED;
		if(s.equals("public")) return Modifier.PUBLIC;
		return 0;
	}
	
	static int applyModification(int access, Map<String, Modification> classMap, String key) {
		Modification mod = classMap.get(key);
		if(mod != null)
			access = (access | mod.set) & ~mod.reset;
		return access;
	}
	
	@Override
	public ClassVisitor transform(String name, ClassVisitor parent) {
		final Map<String, Modification> classMap = mods.get(name);
		if(classMap == null) return parent;
		
		return new ClassVisitor(Opcodes.ASM4, parent) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				
				access = applyModification(access, classMap, name + desc);
				
				return super.visitMethod(access, name, desc, signature, exceptions);
			}
			
			@Override
			public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
				access = applyModification(access, classMap, name);
				return super.visitField(access, name, desc, signature, value);
			}
		};
	}

}
