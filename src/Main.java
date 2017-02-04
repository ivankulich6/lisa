import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

public class Main {

	int width;
	int height;

	JFrame mainFrame;
	DrawingPanel drawing;
	boolean gWindowClosing = false;

	private void _drawShapes(Area.Shape[] shapes, BufferedImage img) {
		Graphics g = img.getGraphics();
		for (Area.Shape shape : shapes) {
			int[] color = shape.getColor();
			g.setColor(new Color(color[0], color[1], color[2], color[3]));
			int npoints = shape.points.length;
			int xpoints[] = new int[npoints];
			int ypoints[] = new int[npoints];
			for (int i = 0; i < npoints; i++) {
				xpoints[i] = shape.points[i][0];
				ypoints[i] = shape.points[i][1];
			}
			g.fillPolygon(xpoints, ypoints, npoints);
		}
	}

	private BufferedImage drawShapes(Area.Shape[] shapes) {
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
		System.out.println("Hello RbTree");
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
		assert !AreaPixel.RBTREE_WITHOUT_REDUCER || AreaPixel.DIFF_INC_IF_ITERATE;
		Area.logClean();
		long startTime = System.currentTimeMillis();

		BufferedImage target = readImage("women_small.jpg");
		BufferedImage img;
		BufferedImage shapesImg;
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
			// System.out.print(area.mutationType);
			if (success) {
				System.out.print("+");
				cntSuccess++;
				if (cntSuccess % 1 == 0) {
					img = drawArea(area);
					drawing.draw(img);
				}
			}
			if (cnt % 100 == 0) {
				double diffAll = area.diffTest();
				System.out.println("");
				System.out.println("Diff=" + area.diff + " DiffAll=" + diffAll + ", cnt=" + cnt + ", polygons="
						+ area.shapesCount + ", temp=" + area.temp);
				// incrementally computed difference should fit whole area
				// recomputed difference diffAll
				// assert Math.abs(area.diff - diffAll) < 1.e-1;
				// Diff: incremental diff, own merging of transparent colors,

				if (cnt >= 5000) {
					gWindowClosing = true;
				}
			}
			if (gWindowClosing) {
				long stopTime = System.currentTimeMillis();
				long elapsedTime = stopTime - startTime;
				System.out.println("elapsedTime = " + elapsedTime + " milliseconds");
				Area.Shape[] exShapes = area.extractShapes();
				assert exShapes.length == area.shapesCount;
				assert area.recalcPointsCount(exShapes) == area.pointsCount;
				if (!Area.useShapesArray) {
					area.shapes = exShapes;
				}
				System.out.println("");
				System.out.println("Diff=" + area.diff + ", cnt=" + cnt + ", polygons=" + area.shapesCount + ", temp="
						+ area.temp);

				shapesImg = drawShapes(area.shapes);
				// drawing.draw(shapesImg);
				double diffAll = area.diffTest();
				double diff2 = area.diffTest(addeDiff(shapesImg, target));
				System.out.println("DiffAll=" + diffAll + " Diff2=" + diff2);
				// Diff2: regenerated whole area diff, merging of transparent
				// colors by imported Graphics (fillPolygon)
				area.saveShapes("testdata/shapes01.txt");
				System.exit(0);
				break;
			}
		}
	}

	public double addeDiff(BufferedImage img1, BufferedImage img2) {
		final int[] pixels1 = ((DataBufferInt) img1.getRaster().getDataBuffer()).getData();
		final int[] pixels2 = ((DataBufferInt) img2.getRaster().getDataBuffer()).getData();
		assert (pixels1.length == pixels2.length);
		double diff = 0;
		int[] rgb1 = new int[3];
		int[] rgb2 = new int[3];
		for (int pixel1 = 0, pixel2 = 0; pixel1 < pixels1.length; pixel1 += 1, pixel2 += 1) {
			Utils.getPixel(pixels1[pixel1], rgb1);
			Utils.getPixel(pixels2[pixel2], rgb2);
			diff += AreaPixel.diff(rgb1, rgb2);
		}

		return diff;
	}

}
