package immibis.lavabukkit.tools.cbmapper;

import java.util.Comparator;
import java.util.TreeSet;

public abstract class MappableObject<FullType> {
	/** true = mcdev, false = vanilla */
	public boolean is_mcdev;
	
	public String lastSignature, nextSignature, selfSignature;
	
	// set of objects dependent on this one
	public TreeSet<Object> referenceSet = new TreeSet<Object>(new Comparator<Object>() {
		public int compare(Object a, Object b) {
			String sa = a.toString();
			String sb = b.toString();
			if(sa.equals(sb))
				return System.identityHashCode(a) - System.identityHashCode(b);
			else
				return sa.compareTo(sb);
		}
	});
	
	public final void updateNextSignature() {
		nextSignature = referenceSet.toString();
	}
	
	public String fullSignature;
	
	public final String toString() {
		if(fullSignature != null)
			return fullSignature;
		return fullSignature = selfSignature + " " + lastSignature;
	}
	
	public void updateSelfSignature() {}

	public String toHumanString() {
		return getClass().getSimpleName();
	}

	public void addReferences() {}
}
