import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

public class Main {

	int width;
	int height;

	JFrame mainFrame;
	DrawingPanel drawing;

	Random randg = new Random();

	/*
	 * shapes: [[[r, g, b, a], [p0x, p0y], [p1x, p2x], ...], ...]
	 */
	
	int[][][] gshapes;
	
	private void _drawShapes(int[][][] shapes, BufferedImage img) {
		Graphics g = img.getGraphics();
		for (int shape[][] : shapes) {
			int[] color = shape[0];
			g.setColor(new Color(color[0], color[1], color[2], color[3]));
			int npoints = shape.length - 1;
			int xpoints[] = new int[npoints];
			int ypoints[] = new int[npoints];
			for (int i = 0; i < npoints; i++) {
				// 0-th element of shape is color
				xpoints[i] = shape[i + 1][0];
				ypoints[i] = shape[i + 1][1];
			}
			g.fillPolygon(xpoints, ypoints, npoints);
		}
	}
	
	private BufferedImage drawShapes(int[][][] shapes) {
		BufferedImage tmp = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_ARGB);
		Graphics g = tmp.getGraphics();
		g.setColor(new Color(255, 255, 255));
		g.fillRect(0, 0, width, height);
		_drawShapes(shapes, tmp);
		return tmp;
	}

	private BufferedImage drawArea(Area area) {
		BufferedImage tmp = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_ARGB);
		for(int jh = 0; jh < height; jh++){
			for(int jw = 0; jw < width; jw++){
				tmp.setRGB(jw, jh, area.getRgbInt(jw, jh));
			}
		}
		return tmp;
	}
	
	public int getRgbaInt(int rgba[]){
		return ((rgba[0]&0x0ff)<<16)|((rgba[1]&0x0ff)<<8)|(rgba[2]&0x0ff)|((rgba[3]&0x0ff)<<24);
	}


	private void printArr(int[] arr) {
		String s = "";
		for (int elem : arr) {
			s += elem + ", ";
		}
		System.out.println(s);
	}

	private String printShapes(int[][][] shapes) {
		String s = "[";
		for (int i = 0; i < shapes.length; i++) {
			s += "[";
			// for (int j = 0; j < shapes[i].length; j++) {
			for (int j = 0; j < 1; j++) {
				s += "[";
				for (int k = 0; k < shapes[i][j].length; k++) {
					s += shapes[i][j][k] + ", ";
				}
				s += "], ";
			}
			s += "], ";
		}
		s += "]";
		return s;
	}

	private StringBuilder ShapeToSb(int[][] shape) {
		StringBuilder sb = new StringBuilder();
		String delimj, delimk;
		sb.append("[");
		delimj = "";
		for (int j = 0; j < shape.length; j++, delimj = ", ") {
			sb.append(delimj).append("[");
			delimk = "";
			for (int k = 0; k < shape[j].length; k++, delimk = ", ") {
				sb.append(delimk).append(shape[j][k]);
			}
			sb.append("]");
		}
		sb.append("]");
		return sb;
	}

	private StringBuilder ShapesToSb(int[][][] shapes) {
		StringBuilder sb = new StringBuilder();
		sb.append(shapes.length).append("\n");
		for (int i = 0; i < shapes.length; i++) {
			sb.append(i).append(": ").append(ShapeToSb(shapes[i])).append("\n");
		}
		return sb;
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

	private double penalty(int[][][] shapes) {
		double res = 0;
		for (int[][] shape : shapes) {
			res += shape.length - 1;
		}
		return res * res / 1000000.0;
	}

	private double penaltyShape(int[][][] shapes, BufferedImage target) {
		BufferedImage tmp = drawShapes(shapes);
		return diff(target, tmp) + penalty(shapes);
	}
	
	private double penaltyShape(Area area, int[][] targetRgb, int[][][] shapes) {
		return area.diff(targetRgb) + penalty(shapes);
	}

	private int[][] getRandomShape() {
		int[] color = new int[4];
		for (int i = 0; i < 3; i++) {
			color[i] = randg.nextInt(256);
		}
		//color[3] = 1;
		color[3] = randg.nextInt(255) + 1;
		int[][] res = new int[4][];
		res[0] = color;
		for (int i = 0; i < 3; i++) {
			res[i + 1] = new int[2];
			res[i + 1][0] = randg.nextInt(width);
			res[i + 1][1] = randg.nextInt(height);
		}
		return res;
	}
	
	private int[][][] addRandomShape(int[][][] shapes){
		int n = shapes.length;
		int[][][] newShapes = new int[n + 1][][];
		System.arraycopy(shapes, 0, newShapes, 0, n);
		newShapes[n] = getRandomShape();
		return newShapes;
	}
	
	private int[][][] alterShapes(int[][][] shapes, int[][][] oldNewShapes){
		
		int opcode = randg.nextInt(100);
		int n = shapes.length;
		oldNewShapes[0] = null;
		oldNewShapes[1] = null;
		
		if (opcode < 20) {
			int[][][] newShapes = new int[n + 1][][];
			System.arraycopy(shapes, 0, newShapes, 0, n);
			newShapes[n] = getRandomShape();
			oldNewShapes[1] = newShapes[n];
			return newShapes;
		} else if (opcode < 40) {
			if (n == 0) {
				return shapes;
			}
			int index = randg.nextInt(n);
			int[][][] newShapes = new int[n - 1][][];
			System.arraycopy(shapes, 0, newShapes, 0, index);
			System.arraycopy(shapes, index + 1, newShapes, index,
					(n - 1 - index));
			oldNewShapes[0] = shapes[index];
			return newShapes;
		} else {
			if (n == 0) {
				return shapes;
			}
			int[][][] newShapes = new int[n][][];
			System.arraycopy(shapes, 0, newShapes, 0, n);
			int index = randg.nextInt(n);
			newShapes[index] = copy(shapes[index]);
			int inner = randg.nextInt(shapes[index].length);
			int inninner = randg.nextInt(newShapes[index][inner].length);
			int move = randg.nextInt(20) - 10;
			int[] tmp = newShapes[index][inner];
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
			oldNewShapes[0] = shapes[index];
			oldNewShapes[1] = newShapes[index];
			return newShapes;
		}
	}

	private void saveShapes(String sFile, int[][][] shapes) {
		BufferedWriter writer = null;
		try {
			File outFile = new File(sFile);
			StringBuilder sb = ShapesToSb(shapes);
		System.out.println(outFile.getCanonicalPath());
			writer = new BufferedWriter(new FileWriter(outFile ));
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

	private void prepareGUI() throws IOException {
		mainFrame = new JFrame("Java Swing Examples");
		//mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		mainFrame.setSize(width, height);
		drawing = new DrawingPanel();
		mainFrame.add(drawing);
		mainFrame.setVisible(true);
		mainFrame.addWindowListener(new WindowAdapter() {
			  public void windowClosing(WindowEvent we) {
			System.out.println("mainFrame is closing");
			if(gshapes != null){
				saveShapes("testdata/shapes01.txt" , gshapes);
			}
		    System.exit(0);
		  }}
		);
	}
	


	public static void main(String[] args) throws IOException {
		System.out.println("Hello");
		Main p = new Main();
		p.run();
		//p.experiment();
		//p.experiment2();
		//p.experiment3();
	}

	public BufferedImage readImage(String filename) throws IOException {
		BufferedImage _img = ImageIO.read(new File(filename));
		BufferedImage img = new BufferedImage(_img.getWidth(),
				_img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		img.getGraphics().drawImage(_img, 0, 0, null);
		return img;
	}
	

	public void run() throws IOException {
		//boolean useArea = false;
		boolean useArea = true;
		BufferedImage target = readImage("women_micro.jpg");
		width = target.getWidth();
		height = target.getHeight();
		int[][] targetRgb = Utils.getPixelsRgb(target);
		int[][][] oldNewShapes = new int[2][][];
		prepareGUI();
		BufferedImage img;

		//test targetRgb
		//BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		/*
		for(int jh = 0; jh < height; jh++){
			for(int jw = 0; jw < width; jw++){
				img.setRGB(jw, jh, Utils.getRgbInt(targetRgb[jw + jh*width]));
			}
		}
		drawing.draw(img);
		*/
		
		int[][][] shapes = new int[0][][];
		gshapes = shapes; 
		Area area = new Area(width, height);
		double d, newDiff;
		if(useArea){
			d = penaltyShape(area, targetRgb, shapes);
		}else{
			d = penaltyShape(shapes, target);
		}
		int cnt = 0;
	
		double temp = 1;
		while (true) {
			temp = Math.max(temp, Math.pow(10, -10));
			cnt++;
			int[][][] newShapes = alterShapes(shapes, oldNewShapes);
			if(useArea){
				area.replaceShape(oldNewShapes[0], oldNewShapes[1], newShapes);
				newDiff = penaltyShape(area, targetRgb, newShapes);
			}else{
				newDiff = penaltyShape(newShapes, target);
			}
			// newDiff < d -> vzdy true
			// newDiff - d = temp -> akceptujem so sancou e^-1
			// newDiff - d = 2temp -> akceptujem so sancou e^-2
			// if (randg.nextDouble() < Math.exp(- (newDiff - d) / temp)) {
			if (newDiff < d) {
				System.out.print("+");
				if (newDiff - d < -0.00001) {
					temp *= 0.5;
				}
				shapes = newShapes;
				gshapes = shapes; 
				d = newDiff;
				if(useArea){
					img = drawArea(area);
				}else{
					img = drawShapes(shapes);
				}
				drawing.draw(img);
			} else {
				if(useArea){
					area.replaceShape(oldNewShapes[1], oldNewShapes[0], shapes);
				}
				temp *= 1.002;
			}
			if (cnt % 100 == 0) {
				System.out.println("");
				System.out.println("Diff: " + d + " cnt: " + cnt
						+ " polygons: " + shapes.length + " temp: " + temp + " avgRange: " + area.getAverageShapeRangeShare());
				System.out.println(printShapes(shapes));
			}
		}
	}
	

	private void experiment() throws IOException {
		width = 100;
		height = 100;
		int x = 0;
		int[][][] shapes1 = new int[][][] { { { 255, 0, 0, 255 }, { 0, 0 },
				{ 0, 100 }, { 100, 100 }, { 100, 0 } } };
		int[][][] shapes2 = new int[][][] { { { 180, 0, 0, 255 }, { 0, 0 },
				{ 70, 70 }, { 0, 100 } } };
		prepareGUI();
		BufferedImage target = drawShapes(shapes1);
		drawing.draw(target);
		System.out.println(penaltyShape(shapes2, target));
	}
	
	private void experiment2() throws IOException {
		width = 150;
		height = 100;
		prepareGUI();
		Area area = new Area(width, height);
		int[][][] shapes = new int[][][] {
				{ { 255, 0, 0, 100 }, { 10, 25 },	{ 10, 50 }, { 50, 50 }, { 50, 25 } },
				{ { 0, 255, 0, 200 }, { 0, 0 },	{ 0, 100 }, { 100, 100 }},
				{ { 0, 0, 255, 50 }, { 0, 0 },	{ 100, 0 }, { 100, 100 }}
		};
		area.addShape(shapes[0], shapes);
		area.addShape(shapes[1], shapes);
		area.addShape(shapes[2], shapes);
		drawing.draw(drawArea(area));
		drawing.draw(drawShapes(shapes));
		
		/*
		area.addShape(shapes[1], shapes);
		drawing.draw(drawArea(area));
		area.addShape(shapes[2], shapes);
		drawing.draw(drawArea(area));
		
		area.removeShape(shapes[0], shapes);
		drawing.draw(drawArea(area)); 
		*/
		return;
	}
	
	private void experiment3() throws IOException {
		width = 150;
		height = 100;
		prepareGUI();
		Area area = new Area(width, height);
		int nShapes = 3;
		int[][][] shapes = new int[0][][];
		for(int j = 0; j < nShapes; j++){
			shapes = addRandomShape(shapes);
			area.addShape(shapes[shapes.length - 1], shapes);
			drawing.draw(drawArea(area));
		}
		for(int j = nShapes - 1; j >= 0; j--){
			area.removeShape(shapes[j], shapes);
			drawing.draw(drawArea(area));
		}
		return;
	}

	public double diff(BufferedImage img1, BufferedImage img2) {
		final int[] pixels1 = ((DataBufferInt) img1.getRaster().getDataBuffer())
				.getData();
		final int[] pixels2 = ((DataBufferInt) img2.getRaster().getDataBuffer())
				.getData();
		assert (pixels1.length == pixels2.length);
		double diff = 0;
		int[] res1 = new int[3];
		int[] res2 = new int[3];
		for (int pixel1 = 0, pixel2 = 0; pixel1 < pixels1.length; pixel1 += 1, pixel2 += 1) {
			Utils.getPixel(pixels1[pixel1], res1);
			Utils.getPixel(pixels2[pixel2], res2);
			diff += Math.sqrt(Math.pow(res1[0] - res2[0], 2)
					+ Math.pow(res1[1] - res2[1], 2)
					+ Math.pow(res1[2] - res2[2], 2));
		}

		return diff / (width * height);
	}

}