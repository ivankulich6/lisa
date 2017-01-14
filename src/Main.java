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
	boolean gWindowClosing = false;



	/*
	 * shapes: [[[r, g, b, a, o], [p0x, p0y], [p1x, p2x], ...], ...] o = unique,
	 * immutable order number, satisfied cond. (if shape1 index < shape2 index,
	 * then shape1 o < shape2 o)
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

	private BufferedImage drawShapes(int[][][] shapes) {
		BufferedImage tmp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics g = tmp.getGraphics();
		g.setColor(new Color(255, 255, 255));
		g.fillRect(0, 0, width, height);
		_drawShapes(shapes, tmp);
		return tmp;
	}

	private BufferedImage drawArea(Area area) {
		BufferedImage tmp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int jh = 0; jh < height; jh++) {
			for (int jw = 0; jw < width; jw++) {
				tmp.setRGB(jw, jh, area.getRgbInt(jw, jh));
			}
		}
		return tmp;
	}

	public int getRgbaInt(int rgba[]) {
		return ((rgba[0] & 0x0ff) << 16) | ((rgba[1] & 0x0ff) << 8) | (rgba[2] & 0x0ff) | ((rgba[3] & 0x0ff) << 24);
	}




	private void prepareGUI() throws IOException {
		mainFrame = new JFrame("Java Swing Examples");
		// mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		mainFrame.setSize(width, height);
		drawing = new DrawingPanel();
		mainFrame.add(drawing);
		mainFrame.setVisible(true);
		mainFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				System.out.println("mainFrame is closing");
				gWindowClosing = true;
			}
		});
	}

	public static void main(String[] args) throws IOException {
		System.out.println("Hello");
		Main p = new Main();
		p.run();
	}

	public BufferedImage readImage(String filename) throws IOException {
		BufferedImage _img = ImageIO.read(new File(filename));
		BufferedImage img = new BufferedImage(_img.getWidth(), _img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		img.getGraphics().drawImage(_img, 0, 0, null);
		return img;
	}

	public void run() throws IOException {
		BufferedImage target = readImage("women_small.jpg");
		BufferedImage img;
		width = target.getWidth();
		height = target.getHeight();
		Area area = new Area(width, height);
		area.setTargetRgb(Utils.getPixelsRgb(target));

		prepareGUI();
		int cnt = 0;
		int cntSuccess = 0;

		while (true) {
			cnt++;
			
			boolean success = area.doRandomChange();
			if (success) {
				System.out.print("+");
				img = drawArea(area);
//				img = drawShapes(area.shapes);
				cntSuccess++;
				drawing.draw(img);
			}
			if (cnt % 100 == 0) {
				System.out.println("");
				System.out.println("Diff=" + area.diff + ", dt=" + area.diffTest() + ", cnt=" + cnt + ", polygons=" + area.shapes.length + ", temp=" + area.temp
				);
			}
			if(gWindowClosing){
				area.saveShapes("testdata/shapes01.txt");
				System.exit(0);
				break;
				
			}
		}
	}


	public double diff(BufferedImage img1, BufferedImage img2) {
		final int[] pixels1 = ((DataBufferInt) img1.getRaster().getDataBuffer()).getData();
		final int[] pixels2 = ((DataBufferInt) img2.getRaster().getDataBuffer()).getData();
		assert (pixels1.length == pixels2.length);
		double diff = 0;
		int[] res1 = new int[3];
		int[] res2 = new int[3];
		for (int pixel1 = 0, pixel2 = 0; pixel1 < pixels1.length; pixel1 += 1, pixel2 += 1) {
			Utils.getPixel(pixels1[pixel1], res1);
			Utils.getPixel(pixels2[pixel2], res2);
			diff += Math.sqrt(
					Math.pow(res1[0] - res2[0], 2) + Math.pow(res1[1] - res2[1], 2) + Math.pow(res1[2] - res2[2], 2));
		}

		return diff / (width * height);
	}

}