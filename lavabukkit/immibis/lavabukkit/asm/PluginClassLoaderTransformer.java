package immibis.lavabukkit.asm;

import immibis.lavabukkit.LavaBukkitMod;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.plugin.java.PluginClassLoader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import cpw.mods.fml.relauncher.IClassTransformer;

// Makes the PluginClassLoader use the AnnoyingPluginTransformer
// also make it load guava classes from an alternate ClassLoader
public class PluginClassLoaderTransformer extends ASMTransformerBase {
	
	public static ClassLoader guavaLoader;

	@Override
	public ClassVisitor transform(String name, ClassVisitor parent) {
		if(!name.equals("org.bukkit.plugin.java.PluginClassLoader"))
			return parent;
		
		return new ClassVisitor(Opcodes.ASM4, parent) {
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				
				superName = "immibis/lavabukkit/asm/TransformingURLClassLoader";
				
				super.visit(version, access, name, signature, superName, interfaces);
				
				/*  public Class<?> loadClass(String name) throws ClassNotFoundException {
				    	if(name.startsWith("com.google"))
				    		return PluginClassLoaderTransformer.guavaLoader.loadClass(name);
				    	else
				    		return super.loadClass(name);
				    }
				*/
				
				Label isGuavaLabel = new Label();
				
				MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;", null, new String[] {"java/lang/ClassNotFoundException"});
				mv.visitCode();
				mv.visitVarInsn(Opcodes.ALOAD, 1);
				mv.visitLdcInsn("com.google");
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z");
				mv.visitJumpInsn(Opcodes.IFNE, isGuavaLabel);
				mv.visitVarInsn(Opcodes.ALOAD, 0);
				mv.visitVarInsn(Opcodes.ALOAD, 1);
				mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/net/URLClassLoader", "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
				mv.visitInsn(Opcodes.ARETURN);
				
				mv.visitLabel(isGuavaLabel);
				mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
				mv.visitFieldInsn(Opcodes.GETSTATIC, "immibis/lavabukkit/asm/PluginClassLoaderTransformer", "guavaLoader", "Ljava/lang/ClassLoader;");
				mv.visitVarInsn(Opcodes.ALOAD, 1);
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/ClassLoader", "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
				mv.visitInsn(Opcodes.ARETURN);
				mv.visitMaxs(2, 2);
				mv.visitEnd();
			}
			
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				
				if(name.equals("<init>"))
					return new MethodVisitor(Opcodes.ASM4, super.visitMethod(access, name, desc, signature, exceptions)) {
						private boolean seenSuperInit = false;
						
						@Override
						public void visitMethodInsn(int opcode, String owner, String name, String desc) {
							
							if(!seenSuperInit && owner.equals("java/net/URLClassLoader") && name.equals("<init>")) {
								seenSuperInit = true;
								owner = "immibis/lavabukkit/asm/TransformingURLClassLoader";
							}
							
							super.visitMethodInsn(opcode, owner, name, desc);
						}
					};
					
				return super.visitMethod(access, name, desc, signature, exceptions);
			}
		};
	}

}
