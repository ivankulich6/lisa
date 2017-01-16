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
	//private  int[][] targetRgb;
	Mutation mutation;
	MutaPixel[] mutaPixels;
	int mutaPixelsCount;
	private double addeDiff;				//additive difference
	private double newAddeDiff;
	double diff;
	private double newDiff;
	double temp;
	Random randg;
	
	String mutationType;	//for debug
	
	public class Mutation{
		int [][] oldShape = null;
		int [][] newShape = null;
		int index = 0;				//mutated shape index
		int shapesCount = 0;		//mutated shapes count
		int pointsCount = 0;		//mutated shapes points count
	}
	
	public class MutaPixel{
		int index = 0;
		int intype = 0;		// 1=only in oldShape, 2 = only in newShape, 3 = in both
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
		mutaPixels = new MutaPixel[width * height];
		for (int j = 0; j < height * width; j++) {
			mutaPixels[j] = new MutaPixel();
		}

		temp = 1;
		randg = new Random(6543210);
		//randg = new Random();
	}
	
	public void setTargetRgb(int[][] rgb){
		assert pixels.length == rgb.length;
		for(int j = 0; j < width * height; j++){
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
	
	public void findMutaPixelsNewShape(int[][] newShape){
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
		
	public void findMutaPixelsOldShape(int[][] oldShape){
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
		
	public void findMutaPixelsOldNewShape(int[][] oldShape, int[][] newShape){
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
				if(pixelInOld + pixelInNew > 0){
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
		for(int j = 0; j < mutaPixelsCount; j++){
			diffOfDiff += pixels[mutaPixels[j].index].prepareAddShape(newShape);
		}
		return diffOfDiff;
	}

	public void addShape(int[][] newShape) {
		for(int j = 0; j < mutaPixelsCount; j++){
			pixels[mutaPixels[j].index].addShape(newShape);
		}
		return;
	}

	public double prepareRemoveShape(int[][] oldShape) {
		double diffOfDiff = 0;
		findMutaPixelsOldShape(oldShape);
		for(int j = 0; j < mutaPixelsCount; j++){
			diffOfDiff += pixels[mutaPixels[j].index].prepareRemoveShape(oldShape);
		}
		return diffOfDiff;
	}
	
	public void removeShape(int[][] oldShape) {
		for(int j = 0; j < mutaPixelsCount; j++){
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
			boolean sameRgba = sameRgba(newShape[0], oldShape[0]);
			MutaPixel mp;
			findMutaPixelsOldNewShape(oldShape, newShape);
			for(int j = 0; j < mutaPixelsCount; j++){
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
			for(int j = 0; j < mutaPixelsCount; j++){
				mp = mutaPixels[j];
				pixels[mp.index].replaceShape(oldShape, newShape, mp.intype);
			}
			return;
			
		}
	}
	
	private boolean sameRgba(int[] rgba1, int[] rgba2){
		if(rgba1[0] != rgba2[0]) return false;
		if(rgba1[1] != rgba2[1]) return false;
		if(rgba1[2] != rgba2[2]) return false;
		if(rgba1[3] != rgba2[3]) return false;
		return true;
	}

	
	private double penalty(int pointsCount) {
		return pointsCount * pointsCount / 10000000000.0;
	}

	private double penaltyRgb() {
		return newAddeDiff/(width * height);
	}

	private double penaltyShape(int pointsCount) {
		return penaltyRgb() + penalty(pointsCount);
	}

	private double penalty(int[][][] shapes) {
		return penalty(recalcPointsCount(shapes));
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
	
	public double diffTest(double addeDiff) {
		return addeDiff/(width * height) + penalty(shapes);
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
	
	int gCnt = 0;
	private void getRandomMutation(){
//		++gCnt;
//		if(gCnt == 1){
//			mutationType = "A";
//			mutation.oldShape = null;
//			mutation.newShape = new int[][]{ { 255, 0, 0, 100, 0 }, { 0, 0 }, { 100, 0 }, { 0, 100 } };
//			mutation.index = shapes.length + 1;
//			mutation.pointsCount = pointsCount + mutation.newShape.length - 1;
//			return;
//		}else if(gCnt == 2){
//			mutationType = "A";
//			mutation.oldShape = null;
//			mutation.newShape = new int[][]{ { 0, 255, 0, 100, 1 },  { 50, 0 }, { 150, 0 }, { 150, 100 } };
//			mutation.index = shapes.length + 1;
//			mutation.pointsCount = pointsCount + mutation.newShape.length - 1;
//			return;
//		}else if(gCnt == 3){
//			int index = 1;
//			mutationType = "R";
//			mutation.oldShape = shapes[index];
//			mutation.newShape = null;
//			mutation.index = index;
//			mutation.pointsCount = pointsCount - (shapes[index].length - 1);
//			return;
//		}
		
		int opcode = randg.nextInt(100);
		int n = shapes.length;
		mutation.oldShape = null;
		mutation.newShape = null;
		mutation.index = n;
		mutation.shapesCount = n;
		mutation.pointsCount = pointsCount;
		if (opcode < 20) {	//add shape
			mutationType = "A";
			mutation.newShape = getRandomShape();
			mutation.index = n + 1;
			mutation.pointsCount = pointsCount + mutation.newShape.length - 1;
			return;
		} else if (opcode < 40) {	//remove shape
			mutationType = "R";
			if (n == 0) {
				mutationType = "0";
				return;
			}
			int index = randg.nextInt(n);
			mutation.oldShape = shapes[index];
			mutation.index = index;
			mutation.shapesCount = n - 1;
			mutation.pointsCount = pointsCount - (shapes[index].length - 1);
			return;
		} else {	//modify shape
			mutationType = "M";
			if (n == 0) {
				mutationType = "0";
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
			shapes = alterShapes(mutation.index, mutation.oldShape, mutation.newShape);
			pointsCount = mutation.pointsCount;
			addeDiff = newAddeDiff;
			diff = newDiff;
			return true;
		} else {
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
	
	public void rgbRegen(){
		for (int j = 0; j < height * width; j++) {
			pixels[j].rgbRegen();
		}
	}


}
