import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JFrame;

public class Main {

	JFrame mainFrame;
	DrawingPanel drawing;
	boolean gWindowClosing = false;

	private void prepareGUI(int width, int height) throws IOException {
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
		//p.runTests();
	}

	public void run() throws IOException {
		BufferedImage shapesImg;
		BufferedImage img;

		Area area = new Area(true);
		boolean withReducer = false;
		area.setTarget("women_small.jpg", withReducer);
		//area.setFromFile("testdata/women_small.shapes", withReducer);

		prepareGUI(area.width, area.height);
		int cnt = 0;
		int cntSuccess = 0;

		long startTime = System.currentTimeMillis();

		while (true) {
			cnt++;
			boolean success = area.doRandomChange(Area.DiffIncIfMethod.ITERATE);
			// System.out.print(area.mutationType);
			if (success) {
				System.out.print("+");
				cntSuccess++;
				if (cnt == 1 || cntSuccess % 10 == 0 || cnt % 100 == 0) {
					img = Utils.drawArea(area);
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

			}
//			if (cnt >= 10000) {
//				gWindowClosing = true;
//			}

			if (gWindowClosing) {
				long stopTime = System.currentTimeMillis();
				long elapsedTime = stopTime - startTime;
				System.out.println("");
				System.out.println("elapsedTime = " + elapsedTime + " milliseconds");
				Area.Shape[] exShapes = area.extractShapes();
				assert exShapes.length == area.shapesCount;
				assert area.recalcPointsCount(exShapes) == area.pointsCount;
				System.out.println("Diff=" + area.diff + ", cnt=" + cnt + ", polygons=" + area.shapesCount + ", temp="
						+ area.temp);
				shapesImg = Utils.drawShapes(area);
				drawing.draw(shapesImg);
				double diffAll = area.diffTest();
				double diff2 = area.diffTest(Utils.addeDiff(shapesImg, area.target));
				System.out.println("DiffAll=" + diffAll + " Diff2=" + diff2 + " AvgPolyPerPixel="
						+ area.getAvgNumOfShapesPerPixel());
				// Diff2: regenerated whole area diff, merging of transparent
				// colors by imported Graphics (fillPolygon)
				area.shapesToFile("testdata/women_small.shapes");
				System.exit(0);
				break;
			}
		}
	}

	public void runTests() throws IOException {
		Test.diffIncIfMethodsCompareTest();
		Test.ccSpeedAndAccuracyTest();
	}

}
