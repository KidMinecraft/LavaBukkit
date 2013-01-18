package immibis.lavabukkit.tools.cbmapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class ClassSignature {
	
	public Set<String> strings = new HashSet<String>();
	public Map<String, Integer> fields = new HashMap<String, Integer>();
	public Map<String, Integer> methods = new HashMap<String, Integer>();
	public int access;
	
	@Override
	public int hashCode() {
		return strings.hashCode() ^ fields.hashCode() ^ methods.hashCode();
	}
	
	@Override
	public String toString() {
		return "s="+strings+", a="+access+", f="+fields+", m="+methods;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof ClassSignature) {
			
			ClassSignature o = (ClassSignature)obj;
			
			return o.strings.equals(strings) && o.fields.equals(fields) && access == o.access && methods.equals(o.methods);
			
		} else
			return false;
	}
	
	public static String getTypeIdentifier(String desc) {
		if(desc.startsWith("Ljava/") || desc.startsWith("Ljavax/") || desc.length() == 1)
			return desc;
		else
			return "X";
	}

	public ClassSignature(ClassNode cn) {
		cn.accept(new ClassVisitor(Opcodes.ASM4) {
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				ClassSignature.this.access = access + 100000*interfaces.length;
			}
			
			@Override
			public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
				String v = String.valueOf(access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC));
				v += getTypeIdentifier(desc);
				if(!fields.containsKey(v))
					fields.put(v, 1);
				else
					fields.put(v, fields.get(v) + 1);
				return null;
			}
			
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				String v = String.valueOf(access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_SYNCHRONIZED | Opcodes.ACC_SYNTHETIC));
				
				Type type = Type.getMethodType(desc);
				
				v += getTypeIdentifier(type.getReturnType().getDescriptor());
				for(Type at : type.getArgumentTypes())
					v += getTypeIdentifier(at.getDescriptor());
				
				final String v_ = v;
						
				return new MethodVisitor(Opcodes.ASM4) {
					String v = v_;
					
					@Override
					public void visitInsn(int opcode) {
						v += " " + opcode;
					}
					
					@Override
					public void visitMethodInsn(int opcode, String owner, String name, String desc) {
						v += " " + opcode;
					}
					
					@Override
					public void visitFieldInsn(int opcode, String owner, String name, String desc) {
						v += " " + opcode;
					}
					
					@Override
					public void visitLdcInsn(Object cst) {
						if(cst instanceof String)
							strings.add((String)cst);
					}
					
					@Override
					public void visitEnd() {
						if(!methods.containsKey(v))
							methods.put(v, 1);
						else
							methods.put(v, methods.get(v) + 1);
					}
				};
			}
		});
	}

	public static String getTypeIdentifier(ClassObject cl) {
		return getTypeIdentifier("L" + cl.cn.name + ";");
	}

	public static String getMethodDescIdentifier(String desc) {
		String v = "";
		Type type = Type.getMethodType(desc);
				
		v += getTypeIdentifier(type.getReturnType().getDescriptor());
		for(Type at : type.getArgumentTypes())
			v += getTypeIdentifier(at.getDescriptor());
		return v;
	}

}
