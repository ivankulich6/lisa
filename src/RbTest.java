import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;

public class RbTest {

	public static void main(String args[]) throws Exception {
		RbTest test = new RbTest();
		test.test1();
		test.test2();
		test.test3();
	}

	public boolean equal(Object o1, Object o2) {
		if (o1 == null) {
			return o2 == null;
		} else {
			return o1.equals(o2);
		}
	}

	Random randg = new Random();

	public void test1() throws Exception {
		for (int k = 0; k < 6; k++) {
			RbTree<Integer, Integer> tree = new RbTree<Integer, Integer>();
			TreeMap<Integer, Integer> tmap = new TreeMap<Integer, Integer>();

			for (int i = 0; i < 100000; i++) {
				int key = randg.nextInt(10000);
				int val = randg.nextInt(10000);
				if (!equal(tmap.get(key), tree.get(key))) {
					System.out.println(key + " " + tmap.get(key) + " " + tree.get(key));
					throw new Exception("just no");
				}
				if (randg.nextInt(6) <= k) {
					tmap.put(key, val);
					tree.put(key, val);
				} else {
					tmap.remove(key);
					tree.remove(key);
				}
			}
		}
		System.out.println("test1 complete");
	}

	public void test2() throws Exception {
		Reducer<Integer> reducer = new Reducer<Integer>() {
			@Override
			public Integer reduce(Integer reduced, Integer value) {
				return reduced + value;
			}
		};

		for (int k = 0; k < 6; k++) {
			RbTree<Integer, Integer> tree = new RbTree<Integer, Integer>(null, reducer);
			TreeMap<Integer, Integer> tmap = new TreeMap<Integer, Integer>();

			for (int i = 0; i < 10000; i++) {
				int key = randg.nextInt(1000);
				int val = randg.nextInt(1000);

				if (randg.nextInt(6) <= k) {
					tmap.put(key, val);
					tree.put(key, val);
				} else {
					tmap.remove(key);
					tree.remove(key);
				}
				Integer res = tmap.values().stream().reduce((a, b) -> a + b).orElse(null);
				if (!equal(res, tree.getReduced())) {
					System.out.println(res + " " + tree.getReduced());
					throw new Exception("just no");
				}
			}
		}
		System.out.println("test2 complete");
	}

	public void test3() throws Exception {
		Reducer<Integer> reducer = new Reducer<Integer>() {
			@Override
			public Integer reduce(Integer reduced, Integer value) {
				return reduced + value;
			}
		};

		for (int k = 0; k < 6; k++) {
			RbTree<Integer, Integer> tree = new RbTree<Integer, Integer>(null, reducer);
			TreeMap<Integer, Integer> tmap = new TreeMap<Integer, Integer>();

			int valRange = 1000;
			int keyRange = 1000;
			for (int i = 0; i < 10000; i++) {
				int key = randg.nextInt(keyRange);
				int val = randg.nextInt(valRange);

				if (randg.nextInt(6) <= k) {
					tmap.put(key, val);
					tree.put(key, val);
				} else {
					tmap.remove(key);
					tree.remove(key);
				}

				ArrayList<Integer> keys = new ArrayList<Integer>(tmap.keySet());
				int ln = keys.size();
				if (ln == 0) {
					continue;
				}
				int fromKey = keys.get(randg.nextInt(ln));
				int toKey = keys.get(randg.nextInt(ln));
				if (toKey == fromKey) {
					continue;
				} else if (toKey < fromKey) {
					int tmp = toKey;
					toKey = fromKey;
					fromKey = tmp;
				}
				Integer res = tmap.headMap(toKey).tailMap(fromKey)
				  .values().stream()
				  .reduce((a, b) -> a + b).orElse(null);
				if (!equal(res, tree.getReduced(fromKey, toKey))) {
					System.out.println(res + " " + tree.getReduced());
					throw new Exception("just no");
				}
			}
		}
		System.out.println("test3 complete");
	}
}
