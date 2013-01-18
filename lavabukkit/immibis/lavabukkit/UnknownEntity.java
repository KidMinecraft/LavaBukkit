package immibis.lavabukkit;

import net.minecraft.entity.Entity;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.EntityType;

public class UnknownEntity extends CraftEntity {
	public UnknownEntity(CraftServer server, Entity ent) {
		super(server, ent);
	}

	@Override
	public EntityType getType() {
		return EntityType.UNKNOWN;
	}
}
