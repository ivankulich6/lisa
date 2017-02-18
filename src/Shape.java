public class Shape {
	// 'order' serves as unique, persistent key of shape.
	// Shapes in array Area.shapes are kept sorted by 'order'.
	int order;
	// shape color components r, g, b and alpha transparency, scaled in
	// <0,1>
	double rgba[];
	// shape geometry vertices (e.g. 3 points for triangel)
	int points[][];
	// helper array - shape pixel indices in compressed form
	int pixinds[];

	Shape() {
	}

	public int[] getColor() {
		return getRgbaInt(rgba);
	}

	public static int[] getRgbaInt(double rgba[]) {
		int rgbaInt[] = { (int) Math.round(255 * rgba[0]), (int) Math.round(255 * rgba[1]),
				(int) Math.round(255 * rgba[2]), (int) Math.round(255 * rgba[3]) };
		return rgbaInt;
	}

}
