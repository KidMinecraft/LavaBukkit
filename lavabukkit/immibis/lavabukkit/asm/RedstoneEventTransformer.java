package immibis.lavabukkit.asm;

import immibis.lavabukkit.nms.Mappings;
import net.minecraft.block.Block;
import net.minecraft.world.World;

import org.bukkit.event.block.BlockRedstoneEvent;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class RedstoneEventTransformer extends ASMTransformerBase {

	private Method methodToEdit = Mappings.MCP_to_current.mapMethod(new Method("net/minecraft/block/Block", "onNeighborBlockChange", "(Lnet/minecraft/world/World;IIII)V"));
	
	@Override
	public ClassVisitor transform(String name, ClassVisitor parent) {
		if(!name.replace('.', '/').equals(methodToEdit.owner))
			return parent;
		
		return new ClassVisitor(Opcodes.ASM4, parent) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				if(!name.equals(methodToEdit.name) || !desc.equals(methodToEdit.desc)) {
					return super.visitMethod(access, name, desc, signature, exceptions);
				}
				
				// replace this with our own method
				MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
				
				mv.visitCode();
					mv.visitVarInsn(Opcodes.ALOAD, 1);
					mv.visitVarInsn(Opcodes.ILOAD, 2);
					mv.visitVarInsn(Opcodes.ILOAD, 3);
					mv.visitVarInsn(Opcodes.ILOAD, 4);
					mv.visitVarInsn(Opcodes.ILOAD, 5);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, "immibis/lavabukkit/asm/RedstoneEventTransformer$Hooks", "onNeighborBlockChange", "(L"+Mappings.MCP_to_current.mapClass("net/minecraft/world/World")+";IIII)V");
					mv.visitInsn(Opcodes.RETURN);
					
					mv.visitMaxs(5, 6);
				mv.visitEnd();
				
				return null;
			}
		};
	}
	
	public static class Hooks {
		// causes early class loading if directly in RedstoneEventTransformer
		public static void onNeighborBlockChange(World world, int x, int y, int z, int id) {
			if (!world.isRemote && Block.blocksList[id] != null && Block.blocksList[id].canProvidePower()) {
	            org.bukkit.block.Block block = world.getWorld().getBlockAt(x, y, z);
	            int power = block.getBlockPower();
	
	            BlockRedstoneEvent event = new BlockRedstoneEvent(block, power, power);
	            world.getServer().getPluginManager().callEvent(event);
	        }
		}
	}

}
