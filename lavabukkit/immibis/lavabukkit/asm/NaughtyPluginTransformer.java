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

public class NaughtyPluginTransformer extends ASMTransformerBase {
	
	public static final boolean PRINT_ALL_ERRORS = false;//MCPMapping.RUNNING_IN_MCP;
	
	private static Mapping mapping = Mappings.CB_to_current;
	
	
	
	
	
	private static boolean isForbiddenClass(String internalName) {
		if(internalName.endsWith(";") || internalName.contains("."))
			throw new IllegalArgumentException("Argument was a type descriptor or normal class name, must be an internal class name: "+internalName);
		
		return internalName.startsWith("org/bukkit/craftbukkit")
			|| internalName.startsWith("net/minecraft")
			|| internalName.startsWith("immibis/lavabukkit");
	}
	
	private static String translateClass(String clazz) {
		if(!isForbiddenClass(clazz))
			return clazz;
		
		return mapping.mapClass(clazz);
	}
	
	private static boolean isForbiddenTypeDescriptor(String desc) {
		if(desc.startsWith("L") && desc.endsWith(";"))
			return isForbiddenClass(desc.substring(1, desc.length() - 1));
		
		if(desc.startsWith("["))
			return isForbiddenTypeDescriptor(desc.substring(1));
		
		return false;
	}
	
	private static String translateTypeDescriptor(String desc) {
		if(desc.startsWith("L") && desc.endsWith(";")) {
			String clazz = desc.substring(1, desc.length() - 1);
			clazz = translateClass(clazz);
			return clazz == null ? null : "L" + clazz + ";";
		}
		
		if(desc.startsWith("[")) {
			String elem = translateTypeDescriptor(desc.substring(1));
			return elem == null ? null : "[" + elem;
		}
		
		return desc;
	}
	
	private static boolean isForbiddenClassOrArray(String desc) {
		if(desc.startsWith("["))
			return isForbiddenTypeDescriptor(desc.substring(1));
		else
			return isForbiddenClass(desc);
	}
	
	private static boolean isForbiddenMethodDescriptor(String desc) {
		if(!desc.startsWith("("))
			throw new IllegalArgumentException("Invalid method descriptor: "+desc);
		
		int pos = 1, end;

		while(pos < desc.length()) {
			switch(desc.charAt(pos++)) {
			case 'L':
				end = desc.indexOf(";", pos);
				if(end == -1)
					throw new IllegalArgumentException("Invalid method descriptor: "+desc);
				if(isForbiddenTypeDescriptor(desc.substring(pos - 1, end + 1)))
					return true;
				pos = end + 1;
				break;
				
			case ')':
				if(isForbiddenTypeDescriptor(desc.substring(pos)))
					return true;
				return false;
				
			default:
				if(isForbiddenTypeDescriptor(String.valueOf(desc.charAt(pos - 1))))
					return true;
				break;
			}
		}
		
		throw new IllegalArgumentException("Invalid method descriptor: "+desc);
	}
	
	private static String translateMethodDescriptor(String desc) {
		if(!desc.startsWith("("))
			throw new IllegalArgumentException("Invalid method descriptor: "+desc);
		
		int pos = 1, end;
		
		StringBuilder rv = new StringBuilder("(");

		String arg;
		while(pos < desc.length()) {
			switch(desc.charAt(pos++)) {
			case 'L':
				end = desc.indexOf(";", pos);
				if(end == -1)
					throw new IllegalArgumentException("Invalid method descriptor: "+desc);
				arg = desc.substring(pos - 1, end + 1);
				if(isForbiddenTypeDescriptor(arg)) {
					arg = translateTypeDescriptor(arg);
					if(arg == null)
						return null;
				}
				rv.append(arg);
				pos = end + 1;
				break;
				
			case ')':
				rv.append(')');
				arg = desc.substring(pos);
				if(isForbiddenTypeDescriptor(arg)) {
					arg = translateTypeDescriptor(arg);
					if(arg == null)
						return null;
				}
				rv.append(arg);
				return rv.toString();
				
			default:
				rv.append(desc.charAt(pos - 1));
				break;
			}
		}
		
		throw new IllegalArgumentException("Invalid method descriptor: "+desc);
	}
	
	private static String translateClassOrArray(String s) {
		if(s.startsWith("["))
			return translateTypeDescriptor(s);
		else
			return translateClass(s);
	}
	
	
	
	
	
	public int getClassWriterFlags() {return ClassWriter.COMPUTE_MAXS;}

	@Override
	public ClassVisitor transform(String name, ClassVisitor parent) {
		return new ClassVisitor(Opcodes.ASM4, parent) {
			
			private String classInternalName;
			private boolean anyErrors;
			
			private void raiseError(String msg) {
				System.err.println(msg);
				anyErrors = true;
			}
			
			@Override
			public void visitEnd() {
				super.visitEnd();
				
				//if(anyErrors)
				//	throw new RuntimeException("class does bad things: "+classInternalName);
			}
			
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				classInternalName = name;
				
				if(isForbiddenClass(superName))
					raiseError(name+" extends forbidden class "+superName);
				
				if(interfaces != null)
					for(String i : interfaces)
						if(isForbiddenClass(i))
							raiseError(name+" implements forbidden interface "+i);

				super.visit(version, access, name, signature, superName, interfaces);
			}
			
			@Override
			public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
				String odesc = desc;
				desc = translateTypeDescriptor(desc);
				if(desc == null)
					raiseError("Field "+classInternalName+"/"+name+" has forbidden type descriptor "+odesc);
				return super.visitField(access, name, desc, signature, value);
			}
			
			@Override
			public MethodVisitor visitMethod(final int methodAccess, final String methodName, String desc, String signature, String[] exceptions) {
				String odesc = desc;
				desc = translateMethodDescriptor(desc);
				if(desc == null)
					raiseError("Method "+classInternalName+"/"+methodName+" has forbidden descriptor "+odesc);
				
				final String method = "Method "+classInternalName+"/"+methodName+desc;
				
				final String methodDesc = desc;
				
				return new MethodVisitor(Opcodes.ASM4, super.visitMethod(methodAccess, methodName, desc, signature, exceptions)) {
					@Override
					public void visitFieldInsn(int opcode, String owner, String name, String desc) {
						String message = null;
						if(isForbiddenClass(owner))
							message = (method+" accesses forbidden field "+owner+"/"+name);
						else if(isForbiddenTypeDescriptor(desc))
							message = (method+" accesses field "+owner+"/"+name+" with forbidden type "+desc);
						
						if(message != null) {
							String key = owner + "/" + name;
							
							Field f1 = new Field(owner, name);
							Field f2 = mapping.mapField(f1);
							if(f1 == f2) {
								raiseError(message);
								super.visitFieldInsn(opcode, owner, name, desc);
								
							} else {
								String odesc = desc;
								desc = translateTypeDescriptor(desc);
								if(desc == null)
									raiseError(method+" access field "+owner+"/"+name+" with forbidden type "+odesc);

								owner = f2.owner;
								name = f2.name;
								
								super.visitFieldInsn(opcode, owner, name, desc);
							}
							
						} else {
							super.visitFieldInsn(opcode, owner, name, desc);
						}
					}
					
					@Override
					public void visitTypeInsn(int opcode, String type) {
						String otype = type;
						type = translateClassOrArray(type);
						if(type == null) {
							raiseError(method+" accesses forbidden class "+otype);
							type = otype;
						}
						
						super.visitTypeInsn(opcode, type);
					}
					
					@Override
					public void visitMethodInsn(int opcode, String owner, String name, String desc) {
						
						String message = null;
						if(isForbiddenClassOrArray(owner))
							message = (method+" accesses forbidden method "+owner+"/"+name+desc);
						else if(isForbiddenMethodDescriptor(desc))
							message = (method+" accesses method "+owner+"/"+name+" with forbidden descriptor "+desc);
						
						if(message != null) {
							Method m1 = new Method(owner, name, desc);
							Method m2 = mapping.mapMethod(m1);
							
							if(m1 != m2) {
								owner = m2.owner;
								name = m2.name;
								desc = m2.desc;

							} else if(!isForbiddenClassOrArray(owner)) {
								desc = translateMethodDescriptor(desc);
								if(desc == null) {
									raiseError(message);
								}
							} else {
								raiseError(message);
							}
						}
						
						boolean debug = false;
						/*for(String s : new String[] {"org/dynmap/bukkit/DynmapPlugin$BukkitServer", "org/dynmap/bukkit/BukkitWorld", "org/dynmap/bukkit/NewMapChunkCache"})
							if(classInternalName.startsWith(s) || owner.startsWith(s))
								debug = true;*/
						
						if(debug) {
							super.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
							super.visitLdcInsn("Calling "+owner+"/"+name+desc+" from "+classInternalName+"/"+methodName);
							super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
						}
						
						super.visitMethodInsn(opcode, owner, name, desc);
						
						//if(classInternalName.startsWith("org/dynmap") && !classInternalName.startsWith("org/dynmap/bukkit") && owner.startsWith("org/dynmap")
						//		&& !classInternalName.contains("snakeyaml") && !owner.contains("snakeyaml") && !owner.equals("org/dynmap/Color")) {
						//if(classInternalName.startsWith("org/dynmap/DynmapCore") || classInternalName.startsWith("org/dynmap/MapManager")) {
						if(debug) {
							super.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
							super.visitLdcInsn("Called "+owner+"/"+name+desc+" from "+classInternalName+"/"+methodName);
							super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
						}
					}
					
					@Override
					public void visitLdcInsn(Object cst) {
						if(cst instanceof Type) {
							String desc = ((Type) cst).getDescriptor();
							desc = translateTypeDescriptor(desc);
							if(desc == null)
								raiseError("method "+methodName+" references forbidden class literal "+((Type)cst).getDescriptor());
							else
								cst = Type.getType(desc);
						}
						super.visitLdcInsn(cst);
					}
					
					Label start = new Label(), end = new Label(), catch_ = new Label();
					
					@SuppressWarnings("unused")
					@Override
					public void visitCode() {
						super.visitCode();
						if((methodAccess & (Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT)) == 0 && PRINT_ALL_ERRORS) {
							super.visitTryCatchBlock(start, end, catch_, "java/lang/Error");
							super.visitLabel(start);
						}
					}

					int maxStack, maxLocals;
					@Override
					public void visitMaxs(int maxStack, int maxLocals) {
						this.maxStack = maxStack;
						this.maxLocals = maxLocals;
					}
					
					@SuppressWarnings("unused")
					@Override
					public void visitEnd() {
						if((methodAccess & (Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT)) == 0 && PRINT_ALL_ERRORS) {
							super.visitLabel(end);
							super.visitLabel(catch_);
							super.visitInsn(Opcodes.DUP);
							super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V");
							//super.visitInsn(Opcodes.ICONST_0);
							//super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "exit", "(I)V");
							super.visitInsn(Opcodes.ATHROW);
							
							if(maxStack < 2)
								maxStack = 2;
							maxStack += 2;
						}
						super.visitMaxs(maxStack, maxLocals);
						super.visitEnd();
					}
				};
			}
		};
	}

}
