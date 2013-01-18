package immibis.lavabukkit.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class CBExtractorTool {
	private static boolean isMapped(int access) {
		return (access & Opcodes.ACC_PRIVATE) == 0;
	}
	
	public static void main(String[] args) throws Exception {
		JarInputStream in = new JarInputStream(new FileInputStream(new File("craftbukkit.jar")));
		JarEntry entry;
		while((entry = in.getNextJarEntry()) != null) {
			if(entry.isDirectory() || !entry.getName().startsWith("net/minecraft/server") || !entry.getName().endsWith(".class")) {
				in.closeEntry();
				continue;
			}
			System.out.println(entry.getName());
			
			ClassReader clazz = new ClassReader(in);
			in.closeEntry();
			
			clazz.accept(new ClassVisitor(Opcodes.ASM4) {
				private PrintStream out;
				
				@Override
				public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
					super.visit(version, access, name, signature, superName, interfaces);
					
					try {
						out = new PrintStream(new File("../../forgebukkit2/immibis/lavabukkit/nmsbridge/extracted/" + name.substring(name.lastIndexOf('/') + 1)));
						out.println(name);
					} catch(IOException e) {
						throw new RuntimeException(e);
					}
				}
				
				@Override
				public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
					
					if(isMapped(access))
						out.println("\t" + name);
					
					return super.visitField(access, name, desc, signature, value);
				}
				
				@Override
				public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
					if(isMapped(access))
						out.println("\t" + name + desc);
					
					return super.visitMethod(access, name, desc, signature, exceptions);
				}
				
				@Override
				public void visitEnd() {
					super.visitEnd();
					
					out.close();
				}
			}, 0);
		}
		in.close();
	}
}
