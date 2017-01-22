import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class Area {
	// maintain array shapes
	public static final boolean useShapesArray = true;
	// shape color field index
	public static final int SHAPE_COLOR_INDEX = 1;
	// shape fields except color and points
	public static final int SHAPE_NOGEOM_COUNT = SHAPE_COLOR_INDEX;
	// shape fields except points
	public static final int SHAPE_NOPOINTS_COUNT = SHAPE_COLOR_INDEX + 1;

	public int width;
	public int height;
	private AreaPixel[] pixels;
	private ShapeRange shapeRange;
	private BufferedImage tempBufferedImage;
	int[][][] shapes;
	int shapesCount;
	int pointsCount;
	private int gOrder;
	Mutation mutation;
	MutaPixel[] mutaPixels;
	int mutaPixelsCount;
	private double addeDiff; // additive difference
	private double newAddeDiff;
	double diff;
	private double newDiff;
	double temp;
	Random randg;

	String mutationType; // for debug

	public class Mutation {
		int[][] oldShape = null;
		int[][] newShape = null;
		// mutated shape index
		int index = 0;
		// expected total shapes count in area after mutation
		int shapesCount = 0;
		// expected total shapes points count in area after mutation
		int pointsCount = 0;
	}

	public class MutaPixel {
		int index = 0;
		int intype = 0; // 1=only in oldShape, 2 = only in newShape, 3 = in both
	}

	Area(int width, int height) {
		assert (width > 0 && height > 0);
		this.width = width;
		this.height = height;
		pixels = new AreaPixel[width * height];
		for (int j = 0; j < height * width; j++) {
			pixels[j] = new AreaPixel();
		}
		shapes = new int[0][][];
		shapesCount = 0;
		pointsCount = 0;
		shapeRange = new ShapeRange();
		tempBufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		gOrder = 0;
		mutation = new Mutation();
		mutaPixels = new MutaPixel[width * height];
		for (int j = 0; j < height * width; j++) {
			mutaPixels[j] = new MutaPixel();
		}

		temp = 1;
		randg = new Random(6543210);
		// randg = new Random();
	}

	public void setTargetRgb(int[][] rgb) {
		assert pixels.length == rgb.length;
		for (int j = 0; j < width * height; j++) {
			pixels[j].targetRgb = rgb[j];
		}
		addeDiff = newAddeDiff = diff();
		diff = penaltyShape(pointsCount);
	}

	private int[] getShapePixels(int[][] shape) {
		Graphics g = tempBufferedImage.getGraphics();
		g.setColor(new Color(255, 255, 255));
		g.fillRect(0, 0, width, height);
		g.setColor(new Color(0, 0, 0, 255));
		int npoints = shape.length - SHAPE_NOPOINTS_COUNT;
		int xpoints[] = new int[npoints];
		int ypoints[] = new int[npoints];
		for (int i = 0; i < npoints; i++) {
			// 0-th element of shape is color
			xpoints[i] = shape[i + SHAPE_COLOR_INDEX + 1][0];
			ypoints[i] = shape[i + SHAPE_COLOR_INDEX + 1][1];
		}
		g.fillPolygon(xpoints, ypoints, npoints);
		return ((DataBufferInt) tempBufferedImage.getRaster().getDataBuffer()).getData();

	}

	public int getRgbInt(int x, int y) {
		return pixels[y * width + x].getRgbInt();
	}

	private int getShapeOrder(int[][] shape) {
		return AreaPixel.getShapeOrder(shape);
	}

	public void findMutaPixelsNewShape(int[][] newShape) {
		final int[] tmppixels = getShapePixels(newShape);
		shapeRange.initialize().add(newShape);
		mutaPixelsCount = 0;
		for (int jh = shapeRange.yMin; jh < shapeRange.yMax; jh++) {
			int jhw = jh * width;
			for (int jw = shapeRange.xMin; jw < shapeRange.xMax; jw++) {
				int ind = jhw + jw;
				if ((tmppixels[ind] & 0xff) == 0) {
					MutaPixel mp = mutaPixels[mutaPixelsCount++];
					mp.index = ind;
					mp.intype = 2;
				}
			}
		}
		return;
	}

	public void findMutaPixelsOldShape(int[][] oldShape) {
		int order = getShapeOrder(oldShape);
		shapeRange.initialize().add(oldShape);
		mutaPixelsCount = 0;
		for (int jh = shapeRange.yMin; jh < shapeRange.yMax; jh++) {
			int jhw = jh * width;
			for (int jw = shapeRange.xMin; jw < shapeRange.xMax; jw++) {
				int ind = jhw + jw;
				AreaPixel p = pixels[ind];
				if (p.shapes.containsKey(order)) {
					MutaPixel mp = mutaPixels[mutaPixelsCount++];
					mp.index = ind;
					mp.intype = 1;
				}
			}
		}
		return;
	}

	public void findMutaPixelsOldNewShape(int[][] oldShape, int[][] newShape) {
		int order = getShapeOrder(oldShape);
		int pixelInOld = 0;
		int pixelInNew = 0;
		final int[] tmppixels = getShapePixels(newShape);
		AreaPixel p;
		shapeRange.initialize().add(oldShape).add(newShape);
		mutaPixelsCount = 0;
		for (int jh = shapeRange.yMin; jh < shapeRange.yMax; jh++) {
			int jhw = jh * width;
			for (int jw = shapeRange.xMin; jw < shapeRange.xMax; jw++) {
				int ind = jhw + jw;
				p = pixels[ind];
				pixelInOld = p.shapes.containsKey(order) ? 1 : 0;
				pixelInNew = (tmppixels[ind] & 0xff) == 0 ? 1 : 0;
				if (pixelInOld + pixelInNew > 0) {
					MutaPixel mp = mutaPixels[mutaPixelsCount++];
					mp.index = ind;
					mp.intype = pixelInOld + 2 * pixelInNew;
				}
			}

		}
		return;
	}

	public double prepareAddShape(int[][] newShape) {
		double diffOfDiff = 0;
		findMutaPixelsNewShape(newShape);
		for (int j = 0; j < mutaPixelsCount; j++) {
			diffOfDiff += pixels[mutaPixels[j].index].prepareAddShape(newShape);
		}
		return diffOfDiff;
	}

	public void addShape(int[][] newShape) {
		for (int j = 0; j < mutaPixelsCount; j++) {
			pixels[mutaPixels[j].index].addShape(newShape);
		}
		return;
	}

	public double prepareRemoveShape(int[][] oldShape) {
		double diffOfDiff = 0;
		findMutaPixelsOldShape(oldShape);
		for (int j = 0; j < mutaPixelsCount; j++) {
			diffOfDiff += pixels[mutaPixels[j].index].prepareRemoveShape(oldShape);
		}
		return diffOfDiff;
	}

	public void removeShape(int[][] oldShape) {
		for (int j = 0; j < mutaPixelsCount; j++) {
			pixels[mutaPixels[j].index].removeShape(oldShape);
		}
		return;
	}

	public double prepareReplaceShape(int[][] oldShape, int[][] newShape) {
		if (oldShape == null && newShape == null) {
			return 0;
		}
		if (oldShape == null) {
			return prepareAddShape(newShape);
		} else if (newShape == null) {
			return prepareRemoveShape(oldShape);
		} else {
			double diffOfDiff = 0;
			boolean sameRgba = sameRgba(newShape[SHAPE_COLOR_INDEX], oldShape[SHAPE_COLOR_INDEX]);
			MutaPixel mp;
			findMutaPixelsOldNewShape(oldShape, newShape);
			for (int j = 0; j < mutaPixelsCount; j++) {
				mp = mutaPixels[j];
				diffOfDiff += pixels[mp.index].prepareReplaceShape(oldShape, newShape, mp.intype, sameRgba);
			}
			return diffOfDiff;

		}
	}

	public void replaceShape(int[][] oldShape, int[][] newShape) {
		if (oldShape == null && newShape == null) {
			return;
		}
		if (oldShape == null) {
			addShape(newShape);
			return;
		} else if (newShape == null) {
			removeShape(oldShape);
			return;
		} else {
			MutaPixel mp;
			for (int j = 0; j < mutaPixelsCount; j++) {
				mp = mutaPixels[j];
				pixels[mp.index].replaceShape(oldShape, newShape, mp.intype);
			}
			return;

		}
	}

	private boolean sameRgba(int[] rgba1, int[] rgba2) {
		if (rgba1[0] != rgba2[0])
			return false;
		if (rgba1[1] != rgba2[1])
			return false;
		if (rgba1[2] != rgba2[2])
			return false;
		if (rgba1[3] != rgba2[3])
			return false;
		return true;
	}

	private double penalty(int pointsCount) {
		return pointsCount * pointsCount / 10000000000.0;
	}

	private double penaltyRgb() {
		return newAddeDiff / (width * height);
	}

	private double penaltyShape(int pointsCount) {
		return penaltyRgb() + penalty(pointsCount);
	}

	public double diffTest() {
		return diff() / (width * height) + penalty(pointsCount);
	}

	public double diffTest(double addeDiff) {
		return addeDiff / (width * height) + penalty(pointsCount);
	}

	private double diff() {
		double diff = 0;
		for (int j = 0; j < pixels.length; j++) {
			diff += pixels[j].diff();
		}
		return diff;
	}

	private int[][] getRandomShape() {
		int[] color = new int[5];
		for (int i = 0; i < 3; i++) {
			color[i] = randg.nextInt(256);
		}
		// color[3] = alpha, from 1 to 255; (0 is useless)
		color[3] = randg.nextInt(255) + 1;
		color[4] = gOrder++;
		int[][] res = new int[SHAPE_NOGEOM_COUNT + 4][];
		res[SHAPE_COLOR_INDEX] = color;
		for (int i = 0; i < 3; i++) {
			res[SHAPE_NOPOINTS_COUNT + i] = new int[2];
			res[SHAPE_NOPOINTS_COUNT + i][0] = randg.nextInt(width);
			res[SHAPE_NOPOINTS_COUNT + i][1] = randg.nextInt(height);
		}
		return res;
	}

	private int[][] copy(int[][] shape) {
		int[][] res = new int[shape.length][];
		for (int i = 0; i < shape.length; i++) {
			if (shape[i] != null) {
				res[i] = new int[shape[i].length];
				for (int j = 0; j < shape[i].length; j++) {
					res[i][j] = shape[i][j];
				}
			}
		}
		return res;
	}

	private int[][] selectPixelsRandomShape() {
		int index = randg.nextInt(width * height);
		int nPixShapes = pixels[index].shapes.size();
		if (nPixShapes == 0) {
			int index0 = index;
			while (nPixShapes == 0) {
				++index;
				if (index >= width * height) {
					index = 0;
				}
				if (index == index0) {
					assert shapesCount == 0;
					mutationType = "0";
					return null;
				}
				nPixShapes = pixels[index].shapes.size();
			}
		}
		int index2 = randg.nextInt(nPixShapes);
		int j = 0;
		for (Map.Entry<Integer, int[][]> entry : pixels[index].shapes.entrySet()) {
			if (j++ >= index2) {
				return entry.getValue();
			}
		}
		return null;
	}

	private void getRandomMutation() {
		int opcode = randg.nextInt(100);
		mutation.oldShape = null;
		mutation.newShape = null;
		mutation.shapesCount = shapesCount;
		mutation.pointsCount = pointsCount;
		if (opcode < 20) { // add shape
			mutationType = "A";
			mutation.newShape = getRandomShape();
			mutation.shapesCount = shapesCount + 1;
			mutation.pointsCount = pointsCount + mutation.newShape.length - SHAPE_NOPOINTS_COUNT;
			return;
		} else if (opcode < 40) { // remove shape
			if (shapesCount == 0) {
				mutationType = "0";
				return;
			}
			mutationType = "R";
			if (useShapesArray) {
				int index = randg.nextInt(shapesCount);
				mutation.oldShape = shapes[index];
				mutation.index = index;
			} else {
				mutation.oldShape = selectPixelsRandomShape();
				mutation.index = -1;
			}
			mutation.shapesCount = shapesCount - 1;
			mutation.pointsCount = pointsCount - (mutation.oldShape.length - SHAPE_NOPOINTS_COUNT);
			return;
		} else { // modify shape
			mutationType = "M";
			if (shapesCount == 0) {
				mutationType = "0";
				return;
			}
			int oldShape[][];
			int newShape[][];
			int index;
			if (useShapesArray) {
				index = randg.nextInt(shapesCount);
				oldShape = shapes[index];
			} else {
				index = -1;
				oldShape = selectPixelsRandomShape();
			}
			newShape = copy(oldShape);
			int inner = randg.nextInt(newShape.length - SHAPE_NOGEOM_COUNT) + SHAPE_COLOR_INDEX;
			int inninner, randMax;
			if (inner == SHAPE_COLOR_INDEX) {
				randMax = 4;
			} else {
				randMax = newShape[inner].length;
			}
			inninner = randg.nextInt(randMax);
			int move = randg.nextInt(20) - 10;
			int[] tmp = newShape[inner];
			tmp[inninner] += move;
			// if messing with color, trim outputs to 0, 255
			if (inner == SHAPE_COLOR_INDEX) {
				tmp[inninner] = Math.min(tmp[inninner], 255);
				tmp[inninner] = Math.max(tmp[inninner], 0);
				// trim x coordinate to 0, width
			} else if (inninner == 0) {
				tmp[inninner] = Math.min(tmp[inninner], width);
				tmp[inninner] = Math.max(tmp[inninner], 0);
				// trim y coordinate to 0, height
			} else if (inninner == 1) {
				tmp[inninner] = Math.min(tmp[inninner], height);
				tmp[inninner] = Math.max(tmp[inninner], 0);
			}
			mutation.oldShape = oldShape;
			mutation.newShape = newShape;
			mutation.index = index;
			mutation.pointsCount = pointsCount - oldShape.length + newShape.length;
			return;
		}

	}

	private int[][][] alterShapes(int index, int[][] oldShape, int[][] newShape) {
		int n = shapes.length;
		if (oldShape == null && newShape == null) {
			return shapes;
		} else if (oldShape == null) {
			int[][][] newShapes = new int[n + 1][][];
			System.arraycopy(shapes, 0, newShapes, 0, n);
			newShapes[n] = newShape;
			return newShapes;
		} else if (newShape == null) {
			int[][][] newShapes = new int[n - 1][][];
			System.arraycopy(shapes, 0, newShapes, 0, index);
			System.arraycopy(shapes, index + 1, newShapes, index, (n - 1 - index));
			return newShapes;
		} else {
			int[][][] newShapes = new int[n][][];
			System.arraycopy(shapes, 0, newShapes, 0, n);
			newShapes[index] = newShape;
			return newShapes;
		}
	}

	public boolean doRandomChange() {

		// IDEA: Don't use alterShapes. To gain more speed, proceed as
		// floows:
		// 1. generate the change you want to do
		// 2. apply the change directly to the Area (note that there is no
		// need for structures such as `shapes` here - Area already contains
		// all information necessary to fully represent a picture.)
		// 3. when applying the change to the Area, calculate also the
		// diff-of-diff for this change - this is simply
		// |newColor - targetColor| - |oldColor - targetColor|
		// summed over all affected pixels
		// 4. if the diff-of-diff is > 0, revert the change

		// NOTE: useShapesArray = true ... shapes is updated after mutation
		// is accepted and used for random selection of shape.
		// useShapesArray = false ... maintenance of shapes is removed
		// completely, random selection of shape is performed on shapes stored
		// in pixels. Mutation speed in this case is less (about 1.7 times),
		// probably because shape selection from trees in pixels is more
		// expensive than simple selection from array.

		temp = Math.max(temp, Math.pow(10, -10));
		getRandomMutation();
		newAddeDiff = addeDiff + prepareReplaceShape(mutation.oldShape, mutation.newShape);
		newDiff = penaltyShape(mutation.pointsCount);
		// newDiff < d -> vzdy true
		// newDiff - d = temp -> akceptujem so sancou e^-1
		// newDiff - d = 2temp -> akceptujem so sancou e^-2
		// if (randg.nextDouble() < Math.exp(- (newDiff - d) / temp)) {
		if (newDiff < diff) {
			if (newDiff - diff < -0.00001) {
				temp *= 0.5;
			}
			replaceShape(mutation.oldShape, mutation.newShape);
			if (useShapesArray) {
				shapes = alterShapes(mutation.index, mutation.oldShape, mutation.newShape);
				assert shapes.length == mutation.shapesCount;
			}
			shapesCount = mutation.shapesCount;
			pointsCount = mutation.pointsCount;
			addeDiff = newAddeDiff;
			diff = newDiff;
			return true;
		} else {
			temp *= 1.002;
			return false;
		}
	}

	public int[][][] extractShapes() {
		TreeMap<Integer, int[][]> exShapes = new TreeMap<Integer, int[][]>();
		for (int j = 0; j < height * width; j++) {
			exShapes.putAll(pixels[j].shapes);
		}
		int[][][] outShapes = new int[exShapes.size()][][];
		int j = 0;
		for (Map.Entry<Integer, int[][]> entry : exShapes.entrySet()) {
			outShapes[j++] = entry.getValue();
		}
		return outShapes;
	}

	public int recalcPointsCount(int[][][] shapes) {
		int pc = 0;
		for (int[][] shape : shapes) {
			pc += shape.length - SHAPE_NOPOINTS_COUNT;
		}
		assert pc == pointsCount;
		return pc;
	}

	public void saveShapes(String sFile) {
		BufferedWriter writer = null;
		try {
			File outFile = new File(sFile);
			StringBuilder sb = Utils.ShapesToSb(width, height, shapes);
			System.out.println(outFile.getCanonicalPath());
			writer = new BufferedWriter(new FileWriter(outFile));
			writer.write(sb.toString());
			writer.close();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (Exception e) {
			}
		}
	}

	public void rgbRegen() {
		for (int j = 0; j < height * width; j++) {
			pixels[j].rgbRegen();
		}
	}

}
