import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Random;

public class Area {
	public int width;
	public int height;
	private AreaPixel[] pixels;
	private ShapeRange shapeRange;
	private BufferedImage tempBufferedImage;
	int[][][] shapes;
	int pointsCount;
	private int gOrder;
	private  int[][] targetRgb;
	Mutation mutation;
	private double addeDiff;				//additive difference
	private double newAddeDiff;
	double diff;
	private double newDiff;
	double temp;
	Random randg;
	
	public class Mutation{
		int [][] oldShape = null;
		int [][] newShape = null;
		int index = 0;				//mutated shape index
		int shapesCount = 0;		//mutated shapes count
		int pointsCount = 0;		//mutated shapes points count
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
		pointsCount = 0;
		shapeRange = new ShapeRange();
		tempBufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		gOrder = 0;
		mutation = new Mutation();
		temp = 1;
		//randg = new Random(0);
		randg = new Random();
	}
	
	public void setTargetRgb(int[][]rgb){
		this.targetRgb = rgb;
		addeDiff = newAddeDiff = diff();
		diff = penaltyShape(pointsCount);
	}

	private int[] getShapePixels(int[][] shape) {
		Graphics g = tempBufferedImage.getGraphics();
		g.setColor(new Color(255, 255, 255));
		g.fillRect(0, 0, width, height);
		g.setColor(new Color(0, 0, 0, 255));
		int npoints = shape.length - 1;
		int xpoints[] = new int[npoints];
		int ypoints[] = new int[npoints];
		for (int i = 0; i < npoints; i++) {
			// 0-th element of shape is color
			xpoints[i] = shape[i + 1][0];
			ypoints[i] = shape[i + 1][1];
		}
		g.fillPolygon(xpoints, ypoints, npoints);
		return ((DataBufferInt) tempBufferedImage.getRaster().getDataBuffer()).getData();

	}

	public int getRgbInt(int x, int y) {
		return pixels[y * width + x].getRgbInt();
	}

	private int getShapeOrder(int[][] shape) {
		return shape[0][4];
	}

	public double addShape(int[][] shape, int phase) {
		final int[] tmppixels = getShapePixels(shape);
		int order = getShapeOrder(shape);
		double diffOfDiff = 0;
		shapeRange.initialize().add(shape);
		for (int jh = shapeRange.yMin; jh < shapeRange.yMax; jh++) {
			int jhw = jh * width;
			for (int jw = shapeRange.xMin; jw < shapeRange.xMax; jw++) {
				int ind = jhw + jw;
				if ((tmppixels[ind] & 0xff) == 0) {
					AreaPixel p = pixels[ind];
					p.shapes.put(order, shape);
					diffOfDiff += p.rgbRegen(targetRgb[ind], phase);
				}
			}
		}
		return diffOfDiff;
	}

	public double removeShape(int[][] shape, int phase) {
		int order = getShapeOrder(shape);
		double diffOfDiff = 0;
		shapeRange.initialize().add(shape);
		for (int jh = shapeRange.yMin; jh < shapeRange.yMax; jh++) {
			int jhw = jh * width;
			for (int jw = shapeRange.xMin; jw < shapeRange.xMax; jw++) {
				int ind = jhw + jw;
				AreaPixel p = pixels[ind];
				if (p.shapes.containsKey(order)) {
					p.shapes.remove(order);
					diffOfDiff += p.rgbRegen(targetRgb[ind], phase);
				}
			}
		}
		return diffOfDiff;
	}

	public double replaceShape(int[][] oldShape, int[][] newShape, int phase) {
		if (oldShape == null && newShape == null) {
			return 0;
		}
		if (oldShape == null) {
			return addShape(newShape, phase);
		} else if (newShape == null) {
			return removeShape(oldShape, phase);
		} else {
			final int[] tmppixels = getShapePixels(newShape);
			boolean pixelChange, pixelInOld, pixelInNew;
			int orderOldShape = getShapeOrder(oldShape);
			int orderNewShape = getShapeOrder(newShape);
			boolean colorsDiffer = colorsDiffer(newShape[0], oldShape[0]);
			AreaPixel p;
			double diffOfDiff = 0;
			shapeRange.initialize().add(oldShape).add(newShape);
			for (int jh = shapeRange.yMin; jh < shapeRange.yMax; jh++) {
				int jhw = jh * width;
				for (int jw = shapeRange.xMin; jw < shapeRange.xMax; jw++) {
					int ind = jhw + jw;
					p = pixels[ind];
					pixelInOld = p.shapes.containsKey(orderOldShape);
					pixelInNew = (tmppixels[ind] & 0xff) == 0;
					pixelChange = false;
					if (pixelInOld && pixelInNew && (orderOldShape == orderNewShape)) {
						p.shapes.put(orderNewShape, newShape);
						if(colorsDiffer){
							pixelChange = true;
						}
					} else{
						if (pixelInOld) {
							p.shapes.remove(orderOldShape);
							pixelChange = true;
						}
						if (pixelInNew) {
							p.shapes.put(orderNewShape, newShape);
							pixelChange = true;
						}
					}
					if (pixelChange) {
						diffOfDiff += p.rgbRegen(targetRgb[ind], phase);
					}
				}
			}
			return diffOfDiff;
		}
	}
	
	private boolean colorsDiffer(int[] color1, int[] color2){
		if(color1[0] != color2[0]) return true;
		if(color1[1] != color2[1]) return true;
		if(color1[2] != color2[2]) return true;
		if(color1[3] != color2[3]) return true;
		return false;
	}

	
	private double penalty(int pointsCount) {
		return pointsCount * pointsCount / 10000000000.0;
	}
	private double penaltyRgb() {
		//return newAddeDiff/(width * height);
		return addeDiff/(width * height);
	}

	private double penaltyShape(int nPoints) {
		return penaltyRgb() + penalty(nPoints);
	}

	private double penalty(int[][][] shapes) {
		return penalty(recalcPointsCount(shapes));
	}
	
	private double penaltyShape(int[][][] shapes) {
		return  penaltyRgb() + penalty(shapes);
	}
	
	private int recalcPointsCount(int[][][] shapes) {
		int pc = 0;
		for (int[][] shape : shapes) {
			pc += shape.length - 1;
		}
		return pc;
	}

	public double diffTest() {
		return diff()/(width * height) + penalty(shapes);
	}
	
	private double diff() {
		assert (pixels.length == targetRgb.length);
		double diff = 0;
		for (int j = 0; j < pixels.length; j++) {
			diff += AreaPixel.diff(pixels[j].rgb, targetRgb[j]);
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
		int[][] res = new int[4][];
		res[0] = color;
		for (int i = 0; i < 3; i++) {
			res[i + 1] = new int[2];
			res[i + 1][0] = randg.nextInt(width);
			res[i + 1][1] = randg.nextInt(height);
		}
		return res;
	}

	private int[][] copy(int[][] shape) {
		int[][] res = new int[shape.length][];
		for (int i = 0; i < shape.length; i++) {
			res[i] = new int[shape[i].length];
			for (int j = 0; j < shape[i].length; j++) {
				res[i][j] = shape[i][j];
			}
		}
		return res;
	}
	

	private void getRandomMutation(){
		int opcode = randg.nextInt(100);
		int n = shapes.length;
		mutation.oldShape = null;
		mutation.newShape = null;
		mutation.index = n;
		mutation.shapesCount = n;
		mutation.pointsCount = pointsCount;
		if (opcode < 20) {	//add shape
			mutation.newShape = getRandomShape();
			mutation.index = n + 1;
			mutation.pointsCount = pointsCount + mutation.newShape.length - 1;
			return;
		} else if (opcode < 40) {	//remove shape
			if (n == 0) {
				return;
			}
			int index = randg.nextInt(n);
			mutation.oldShape = shapes[index];
			mutation.index = index;
			mutation.shapesCount = n - 1;
			mutation.pointsCount = pointsCount - (shapes[index].length - 1);
			return;
		} else {	//modify shape
			if (n == 0) {
				return;
			}
			int index = randg.nextInt(n);
			int newShape[][] = copy(shapes[index]);

			int inner = randg.nextInt(newShape.length);
			int inninner, randMax;
			if (inner == 0) {
				randMax = 4;
			} else {
				randMax = newShape[inner].length;
			}
			inninner = randg.nextInt(randMax);
			int move = randg.nextInt(20) - 10;
			int[] tmp = newShape[inner];
			tmp[inninner] += move;
			// if messing with color, trim outputs to 0, 255
			if (inner == 0) {
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
			mutation.oldShape = shapes[index];
			mutation.newShape = newShape;
			mutation.index = index;
			mutation.pointsCount =  pointsCount - shapes[index].length + newShape.length;
			return;
		}
		
	}
	
	private int[][][] alterShapes(int index, int[][] oldShape, int[][] newShape) {
		int n = shapes.length;
		if(oldShape == null && newShape == null){
			return shapes;
		} else if (oldShape == null){
			int[][][] newShapes = new int[n + 1][][];
			System.arraycopy(shapes, 0, newShapes, 0, n);
			newShapes[n] = newShape;
			return newShapes;
		} else if (newShape == null){
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


	
	public boolean doRandomChange(){

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
		

		temp = Math.max(temp, Math.pow(10, -10));
		int changeParams[] = new int[3];
		getRandomMutation();
		//newAddeDiff = addeDiff + replaceShape(mutation.oldShape, mutation.newShape, 0);
		addeDiff = addeDiff + replaceShape(mutation.oldShape, mutation.newShape, 0);
		newDiff = penaltyShape(mutation.pointsCount);
		// newDiff < d -> vzdy true
		// newDiff - d = temp -> akceptujem so sancou e^-1
		// newDiff - d = 2temp -> akceptujem so sancou e^-2
		// if (randg.nextDouble() < Math.exp(- (newDiff - d) / temp)) {
		if (newDiff < diff) {
			if (newDiff - diff < -0.00001) {
				temp *= 0.5;
			}
			//replaceShape(mutation.oldShape, mutation.newShape, 1);
			shapes = alterShapes(mutation.index, mutation.oldShape, mutation.newShape);
			pointsCount = mutation.pointsCount;
			//addeDiff = newAddeDiff;
			diff = newDiff;
			return true;
		} else {
			addeDiff = addeDiff + replaceShape(mutation.newShape, mutation.oldShape, 2);
			temp *= 1.002;
			return false;
		}
	}

	public void saveShapes(String sFile) {
		BufferedWriter writer = null;
		try {
			File outFile = new File(sFile);
			StringBuilder sb = Utils.ShapesToSb(shapes);
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


}
