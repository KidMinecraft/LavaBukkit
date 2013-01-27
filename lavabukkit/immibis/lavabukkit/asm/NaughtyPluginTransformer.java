package immibis.lavabukkit.asm;

import immibis.lavabukkit.nms.Mapping;
import immibis.lavabukkit.nms.Mappings;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class NaughtyPluginTransformer extends ASMTransformerBase {
	
	private static Mapping mapping = Mappings.CB_to_current;
	private static Mapping mapping_inv = mapping.reverse();
	
	
	
	public final TransformingURLClassLoader classLoader;
	
	public NaughtyPluginTransformer(TransformingURLClassLoader loader) {
		classLoader = loader;
	}
	
	
	
	
	public int getClassWriterFlags() {return ClassWriter.COMPUTE_MAXS;}

	@Override
	public ClassVisitor transform(String name, ClassVisitor parent) {
		VERIFY_BYTECODE = name.equals("me.jsn_man.Reforestation.Reforestation");
		return new ClassVisitor(Opcodes.ASM4, parent) {
			
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				superName = mapping.mapClass(superName);
				
				if(interfaces != null)
					for(int k = 0; k < interfaces.length; k++)
						interfaces[k] = mapping.mapClass(interfaces[k]);

				super.visit(version, access, name, signature, superName, interfaces);
			}
			
			@Override
			public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
				desc = Mappings.mapTypeDescriptor(mapping, desc);
				return super.visitField(access, name, desc, signature, value);
			}
			
			@Override
			public MethodVisitor visitMethod(final int methodAccess, final String methodName, String desc, String signature, String[] exceptions) {
				desc = Mappings.mapMethodDescriptor(mapping, desc);
				
				return new MethodVisitor(Opcodes.ASM4, super.visitMethod(methodAccess, methodName, desc, signature, exceptions)) {
					@Override
					public void visitFieldInsn(int opcode, String owner, String name, String desc) {
						Field f1 = Mappings.resolveField(new Field(owner, name), classLoader.ACCESSOR, mapping, mapping_inv);
						Field f2 = mapping.mapField(f1);
						owner = mapping.mapClass(f1.owner);
						name = f2.name;
						desc = Mappings.mapTypeDescriptor(mapping, desc);
						super.visitFieldInsn(opcode, owner, name, desc);
					}
					
					@Override
					public void visitTypeInsn(int opcode, String type) {
						type = Mappings.mapTypeDescriptor(mapping, type);
						super.visitTypeInsn(opcode, type);
					}
					
					@Override
					public void visitMethodInsn(int opcode, String owner, String name, String desc) {
						Method m1 = new Method(owner, name, desc);
						Method m2 = mapping.mapMethod(m1);
						owner = mapping.mapClass(m1.owner);
						name = m2.name;
						desc = Mappings.mapMethodDescriptor(mapping, desc);
						
						super.visitMethodInsn(opcode, owner, name, desc);
					}
					
					@Override
					public void visitLdcInsn(Object cst) {
						if(cst instanceof Type) {
							String desc = ((Type) cst).getDescriptor();
							desc = Mappings.mapTypeDescriptor(mapping, desc);
							cst = Type.getType(desc);
						}
						super.visitLdcInsn(cst);
					}
					
					@Override
					public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
						if(local != null)
							for(int k = 0; k < local.length; k++)
								if(local[k] instanceof String)
									local[k] = Mappings.mapTypeDescriptor(mapping, (String)local[k]);
						if(stack != null)
							for(int k = 0; k < stack.length; k++)
								if(stack[k] instanceof String)
									stack[k] = Mappings.mapTypeDescriptor(mapping, (String)stack[k]);
						super.visitFrame(type, nLocal, local, nStack, stack);
					}
				};
			}
		};
	}

}
