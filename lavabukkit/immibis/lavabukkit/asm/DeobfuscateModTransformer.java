package immibis.lavabukkit.asm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import immibis.lavabukkit.nms.Mapping;
import immibis.lavabukkit.nms.Mappings;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import cpw.mods.fml.relauncher.RelaunchClassLoader;

public class DeobfuscateModTransformer extends ASMTransformerBase {

	private static final Mapping M = Mappings.obf_to_current;
	private static final Mapping Minv = M.reverse();
	
	private static final Set<String> NET_MINECRAFT_SRC = ImmutableSet.of("BaseMod", "MLProp", "EntityRendererProxy", "FMLRenderAccessLibrary", "ModLoader", "ModTextureAnimation", "ModTextureStatic", "TradeEntry");
	
	private static String mapClass(String in) {
		String s = M.mapClass(in);
		if(s != in)
			return s;
		if(NET_MINECRAFT_SRC.contains(in))
			return "net/minecraft/src/" + in;
		return in;
	}
	
	private static Set<String> unknownSuperclasses = new HashSet<String>();
	private static Collection<String> getSuperclasses(String s) {
		if(superclasses.containsKey(s))
			return superclasses.get(s);
		if(unknownSuperclasses.contains(s))
			return Collections.emptyList();
		
		try {
			RelaunchClassLoader cl = (RelaunchClassLoader)HookInsertionTransformer.class.getClassLoader();
			byte[] bytes = cl.getClassBytes(M.mapClass(s).replace('/', '.'));
			if(bytes == null) {
				//throw new IllegalArgumentException("couldn't read "+Mappings.MCP_to_current.mapClass(s)+" ("+s+")");
				unknownSuperclasses.add(s);
				return Collections.emptyList();
			}
			ClassNode cn = new ClassNode();
			new ClassReader(bytes).accept(cn, ClassReader.SKIP_CODE);
			
			superclasses.put(s, Minv.mapClass(cn.superName));
			for(String i : (List<String>)cn.interfaces)
				superclasses.put(s, Minv.mapClass(i));
			return superclasses.get(s);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static Method mapMethod(Method in, boolean recursive) {
		String owner = in.owner, name = in.name, desc = in.desc;
		
		Method m = new Method(owner, name, desc);
		Method m2 = M.mapMethod(m);
		if(m != m2)
			return m2;
	
		String original_owner = owner;
		for(String sc : getSuperclasses(owner)) {
			Method m3 = new Method(sc, name, desc);
			m2 = mapMethod(m3, true);
			if(m3 != m2 && m2 != null) {
				m2 = new Method(M.mapClass(original_owner), m2.name, m2.desc);
				return m2;
			}
		}
		
		if(recursive)
			return null;
		
		return new Method(mapClass(in.owner), in.name, Mappings.mapMethodDescriptor(M, in.desc));
	}
	
	private static Field mapField(String owner, String name, String desc, boolean recursive) {
		
		Field m = new Field(owner, name);
		Field m2 = M.mapField(m);
		if(m != m2)
			return m2;
	
		String original_owner = owner;
		for(String sc : getSuperclasses(owner)) {
			Field m3 = new Field(sc, name);
			m2 = mapField(m3.owner, m3.name, desc, true);
			if(m3 != m2 && m2 != null)
				return new Field(M.mapClass(original_owner), m2.name);
		}
			
		if(recursive)
			return null;
		
		return new Field(M.mapClass(original_owner), name);
	}
	
	private static Multimap<String, String> superclasses = ArrayListMultimap.create();
	
	@Override
	public ClassVisitor transform(String name, ClassVisitor parent) {
		if(!Mappings.RUNNING_IN_MCP)
			return parent;
		if(name.startsWith("net.minecraft") || name.startsWith("immibis.lavabukkit")
			|| name.startsWith("org.bukkit") || name.startsWith("cpw.mods.fml"))
			return parent;
		
		return new ClassVisitor(Opcodes.ASM4, parent) {
			private String className;
			
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				superclasses.put(name, superName);
				
				superName = mapClass(superName);
				for(int k = 0; k < interfaces.length; k++) {
					superclasses.put(name, interfaces[k]);
					
					interfaces[k] = mapClass(interfaces[k]);
				}
				
				className = name;
				
				super.visit(version, access, name, signature, superName, interfaces);
			}
			
			@Override
			public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
				desc = Mappings.mapTypeDescriptor(M, desc);
				return super.visitField(access, name, desc, signature, value);
			}
			
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				Method m = new Method(className, name, desc);
				Method m2 = mapMethod(m, false);
				name = m2.name;
				desc = m2.desc;
				
				if(exceptions != null)
					for(int k = 0; k < exceptions.length; k++)
						exceptions[k] = M.mapClass(exceptions[k]);
				
				return new MethodVisitor(Opcodes.ASM4, super.visitMethod(access, name, desc, signature, exceptions)) {
					@Override
					public void visitMethodInsn(int opcode, String owner, String name, String desc) {
						Method m2 = mapMethod(new Method(owner, name, desc), false);
						owner = m2.owner;
						name = m2.name;
						desc = m2.desc;
						
						
						super.visitMethodInsn(opcode, owner, name, desc);
					}
					
					@Override
					public void visitFieldInsn(int opcode, String owner, String name, String desc) {
						Field f2 = mapField(owner, name, desc, false);
						owner = f2.owner;
						name = f2.name;
						desc = Mappings.mapTypeDescriptor(M, desc);
						
						super.visitFieldInsn(opcode, owner, name, desc);
					}
					
					@Override
					public void visitTypeInsn(int opcode, String type) {
						type = Mappings.mapTypeDescriptor(M, type);
						super.visitTypeInsn(opcode, type);
					}
					
					@Override
					public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
						if(local != null)
							for(int k = 0; k < local.length; k++)
								if(local[k] instanceof String)
									local[k] = mapClass((String)local[k]);
						if(stack != null)
							for(int k = 0; k < stack.length; k++)
								if(stack[k] instanceof String)
									stack[k] = mapClass((String)stack[k]);
						
						super.visitFrame(type, nLocal, local, nStack, stack);
					}
					
					@Override
					public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
						desc = Mappings.mapTypeDescriptor(M, desc);
						super.visitLocalVariable(name, desc, signature, start, end, index);
					}
					
					@Override
					public void visitMultiANewArrayInsn(String desc, int dims) {
						desc = Mappings.mapTypeDescriptor(M, desc);
						super.visitMultiANewArrayInsn(desc, dims);
					}
					
					@Override
					public void visitLdcInsn(Object cst) {
						if(cst instanceof Type)
							cst = Type.getType(mapClass(((Type)cst).getDescriptor()));
						super.visitLdcInsn(cst);
					}
				};
			}
		};
	}

}
