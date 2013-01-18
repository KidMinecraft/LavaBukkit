package immibis.lavabukkit.asm;

import immibis.lavabukkit.LavaBukkitMod;
import immibis.lavabukkit.nms.Mapping;
import immibis.lavabukkit.nms.Mappings;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import cpw.mods.fml.relauncher.IClassTransformer;

public class PluginReflectionTransformer extends ASMTransformerBase {

	@Override
	public int getClassWriterFlags() {
		return ClassWriter.COMPUTE_MAXS;
	}
	
	//{VERIFY_BYTECODE = true;}
	
	private static Mapping fromCB = Mappings.CB_to_current;
	private static Mapping toCB = Mapping.reverse(fromCB);
	
	@Override
	public ClassVisitor transform(String name, ClassVisitor parent) {
		return new ClassVisitor(Opcodes.ASM4, parent) {
			Type thisType;
			int classVersion;
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				thisType = Type.getType("L" + name + ";");
				classVersion = version;
				super.visit(version, access, name, signature, superName, interfaces);
			}
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				final boolean isStaticMethod = (access & Opcodes.ACC_STATIC) != 0;
				
				return new MethodVisitor(Opcodes.ASM4, super.visitMethod(access, name, desc, signature, exceptions)) {
					@Override
					public void visitMethodInsn(int opcode, String owner, String name, String desc) {
						if(owner.equals("java/lang/Class") || owner.equals("java/lang/Package")) {
							if(owner.equals("java/lang/Class") && name.equals("forName") && desc.equals("(Ljava/lang/String;)Ljava/lang/Class;")) {
								
								// can't use ldc(Type) on class versions before Java 1.5
								if(classVersion < Opcodes.V1_5) {
									super.visitMethodInsn(opcode, owner, name, desc);
									return;
								}
						
								// convert forName(x) into forName(x, true, <current class>.getClassLoader())
								visitInsn(Opcodes.ICONST_1);
								visitLdcInsn(thisType);
								visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;");
								desc = "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;";
							}
							
							String simpleName = owner.substring(owner.lastIndexOf('/') + 1);
							for(java.lang.reflect.Method m : PluginReflectionTransformer.class.getMethods()) {
								if(m.getName().equals(simpleName + "_" + name)) {
									owner = "immibis/lavabukkit/asm/PluginReflectionTransformer";
									name = m.getName();
									desc = Type.getMethodDescriptor(m);
									opcode = Opcodes.INVOKESTATIC;
									break;
								}
							}
						}
						super.visitMethodInsn(opcode, owner, name, desc);
					}
				};
			}
		};
	}
	
	public static Class<?> Class_forName(String name, boolean resolve, ClassLoader loader) throws ClassNotFoundException {
		return Class.forName(fromCB.mapClass(name.replace('.','/')).replace('/','.'), resolve, loader);
	}
	
	public static java.lang.reflect.Field Class_getField(Class<?> c, String name) throws NoSuchFieldException {
		String cbClassName = toCB.mapClass(c.getName().replace('.','/'));
		String currentFieldName = fromCB.mapField(new Field(cbClassName, name)).name;
		return c.getField(currentFieldName);
	}
	
	public static java.lang.reflect.Method Class_getMethod(Class<?> c, String name, Class<?>[] args) throws NoSuchMethodException {
		String cbClassName = toCB.mapClass(c.getName().replace('.','/'));
		String cbDesc = "(";
		for(Class<?> a : args) {
			if(a.isPrimitive())
				cbDesc += Type.getDescriptor(a);
			else {
				String cbParamName = toCB.mapClass(a.getName().replace('.', '/'));
				if(!cbParamName.startsWith("["))
					cbParamName = "L" + cbParamName + ";";
				cbDesc += cbParamName;
			}
		}
		cbDesc += ")";
		
		for(Method m : fromCB.getMethods()) {
			if(m.owner.equals(cbClassName) && m.name.equals(name) && m.desc.startsWith(cbDesc)) {
				Method currentM = fromCB.mapMethod(m);
				for(java.lang.reflect.Method m2 : c.getMethods()) {
					if(m2.getName().equals(currentM.name) && Type.getMethodDescriptor(m2).equals(currentM.desc)) {
						return m2;
					}
				}
			}
		}
		
		return c.getMethod(name, args);
	}
	
	public static String Package_getName(Package p) {
		String name = p.getName();
		if(name.startsWith("net.minecraft"))
			name = "net.minecraft.server";
		return name;
	}
	

}
