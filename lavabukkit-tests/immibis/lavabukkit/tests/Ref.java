package immibis.lavabukkit.tests;

public class Ref<T> {
	private T val;
	public Ref() {val = null;}
	public Ref(T t) {val = t;}
	public synchronized void set(T t) {
		val = t;
	}
	public synchronized T get() {
		return val;
	}
}
