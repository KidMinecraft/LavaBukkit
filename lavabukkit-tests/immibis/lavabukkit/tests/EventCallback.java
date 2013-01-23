package immibis.lavabukkit.tests;

public interface EventCallback<T extends org.bukkit.event.Event> {
	public void handle(T event) throws Exception;
}
