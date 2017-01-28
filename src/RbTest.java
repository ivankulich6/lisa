import java.util.Random;
import java.util.TreeMap;

public class RbTest {

	public static void main(String args[]) throws Exception {
		RbTest test = new RbTest();
		test.run();
	}

	public boolean equal(Object o1, Object o2) {
		if (o1 == null) {
			return o2 == null;
		} else {
			return o1.equals(o2);
		}
	}

	public void run() throws Exception {
		RbTree<Integer, Integer> tree = new RbTree<Integer, Integer>();
		TreeMap<Integer, Integer> tmap = new TreeMap<Integer, Integer>();

		Random randg = new Random();

		for (int i = 0; i < 1000000; i++) {
			int key = randg.nextInt(10000);
			int val = randg.nextInt(1000000);
			if (!equal(tmap.get(key), tree.get(key))) {
				System.out.println(
						key + " " + tmap.get(key) + " " + tree.get(key));
				throw new Exception("just no");
			}
			tmap.put(key, val);
			tree.put(key, val);
		}
	}

}
