package immibis.lavabukkit.nms;

public interface IClassAccessor {
	// name is not in internal form - uses . as package separator
	public byte[] getUntransformedClassBytes(String name) throws ClassNotFoundException;
}
