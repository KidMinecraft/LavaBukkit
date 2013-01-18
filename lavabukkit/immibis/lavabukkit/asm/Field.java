package immibis.lavabukkit.asm;

import org.apache.commons.lang.Validate;

public class Field {
	public final String name;
	public final String owner;
	
	public Field(String o, String n) {
		this.name = n;
		this.owner = o;
		
		Validate.notNull(name, "Name cannot be null");
		Validate.notNull(owner, "Owner cannot be null");
	}
	
	@Override
	public String toString() {
		return owner + "/" + name;
	}
	
	
	@Override
	public int hashCode() {
		return name.hashCode() + owner.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof Field) {
			Field f = (Field)o;
			return f.name.equals(name) && f.owner.equals(owner);
		}
		return false;
	}
	
	// parses a string of the form package/class/field
	public static Field parse(String s) {
		int j = s.lastIndexOf('/');
		return new Field(s.substring(0, j), s.substring(j+1));
	}
}