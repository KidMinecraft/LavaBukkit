package immibis.lavabukkit.tests;

public interface EventMatcher<T extends org.bukkit.event.Event> {
	public boolean matches(T event) throws Exception;
}
