import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

interface Reducer<V> {
	V reduce(V reduced, V value);
}

public class RbTree<K, V> {

	private final Comparator<? super K> comparator;

	final Reducer<V> reducer;

	private transient Entry root = null;

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
		reducer = null;
	}

	public RbTree(Comparator<? super K> comparator, Reducer<V> reducer) {
		this.comparator = comparator;
		this.reducer = reducer;
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

	public V get(Object key) {
		Entry p = getEntry(key);
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

	final Entry getEntry(Object key) {
		// Offload comparator-based version for sake of performance
		if (comparator != null)
			return getEntryUsingComparator(key);
		if (key == null)
			throw new NullPointerException();
		Comparable<? super K> k = (Comparable<? super K>) key;
		Entry p = root;
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
	final Entry getEntryUsingComparator(Object key) {
		K k = (K) key;
		Comparator<? super K> cpr = comparator;
		if (cpr != null) {
			Entry p = root;
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
	final Entry getCeilingEntry(K key) {
		Entry p = root;
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
					Entry parent = p.parent;
					Entry ch = p;
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
	final Entry getFloorEntry(K key) {
		Entry p = root;
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
					Entry parent = p.parent;
					Entry ch = p;
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
	final Entry getHigherEntry(K key) {
		Entry p = root;
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
					Entry parent = p.parent;
					Entry ch = p;
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
	final Entry getLowerEntry(K key) {
		Entry p = root;
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
					Entry parent = p.parent;
					Entry ch = p;
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

	public V getReduced() {
		if (root == null) {
			return null;
		} else {
			return root.reduced;
		}
	}

	/** NEW: returns all values from `from` (inclusive) to `to` (exclusive) reduced
	 *  by RbTree reducer.
	 */

	public V getReduced(K from, K to) {
		return _getReduced(root, from, to);
	}

	private V _getReduced(Entry node, K from, K to) {

		if (node == null) {
			return null;
		}

		Comparator<? super K> cpr = comparator;
		/*
		 * IKU v nasledovnom riadku som zmenil povodnu nerovnost <= na >=, robilo to zle
		 */
		if (compare(this.firstEntry().key, from) >= 0 &&
				compare(this.lastEntry().key, to) == -1) {
			return node.reduced;
		}

		if (compare(from, node.key) == -1) {
			if (compare(to, node.key) <= 0) {
				return _getReduced(node.left, from, to);
			} else {
				V leftReduced = _getReduced(node.left, from, to);
				V rightReduced = _getReduced(node.right, from, to);
				return combine3(leftReduced, node.value, rightReduced);
			}
		} else {
			V rightReduced = _getReduced(node.right, from, to);
			if (compare(from, node.key) == 0) {
				return combine3(null, node.value, rightReduced);
			} else {
				return combine3(null, null, rightReduced);
			}
		}
	}

	public V put(K key, V value) {
		Entry t = root;
		if (t == null) {
			root = new Entry(key, value, null);
			size = 1;
			modCount++;
			return null;
		}
		int cmp;
		Entry parent;
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
		Entry e = new Entry(key, value, parent);
		if (cmp < 0)
			parent.left = e;
		else
			parent.right = e;
		fixAfterInsertion(e);
		// NEW recalculate reduced value.
		parent.fixReducedValue();
		size++;
		modCount++;
		return null;
	}

	public V remove(Object key) {
		Entry p = getEntry(key);
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
	public Entry firstEntry() {
		return exportEntry(getFirstEntry());
	}

	/**
	 * @since 1.6
	 */
	public Entry lastEntry() {
		return exportEntry(getLastEntry());
	}

	public Entry lowerEntry(K key) {
		return exportEntry(getLowerEntry(key));
	}

	public K lowerKey(K key) {
		return keyOrNull(getLowerEntry(key));
	}

	public Entry floorEntry(K key) {
		return exportEntry(getFloorEntry(key));
	}

	public K floorKey(K key) {
		return keyOrNull(getFloorEntry(key));
	}

	public Entry ceilingEntry(K key) {
		return exportEntry(getCeilingEntry(key));
	}

	public K ceilingKey(K key) {
		return keyOrNull(getCeilingEntry(key));
	}

	public Entry higherEntry(K key) {
		return exportEntry(getHigherEntry(key));
	}

	public K higherKey(K key) {
		return keyOrNull(getHigherEntry(key));
	}

	Iterator<K> keyIterator() {
		return new KeyIterator(getFirstEntry());
	}

	/*
	 * NEW combines two values using given reducing function. Takes care of null values
	 */
	private V combine2(V l, V r) {
		if (l == null) {
			return r;
		} else if (r == null) {
			return l;
		} else {
			return reducer.reduce(l, r);
		}
	}

	/*
	 * NEW combines three values using given reducing function. Takes care of null values
	 */
	V combine3(V l, V v, V r) {
		return combine2(combine2(l, v), r);
	}

	/**
	 * Base class for TreeMap Iterators
	 */
	abstract class PrivateEntryIterator<T> implements Iterator<T> {
		Entry next;
		Entry lastReturned;
		int expectedModCount;

		PrivateEntryIterator(Entry first) {
			expectedModCount = modCount;
			lastReturned = null;
			next = first;
		}

		@Override
		public final boolean hasNext() {
			return next != null;
		}

		final Entry nextEntry() {
			Entry e = next;
			if (e == null)
				throw new NoSuchElementException();
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
			next = successor(e);
			lastReturned = e;
			return e;
		}

		final Entry prevEntry() {
			Entry e = next;
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

	final class EntryIterator extends PrivateEntryIterator<Entry> {
		EntryIterator(Entry first) {
			super(first);
		}

		@Override
		public Entry next() {
			return nextEntry();
		}
	}

	final class KeyIterator extends PrivateEntryIterator<K> {
		KeyIterator(Entry first) {
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
		return comparator == null ? ((Comparable<? super K>) k1).compareTo((K) k2) : comparator.compare((K) k1, (K) k2);
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
	Entry exportEntry(Entry e) {
		// This was supposed to do some kind of copy..?
		// Don't understand it, identity will work for now ;)
		return e;
	}

	/**
	 * Return key for entry, or null if null
	 */
	K keyOrNull(Entry e) {
		return e == null ? null : e.key;
	}

	/**
	 * Returns the key corresponding to the specified Entry.
	 *
	 * @throws NoSuchElementException
	 *             if the Entry is null
	 */
	K key(Entry e) {
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

	final class Entry {
		K key;
		V value;
		V reduced;
		Entry left = null;
		Entry right = null;
		Entry parent;
		boolean color = BLACK;

		/**
		 * Make a new cell with given key, value, and parent, and with
		 * <tt>null</tt> child links, and BLACK color.
		 */
		Entry(K key, V value, Entry parent) {
			this.key = key;
			this.value = value;
			this.parent = parent;
			this.reduced = value;
		}

		/**
		 * Returns the key.
		 *
		 * @return the key
		 */
		public K getKey() {
			return key;
		}

		/**
		 * Returns the value associated with the key.
		 *
		 * @return the value associated with the key
		 */
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
		public V setValue(V value) {
			V oldValue = this.value;
			this.value = value;
			// NEW recalculate reduced value.
			fixReducedValue();
			return oldValue;
		}

		/**
		 * NEW recalculate the reduced value in the node.
		 */
		private void fixReducedValue() {
			if (reducer != null) {
				this.fixReducedValueOne();
				if (this.parent != null) {
					this.parent.fixReducedValue();
				}
			}
		}

		/**
		 * NEW recalculate the reduced value in the node.
		 */
		private void fixReducedValueOne() {
			if (this.value == null) {
				this.reduced = null;
				return;
			}
			V l = null;
			V r = null;
			if (this.left != null) {
				l = this.left.reduced;
			}
			if (this.right != null) {
				r = this.right.reduced;
			}
			this.reduced = combine3(l, this.value, r);
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
	final Entry getFirstEntry() {
		Entry p = root;
		if (p != null)
			while (p.left != null)
				p = p.left;
		return p;
	}

	/**
	 * Returns the last Entry in the TreeMap (according to the TreeMap's
	 * key-sort function). Returns null if the TreeMap is empty.
	 */
	final Entry getLastEntry() {
		Entry p = root;
		if (p != null)
			while (p.right != null)
				p = p.right;
		return p;
	}

	/**
	 * Returns the successor of the specified Entry, or null if no such.
	 */
	<K, V> Entry successor(Entry t) {
		if (t == null)
			return null;
		else if (t.right != null) {
			Entry p = t.right;
			while (p.left != null)
				p = p.left;
			return p;
		} else {
			Entry p = t.parent;
			Entry ch = t;
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
	<K, V> Entry predecessor(Entry t) {
		if (t == null)
			return null;
		else if (t.left != null) {
			Entry p = t.left;
			while (p.right != null)
				p = p.right;
			return p;
		} else {
			Entry p = t.parent;
			Entry ch = t;
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

	private boolean colorOf(Entry p) {
		return (p == null ? BLACK : p.color);
	}

	private Entry parentOf(Entry p) {
		return (p == null ? null : p.parent);
	}

	private void setColor(Entry p, boolean c) {
		if (p != null)
			p.color = c;
	}

	private Entry leftOf(Entry p) {
		return (p == null) ? null : p.left;
	}

	private Entry rightOf(Entry p) {
		return (p == null) ? null : p.right;
	}

	/** From CLR */

	/**
	 *     p
	 *      \
	 *       r
	 *      /
	 *     a
	 *
	 *
	 *     r
	 *    / \
	 *   p   a
	 */
	private void rotateLeft(Entry p) {
		if (p != null) {
			Entry r = p.right;
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
		// NEW recalculate reduced value. Note that it is enough to
		// run this on vertex `p` as it also updates it's parent (`r`)
		p.fixReducedValue();
	}

	/** From CLR */
	private void rotateRight(Entry p) {
		if (p != null) {
			Entry l = p.left;
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
		// NEW recalculate reduced value.
		p.fixReducedValue();
	}

	/** From CLR */
	private void fixAfterInsertion(Entry x) {
		x.color = RED;

		while (x != null && x != root && x.parent.color == RED) {
			if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
				Entry y = rightOf(parentOf(parentOf(x)));
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
				Entry y = leftOf(parentOf(parentOf(x)));
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
	private void deleteEntry(Entry p) {
		modCount++;
		size--;

		// If strictly internal, copy successor's element to p and then make p
		// point to successor.
		if (p.left != null && p.right != null) {
			Entry s = successor(p);
			p.key = s.key;
			p.value = s.value;
			p = s;
		} // p has 2 children

		// Start fixup at replacement node, if it exists.
		Entry replacement = (p.left != null ? p.left : p.right);

		if (replacement != null) {
			// Link replacement to parent
			replacement.parent = p.parent;
			if (p.parent == null)
				root = replacement;
			else if (p == p.parent.left)
				p.parent.left = replacement;
			else
				p.parent.right = replacement;

			// NEW recalculate reduced value.
			/*
			 * IKU nasledovné volanie som obalil do if, lebo mi to padalo na null pointri
			 */
			if (p.parent != null){
				p.parent.fixReducedValue();
			}
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
				// NEW recalculate reduced value.
				p.parent.fixReducedValue();
				p.parent = null;
			}
		}
		// NEW recalculate reduced value.
		// TODO this is probably not always necessary?
		p.fixReducedValue();
	}

	/** From CLR */
	private void fixAfterDeletion(Entry x) {
		while (x != root && colorOf(x) == BLACK) {
			if (x == leftOf(parentOf(x))) {
				Entry sib = rightOf(parentOf(x));

				if (colorOf(sib) == RED) {
					setColor(sib, BLACK);
					setColor(parentOf(x), RED);
					rotateLeft(parentOf(x));
					sib = rightOf(parentOf(x));
				}

				if (colorOf(leftOf(sib)) == BLACK && colorOf(rightOf(sib)) == BLACK) {
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
				Entry sib = leftOf(parentOf(x));

				if (colorOf(sib) == RED) {
					setColor(sib, BLACK);
					setColor(parentOf(x), RED);
					rotateRight(parentOf(x));
					sib = leftOf(parentOf(x));
				}

				if (colorOf(rightOf(sib)) == BLACK && colorOf(leftOf(sib)) == BLACK) {
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
