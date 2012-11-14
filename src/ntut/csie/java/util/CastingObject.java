package ntut.csie.java.util;

import java.util.HashMap;
import java.util.TreeMap;

/**
 * 這個 class是從StackOverFlow找來的。
 * 它的目的是我幫助我們在casting物件時候，透過這些檢查機制，讓java的warning不會出現。
 * @author charles
 *
 */
public class CastingObject {
	public static <K, V> HashMap<K, V> castHash(HashMap<?, ?> input,
			Class<K> keyClass, Class<V> valueClass) {
		HashMap<K, V> output = new HashMap<K, V>();
		if (input == null)
			return output;
		for (Object key : input.keySet().toArray()) {
			if ((key == null) || (keyClass.isAssignableFrom(key.getClass()))) {
				Object value = input.get(key);
				if ((value == null)
						|| (valueClass.isAssignableFrom(value.getClass()))) {
					K k = keyClass.cast(key);
					V v = valueClass.cast(value);
					output.put(k, v);
				} else {
					throw new AssertionError("Cannot cast to HashMap<"
							+ keyClass.getSimpleName() + ", "
							+ valueClass.getSimpleName() + ">" + ", value "
							+ value + " is not a " + valueClass.getSimpleName());
				}
			} else {
				throw new AssertionError("Cannot cast to HashMap<"
						+ keyClass.getSimpleName() + ", "
						+ valueClass.getSimpleName() + ">" + ", key " + key
						+ " is not a " + keyClass.getSimpleName());
			}
		}
		return output;
	}
	
	public static <K, V> TreeMap<K, V> castTreeMap(TreeMap<?, ?> input,
			Class<K> keyClass, Class<V> valueClass) {
		TreeMap<K, V> output = new TreeMap<K, V>();
		if (input == null)
			return output;
		for (Object key : input.keySet().toArray()) {
			if ((key == null) || (keyClass.isAssignableFrom(key.getClass()))) {
				Object value = input.get(key);
				if ((value == null)
						|| (valueClass.isAssignableFrom(value.getClass()))) {
					K k = keyClass.cast(key);
					V v = valueClass.cast(value);
					output.put(k, v);
				} else {
					throw new AssertionError("Cannot cast to TreeMap<"
							+ keyClass.getSimpleName() + ", "
							+ valueClass.getSimpleName() + ">" + ", value "
							+ value + " is not a " + valueClass.getSimpleName());
				}
			} else {
				throw new AssertionError("Cannot cast to TreeMap<"
						+ keyClass.getSimpleName() + ", "
						+ valueClass.getSimpleName() + ">" + ", key " + key
						+ " is not a " + keyClass.getSimpleName());
			}
		}
		return output;
	}
	
	public static <K, V> TreeMap<K, V> castTreeMap(Object inputObject,
			Class<K> keyClass, Class<V> valueClass) {
		TreeMap<K, V> output = new TreeMap<K, V>();
		
		if(!(inputObject instanceof TreeMap<?, ?>)) {
			return output;
		}
		
		TreeMap<?, ?> input = (TreeMap<?, ?>)inputObject;
		
		for (Object key : input.keySet().toArray()) {
			if ((key == null) || (keyClass.isAssignableFrom(key.getClass()))) {
				Object value = input.get(key);
				if ((value == null)
						|| (valueClass.isAssignableFrom(value.getClass()))) {
					K k = keyClass.cast(key);
					V v = valueClass.cast(value);
					output.put(k, v);
				} else {
					throw new AssertionError("Cannot cast to TreeMap<"
							+ keyClass.getSimpleName() + ", "
							+ valueClass.getSimpleName() + ">" + ", value "
							+ value + " is not a " + valueClass.getSimpleName());
				}
			} else {
				throw new AssertionError("Cannot cast to TreeMap<"
						+ keyClass.getSimpleName() + ", "
						+ valueClass.getSimpleName() + ">" + ", key " + key
						+ " is not a " + keyClass.getSimpleName());
			}
		}
		return output;
	}
}