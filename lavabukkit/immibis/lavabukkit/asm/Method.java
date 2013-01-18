package immibis.lavabukkit.asm;

import org.apache.commons.lang.Validate;

public class Method {
	public final String name, owner, desc;
	public Method(String o, String n, String d) {
		name = n;
		owner = o;
		desc = d;
		
		Validate.notNull(name, "Name cannot be null");
		Validate.notNull(owner, "Owner cannot be null");
		Validate.notNull(desc, "Descriptor cannot be null");
	}
	
	@Override
	public String toString() {
		return owner + "/" + name + desc;
	}
	
	@Override
	public int hashCode() {
		return name.hashCode() + owner.hashCode() + desc.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof Method) {
			Method m = (Method)o;
			return m.name.equals(name) && m.owner.equals(owner) && m.desc.equals(desc);
		}
		return false;
	}

	// parses a string of the form package/class/method(params)returntype
	public static Method parse(String s) {
		int i = s.indexOf('(');
		int j = s.lastIndexOf('/', i);
		return new Method(s.substring(0, j), s.substring(j+1, i), s.substring(i));
	}
}