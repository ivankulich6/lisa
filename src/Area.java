import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

public class Area {

	// Testing methods of pre-finding difference increment
	public static enum DiffIncIfMethod {
		// Iterate shapes tree parts and apply convexCombine. Reducer not used.
		ITERATE("ITERATE"),
		// Use reduced from-to calls.
		RFT("RFT"),
		// Do change in shapes tree (put, remove), use reduced value, revert
		// change. Reducer is used, but from-to calls are not used.
		PUTREM("PUTREM"),
		// Not implemented. Do change in shapes tree (put, remove), iterate full
		// tree and apply convexCombine, revert change in tree. Reducer not
		// used.
		PUTREM_ITERATE("PUTREM_ITERATE");

		private final String name;

		private DiffIncIfMethod(String s) {
			name = s;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}

	public String targetPath;
	BufferedImage target;
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
	Random randg;
	boolean useShapesColorsReducers;
	double penaltyPointsCountParam;
	// annealing support
	double temperature;
	double diffMin;
	private boolean needSaveShapesMin;
	Shape[] shapesMin;
	int shapesCountMin;
	int pointsCountMin;
	// mutations counters
	public int mutationsTotal;
	public int mutationsAccepted;
	public int mutationsAdd;
	public int mutationsRemove;
	public int mutationsReplace;
	// debug helpers
	public static int cntRandomChange;
	public static int currPixelOrder;
	public static long ccCount;

	public class Mutation {
		Shape oldShape = null;
		Shape newShape = null;
		// mutated shape index in shapes array
		int index = 0;
		// expected total shapes count in area after mutation
		int shapesCount = 0;
		// expected total shapes points count in area after mutation
		int pointsCount = 0;
		// 0... no mutation, 1... remove (newShape = null),
		// 2... add (newShape = null), 3... replace
		int type;
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

	Area(boolean init) {
		if (init) {
			shapes = new Shape[0];
			shapesCount = 0;
			pointsCount = 0;
			gOrder = 0;
			mutation = new Mutation();
			randg = new Random();
			penaltyPointsCountParam = 1.e-5;
			needSaveShapesMin = false;
			temperature = -1;
			diffMin = Double.MAX_VALUE;
			mutationsTotal = 0;
			mutationsAccepted = 0;
			mutationsAdd = 0;
			mutationsRemove = 0;
			mutationsReplace = 0;
			cntRandomChange = 0;
		}
	}

	public void setTarget(String targetPath, boolean withReducer) throws IOException {
		useShapesColorsReducers = withReducer;
		this.targetPath = targetPath;
		target = Utils.readImage(targetPath);
		width = target.getWidth();
		height = target.getHeight();
		pixels = new AreaPixel[width * height];
		for (int j = 0; j < height * width; j++) {
			pixels[j] = new AreaPixel(withReducer);
		}
		tempBufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		mutaPixels = new MutaPixel[width * height];
		for (int j = 0; j < height * width; j++) {
			mutaPixels[j] = new MutaPixel();
		}
		mutaPixelsWork = new int[2 * width * height];
		setTargetRgb(Utils.getPixelsRgb(target));
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
		if (temperature >= 0) {
			diffMin = diff;
		}
	}

	public void setPenaltyPointsCountParam(double value) {
		penaltyPointsCountParam = value;
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
		} else {
			shape.pixinds = new int[0];
		}
	}

	private void assertReducerInitialized() {
		assert useShapesColorsReducers : "Reducer is not initialized";
	}

	public double diffIncIfAdded(Shape newShape, DiffIncIfMethod method) {
		double diffOfDiff = 0;
		findMutaPixelsNewShape(newShape);
		if (method == DiffIncIfMethod.ITERATE) {
			for (int j = 0; j < mutaPixelsCount; j++) {
				currPixelOrder = j; // for debug
				diffOfDiff += pixels[mutaPixels[j].index].diffIncIfAdded_ITERATE(newShape);
			}
		} else if (method == DiffIncIfMethod.RFT) {
			assertReducerInitialized();
			for (int j = 0; j < mutaPixelsCount; j++) {
				currPixelOrder = j; // for debug
				diffOfDiff += pixels[mutaPixels[j].index].diffIncIfAdded_RFT(newShape);
			}
		} else if (method == DiffIncIfMethod.PUTREM) {
			assertReducerInitialized();
			for (int j = 0; j < mutaPixelsCount; j++) {
				currPixelOrder = j; // for debug
				diffOfDiff += pixels[mutaPixels[j].index].diffIncIfAdded_PUTREM(newShape);
			}
		}
		return diffOfDiff;
	}

	public void addShape(Shape newShape) {
		for (int j = 0; j < mutaPixelsCount; j++) {
			pixels[mutaPixels[j].index].addShape(newShape);
		}
		return;
	}

	public double diffIncIfRemoved(Shape oldShape, DiffIncIfMethod method) {
		double diffOfDiff = 0;
		findMutaPixelsOldShape(oldShape);
		if (method == DiffIncIfMethod.ITERATE) {
			for (int j = 0; j < mutaPixelsCount; j++) {
				currPixelOrder = j; // for debug
				diffOfDiff += pixels[mutaPixels[j].index].diffIncIfRemoved_ITERATE(oldShape);
			}
		} else if (method == DiffIncIfMethod.RFT) {
			assertReducerInitialized();
			for (int j = 0; j < mutaPixelsCount; j++) {
				currPixelOrder = j; // for debug
				diffOfDiff += pixels[mutaPixels[j].index].diffIncIfRemoved_RFT(oldShape);
			}
		} else if (method == DiffIncIfMethod.PUTREM) {
			assertReducerInitialized();
			for (int j = 0; j < mutaPixelsCount; j++) {
				currPixelOrder = j; // for debug
				diffOfDiff += pixels[mutaPixels[j].index].diffIncIfRemoved_PUTREM(oldShape);
			}
		}

		return diffOfDiff;
	}

	public void removeShape(Shape oldShape) {
		for (int j = 0; j < mutaPixelsCount; j++) {
			pixels[mutaPixels[j].index].removeShape(oldShape);
		}
		return;
	}

	public double diffIncIfReplaced(Shape oldShape, Shape newShape, DiffIncIfMethod method) {
		if (oldShape == null && newShape == null) {
			return 0;
		}
		if (oldShape == null) {
			return diffIncIfAdded(newShape, method);
		} else if (newShape == null) {
			return diffIncIfRemoved(oldShape, method);
		} else {
			double diffOfDiff = 0;
			boolean sameRgba = sameRgba(newShape.rgba, oldShape.rgba);
			MutaPixel mp;
			findMutaPixelsOldNewShape(oldShape, newShape);
			if (method == DiffIncIfMethod.ITERATE) {
				for (int j = 0; j < mutaPixelsCount; j++) {
					mp = mutaPixels[j];
					currPixelOrder = j; // for debug
					diffOfDiff += pixels[mp.index].diffIncIfReplaced_ITERATE(oldShape, newShape, mp.intype, sameRgba);
				}
			} else if (method == DiffIncIfMethod.RFT) {
				assertReducerInitialized();
				for (int j = 0; j < mutaPixelsCount; j++) {
					mp = mutaPixels[j];
					currPixelOrder = j; // for debug
					diffOfDiff += pixels[mp.index].diffIncIfReplaced_RFT(oldShape, newShape, mp.intype, sameRgba);
				}
			} else if (method == DiffIncIfMethod.PUTREM) {
				assertReducerInitialized();
				for (int j = 0; j < mutaPixelsCount; j++) {
					mp = mutaPixels[j];
					currPixelOrder = j; // for debug
					diffOfDiff += pixels[mp.index].diffIncIfReplaced_PUTREM(oldShape, newShape, mp.intype, sameRgba);
				}
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

	private double penaltyPointsCount(int pointsCount) {
		double p = pointsCount * penaltyPointsCountParam;
		return p * p;
	}

	public double penaltyRgb(double addeDiff) {
		return addeDiff / (width * height);
	}

	private double penaltyShape(double addeDiff, int pointsCount) {
		return penaltyRgb(addeDiff) + penaltyPointsCount(pointsCount);
	}

	public double diffTest() {
		return diff() / (width * height) + penaltyPointsCount(pointsCount);
	}

	public double diffTest(double addeDiff) {
		return addeDiff / (width * height) + penaltyPointsCount(pointsCount);
	}

	private double diff() {
		double diff = 0;
		for (AreaPixel pixel : pixels) {
			diff += pixel.diff();
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

	private Shape[] copyShapes(Shape shapesSrc[], Shape shapesDst[]) {
		shapesDst = new Shape[shapesSrc.length];
		System.arraycopy(shapesSrc, 0, shapesDst, 0, shapesSrc.length);
		return shapesDst;
	}

	void saveShapesMin() {
		shapesMin = copyShapes(shapes, shapesMin);
		shapesCountMin = shapesCount;
		pointsCountMin = pointsCount;
	}

	void restoreShapesMin() {
		if (shapesMin == null) {
			return;
		}
		shapes = copyShapes(shapesMin, shapes);
		shapesCount = shapesCountMin;
		pointsCount = pointsCountMin;
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
			mutation.type = 2;
			return;
		} else if (opcode < 40) { // remove shape
			if (shapesCount == 0) {
				mutation.type = 0;
				return;
			}
			int index = randg.nextInt(shapesCount);
			mutation.oldShape = shapes[index];
			mutation.index = index;
			mutation.shapesCount = shapesCount - 1;
			mutation.pointsCount = pointsCount - (mutation.oldShape.points.length);
			mutation.type = 1;
			return;
		} else { // modify shape
			if (shapesCount == 0) {
				mutation.type = 0;
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
				double move = (1 / (double) 255) * (randg.nextInt(20) - 10);
				double[] tmp = newShape.rgba;
				tmp[inninner] += move;
				// trim color coordinate to 0, 1
				tmp[inninner] = Math.min(Math.max(tmp[inninner], 0), 1);
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
			mutation.type = 3;
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

	public int doRandomChange(DiffIncIfMethod method) {
		return doRandomChange(false, method);
	}

	public int doRandomChange(boolean isLast, DiffIncIfMethod method) {
		/*
		 * return value: 0=change not accepted, 1=difference increased within
		 * annealing limit, 2=difference decreased but not 3, 3=difference less
		 * than current global minimum
		 */
		cntRandomChange++;
		mutationsTotal++;
		getRandomMutation();
		if (mutation.type == 0) {
			if (isLast && temperature >= 0) {
				if (needSaveShapesMin) {
					saveShapesMin();
					needSaveShapesMin = false;
				} else {
					restoreShapesMin();
				}
			}
			return 0;
		}
		double newAddeDiff = addeDiff + diffIncIfReplaced(mutation.oldShape, mutation.newShape, method);
		double newDiff = penaltyShape(newAddeDiff, mutation.pointsCount);
		// newDiff < diff -> vzdy true
		// newDiff - diff = temperature -> akceptujem so sancou e^-1
		// newDiff - diff = 2temp -> akceptujem so sancou e^-2
		boolean success = false;
		int ret = 0;
		if (newDiff < diff) {
			success = true;
			ret = (temperature < 0 ? 3 : 2);
		} else if (temperature > 0 && newDiff > diff) {
			if (randg.nextDouble() < Math.exp(-(newDiff - diff) / temperature)) {
				success = true;
			}
		}
		if (success) {
			if (temperature >= 0) {
				if (newDiff < diffMin) {
					diffMin = newDiff;
					needSaveShapesMin = true;
					ret = 3;
				} else if (newDiff >= diff) {
					ret = 1;
					if (needSaveShapesMin) {
						saveShapesMin();
						needSaveShapesMin = false;
					}
				}
			}
			replaceShape(mutation.oldShape, mutation.newShape);
			shapes = alterShapes(mutation.index, mutation.oldShape, mutation.newShape);
			assert shapes.length == mutation.shapesCount;
			shapesCount = mutation.shapesCount;
			pointsCount = mutation.pointsCount;
			addeDiff = newAddeDiff;
			diff = newDiff;
			if (ret == 3) {
				mutationsAccepted++;
				if (mutation.oldShape == null) {
					mutationsAdd++;
				} else if (mutation.newShape == null) {
					mutationsRemove++;
				} else {
					mutationsReplace++;
				}
			}
		}
		if (isLast && temperature >= 0) {
			if (needSaveShapesMin) {
				saveShapesMin();
				needSaveShapesMin = false;
			} else {
				restoreShapesMin();
			}
		}
		return ret;
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

	public ShapeRange getShapeRange(Shape shape) {
		ShapeRange range = new ShapeRange();
		range.xMin = range.yMin = Integer.MAX_VALUE;
		range.xMax = range.yMax = Integer.MIN_VALUE;
		if (shape != null) {
			for (int[] xy : shape.points) {
				range.xMin = Math.min(range.xMin, xy[0]);
				range.xMax = Math.max(range.xMax, xy[0]);
				range.yMin = Math.min(range.yMin, xy[1]);
				range.yMax = Math.max(range.yMax, xy[1]);
			}
		}
		return range;
	}

	public int getRgbInt(int x, int y) {
		return pixels[y * width + x].getRgbInt();
	}

	public void refreshPixelsByShapes(boolean withReducer) {
		for (int j = 0; j < width * height; j++) {
			pixels[j].initShapes(withReducer);
		}
		for (Shape shape : shapes) {
			findMutaPixelsNewShape(shape);
			addShape(shape);
			saveShapePixels(shape);
		}
		for (int j = 0; j < width * height; j++) {
			pixels[j].rgbRegen();
		}
	}

	public void shapesToFile(String sFile) {
		BufferedWriter writer = null;
		try {
			File outFile = new File(sFile);
			outFile.getParentFile().mkdirs();
			StringBuilder sb = Utils.ShapesToSb(this);
			// System.out.println(outFile.getCanonicalPath());
			writer = new BufferedWriter(new FileWriter(outFile));
			writer.write(sb.toString());
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void setFromFile(String sFile, boolean withReducer) {
		setFromFile(sFile, true, withReducer);
	}

	public void setFromFile(String sFile, boolean initPixels, boolean withReducer) {
		Path path = Paths.get(sFile);
		Charset charset = Charset.forName("UTF-8");
		List<String> lines = null;
		int dataWidth = -1;
		int dataHeight = -1;
		int shapesCount = -1;
		int dataPointsCount = -1;
		double shapeData[][][] = null;
		int nLine = 0;
		int currIndex = 0;
		String errTitle = "Error reading shapes: ";
		temperature = -1;
		useShapesColorsReducers = withReducer;
		mutationsTotal = 0;
		mutationsAccepted = 0;
		mutationsAdd = 0;
		mutationsRemove = 0;
		mutationsReplace = 0;
		try {
			lines = Files.readAllLines(path, charset);
			for (String line : lines) {
				++nLine;
				String keyVal[] = Utils.decodeKeyVal(line);
				if (keyVal[0].length() >= 6 && keyVal[0].substring(0, 6).equalsIgnoreCase("Shape_")) {
					assert shapesCount > 0 : errTitle + "Invalid or missing ShapesCount before line " + nLine;
					int index = Integer.parseInt(keyVal[0].substring(6));
					assert index == currIndex && shapesCount > 0 && index < shapesCount : errTitle
							+ "Invalid sequence.";
					currIndex++;
					shapeData[index] = Utils.decodeShapeData(keyVal[1]);
					assert null != shapeData[index] : errTitle + "Invalid shape format, sequence =" + index;
				} else if (keyVal[0].equalsIgnoreCase("TargetPath")) {
					targetPath = keyVal[1];
				} else if (keyVal[0].equalsIgnoreCase("Width")) {
					dataWidth = Integer.parseInt(keyVal[1]);
				} else if (keyVal[0].equalsIgnoreCase("Height")) {
					dataHeight = Integer.parseInt(keyVal[1]);
				} else if (keyVal[0].equalsIgnoreCase("PenaltyPointsCountParam")) {
					penaltyPointsCountParam = Double.parseDouble(keyVal[1]);
				} else if (keyVal[0].equalsIgnoreCase("PointsCount")) {
					dataPointsCount = Integer.parseInt(keyVal[1]);
				} else if (keyVal[0].equalsIgnoreCase("DistancePerPixel")) {
				} else if (keyVal[0].equalsIgnoreCase("Temperature")) {
					temperature = Double.parseDouble(keyVal[1]);
				} else if (keyVal[0].equalsIgnoreCase("mutationsTotal")) {
					mutationsTotal = Integer.parseInt(keyVal[1]);
				} else if (keyVal[0].equalsIgnoreCase("mutationsAccepted")) {
					mutationsAccepted = Integer.parseInt(keyVal[1]);
				} else if (keyVal[0].equalsIgnoreCase("mutationsAdd")) {
					mutationsAdd = Integer.parseInt(keyVal[1]);
				} else if (keyVal[0].equalsIgnoreCase("mutationsRemove")) {
					mutationsRemove = Integer.parseInt(keyVal[1]);
				} else if (keyVal[0].equalsIgnoreCase("mutationsReplace")) {
					mutationsReplace = Integer.parseInt(keyVal[1]);
				} else if (keyVal[0].equalsIgnoreCase("ShapesCount")) {
					shapesCount = Integer.parseInt(keyVal[1]);
					shapeData = new double[shapesCount][][];
				} else {
					assert false : errTitle + "Unknown or missing keyword in line " + nLine;
				}
			}
			assert targetPath != null : errTitle + "Missing TargetPath.";
			assert dataWidth > 0 : errTitle + "Invalid Width.";
			assert dataHeight > 0 : errTitle + "Invalid Height.";
			assert currIndex == shapesCount : errTitle + "Missing shape after line " + nLine;

			this.shapesCount = shapesCount;
			shapes = new Shape[shapesCount];
			pointsCount = 0;
			for (int j = 0; j < shapesCount; j++) {
				double shapeData1[][] = shapeData[j];
				assert shapeData1.length >= 4 : errTitle + "Missing data in shape " + j;
				assert shapeData1[0].length == 4 : errTitle + "Invalid format of shape " + j;
				Shape shape = new Shape();
				shapes[j] = shape;
				shape.rgba = new double[4];
				for (int k = 0; k < 4; k++) {
					double val = shapeData1[0][k];
					assert val >= 0 && val <= 1 : errTitle + "Color out of range in shape " + j;
					shape.rgba[k] = val;
				}
				shape.order = j;
				int nPoints = shapeData1.length - 1;
				pointsCount += nPoints;
				shape.points = new int[nPoints][];
				for (int k = 0; k < nPoints; k++) {
					assert shapeData1[k + 1].length == 2 : errTitle + "Invalid points data in shape " + j;
					shape.points[k] = new int[2];
					for (int l = 0; l < 2; l++) {
						int coord = (int) Math.round(shapeData1[k + 1][l]);
						assert coord >= 0
								&& ((l == 0 && coord <= dataWidth) || (l == 1 && coord <= dataHeight)) : errTitle
										+ "Point out of range in shape " + j;
						shape.points[k][l] = coord;
					}
				}

			}
			assert dataPointsCount == -1 || dataPointsCount == pointsCount : errTitle
					+ "PointsCount does not match shapes data";

			if (initPixels) {
				setTarget(targetPath, withReducer);
				assert width == dataWidth && height == dataHeight : errTitle + "Shape/Target dimemsions mismatch"
						+ nLine;
				refreshPixelsByShapes(withReducer);
				gOrder = shapesCount;
				addeDiff = diff();
				diff = diffTest(addeDiff);
				if (temperature >= 0) {
					diffMin = diff;
					saveShapesMin();
				}
			} else {
				width = dataWidth;
				height = dataHeight;
				diffMin = Double.MAX_VALUE;
			}

		} catch (IOException e) {
			System.out.println(e);
		}

	}

	public double getAvgNumOfShapesPerPixel() {
		double totalShapes = 0;
		for (int j = 0; j < height * width; j++) {
			totalShapes += pixels[j].shapes.size();
		}
		return totalShapes / (height * width);
	}

}
