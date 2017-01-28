import java.util.AbstractMap;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

public class RbTree<K, V> {

	private final Comparator<? super K> comparator;

	private transient Entry<K, V> root = null;

	/**
	 * The number of entries in the tree
	 */
	private transient int size = 0;

	/**
	 * The number of structural modifications to the tree.
	 */
	private transient int modCount = 0;

	public RbTree() {
		comparator = null;
	}

	public RbTree(Comparator<? super K> comparator) {
		this.comparator = comparator;
	}

	// Query Operations

	/**
	 * Returns the number of key-value mappings in this map.
	 *
	 * @return the number of key-value mappings in this map
	 */
	public int size() {
		return size;
	}

	public boolean containsKey(Object key) {
		return getEntry(key) != null;
	}

	public boolean containsValue(Object value) {
		for (Entry<K, V> e = getFirstEntry(); e != null; e = successor(e))
			if (valEquals(value, e.value))
				return true;
		return false;
	}

	public V get(Object key) {
		Entry<K, V> p = getEntry(key);
		return (p == null ? null : p.value);
	}

	public Comparator<? super K> comparator() {
		return comparator;
	}

	public K firstKey() {
		return key(getFirstEntry());
	}

	public K lastKey() {
		return key(getLastEntry());
	}

	final Entry<K, V> getEntry(Object key) {
		// Offload comparator-based version for sake of performance
		if (comparator != null)
			return getEntryUsingComparator(key);
		if (key == null)
			throw new NullPointerException();
		Comparable<? super K> k = (Comparable<? super K>) key;
		Entry<K, V> p = root;
		while (p != null) {
			int cmp = k.compareTo(p.key);
			if (cmp < 0)
				p = p.left;
			else if (cmp > 0)
				p = p.right;
			else
				return p;
		}
		return null;
	}

	/**
	 * Version of getEntry using comparator. Split off from getEntry for
	 * performance. (This is not worth doing for most methods, that are less
	 * dependent on comparator performance, but is worthwhile here.)
	 */
	final Entry<K, V> getEntryUsingComparator(Object key) {
		K k = (K) key;
		Comparator<? super K> cpr = comparator;
		if (cpr != null) {
			Entry<K, V> p = root;
			while (p != null) {
				int cmp = cpr.compare(k, p.key);
				if (cmp < 0)
					p = p.left;
				else if (cmp > 0)
					p = p.right;
				else
					return p;
			}
		}
		return null;
	}

	/**
	 * Gets the entry corresponding to the specified key; if no such entry
	 * exists, returns the entry for the least key greater than the specified
	 * key; if no such entry exists (i.e., the greatest key in the Tree is less
	 * than the specified key), returns <tt>null</tt>.
	 */
	final Entry<K, V> getCeilingEntry(K key) {
		Entry<K, V> p = root;
		while (p != null) {
			int cmp = compare(key, p.key);
			if (cmp < 0) {
				if (p.left != null)
					p = p.left;
				else
					return p;
			} else if (cmp > 0) {
				if (p.right != null) {
					p = p.right;
				} else {
					Entry<K, V> parent = p.parent;
					Entry<K, V> ch = p;
					while (parent != null && ch == parent.right) {
						ch = parent;
						parent = parent.parent;
					}
					return parent;
				}
			} else
				return p;
		}
		return null;
	}

	/**
	 * Gets the entry corresponding to the specified key; if no such entry
	 * exists, returns the entry for the greatest key less than the specified
	 * key; if no such entry exists, returns <tt>null</tt>.
	 */
	final Entry<K, V> getFloorEntry(K key) {
		Entry<K, V> p = root;
		while (p != null) {
			int cmp = compare(key, p.key);
			if (cmp > 0) {
				if (p.right != null)
					p = p.right;
				else
					return p;
			} else if (cmp < 0) {
				if (p.left != null) {
					p = p.left;
				} else {
					Entry<K, V> parent = p.parent;
					Entry<K, V> ch = p;
					while (parent != null && ch == parent.left) {
						ch = parent;
						parent = parent.parent;
					}
					return parent;
				}
			} else
				return p;

		}
		return null;
	}

	/**
	 * Gets the entry for the least key greater than the specified key; if no
	 * such entry exists, returns the entry for the least key greater than the
	 * specified key; if no such entry exists returns <tt>null</tt>.
	 */
	final Entry<K, V> getHigherEntry(K key) {
		Entry<K, V> p = root;
		while (p != null) {
			int cmp = compare(key, p.key);
			if (cmp < 0) {
				if (p.left != null)
					p = p.left;
				else
					return p;
			} else {
				if (p.right != null) {
					p = p.right;
				} else {
					Entry<K, V> parent = p.parent;
					Entry<K, V> ch = p;
					while (parent != null && ch == parent.right) {
						ch = parent;
						parent = parent.parent;
					}
					return parent;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the entry for the greatest key less than the specified key; if no
	 * such entry exists (i.e., the least key in the Tree is greater than the
	 * specified key), returns <tt>null</tt>.
	 */
	final Entry<K, V> getLowerEntry(K key) {
		Entry<K, V> p = root;
		while (p != null) {
			int cmp = compare(key, p.key);
			if (cmp > 0) {
				if (p.right != null)
					p = p.right;
				else
					return p;
			} else {
				if (p.left != null) {
					p = p.left;
				} else {
					Entry<K, V> parent = p.parent;
					Entry<K, V> ch = p;
					while (parent != null && ch == parent.left) {
						ch = parent;
						parent = parent.parent;
					}
					return parent;
				}
			}
		}
		return null;
	}

	public V put(K key, V value) {
		Entry<K, V> t = root;
		if (t == null) {
			root = new Entry<K, V>(key, value, null);
			size = 1;
			modCount++;
			return null;
		}
		int cmp;
		Entry<K, V> parent;
		// split comparator and comparable paths
		Comparator<? super K> cpr = comparator;
		if (cpr != null) {
			do {
				parent = t;
				cmp = cpr.compare(key, t.key);
				if (cmp < 0)
					t = t.left;
				else if (cmp > 0)
					t = t.right;
				else
					return t.setValue(value);
			} while (t != null);
		} else {
			if (key == null)
				throw new NullPointerException();
			Comparable<? super K> k = (Comparable<? super K>) key;
			do {
				parent = t;
				cmp = k.compareTo(t.key);
				if (cmp < 0)
					t = t.left;
				else if (cmp > 0)
					t = t.right;
				else
					return t.setValue(value);
			} while (t != null);
		}
		Entry<K, V> e = new Entry<K, V>(key, value, parent);
		if (cmp < 0)
			parent.left = e;
		else
			parent.right = e;
		fixAfterInsertion(e);
		size++;
		modCount++;
		return null;
	}

	public V remove(Object key) {
		Entry<K, V> p = getEntry(key);
		if (p == null)
			return null;

		V oldValue = p.value;
		deleteEntry(p);
		return oldValue;
	}

	public void clear() {
		modCount++;
		size = 0;
		root = null;
	}

	// NavigableMap API methods

	/**
	 * @since 1.6
	 */
	public Map.Entry<K, V> firstEntry() {
		return exportEntry(getFirstEntry());
	}

	/**
	 * @since 1.6
	 */
	public Map.Entry<K, V> lastEntry() {
		return exportEntry(getLastEntry());
	}

	public Map.Entry<K, V> lowerEntry(K key) {
		return exportEntry(getLowerEntry(key));
	}

	public K lowerKey(K key) {
		return keyOrNull(getLowerEntry(key));
	}

	public Map.Entry<K, V> floorEntry(K key) {
		return exportEntry(getFloorEntry(key));
	}

	public K floorKey(K key) {
		return keyOrNull(getFloorEntry(key));
	}

	public Map.Entry<K, V> ceilingEntry(K key) {
		return exportEntry(getCeilingEntry(key));
	}

	public K ceilingKey(K key) {
		return keyOrNull(getCeilingEntry(key));
	}

	public Map.Entry<K, V> higherEntry(K key) {
		return exportEntry(getHigherEntry(key));
	}

	public K higherKey(K key) {
		return keyOrNull(getHigherEntry(key));
	}

	Iterator<K> keyIterator() {
		return new KeyIterator(getFirstEntry());
	}

	/**
	 * Base class for TreeMap Iterators
	 */
	abstract class PrivateEntryIterator<T> implements Iterator<T> {
		Entry<K, V> next;
		Entry<K, V> lastReturned;
		int expectedModCount;

		PrivateEntryIterator(Entry<K, V> first) {
			expectedModCount = modCount;
			lastReturned = null;
			next = first;
		}

		@Override
		public final boolean hasNext() {
			return next != null;
		}

		final Entry<K, V> nextEntry() {
			Entry<K, V> e = next;
			if (e == null)
				throw new NoSuchElementException();
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
			next = successor(e);
			lastReturned = e;
			return e;
		}

		final Entry<K, V> prevEntry() {
			Entry<K, V> e = next;
			if (e == null)
				throw new NoSuchElementException();
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
			next = predecessor(e);
			lastReturned = e;
			return e;
		}

		@Override
		public void remove() {
			if (lastReturned == null)
				throw new IllegalStateException();
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
			// deleted entries are replaced by their successors
			if (lastReturned.left != null && lastReturned.right != null)
				next = lastReturned;
			deleteEntry(lastReturned);
			expectedModCount = modCount;
			lastReturned = null;
		}
	}

	final class EntryIterator extends PrivateEntryIterator<Map.Entry<K, V>> {
		EntryIterator(Entry<K, V> first) {
			super(first);
		}
		@Override
		public Map.Entry<K, V> next() {
			return nextEntry();
		}
	}

	final class KeyIterator extends PrivateEntryIterator<K> {
		KeyIterator(Entry<K, V> first) {
			super(first);
		}
		@Override
		public K next() {
			return nextEntry().key;
		}
	}

	// Little utilities

	/**
	 * Compares two keys using the correct comparison method for this TreeMap.
	 */
	final int compare(Object k1, Object k2) {
		return comparator == null
				? ((Comparable<? super K>) k1).compareTo((K) k2)
				: comparator.compare((K) k1, (K) k2);
	}

	/**
	 * Test two values for equality. Differs from o1.equals(o2) only in that it
	 * copes with <tt>null</tt> o1 properly.
	 */
	final static boolean valEquals(Object o1, Object o2) {
		return (o1 == null ? o2 == null : o1.equals(o2));
	}

	/**
	 * Return SimpleImmutableEntry for entry, or null if null
	 */
	static <K, V> Map.Entry<K, V> exportEntry(RbTree.Entry<K, V> e) {
		return e == null ? null : new AbstractMap.SimpleImmutableEntry<K, V>(e);
	}

	/**
	 * Return key for entry, or null if null
	 */
	static <K, V> K keyOrNull(RbTree.Entry<K, V> e) {
		return e == null ? null : e.key;
	}

	/**
	 * Returns the key corresponding to the specified Entry.
	 * 
	 * @throws NoSuchElementException
	 *             if the Entry is null
	 */
	static <K> K key(Entry<K, ?> e) {
		if (e == null)
			throw new NoSuchElementException();
		return e.key;
	}

	// Red-black mechanics

	private static final boolean RED = false;
	private static final boolean BLACK = true;

	/**
	 * Node in the Tree. Doubles as a means to pass key-value pairs back to user
	 * (see Map.Entry).
	 */

	static final class Entry<K, V> implements Map.Entry<K, V> {
		K key;
		V value;
		Entry<K, V> left = null;
		Entry<K, V> right = null;
		Entry<K, V> parent;
		boolean color = BLACK;

		/**
		 * Make a new cell with given key, value, and parent, and with
		 * <tt>null</tt> child links, and BLACK color.
		 */
		Entry(K key, V value, Entry<K, V> parent) {
			this.key = key;
			this.value = value;
			this.parent = parent;
		}

		/**
		 * Returns the key.
		 *
		 * @return the key
		 */
		@Override
		public K getKey() {
			return key;
		}

		/**
		 * Returns the value associated with the key.
		 *
		 * @return the value associated with the key
		 */
		@Override
		public V getValue() {
			return value;
		}

		/**
		 * Replaces the value currently associated with the key with the given
		 * value.
		 *
		 * @return the value associated with the key before this method was
		 *         called
		 */
		@Override
		public V setValue(V value) {
			V oldValue = this.value;
			this.value = value;
			return oldValue;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;

			return valEquals(key, e.getKey()) && valEquals(value, e.getValue());
		}

		@Override
		public int hashCode() {
			int keyHash = (key == null ? 0 : key.hashCode());
			int valueHash = (value == null ? 0 : value.hashCode());
			return keyHash ^ valueHash;
		}

		@Override
		public String toString() {
			return key + "=" + value;
		}
	}

	/**
	 * Returns the first Entry in the TreeMap (according to the TreeMap's
	 * key-sort function). Returns null if the TreeMap is empty.
	 */
	final Entry<K, V> getFirstEntry() {
		Entry<K, V> p = root;
		if (p != null)
			while (p.left != null)
				p = p.left;
		return p;
	}

	/**
	 * Returns the last Entry in the TreeMap (according to the TreeMap's
	 * key-sort function). Returns null if the TreeMap is empty.
	 */
	final Entry<K, V> getLastEntry() {
		Entry<K, V> p = root;
		if (p != null)
			while (p.right != null)
				p = p.right;
		return p;
	}

	/**
	 * Returns the successor of the specified Entry, or null if no such.
	 */
	static <K, V> RbTree.Entry<K, V> successor(Entry<K, V> t) {
		if (t == null)
			return null;
		else if (t.right != null) {
			Entry<K, V> p = t.right;
			while (p.left != null)
				p = p.left;
			return p;
		} else {
			Entry<K, V> p = t.parent;
			Entry<K, V> ch = t;
			while (p != null && ch == p.right) {
				ch = p;
				p = p.parent;
			}
			return p;
		}
	}

	/**
	 * Returns the predecessor of the specified Entry, or null if no such.
	 */
	static <K, V> Entry<K, V> predecessor(Entry<K, V> t) {
		if (t == null)
			return null;
		else if (t.left != null) {
			Entry<K, V> p = t.left;
			while (p.right != null)
				p = p.right;
			return p;
		} else {
			Entry<K, V> p = t.parent;
			Entry<K, V> ch = t;
			while (p != null && ch == p.left) {
				ch = p;
				p = p.parent;
			}
			return p;
		}
	}

	/**
	 * Balancing operations.
	 *
	 * Implementations of rebalancings during insertion and deletion are
	 * slightly different than the CLR version. Rather than using dummy
	 * nilnodes, we use a set of accessors that deal properly with null. They
	 * are used to avoid messiness surrounding nullness checks in the main
	 * algorithms.
	 */

	private static <K, V> boolean colorOf(Entry<K, V> p) {
		return (p == null ? BLACK : p.color);
	}

	private static <K, V> Entry<K, V> parentOf(Entry<K, V> p) {
		return (p == null ? null : p.parent);
	}

	private static <K, V> void setColor(Entry<K, V> p, boolean c) {
		if (p != null)
			p.color = c;
	}

	private static <K, V> Entry<K, V> leftOf(Entry<K, V> p) {
		return (p == null) ? null : p.left;
	}

	private static <K, V> Entry<K, V> rightOf(Entry<K, V> p) {
		return (p == null) ? null : p.right;
	}

	/** From CLR */
	private void rotateLeft(Entry<K, V> p) {
		if (p != null) {
			Entry<K, V> r = p.right;
			p.right = r.left;
			if (r.left != null)
				r.left.parent = p;
			r.parent = p.parent;
			if (p.parent == null)
				root = r;
			else if (p.parent.left == p)
				p.parent.left = r;
			else
				p.parent.right = r;
			r.left = p;
			p.parent = r;
		}
	}

	/** From CLR */
	private void rotateRight(Entry<K, V> p) {
		if (p != null) {
			Entry<K, V> l = p.left;
			p.left = l.right;
			if (l.right != null)
				l.right.parent = p;
			l.parent = p.parent;
			if (p.parent == null)
				root = l;
			else if (p.parent.right == p)
				p.parent.right = l;
			else
				p.parent.left = l;
			l.right = p;
			p.parent = l;
		}
	}

	/** From CLR */
	private void fixAfterInsertion(Entry<K, V> x) {
		x.color = RED;

		while (x != null && x != root && x.parent.color == RED) {
			if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
				Entry<K, V> y = rightOf(parentOf(parentOf(x)));
				if (colorOf(y) == RED) {
					setColor(parentOf(x), BLACK);
					setColor(y, BLACK);
					setColor(parentOf(parentOf(x)), RED);
					x = parentOf(parentOf(x));
				} else {
					if (x == rightOf(parentOf(x))) {
						x = parentOf(x);
						rotateLeft(x);
					}
					setColor(parentOf(x), BLACK);
					setColor(parentOf(parentOf(x)), RED);
					rotateRight(parentOf(parentOf(x)));
				}
			} else {
				Entry<K, V> y = leftOf(parentOf(parentOf(x)));
				if (colorOf(y) == RED) {
					setColor(parentOf(x), BLACK);
					setColor(y, BLACK);
					setColor(parentOf(parentOf(x)), RED);
					x = parentOf(parentOf(x));
				} else {
					if (x == leftOf(parentOf(x))) {
						x = parentOf(x);
						rotateRight(x);
					}
					setColor(parentOf(x), BLACK);
					setColor(parentOf(parentOf(x)), RED);
					rotateLeft(parentOf(parentOf(x)));
				}
			}
		}
		root.color = BLACK;
	}

	/**
	 * Delete node p, and then rebalance the tree.
	 */
	private void deleteEntry(Entry<K, V> p) {
		modCount++;
		size--;

		// If strictly internal, copy successor's element to p and then make p
		// point to successor.
		if (p.left != null && p.right != null) {
			Entry<K, V> s = successor(p);
			p.key = s.key;
			p.value = s.value;
			p = s;
		} // p has 2 children

		// Start fixup at replacement node, if it exists.
		Entry<K, V> replacement = (p.left != null ? p.left : p.right);

		if (replacement != null) {
			// Link replacement to parent
			replacement.parent = p.parent;
			if (p.parent == null)
				root = replacement;
			else if (p == p.parent.left)
				p.parent.left = replacement;
			else
				p.parent.right = replacement;

			// Null out links so they are OK to use by fixAfterDeletion.
			p.left = p.right = p.parent = null;

			// Fix replacement
			if (p.color == BLACK)
				fixAfterDeletion(replacement);
		} else if (p.parent == null) { // return if we are the only node.
			root = null;
		} else { // No children. Use self as phantom replacement and unlink.
			if (p.color == BLACK)
				fixAfterDeletion(p);

			if (p.parent != null) {
				if (p == p.parent.left)
					p.parent.left = null;
				else if (p == p.parent.right)
					p.parent.right = null;
				p.parent = null;
			}
		}
	}

	/** From CLR */
	private void fixAfterDeletion(Entry<K, V> x) {
		while (x != root && colorOf(x) == BLACK) {
			if (x == leftOf(parentOf(x))) {
				Entry<K, V> sib = rightOf(parentOf(x));

				if (colorOf(sib) == RED) {
					setColor(sib, BLACK);
					setColor(parentOf(x), RED);
					rotateLeft(parentOf(x));
					sib = rightOf(parentOf(x));
				}

				if (colorOf(leftOf(sib)) == BLACK
						&& colorOf(rightOf(sib)) == BLACK) {
					setColor(sib, RED);
					x = parentOf(x);
				} else {
					if (colorOf(rightOf(sib)) == BLACK) {
						setColor(leftOf(sib), BLACK);
						setColor(sib, RED);
						rotateRight(sib);
						sib = rightOf(parentOf(x));
					}
					setColor(sib, colorOf(parentOf(x)));
					setColor(parentOf(x), BLACK);
					setColor(rightOf(sib), BLACK);
					rotateLeft(parentOf(x));
					x = root;
				}
			} else { // symmetric
				Entry<K, V> sib = leftOf(parentOf(x));

				if (colorOf(sib) == RED) {
					setColor(sib, BLACK);
					setColor(parentOf(x), RED);
					rotateRight(parentOf(x));
					sib = leftOf(parentOf(x));
				}

				if (colorOf(rightOf(sib)) == BLACK
						&& colorOf(leftOf(sib)) == BLACK) {
					setColor(sib, RED);
					x = parentOf(x);
				} else {
					if (colorOf(leftOf(sib)) == BLACK) {
						setColor(rightOf(sib), BLACK);
						setColor(sib, RED);
						rotateLeft(sib);
						sib = leftOf(parentOf(x));
					}
					setColor(sib, colorOf(parentOf(x)));
					setColor(parentOf(x), BLACK);
					setColor(leftOf(sib), BLACK);
					rotateRight(parentOf(x));
					x = root;
				}
			}
		}

		setColor(x, BLACK);
	}

	/**
	 * Find the level down to which to assign all nodes BLACK. This is the last
	 * `full' level of the complete binary tree produced by buildTree. The
	 * remaining nodes are colored RED. (This makes a `nice' set of color
	 * assignments wrt future insertions.) This level number is computed by
	 * finding the number of splits needed to reach the zeroeth node. (The
	 * answer is ~lg(N), but in any case must be computed by same quick O(lg(N))
	 * loop.)
	 */
	private static int computeRedLevel(int sz) {
		int level = 0;
		for (int m = sz - 1; m >= 0; m = m / 2 - 1)
			level++;
		return level;
	}
}