package immibis.lavabukkit.asm;

import immibis.lavabukkit.nms.Mappings;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.CheckMethodAdapter;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.util.TraceMethodVisitor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import cpw.mods.fml.relauncher.RelaunchClassLoader;

public class HookInsertionTransformer extends ASMTransformerBase {
	
	/*public static class HookPoint {
		public final Method caller, callee, replacement;
		
		public HookPoint(Method caller, Method callee, Method replacement) {
			this.caller = caller;
			this.callee = callee;
			this.replacement = replacement;
		}
	}*/
	
	private static Map<Method, Map<Method, Method>> hooks = new HashMap<Method, Map<Method,Method>>();
	private static Set<String> hookedClasses = new HashSet<String>();
	
	private static ListMultimap<Method, Method> invokeAtEnd = ArrayListMultimap.create();
	private static ListMultimap<Method, Method> invokeAtStart = ArrayListMultimap.create();
	
	// Replaces one method with another static method
	// The static method receives the object the replaced method was called on (if applicable),
	// followed by the replaced method's arguments, followed by "this" from the calling
	// method (if applicable), followed by the value of the calling method's parameters
	// at the point of the replaced call
	public static void replaceMethod(Method caller, Method callee, Method replacement) {
		Validate.notNull(caller, "Caller cannot be null");
		Validate.notNull(callee, "Callee cannot be null");
		Validate.notNull(replacement, "Replacement cannot be null");
		
		hookedClasses.add(caller.owner);
		
		Map<Method, Method> m = hooks.get(caller);
		if(m == null)
			hooks.put(caller, m = new HashMap<Method, Method>());
		else if(m.containsKey(callee))
			throw new IllegalArgumentException("Hook already installed for "+callee+" called from "+caller+" (existing replacement: "+m.get(callee)+", new replacement: "+replacement+")");
		
		m.put(callee, replacement);
	}
	
	// Calls one method (the injected method) before executing the body of another (the hooked method).
	// The arguments to the method are "this" from the hooked method, followed by the hooked method's arguments.
	// The return type must be Object. If the return value is not Hooks.EXECUTE_NORMALLY,
	// it is unboxed (if necessary) and returned from the hooked method without executing the
	// method body.
	public static void callBefore(Method hooked, Method injected) {
		Validate.notNull(hooked, "Hooked method cannot be null");
		Validate.notNull(injected, "Injected method cannot be null");
		
		invokeAtStart.put(hooked, injected);
		hookedClasses.add(hooked.owner);
	}
	
	// Calls one method (the injected method) before executing the body of another (the hooked method).
	// The arguments to the method are the value the hooked method would have returned, if its return type is not void,
	// followed by "this" from the hooked method, followed by the values of the hooked method's parameters at that point
	// (including any modifications it may have made)
	// The return type of the injected method must match the return type of the hooked method.
	// The return value from the injected method is returned from the hooked method.
	public static void callAfter(Method hooked, Method injected) {
		Validate.notNull(hooked, "Hooked method cannot be null");
		Validate.notNull(injected, "Injected method cannot be null");
		
		invokeAtEnd.put(hooked, injected);
		hookedClasses.add(hooked.owner);
	}
	
	
	
	private static boolean isPreloadingClasses = false;
	private static boolean preloadErrors = false;
	
	// loads all classes that contain any hooks, so errors in applying hooks are caught at load time
	public static void loadAllHookedClasses() {
		boolean errors = false;
		isPreloadingClasses = true;
		for(String s : hookedClasses) {
			String className = s.replace('/', '.');
			try {
				preloadErrors = false;
				Class.forName(className);
				//if(!preloadErrors)
					//System.err.println("Preloaded "+className);
				errors |= preloadErrors;
			} catch(ClassNotFoundException e) {
				System.err.println("Hooked class "+className+" could not be preloaded.");
				errors = true;
			}
		}
		isPreloadingClasses = false;
		if(errors)
			System.exit(1);
	}

	
	
	@Override
	public int getClassWriterFlags() {
		return ClassWriter.COMPUTE_MAXS;
	}

	@Override
	public ClassVisitor transform(String name, ClassVisitor parent) {
		if(!hookedClasses.contains(name.replace('.', '/')))
			return parent;
		
		//VERIFY_BYTECODE = name.equals("net.minecraft.block.BlockLeaves");
		
		return new ClassVisitor(Opcodes.ASM4, parent) {
			private String classInternalName;
			private Set<Method> actuallyHookedMethods = new HashSet<Method>();
			private Set<Method> hookedMethods = new HashSet<Method>();
			
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				classInternalName = name;
				
				for(Method m : hooks.keySet())
					if(m.owner.equals(classInternalName))
						hookedMethods.add(m);
				
				for(Method m : invokeAtEnd.keySet())
					if(m.owner.equals(classInternalName))
						hookedMethods.add(m);
				
				for(Method m : invokeAtStart.keySet())
					if(m.owner.equals(classInternalName))
						hookedMethods.add(m);
				
				super.visit(version, access, name, signature, superName, interfaces);
			}
			
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				final Method caller = new Method(classInternalName, name, desc);
				MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
				
				final Map<Method, Method> hookMap = hooks.get(caller);
				final List<Method> callAfter = invokeAtEnd.get(caller);
				final List<Method> callBefore = invokeAtStart.get(caller);
				
				final boolean callerIsStatic = (access & Opcodes.ACC_STATIC) != 0;
				
				
				if(hookMap == null && callAfter.isEmpty() && callBefore.isEmpty()) {
					//System.err.println("Not transforming "+classInternalName+"/"+name+desc);
					return mv;
				}
				
				//System.err.println("Transforming "+classInternalName+"/"+name+desc);
				
				//mv = new DebuggingMethodVisitor(mv);
				
				actuallyHookedMethods.add(caller);
				
				mv = new MethodVisitor(Opcodes.ASM4, mv) {
					private Set<Method> matchedReplacements = new HashSet<Method>();
					
					private void pushCallerParameters() {
						Type type = Type.getMethodType(caller.desc);
						int pos = 0;
						if(!callerIsStatic) {
							super.visitVarInsn(Opcodes.ALOAD, pos);
							pos++;
						}
						for(Type arg : type.getArgumentTypes()) {
							if(arg.getSort() == Type.DOUBLE) {
								super.visitVarInsn(Opcodes.DLOAD, pos);
								pos += 2;
							} else if(arg.getSort() == Type.FLOAT) {
								super.visitVarInsn(Opcodes.FLOAD, pos);
								pos += 1;
							} else if(arg.getSort() == Type.LONG) {
								super.visitVarInsn(Opcodes.LLOAD, pos);
								pos += 2;
							} else if(arg.getSort() == Type.OBJECT || arg.getSort() == Type.ARRAY) {
								super.visitVarInsn(Opcodes.ALOAD, pos);
								pos += 1;
							} else {
								super.visitVarInsn(Opcodes.ILOAD, pos);
								pos += 1;
							}
						}
					}
					
					@Override
					public void visitCode() {
						super.visitCode();
						//System.err.println("visitCode "+callBefore.size());
						
						Label earlyReturnLabel = new Label();
						Label normalExecLabel = new Label();
						
						for(Method injected : callBefore) {
							pushCallerParameters();
							super.visitMethodInsn(Opcodes.INVOKESTATIC, injected.owner, injected.name, injected.desc);
							super.visitInsn(Opcodes.DUP);
							super.visitFieldInsn(Opcodes.GETSTATIC, "immibis/lavabukkit/nms/Hooks", "EXECUTE_NORMALLY", "Ljava/lang/Object;");
							super.visitJumpInsn(Opcodes.IF_ACMPNE, earlyReturnLabel);
							super.visitInsn(Opcodes.POP);
						}
						
						if(!callBefore.isEmpty()) {
							super.visitJumpInsn(Opcodes.GOTO, normalExecLabel);
							super.visitLabel(earlyReturnLabel);
							super.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/Object"});
							
							ASMUtils.unboxAndReturn(this, Type.getReturnType(caller.desc));
							
							super.visitLabel(normalExecLabel);
							super.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
						}
					}
					
					@Override
					public void visitMethodInsn(int opcode, String owner, String name, String desc) {
						Method callee = new Method(owner, name, desc);
						Method replacement = hookMap == null ? null : hookMap.get(callee);
						
						if(replacement != null) {
							//System.out.println("Replacing "+callee+" with "+replacement+" in "+caller);
							owner = replacement.owner;
							name = replacement.name;
							desc = replacement.desc;
							
							matchedReplacements.add(callee);
							
							pushCallerParameters();
							
							opcode = Opcodes.INVOKESTATIC;
						}
						
						super.visitMethodInsn(opcode, owner, name, desc);
					}
					
					@Override
					public void visitInsn(int opcode) {
						if(opcode == Opcodes.IRETURN || opcode == Opcodes.ARETURN
						|| opcode == Opcodes.FRETURN || opcode == Opcodes.LRETURN
						|| opcode == Opcodes.DRETURN || opcode == Opcodes.RETURN) {
							for(Method m : callAfter) {
								//System.err.println("Calling "+m+" after "+caller);
								pushCallerParameters();
								super.visitMethodInsn(Opcodes.INVOKESTATIC, m.owner, m.name, m.desc);
							}
						}
						
						super.visitInsn(opcode);
					}
					
					@Override
					public void visitEnd() {
						super.visitEnd();
						
						if(hookMap != null) {
							for(Method callee : hookMap.keySet())
								if(!matchedReplacements.contains(callee))
									System.err.println("Didn't match method hook "+callee+" in "+caller);
							
							if(hookMap.size() != matchedReplacements.size()) {
								System.err.println("Some method hooks didn't match in "+caller+", aborting load");
								if(isPreloadingClasses)
									preloadErrors = true;
								else
									System.exit(1);
							}
						}
					}
				};
				
				return mv;
			}
			
			@Override
			public void visitEnd() {
				super.visitEnd();
				
				for(Method caller : hookedMethods)
					if(!actuallyHookedMethods.contains(caller))
						System.err.println("Couldn't find hooked method "+caller+" in "+classInternalName);
				
				if(hookedMethods.size() != actuallyHookedMethods.size()) {
					System.err.println("Some hooked methods weren't found "+classInternalName+", aborting load");
					if(isPreloadingClasses)
						preloadErrors = true;
					else
						System.exit(1);
				}
			}
		};
	}

	
	private static Map<String, Method> hooksMethods = null;
	private static Method findHookMethod(String name) {
		if(hooksMethods == null) {
			hooksMethods = new HashMap<String, Method>();
			try {
				new ClassReader(HookInsertionTransformer.class.getResourceAsStream("/immibis/lavabukkit/nms/Hooks.class")).accept(new ClassVisitor(Opcodes.ASM4) {
					@Override
					public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
						hooksMethods.put(name, new Method("immibis/lavabukkit/nms/Hooks", name, desc));
						return null;
					}
				}, 0);
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
		if(hooksMethods.containsKey(name))
			return hooksMethods.get(name);
		throw new IllegalArgumentException("No hook method found called "+name);
	}
	
	private static Map<String, String> cachedSupers = new HashMap<String, String>();
	// MCP class -> MCP superclass (both with /)
	private static String getSuperclass(String s) {
		if(cachedSupers.containsKey(s))
			return cachedSupers.get(s);
		
		try {
			RelaunchClassLoader cl = (RelaunchClassLoader)HookInsertionTransformer.class.getClassLoader();
			byte[] bytes = cl.getClassBytes(Mappings.MCP_to_current.mapClass(s).replace('/', '.'));
			if(bytes == null) {
				//throw new IllegalArgumentException("couldn't read "+Mappings.MCP_to_current.mapClass(s)+" ("+s+")");
				cachedSupers.put(s, null);
				return null;
			}
			ClassNode cn = new ClassNode();
			new ClassReader(bytes).accept(cn, ClassReader.SKIP_CODE);
			
			String c = Mappings.current_to_MCP.mapClass(cn.superName);
			cachedSupers.put(s, c);
			return c;
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	// obfuscates a vanilla method
	private static Method mapmethod(String owner, String name, String desc) {
		if(name.startsWith("<"))
			return mapforgemethod(owner, name, desc); // name isn't obfuscated
		
		Method m = new Method(owner, name, desc);
		Method m2 = Mappings.MCP_to_current.mapMethod(m);
		if(m != m2)
			return m2;
	
		String original_owner = owner;
		while((owner = getSuperclass(owner)) != null) {
			Method m3 = new Method(owner, name, desc);
			m2 = Mappings.MCP_to_current.mapMethod(m3);
			if(m3 != m2)
				return new Method(Mappings.MCP_to_current.mapClass(original_owner), m2.name, m2.desc);
		}
		throw new IllegalArgumentException("MCP mapping not found: "+m);
	}
	
	// obfuscates a forge-added method in a vanilla class
	private static Method mapforgemethod(String owner, String name, String desc) {
		String owner2 = Mappings.MCP_to_current.mapClass(owner);
		if(owner2 == owner)
			throw new IllegalArgumentException("MCP mapping not found: "+owner);
		return new Method(owner2, name, Mappings.mapMethodDescriptor(Mappings.MCP_to_current, desc));
	}
	
	static {
		replaceMethod(
			mapmethod("net/minecraft/block/BlockMushroom", "updateTick", "(Lnet/minecraft/world/World;IIILjava/util/Random;)V"),
			mapmethod("net/minecraft/world/World", "setBlockWithNotify", "(IIII)Z"),
			findHookMethod("onMushroomSpread"));
		replaceMethod(
			mapmethod("net/minecraft/block/BlockMycelium", "updateTick", "(Lnet/minecraft/world/World;IIILjava/util/Random;)V"),
			mapmethod("net/minecraft/world/World", "setBlockWithNotify", "(IIII)Z"),
			findHookMethod("onMyceliumSpreadOrFade"));
		replaceMethod(
			mapmethod("net/minecraft/block/BlockNetherStalk", "updateTick", "(Lnet/minecraft/world/World;IIILjava/util/Random;)V"),
			mapmethod("net/minecraft/world/World", "setBlockMetadataWithNotify", "(IIII)V"),
			findHookMethod("onNetherwartGrow"));
		replaceMethod(
			mapmethod("net/minecraft/block/BlockRedstoneLight", "onBlockAdded", "(Lnet/minecraft/world/World;III)V"),
			mapmethod("net/minecraft/world/World", "setBlockWithNotify", "(IIII)Z"),
			findHookMethod("onRedstoneLampOn1"));
		replaceMethod(
			mapmethod("net/minecraft/block/BlockRedstoneLight", "onNeighborBlockChange", "(Lnet/minecraft/world/World;IIII)V"),
			mapmethod("net/minecraft/world/World", "setBlockWithNotify", "(IIII)Z"),
			findHookMethod("onRedstoneLampOn2"));
		replaceMethod(
			mapmethod("net/minecraft/block/BlockRedstoneLight", "updateTick", "(Lnet/minecraft/world/World;IIILjava/util/Random;)V"),
			mapmethod("net/minecraft/world/World", "setBlockWithNotify", "(IIII)Z"),
			findHookMethod("onRedstoneLampOff"));
		callBefore(
			mapmethod("net/minecraft/block/BlockRedstoneOre", "onEntityWalking", "(Lnet/minecraft/world/World;IIILnet/minecraft/entity/Entity;)V"),
			findHookMethod("onRedstoneOreWalking"));
		replaceMethod(
			mapmethod("net/minecraft/block/BlockReed", "updateTick", "(Lnet/minecraft/world/World;IIILjava/util/Random;)V"),
			mapmethod("net/minecraft/world/World", "setBlockWithNotify", "(IIII)Z"),
			findHookMethod("onReedGrow"));
		replaceMethod(
			mapmethod("net/minecraft/block/BlockCactus", "updateTick", "(Lnet/minecraft/world/World;IIILjava/util/Random;)V"),
			mapmethod("net/minecraft/world/World", "setBlockWithNotify", "(IIII)Z"),
			findHookMethod("onCactusGrow"));
		replaceMethod(
			mapmethod("net/minecraft/block/BlockSnow", "canSnowStay", "(Lnet/minecraft/world/World;III)Z"),
			mapmethod("net/minecraft/world/World", "setBlockWithNotify", "(IIII)Z"),
			findHookMethod("onSnowFade"));
		replaceMethod(
			mapmethod("net/minecraft/block/BlockSnow", "updateTick", "(Lnet/minecraft/world/World;IIILjava/util/Random;)V"),
			mapmethod("net/minecraft/world/World", "setBlockWithNotify", "(IIII)Z"),
			findHookMethod("onSnowMelt"));
		replaceMethod(
			mapmethod("net/minecraft/block/BlockTNT", "onBlockAdded", "(Lnet/minecraft/world/World;III)V"),
			mapmethod("net/minecraft/world/World", "isBlockIndirectlyGettingPowered", "(III)Z"),
			findHookMethod("onTNTPlacedPowerCheck"));
		replaceMethod(
			mapmethod("net/minecraft/block/BlockCrops", "updateTick", "(Lnet/minecraft/world/World;IIILjava/util/Random;)V"),
			mapmethod("net/minecraft/world/World", "setBlockMetadataWithNotify", "(IIII)V"),
			findHookMethod("onCropsGrow"));
		replaceMethod(
			mapmethod("net/minecraft/block/BlockGrass", "updateTick", "(Lnet/minecraft/world/World;IIILjava/util/Random;)V"),
			mapmethod("net/minecraft/world/World", "setBlockWithNotify", "(IIII)Z"),
			findHookMethod("onGrassSpreadOrFade"));
		replaceMethod(
			mapmethod("net/minecraft/block/BlockIce", "updateTick", "(Lnet/minecraft/world/World;IIILjava/util/Random;)V"),
			mapmethod("net/minecraft/world/World", "setBlockWithNotify", "(IIII)Z"),
			findHookMethod("onIceMelt"));
		callBefore(
			mapmethod("net/minecraft/block/BlockLeaves", "removeLeaves", "(Lnet/minecraft/world/World;III)V"),
			findHookMethod("onLeafDecay"));
		replaceMethod(
			mapmethod("net/minecraft/block/BlockFarmland", "updateTick", "(Lnet/minecraft/world/World;IIILjava/util/Random;)V"),
			mapmethod("net/minecraft/world/World", "setBlockWithNotify", "(IIII)Z"),
			findHookMethod("onFarmlandDry"));
		replaceMethod(
			mapmethod("net/minecraft/block/BlockFarmland", "onFallenUpon", "(Lnet/minecraft/world/World;IIILnet/minecraft/entity/Entity;F)V"),
			mapmethod("net/minecraft/world/World", "setBlockWithNotify", "(IIII)Z"),
			findHookMethod("onFarmlandTrampled"));
		replaceMethod(
			mapmethod("net/minecraft/block/BlockStem", "updateTick", "(Lnet/minecraft/world/World;IIILjava/util/Random;)V"),
			mapmethod("net/minecraft/world/World", "setBlockWithNotify", "(IIII)Z"),
			findHookMethod("onStemProduce"));
		replaceMethod(
			mapmethod("net/minecraft/block/BlockStem", "updateTick", "(Lnet/minecraft/world/World;IIILjava/util/Random;)V"),
			mapmethod("net/minecraft/world/World", "setBlockMetadataWithNotify", "(IIII)V"),
			findHookMethod("onStemGrow"));
		replaceMethod(
			mapmethod("net/minecraft/block/BlockVine", "updateTick", "(Lnet/minecraft/world/World;IIILjava/util/Random;)V"),
			mapmethod("net/minecraft/world/World", "setBlockAndMetadataWithNotify", "(IIIII)Z"),
			findHookMethod("onVineSpread"));
		replaceMethod(
			mapmethod("net/minecraft/entity/Entity", "onEntityUpdate", "()V"),
			mapmethod("net/minecraft/entity/Entity", "attackEntityFrom", "(Lnet/minecraft/util/DamageSource;I)Z"),
			findHookMethod("onEntityOnFireDamage"));
		callBefore(
			mapmethod("net/minecraft/entity/Entity", "setOnFireFromLava", "()V"),
			findHookMethod("onEntityInLavaDamage"));
		replaceMethod(
			mapmethod("net/minecraft/entity/Entity", "moveEntity", "(DDD)V"),
			mapmethod("net/minecraft/entity/Entity", "setFire", "(I)V"),
			findHookMethod("onEntityCombustFromLava"));
		callBefore(
			mapmethod("net/minecraft/entity/Entity", "dealFireDamage", "(I)V"),
			findHookMethod("onEntityInFireDamage"));
		callBefore(
			mapmethod("net/minecraft/entity/Entity", "setWorld", "(Lnet/minecraft/world/World;)V"),
			findHookMethod("onEntitySetWorld"));
		callBefore(
			mapmethod("net/minecraft/entity/Entity", "writeToNBT", "(Lnet/minecraft/nbt/NBTTagCompound;)V"),
			findHookMethod("onEntityWriteToNBT"));
		replaceMethod(
			mapmethod("net/minecraft/entity/Entity", "onStruckByLightning", "(Lnet/minecraft/entity/effect/EntityLightningBolt;)V"),
			mapmethod("net/minecraft/entity/Entity", "setFire", "(I)V"),
			findHookMethod("onEntityCombustFromLightning"));
		callBefore(
			mapmethod("net/minecraft/entity/Entity", "travelToDimension", "(I)V"),
			findHookMethod("onEntityTravelToDimension"));
		callAfter(
			mapmethod("net/minecraft/entity/EntityAgeable", "writeEntityToNBT", "(Lnet/minecraft/nbt/NBTTagCompound;)V"),
			findHookMethod("onAgeableWriteToNBT"));
		callAfter(
			mapmethod("net/minecraft/entity/EntityAgeable", "readEntityFromNBT", "(Lnet/minecraft/nbt/NBTTagCompound;)V"),
			findHookMethod("onAgeableReadFromNBT"));
		replaceMethod(
			mapmethod("net/minecraft/entity/EntityAgeable", "onLivingUpdate", "()V"),
			mapmethod("net/minecraft/entity/EntityAgeable", "setGrowingAge", "(I)V"),
			findHookMethod("onAgeableUpdateGrowingAge"));
		replaceMethod(
			mapforgemethod("net/minecraft/world/World", "updateWeatherBody", "()V"),
			mapmethod("net/minecraft/world/storage/WorldInfo", "setThundering", "(Z)V"),
			findHookMethod("onThunderChange"));
		replaceMethod(
			mapforgemethod("net/minecraft/world/World", "updateWeatherBody", "()V"),
			mapmethod("net/minecraft/world/storage/WorldInfo", "setRaining", "(Z)V"),
			findHookMethod("onRainChange"));
		callAfter(
			mapmethod("net/minecraft/world/World", "canPlaceEntityOnSide", "(IIIIZILnet/minecraft/entity/Entity;)Z"),
			findHookMethod("doBlockCanBuildEvent"));
		replaceMethod(
			mapmethod("net/minecraft/entity/projectile/EntityArrow", "onUpdate", "()V"),
			mapmethod("net/minecraft/entity/Entity", "setFire", "(I)V"),
			findHookMethod("onEntityCombustByArrow"));
		replaceMethod(
			mapmethod("net/minecraft/entity/boss/EntityDragon", "attackEntitiesInList", "(Ljava/util/List;)V"),
			mapmethod("net/minecraft/entity/Entity", "attackEntityFrom", "(Lnet/minecraft/util/DamageSource;I)Z"),
			findHookMethod("onDragonAttackEntity"));
		callBefore(
			mapmethod("net/minecraft/entity/item/EntityXPOrb", "getXPSplit", "(I)I"),
			findHookMethod("betterXPSplit"));
		replaceMethod(
			mapmethod("net/minecraft/entity/item/EntityFallingSand", "fall", "(F)V"),
			mapmethod("net/minecraft/entity/Entity", "attackEntityFrom", "(Lnet/minecraft/util/DamageSource;I)Z"),
			findHookMethod("onAnvilAttackEntity"));
		callAfter(
			mapmethod("net/minecraft/entity/player/EntityPlayer", "setSpawnChunk", "(Lnet/minecraft/util/ChunkCoordinates;Z)V"),
			findHookMethod("onSetPlayerBedLocation"));
		callBefore(
			mapmethod("net/minecraft/entity/item/EntityItem", "combineItems", "(Lnet/minecraft/entity/item/EntityItem;)Z"),
			findHookMethod("dontMergeItemsWithEnchantments"));
		replaceMethod(
			mapmethod("net/minecraft/entity/projectile/EntityLargeFireball", "onImpact", "(Lnet/minecraft/util/MovingObjectPosition;)V"),
			mapmethod("net/minecraft/world/World", "newExplosion", "(Lnet/minecraft/entity/Entity;DDDFZZ)Lnet/minecraft/world/Explosion;"),
			findHookMethod("onLargeFireballExplosion"));
		replaceMethod(
			mapmethod("net/minecraft/entity/effect/EntityLightningBolt", "<init>", "(Lnet/minecraft/world/World;DDDZ)V"),
			mapmethod("net/minecraft/world/World", "setBlockWithNotify", "(IIII)Z"),
			findHookMethod("onBlockIgniteByLightning"));
		replaceMethod(
			mapmethod("net/minecraft/entity/effect/EntityLightningBolt", "onUpdate", "()V"),
			mapmethod("net/minecraft/world/World", "setBlockWithNotify", "(IIII)Z"),
			findHookMethod("onBlockIgniteByLightningUpdate"));
		replaceMethod(
			mapmethod("net/minecraft/entity/effect/EntityLightningBolt", "onUpdate", "()V"),
			mapmethod("net/minecraft/entity/Entity", "onStruckByLightning", "(Lnet/minecraft/entity/effect/EntityLightningBolt;)V"),
			findHookMethod("onLightningStrikeEntity"));
		replaceMethod(
			mapmethod("net/minecraft/entity/EntityLiving", "onEntityUpdate", "()V"),
			mapmethod("net/minecraft/entity/EntityLiving", "isEntityInsideOpaqueBlock", "()Z"),
			findHookMethod("dontSuffocateEnderDragons"));
		replaceMethod(
			mapmethod("net/minecraft/entity/EntityLiving", "onEntityUpdate", "()V"),
			mapmethod("net/minecraft/entity/EntityLiving", "attackEntityFrom", "(Lnet/minecraft/util/DamageSource;I)Z"),
			findHookMethod("onLivingUpdateDamage"));
		replaceMethod(
			mapmethod("net/minecraft/entity/EntityLiving", "onEntityUpdate", "()V"),
			mapmethod("net/minecraft/entity/EntityLiving", "setAir", "(I)V"),
			findHookMethod("onlySetAirIfNecessary"));
		replaceMethod(
			mapmethod("net/minecraft/entity/EntityLiving", "kill", "()V"),
			mapmethod("net/minecraft/entity/EntityLiving", "attackEntityFrom", "(Lnet/minecraft/util/DamageSource;I)Z"),
			findHookMethod("onLivingVoidDamage"));
		callBefore(
			mapforgemethod("net/minecraft/entity/passive/EntityMooshroom", "onSheared", "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;IIII)Ljava/util/ArrayList;"),
			findHookMethod("beforeShearEntity"));
		callBefore(
			mapforgemethod("net/minecraft/entity/passive/EntitySheep", "onSheared", "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;IIII)Ljava/util/ArrayList;"),
			findHookMethod("beforeShearEntity"));
		replaceMethod(
			mapmethod("net/minecraft/entity/passive/EntitySheep", "eatGrassBonus", "()V"),
			mapmethod("net/minecraft/entity/passive/EntitySheep", "setSheared", "(Z)V"),
			findHookMethod("onSheepRegrow"));
		replaceMethod(
			mapmethod("net/minecraft/entity/monster/EntitySkeleton", "onLivingUpdate", "()V"),
			mapmethod("net/minecraft/entity/monster/EntitySkeleton", "setFire", "(I)V"),
			findHookMethod("onUndeadCombust"));
		replaceMethod(
			mapmethod("net/minecraft/entity/projectile/EntitySmallFireball", "onImpact", "(Lnet/minecraft/util/MovingObjectPosition;)V"),
			mapmethod("net/minecraft/entity/Entity", "setFire", "(I)V"),
			findHookMethod("onEntityCombustByFireball"));
		replaceMethod(
			mapmethod("net/minecraft/entity/projectile/EntitySmallFireball", "onImpact", "(Lnet/minecraft/util/MovingObjectPosition;)V"),
			mapmethod("net/minecraft/world/World", "setBlockWithNotify", "(IIII)Z"),
			findHookMethod("onBlockCombustByFireball"));
		replaceMethod(
			mapmethod("net/minecraft/entity/monster/EntitySnowman", "onLivingUpdate", "()V"),
			mapmethod("net/minecraft/entity/monster/EntitySnowman", "attackEntityFrom", "(Lnet/minecraft/util/DamageSource;I)Z"),
			findHookMethod("onSnowmanEnvironmentalDamage"));
		replaceMethod(
			mapmethod("net/minecraft/entity/monster/EntitySnowman", "onLivingUpdate", "()V"),
			mapmethod("net/minecraft/world/World", "setBlockWithNotify", "(IIII)Z"),
			findHookMethod("onSnowmanPlaceTrail"));
		replaceMethod(
			mapmethod("net/minecraft/entity/boss/EntityWither", "updateAITasks", "()V"),
			mapmethod("net/minecraft/world/World", "newExplosion", "(Lnet/minecraft/entity/Entity;DDDFZZ)Lnet/minecraft/world/Explosion;"),
			findHookMethod("onWitherExplode"));
		replaceMethod(
			mapmethod("net/minecraft/entity/projectile/EntityWitherSkull", "onImpact", "(Lnet/minecraft/util/MovingObjectPosition;)V"),
			mapmethod("net/minecraft/entity/EntityLiving", "heal", "(I)V"),
			findHookMethod("onWitherHealFromSkullImpact"));
		replaceMethod(
			mapmethod("net/minecraft/entity/projectile/EntityWitherSkull", "onImpact", "(Lnet/minecraft/util/MovingObjectPosition;)V"),
			mapmethod("net/minecraft/world/World", "newExplosion", "(Lnet/minecraft/entity/Entity;DDDFZZ)Lnet/minecraft/world/Explosion;"),
			findHookMethod("onWitherSkullExplode"));
		replaceMethod(
			mapmethod("net/minecraft/entity/monster/EntityZombie", "onLivingUpdate", "()V"),
			mapmethod("net/minecraft/entity/monster/EntityZombie", "setFire", "(I)V"),
			findHookMethod("onUndeadCombust"));
		replaceMethod(
			mapmethod("net/minecraft/util/FoodStats", "onUpdate", "(Lnet/minecraft/entity/player/EntityPlayer;)V"),
			mapmethod("net/minecraft/entity/player/EntityPlayer", "heal", "(I)V"),
			findHookMethod("onFullHungerHeal"));
		replaceMethod(
			mapmethod("net/minecraft/util/FoodStats", "onUpdate", "(Lnet/minecraft/entity/player/EntityPlayer;)V"),
			mapmethod("net/minecraft/entity/player/EntityPlayer", "attackEntityFrom", "(Lnet/minecraft/util/DamageSource;I)Z"),
			findHookMethod("onPlayerStarve"));
		callBefore(
			mapmethod("net/minecraft/server/MinecraftServer", "getPlugins", "()Ljava/lang/String;"),
			findHookMethod("getPlugins"));
		callBefore(
			mapmethod("net/minecraft/server/MinecraftServer", "executeCommand", "(Ljava/lang/String;)Ljava/lang/String;"),
			findHookMethod("executeCommandThreadSafely"));
		callBefore(
			mapmethod("net/minecraft/server/MinecraftServer", "getServerModName", "()Ljava/lang/String;"),
			findHookMethod("getServerModName"));
		callBefore(
			mapmethod("net/minecraft/server/MinecraftServer", "getPossibleCompletions", "(Lnet/minecraft/command/ICommandSender;Ljava/lang/String;)Ljava/util/List;"),
			findHookMethod("getPossibleTabCompletions"));
		replaceMethod(
			mapmethod("net/minecraft/potion/Potion", "performEffect", "(Lnet/minecraft/entity/EntityLiving;I)V"),
			mapmethod("net/minecraft/entity/EntityLiving", "heal", "(I)V"),
			findHookMethod("onPotionRegen"));
		replaceMethod(
			mapmethod("net/minecraft/potion/Potion", "performEffect", "(Lnet/minecraft/entity/EntityLiving;I)V"),
			mapmethod("net/minecraft/entity/EntityLiving", "attackEntityFrom", "(Lnet/minecraft/util/DamageSource;I)Z"),
			findHookMethod("onPotionDamage"));
		callBefore(
			mapmethod("net/minecraft/network/NetServerHandler", "kickPlayerFromServer", "(Ljava/lang/String;)V"),
			findHookMethod("onPlayerKick"));
		replaceMethod(
			mapmethod("net/minecraft/network/NetServerHandler", "handleChat", "(Lnet/minecraft/network/packet/Packet3Chat;)V"),
			mapmethod("net/minecraft/network/NetServerHandler", "kickPlayerFromServer", "(Ljava/lang/String;)V"),
			findHookMethod("threadSafelyKickPlayer"));
		callBefore(
			mapmethod("net/minecraft/network/NetServerHandler", "handleSlashCommand", "(Ljava/lang/String;)V"),
			findHookMethod("processBukkitCommand"));
		replaceMethod(
			mapmethod("net/minecraft/network/NetServerHandler", "handleAnimation", "(Lnet/minecraft/network/packet/Packet18Animation;)V"),
			mapmethod("net/minecraft/entity/player/EntityPlayerMP", "swingItem", "()V"),
			findHookMethod("onSwingItem"));
		callBefore(
			mapmethod("net/minecraft/network/NetServerHandler", "handleEntityAction", "(Lnet/minecraft/network/packet/Packet19EntityAction;)V"),
			findHookMethod("onEntityActionPacket"));
		callBefore(
			mapmethod("net/minecraft/network/NetServerHandler", "handleKickDisconnect", "(Lnet/minecraft/network/packet/Packet255KickDisconnect;)V"),
			findHookMethod("onPlayerDisconnect"));
		replaceMethod(
			mapmethod("net/minecraft/network/NetServerHandler", "handleUseEntity", "(Lnet/minecraft/network/packet/Packet7UseEntity;)V"),
			mapmethod("net/minecraft/entity/player/EntityPlayerMP", "interactWith", "(Lnet/minecraft/entity/Entity;)Z"),
			findHookMethod("onPlayerRightClickEntity"));
		replaceMethod(
			mapmethod("net/minecraft/network/NetServerHandler", "handleUseEntity", "(Lnet/minecraft/network/packet/Packet7UseEntity;)V"),
			mapmethod("net/minecraft/entity/player/EntityPlayerMP", "attackTargetEntityWithCurrentItem", "(Lnet/minecraft/entity/Entity;)V"),
			findHookMethod("onPlayerLeftClickEntity"));
		callBefore(
			mapmethod("net/minecraft/network/NetServerHandler", "handleCloseWindow", "(Lnet/minecraft/network/packet/Packet101CloseWindow;)V"),
			findHookMethod("onPlayerCloseWindow"));
		callBefore(
			mapmethod("net/minecraft/network/NetServerHandler", "handleTransaction", "(Lnet/minecraft/network/packet/Packet106Transaction;)V"),
			findHookMethod("checkPlayerDead"));
		callBefore(
			mapmethod("net/minecraft/network/NetServerHandler", "handleUpdateSign", "(Lnet/minecraft/network/packet/Packet130UpdateSign;)V"),
			findHookMethod("checkPlayerDead"));
		replaceMethod(
			mapmethod("net/minecraft/network/TcpConnection", "processReadPackets", "()V"),
			mapmethod("net/minecraft/network/packet/Packet", "processPacket", "(Lnet/minecraft/network/packet/NetHandler;)V"),
			findHookMethod("checkNetHandlerDisconnected"));
		replaceMethod(
			mapmethod("net/minecraft/network/TcpWriterThread", "run", "()V"),
			new Method("java/io/IOException", "printStackTrace", "()V"),
			findHookMethod("dontSpamConsoleOnUnexpectedDisconnect"));
		callBefore(
			mapmethod("net/minecraft/entity/ai/EntityAIArrowAttack", "resetTask", "()V"),
			findHookMethod("onArrowAttackTaskReset"));
		replaceMethod(
			mapmethod("net/minecraft/entity/ai/EntityAIMate", "spawnBaby", "()V"),
			mapmethod("net/minecraft/world/World", "spawnEntityInWorld", "(Lnet/minecraft/entity/Entity;)Z"),
			findHookMethod("onEntitySpawnBaby"));
		callBefore(
			mapmethod("net/minecraft/entity/ai/EntityAIAttackOnCollide", "resetTask", "()V"),
			findHookMethod("onMeleeAttackTaskReset"));
		callBefore(
			mapmethod("net/minecraft/server/management/ServerConfigurationManager", "playerLoggedIn", "(Lnet/minecraft/entity/player/EntityPlayerMP;)V"),
			findHookMethod("detectListNameConflict"));
		callAfter(
			mapmethod("net/minecraft/server/management/ServerConfigurationManager", "addOp", "(Ljava/lang/String;)V"),
			findHookMethod("recalcPermissionsOnOperatorChange"));
		callAfter(
			mapmethod("net/minecraft/server/management/ServerConfigurationManager", "removeOp", "(Ljava/lang/String;)V"),
			findHookMethod("recalcPermissionsOnOperatorChange"));
		replaceMethod(
			mapmethod("net/minecraft/server/management/ServerConfigurationManager", "removeAllPlayers", "()V"),
			mapmethod("net/minecraft/network/NetServerHandler", "kickPlayerFromServer", "(Ljava/lang/String;)V"),
			findHookMethod("useCustomShutdownMessage"));
		replaceMethod(
			mapmethod("net/minecraft/tileentity/TileEntityFurnace", "updateEntity", "()V"),
			mapmethod("net/minecraft/tileentity/TileEntityFurnace", "getItemBurnTime", "(Lnet/minecraft/item/ItemStack;)I"),
			findHookMethod("callFurnaceBurnEvent"));
		replaceMethod(
			mapmethod("net/minecraft/tileentity/TileEntityFurnace", "smeltItem", "()V"),
			mapforgemethod("net/minecraft/item/crafting/FurnaceRecipes", "getSmeltingResult", "(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"),
			findHookMethod("callFurnaceSmeltEvent"));
		replaceMethod(
			mapmethod("net/minecraft/tileentity/TileEntityMobSpawner", "updateEntity", "()V"),
			mapmethod("net/minecraft/world/World", "spawnEntityInWorld", "(Lnet/minecraft/entity/Entity;)Z"),
			findHookMethod("useMobSpawnerSpawnReason"));
		replaceMethod(
			mapmethod("net/minecraft/tileentity/TileEntityNote", "triggerNote", "(Lnet/minecraft/world/World;III)V"),
			mapmethod("net/minecraft/world/World", "addBlockEvent", "(IIIIII)V"),
			findHookMethod("callNotePlayEvent"));
		replaceMethod(
			mapmethod("net/minecraft/village/Village", "tick", "(I)V"),
			mapmethod("net/minecraft/world/World", "spawnEntityInWorld", "(Lnet/minecraft/entity/Entity;)Z"),
			findHookMethod("useVillageDefenseSpawnReason"));
		replaceMethod(
			mapmethod("net/minecraft/village/VillageSiege", "spawnZombie", "()Z"),
			mapmethod("net/minecraft/world/World", "spawnEntityInWorld", "(Lnet/minecraft/entity/Entity;)Z"),
			findHookMethod("useVillageInvasionSpawnReason"));
		callBefore(
			mapmethod("net/minecraft/world/WorldServer", "resetRainAndThunder", "()V"),
			findHookMethod("resetRainAndThunder"));
		callBefore(
			mapmethod("net/minecraft/world/WorldServer", "addWeatherEffect", "(Lnet/minecraft/entity/Entity;)Z"),
			findHookMethod("callLightningStrikeEvent"));
	}
}
