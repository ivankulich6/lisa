// TODO: Remove this class entirely. All we need is function:
// int[] shapeRange(int[][][] shapes)
// that accepts array of shapes and returs four numbers: xmin, ymin, xmax, ymax. 
// If you find returning 4 numbers in an array confusing, create simple data-holding 
// class with these four attributes and no logic.

public class ShapeRange {

	public int xMin;
	public int xMax;
	public int yMin;
	public int yMax;

	public ShapeRange() {
		initialize();
	}

	public ShapeRange initialize() {
		xMin = yMin = Integer.MAX_VALUE;
		xMax = yMax = Integer.MIN_VALUE;
		return this;
	}

	public ShapeRange add(int[][] shape) {
		if (shape != null) {
			for (int j = Area.SHAPE_NOPOINTS_COUNT; j < shape.length; j++) {
				int xy[] = shape[j];
				xMin = Math.min(xMin, xy[0]);
				xMax = Math.max(xMax, xy[0]);
				yMin = Math.min(yMin, xy[1]);
				yMax = Math.max(yMax, xy[1]);
			}
		}
		return this;
	}

}
