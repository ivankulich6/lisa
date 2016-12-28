import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
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
	
	private void printArr(int[] arr){
		String s = "";
		for (int elem : arr){
			s+= elem + ", ";
		}
		System.out.println(s);
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

	private double penalty(int[][][] shapes) {
		double res = 0;
		for (int[][] shape : shapes) {
			res += shape.length - 1;
		}
		return res / 10000.0;
	}

	private double penaltyShape(int[][][] shapes, BufferedImage target) {
		BufferedImage tmp = drawShapes(shapes);
		return diff(target, tmp) + penalty(shapes);
	}

	private int[][] getRandomShape() {
		int[] color = new int[4];
		for (int i = 0; i < 3; i++) {
			color[i] = randg.nextInt(256);
		}
		color[3] = 1;
		int[][] res = new int[4][];
		res[0] = color;
		for (int i = 0; i < 3; i++) {
			res[i + 1] = new int[2];
			res[i + 1][0] = randg.nextInt(width);
			res[i + 1][1] = randg.nextInt(height);
		}
		return res;
	}

	private int[][][] alterShapes(int[][][] shapes) {
		int opcode = randg.nextInt(100);
		int n = shapes.length;
		if (opcode < 10) {
			int[][][] newShapes = new int[n + 1][][];
			System.arraycopy(shapes, 0, newShapes, 0, n);
			newShapes[n] = getRandomShape();
			return newShapes;
		} else if (opcode < 20) {
			if (n == 0) {
				return shapes;
			}
			int index = randg.nextInt(n);
			int[][][] newShapes = new int[n - 1][][];
			System.arraycopy(shapes, 0, newShapes, 0, index);
			System.arraycopy(shapes, index + 1, newShapes, index,
					(n - 1 - index));
			return newShapes;
		} else {
			if (n == 0) {
				return shapes;
			}
			int[][][] newShapes = new int[n][][];			
			System.arraycopy(shapes, 0, newShapes, 0, n);
			int index = randg.nextInt(n);
			int inner = randg.nextInt(shapes[index].length);
			int inninner = randg.nextInt(shapes[index][inner].length);
			int move = randg.nextInt(2) * 2 - 1;
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
			return newShapes;
		}
	}

	private void prepareGUI() throws IOException {
		mainFrame = new JFrame("Java Swing Examples");
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setSize(width, height);
		drawing = new DrawingPanel();
		mainFrame.add(drawing);
		mainFrame.setVisible(true);
	}

	public static void main(String[] args) throws IOException {
		System.out.println("Hello");
		Main p = new Main();
		p.run();
	}

	public BufferedImage readImage(String filename) throws IOException {
		BufferedImage _img = ImageIO.read(new File(filename));
		BufferedImage img = new BufferedImage(_img.getWidth(),
				_img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		img.getGraphics().drawImage(_img, 0, 0, null);
		return img;
	}

	public void run() throws IOException {
		BufferedImage target = readImage("women_small.jpg");
		width = target.getWidth();
		height = target.getHeight();
		prepareGUI();
		BufferedImage img = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_ARGB);
		int[][][] shapes = new int[0][][];
		double d = penaltyShape(shapes, target);
		int cnt = 0;
		double temp = 1;
		while (true) {
			temp = Math.max(temp, Math.pow(10, -10));
			cnt++;
			int[][][] newShapes = alterShapes(shapes);
			double newDiff = penaltyShape(newShapes, target);
			if (randg.nextDouble() < Math.exp(- (newDiff - d) / temp)) {
				System.out.println("Gotcha");
				temp *= 0.5;
				shapes = newShapes;
				d = newDiff;
				img = drawShapes(shapes);
				drawing.draw(img);
			} else {
				temp *= 1.002;
			}
			if (cnt % 100 == 0) {
			  System.out.println("Diff: " + d + " cnt: " + cnt + " polygons: " + shapes.length + " temp: " + temp);
			}  

		}
	}

	public int colorNoAlpha(int rgb, int alpha) {
		double a = (double) alpha / (double) 255;
		return (int) Math.round(a * rgb + (1 - a) * 255);
	}

	public void getPixel(int pixel, int[] res) {
		int c1 = (int) pixel & 0xff; // b
		int c2 = (int) pixel >> 8 & 0xff; // g
		int c3 = (int) pixel >> 16 & 0xff; // r
		int c0 = (int) pixel >> 24 & 0xff; // alpha
//		System.out.println("raw: " + c0 + " " + c1 + " " + c2 + " " + c3);
		res[0] = colorNoAlpha(c1, c0);
		res[1] = colorNoAlpha(c2, c0);
		res[2] = colorNoAlpha(c3, c0);
//		System.out.println("alpha: " + res[0] + " " + res[1] + " " + res[2]);
	}
	
	private void experiment() throws IOException {
		width = 500;
		height = 500;
		int x = 0;
		int[][][] shapes1 = new int[][][] {{{255,255,0,255}, {0, 0}, {0, 100}, {100, 100}, {100, 0}}};
		int[][][] shapes2 = new int[][][] {{{0,0,0,255}, {x, x}, {x, x+ 100}, {100+x, x+100}, {100+x, x}}};
		prepareGUI();
	    BufferedImage target = drawShapes(shapes1);
	    drawing.draw(target);
	    System.out.println(penaltyShape(shapes2, target));
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
			getPixel(pixels1[pixel1], res1);
			getPixel(pixels2[pixel2], res2);
//			if (pixel1 == -1) {
//				printArr(res1);
//				printArr(res2);
//				break;
//			}
			diff += Math.sqrt(Math.pow(res1[0] - res2[0], 2)
					+ Math.pow(res1[1] - res2[1], 2)
					+ Math.pow(res1[2] - res2[2], 2));
		}

		return diff / (width * height);
	}

}