import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class Area {

	public int width;
	public int height;
	private AreaPixel[] pixels;
	private ShapeRange shapeRange;
	private BufferedImage tempBufferedImage;
	Shape[] shapes;
	int shapesCount;
	int pointsCount;
	private int gOrder;
	Mutation mutation;
	MutaPixel[] mutaPixels;
	int[] mutaPixelsWork;
	int mutaPixelsCount;
	private double addeDiff; // additive difference
	double diff;
	double temp;
	Random randg;
	// debug helpers
	public static int cntRandomChange;
	public static int currPixelOrder;
	public static String mutationType;

	public class Shape {
		// 'order' serves as unique, persistent key of shape.
		// Shapes in array Area.shapes are kept sorted by 'order'.
		int order;
		// shape color components r, g, b and alpha transparency, scaled in <0,
		// 255>
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

	}

	public class Mutation {
		Shape oldShape = null;
		Shape newShape = null;
		// mutated shape index in shapes array
		int index = 0;
		// expected total shapes count in area after mutation
		int shapesCount = 0;
		// expected total shapes points count in area after mutation
		int pointsCount = 0;
	}

	public class MutaPixel {
		int index = 0;
		// 1=only in oldShape, 2 = only in newShape, 3 = in both
		int intype = 0;
	}

	public class ShapeRange {
		public int xMin;
		public int xMax;
		public int yMin;
		public int yMax;
	}

	Area() {
	}

	Area(int width, int height) {
		assert (width > 0 && height > 0);
		this.width = width;
		this.height = height;
		pixels = new AreaPixel[width * height];
		for (int j = 0; j < height * width; j++) {
			pixels[j] = new AreaPixel();
		}
		shapes = new Shape[0];
		shapesCount = 0;
		pointsCount = 0;
		tempBufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		gOrder = 0;
		mutation = new Mutation();
		mutaPixels = new MutaPixel[width * height];
		for (int j = 0; j < height * width; j++) {
			mutaPixels[j] = new MutaPixel();
		}
		mutaPixelsWork = new int[2 * width * height];
		temp = 1;
		randg = new Random(6543210);
		// randg = new Random();
		cntRandomChange = 0;
	}

	public void setTargetRgb(int[][] rgb) {
		assert pixels.length == rgb.length;
		for (int j = 0; j < width * height; j++) {
			for (int k = 0; k < 3; k++) {
				pixels[j].targetRgb[k] = (1 / (double) 255) * rgb[j][k];
			}
		}
		addeDiff = diff();
		diff = penaltyShape(addeDiff, pointsCount);
	}

	private int[] getShapePixels(Shape shape) {
		Graphics g = tempBufferedImage.getGraphics();
		g.setColor(new Color(255, 255, 255, 255));
		g.fillRect(0, 0, width, height);
		g.setColor(new Color(0, 0, 0, 255));
		int npoints = shape.points.length;
		int xpoints[] = new int[npoints];
		int ypoints[] = new int[npoints];
		for (int i = 0; i < npoints; i++) {
			xpoints[i] = shape.points[i][0];
			ypoints[i] = shape.points[i][1];
		}
		g.fillPolygon(xpoints, ypoints, npoints);
		return ((DataBufferInt) tempBufferedImage.getRaster().getDataBuffer()).getData();

	}

	// returns next pixel index for shape specified by pixelColors
	// pixelsColors = area array of colors, black in shape, white outside of
	// shape
	// iter = [height index, width index, area index]
	private int getNextIndexInNew(int[] iter, int[] pixelsColors) {
		while (true) {
			if (++iter[1] < shapeRange.xMax) {
				if ((pixelsColors[++iter[2]] & 0xff) == 0) {
					return iter[2];
				}
			} else {
				if (++iter[0] >= shapeRange.yMax) {
					return Integer.MAX_VALUE;
				}
				iter[1] = shapeRange.xMin - 1;
				iter[2] += width + shapeRange.xMin - shapeRange.xMax;
			}
		}
	}

	private int[] initIndexInNew() {
		int[] iter = { shapeRange.yMin, shapeRange.xMin - 1, width * shapeRange.yMin + shapeRange.xMin - 1 };
		if (shapeRange.yMin == shapeRange.yMax) {
			iter[1] = shapeRange.xMax;
		}
		return iter;
	}

	// returns next pixel index stored in compressed array pixinds
	// iter = [pos. in pixinds, pos. in sequence, length of sequence, current
	// index]
	private int getNextIndexInOld(int[] iter, int[] pixinds) {
		if (++iter[1] < iter[2]) {
			return ++iter[3];
		} else {
			if (++iter[0] >= pixinds.length) {
				return Integer.MAX_VALUE;
			}
			iter[1] = 0;
			iter[3] = pixinds[iter[0]];
			iter[2] = pixinds[++iter[0]];
			return iter[3];
		}
	}

	private int[] initIndexInOld() {
		int[] iter = { -1, 0, 0, 0 };
		return iter;
	}

	public void findMutaPixelsNewShape(Shape newShape) {
		final int[] pixelcolors = getShapePixels(newShape);
		shapeRange = getShapeRange(newShape);
		mutaPixelsCount = 0;
		int[] iterNew = initIndexInNew();
		int index;
		while (true) {
			index = getNextIndexInNew(iterNew, pixelcolors);
			if (index == Integer.MAX_VALUE) {
				break;
			}
			MutaPixel mp = mutaPixels[mutaPixelsCount++];
			mp.index = index;
			mp.intype = 2;
		}
		return;
	}

	public void findMutaPixelsOldShape(Shape oldShape) {
		mutaPixelsCount = 0;
		int[] pixinds = oldShape.pixinds;
		int[] iterOld = initIndexInOld();
		int index;
		while (true) {
			index = getNextIndexInOld(iterOld, pixinds);
			if (index == Integer.MAX_VALUE) {
				break;
			}
			MutaPixel mp = mutaPixels[mutaPixelsCount++];
			mp.index = index;
			mp.intype = 1;
		}
		return;
	}

	public void findMutaPixelsOldNewShape(Shape oldShape, Shape newShape) {
		mutaPixelsCount = 0;
		int[] pixinds = oldShape.pixinds;
		int[] iterOld = initIndexInOld();
		int indexOld = -1;
		final int[] pixelcolors = getShapePixels(newShape);
		shapeRange = getShapeRange(newShape);
		int[] iterNew = initIndexInNew();
		int indexNew = -1;
		MutaPixel mp;

		while (true) {
			if (indexNew < indexOld) {
				indexNew = getNextIndexInNew(iterNew, pixelcolors);
			} else if (indexOld < indexNew) {
				indexOld = getNextIndexInOld(iterOld, pixinds);
			} else if (indexNew != Integer.MAX_VALUE) {
				indexNew = getNextIndexInNew(iterNew, pixelcolors);
				indexOld = getNextIndexInOld(iterOld, pixinds);
			} else {
				break;
			}
			if (indexOld < indexNew) {
				mp = mutaPixels[mutaPixelsCount++];
				mp.index = indexOld;
				mp.intype = 1;
			} else if (indexNew < indexOld) {
				mp = mutaPixels[mutaPixelsCount++];
				mp.index = indexNew;
				mp.intype = 2;
			} else if (indexNew != Integer.MAX_VALUE) {
				mp = mutaPixels[mutaPixelsCount++];
				mp.index = indexNew;
				mp.intype = 3;
			} else {
				break;
			}
		}
		return;
	}

	/*
	 * sequence of pixel indices is stored in compressed form [ind1, cnt1, ind2,
	 * cnt2,...] where cnti is number of consecutive indices {indi, indi+1,...}
	 */
	public void saveShapePixels(Shape shape) {
		int currIndex = -1;
		int currPos = -1;
		int currCount = 0;
		int index;
		int index0 = -1;
		for (int j = 0; j < mutaPixelsCount; j++, currIndex++) {
			if (mutaPixels[j].intype > 1) {
				index = mutaPixels[j].index;
				if (index != currIndex) {
					if (currPos >= 0) {
						mutaPixelsWork[currPos++] = index0;
						mutaPixelsWork[currPos++] = currCount;
					} else {
						currPos = 0;
					}
					currIndex = index;
					index0 = index;
					currCount = 1;
				} else {
					++currCount;
				}
			}
		}
		if (currPos >= 0) {
			mutaPixelsWork[currPos++] = index0;
			mutaPixelsWork[currPos++] = currCount;
			shape.pixinds = new int[currPos];
			System.arraycopy(mutaPixelsWork, 0, shape.pixinds, 0, currPos);
		}
	}

	public double diffIncIfAdded(Shape newShape) {
		double diffOfDiff = 0;
		findMutaPixelsNewShape(newShape);
		for (int j = 0; j < mutaPixelsCount; j++) {
			currPixelOrder = j; // for debug
			diffOfDiff += pixels[mutaPixels[j].index].diffIncIfAdded(newShape);
		}
		return diffOfDiff;
	}

	public void addShape(Shape newShape) {
		for (int j = 0; j < mutaPixelsCount; j++) {
			pixels[mutaPixels[j].index].addShape(newShape);
		}
		return;
	}

	public double diffIncIfRemoved(Shape oldShape) {
		double diffOfDiff = 0;
		findMutaPixelsOldShape(oldShape);
		for (int j = 0; j < mutaPixelsCount; j++) {
			currPixelOrder = j; // for debug
			diffOfDiff += pixels[mutaPixels[j].index].diffIncIfRemoved(oldShape);
		}
		return diffOfDiff;
	}

	public void removeShape(Shape oldShape) {
		for (int j = 0; j < mutaPixelsCount; j++) {
			pixels[mutaPixels[j].index].removeShape(oldShape);
		}
		return;
	}

	public double diffIncIfReplaced(Shape oldShape, Shape newShape) {
		if (oldShape == null && newShape == null) {
			return 0;
		}
		if (oldShape == null) {
			return diffIncIfAdded(newShape);
		} else if (newShape == null) {
			return diffIncIfRemoved(oldShape);
		} else {
			double diffOfDiff = 0;
			boolean sameRgba = sameRgba(newShape.rgba, oldShape.rgba);
			MutaPixel mp;
			findMutaPixelsOldNewShape(oldShape, newShape);
			for (int j = 0; j < mutaPixelsCount; j++) {
				mp = mutaPixels[j];
				currPixelOrder = j; // for debug
				diffOfDiff += pixels[mp.index].diffIncIfReplaced(oldShape, newShape, mp.intype, sameRgba);
			}
			return diffOfDiff;

		}
	}

	public void replaceShape(Shape oldShape, Shape newShape) {
		if (oldShape == null && newShape == null) {
			return;
		}
		if (oldShape == null) {
			addShape(newShape);
			saveShapePixels(newShape);
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
			saveShapePixels(newShape);
			return;

		}
	}

	private boolean sameRgba(double[] rgba1, double[] rgba2) {
		for (int j = 0; j < 4; j++) {
			if (Math.abs(rgba1[j] - rgba2[j]) > 1.e-5) {
				return false;
			}
		}
		return true;
	}

	private double penalty(int pointsCount) {
		return (double) pointsCount * (double) pointsCount / (double) 10000000000.0;
	}

	private double penaltyRgb(double addeDiff) {
		return addeDiff / (width * height);
	}

	private double penaltyShape(double addeDiff, int pointsCount) {
		return penaltyRgb(addeDiff) + penalty(pointsCount);
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

	private Shape getRandomShape() {
		Shape shape = new Shape();
		shape.order = gOrder++;
		double[] color = new double[4];
		for (int i = 0; i < 3; i++) {
			color[i] = (1 / (double) 255) * randg.nextInt(256);
		}
		// color[3] = alpha, from 1 to 255; (0 is useless)
		color[3] = (1 / (double) 255) * (randg.nextInt(255) + 1);
		shape.rgba = color;
		int[][] points = new int[3][];
		for (int i = 0; i < 3; i++) {
			points[i] = new int[2];
			points[i][0] = randg.nextInt(width);
			points[i][1] = randg.nextInt(height);
		}
		shape.points = points;
		return shape;
	}

	private Shape copy(Shape shape) {
		Shape res = new Shape();
		res.order = shape.order;
		if (shape.rgba != null) {
			res.rgba = new double[shape.rgba.length];
			System.arraycopy(shape.rgba, 0, res.rgba, 0, shape.rgba.length);
		}
		if (shape.points != null) {
			res.points = new int[shape.points.length][];
			for (int i = 0; i < shape.points.length; i++) {
				if (shape.points[i] != null) {
					int[] point = new int[2];
					System.arraycopy(shape.points[i], 0, point, 0, shape.points[i].length);
					res.points[i] = point;
				}
			}
		}
		if (shape.pixinds != null) {
			res.pixinds = new int[shape.pixinds.length];
			System.arraycopy(shape.pixinds, 0, res.pixinds, 0, shape.pixinds.length);
		}
		return res;
	}

	private Shape selectPixelsRandomShape() {
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
		RbTree<Integer, Shape>.EntryIterator it = pixels[index].shapes.new EntryIterator(
				pixels[index].shapes.firstEntry());
		while (it.hasNext()) {
			if (j++ >= index2) {
				return it.next().getValue();
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
			mutation.newShape = getRandomShape();
			mutation.shapesCount = shapesCount + 1;
			mutation.pointsCount = pointsCount + mutation.newShape.points.length;
			mutationType = "A(" + mutation.newShape.order + ")";
			return;
		} else if (opcode < 40) { // remove shape
			if (shapesCount == 0) {
				mutationType = "0";
				return;
			}
			int index = randg.nextInt(shapesCount);
			mutation.oldShape = shapes[index];
			mutation.index = index;
			mutation.shapesCount = shapesCount - 1;
			mutation.pointsCount = pointsCount - (mutation.oldShape.points.length);
			mutationType = "R(" + mutation.oldShape.order + ")";
			return;
		} else { // modify shape
			if (shapesCount == 0) {
				mutationType = "0";
				return;
			}
			Shape oldShape;
			Shape newShape;
			int index;
			index = randg.nextInt(shapesCount);
			oldShape = shapes[index];
			newShape = copy(oldShape);
			int inner = randg.nextInt(newShape.points.length + 1);
			int inninner;
			if (inner == 0) { // rgba
				inninner = randg.nextInt(4);
				int move = randg.nextInt(20) - 10;
				double[] tmp = newShape.rgba;
				tmp[inninner] += move;
				// if messing with color, trim outputs to 0, 255
				tmp[inninner] = Math.min(tmp[inninner], 255);
				tmp[inninner] = Math.max(tmp[inninner], 0);
				tmp[inninner] = (1 / (double) 255) * tmp[inninner];

			} else {
				inninner = randg.nextInt(newShape.points[inner - 1].length);
				int move = randg.nextInt(20) - 10;
				int[] tmp = newShape.points[inner - 1];
				tmp[inninner] += move;
				// trim x coordinate to 0, width
				if (inninner == 0) {
					tmp[inninner] = Math.min(tmp[inninner], width);
					tmp[inninner] = Math.max(tmp[inninner], 0);
					// trim y coordinate to 0, height
				} else if (inninner == 1) {
					tmp[inninner] = Math.min(tmp[inninner], height);
					tmp[inninner] = Math.max(tmp[inninner], 0);
				}
			}
			mutation.oldShape = oldShape;
			mutation.newShape = newShape;
			mutation.index = index;
			mutation.pointsCount = pointsCount - oldShape.points.length + newShape.points.length;
			mutationType = "M(" + mutation.oldShape.order + ")";
			return;
		}
	}

	private Shape[] alterShapes(int index, Shape oldShape, Shape newShape) {
		int n = shapes.length;
		if (oldShape == null && newShape == null) {
			return shapes;
		} else if (oldShape == null) {
			Shape[] newShapes = new Shape[n + 1];
			System.arraycopy(shapes, 0, newShapes, 0, n);
			newShapes[n] = newShape;
			return newShapes;
		} else if (newShape == null) {
			Shape[] newShapes = new Shape[n - 1];
			System.arraycopy(shapes, 0, newShapes, 0, index);
			System.arraycopy(shapes, index + 1, newShapes, index, (n - 1 - index));
			return newShapes;
		} else {
			Shape[] newShapes = new Shape[n];
			System.arraycopy(shapes, 0, newShapes, 0, n);
			newShapes[index] = newShape;
			return newShapes;
		}
	}

	public boolean doRandomChange() {

		cntRandomChange++;
		temp = Math.max(temp, Math.pow(10, -10));
		getRandomMutation();
		double newAddeDiff = addeDiff + diffIncIfReplaced(mutation.oldShape, mutation.newShape);
		double newDiff = penaltyShape(newAddeDiff, mutation.pointsCount);
		// newDiff < d -> vzdy true
		// newDiff - d = temp -> akceptujem so sancou e^-1
		// newDiff - d = 2temp -> akceptujem so sancou e^-2
		// if (randg.nextDouble() < Math.exp(- (newDiff - d) / temp)) {
		if (newDiff < diff) {
			if (newDiff - diff < -0.00001) {
				temp *= 0.5;
			}
			replaceShape(mutation.oldShape, mutation.newShape);
			shapes = alterShapes(mutation.index, mutation.oldShape, mutation.newShape);
			assert shapes.length == mutation.shapesCount;
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

	public Shape[] extractShapes() {
		RbTree<Integer, Shape> exShapes = new RbTree<Integer, Shape>();
		for (int j = 0; j < height * width; j++) {
			RbTree<Integer, Shape>.EntryIterator it = pixels[j].shapes.new EntryIterator(pixels[j].shapes.firstEntry());
			while (it.hasNext()) {
				RbTree<Integer, Shape>.Entry entry = it.next();
				exShapes.put(entry.getKey(), entry.getValue());
			}
		}
		Shape[] outShapes = new Shape[exShapes.size()];
		int j = 0;
		RbTree<Integer, Shape>.EntryIterator it = exShapes.new EntryIterator(exShapes.firstEntry());
		while (it.hasNext()) {
			outShapes[j++] = it.next().getValue();
		}
		return outShapes;
	}

	public int recalcPointsCount(Shape[] shapes) {
		int pc = 0;
		for (Shape shape : shapes) {
			pc += shape.points.length;
		}
		assert pc == pointsCount;
		return pc;
	}

	// public void rgbRegen() {
	// for (int j = 0; j < height * width; j++) {
	// pixels[j].rgbRegen();
	// }
	// }

	public ShapeRange getShapeRange(Shape shape) {
		ShapeRange range = new ShapeRange();
		range.xMin = range.yMin = Integer.MAX_VALUE;
		range.xMax = range.yMax = Integer.MIN_VALUE;
		if (shape != null) {
			for (int j = 0; j < shape.points.length; j++) {
				int xy[] = shape.points[j];
				range.xMin = Math.min(range.xMin, xy[0]);
				range.xMax = Math.max(range.xMax, xy[0]);
				range.yMin = Math.min(range.yMin, xy[1]);
				range.yMax = Math.max(range.yMax, xy[1]);
			}
		}
		return range;
	}

	public static int[] getRgbaInt(double rgba[]) {
		int rgbaInt[] = { (int) Math.round(255 * rgba[0]), (int) Math.round(255 * rgba[1]),
				(int) Math.round(255 * rgba[2]), (int) Math.round(255 * rgba[3]) };
		return rgbaInt;
	}

	public int getRgbInt(int x, int y) {
		return pixels[y * width + x].getRgbInt();
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

	public double getAvgNumOfShapesPerPixel() {
		double totalShapes = 0;
		for (int j = 0; j < height * width; j++) {
			totalShapes += pixels[j].shapes.size();
		}
		return totalShapes / (height * width);
	}

	public static boolean doLog = false;

	public static void log(String text) {
		if (doLog) {
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter("testdata/log.txt", true));
				writer.append(text);
				writer.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public static void log(StringBuilder sb) {
		log(sb.toString());
	}

	public static void logNewline() {
		log("\n");
	}

	public static void logClean() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("testdata/log.txt"));
			writer.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
