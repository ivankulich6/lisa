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
		mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		mainFrame.setSize(width, height);
		drawing = new DrawingPanel();

		mainFrame.add(drawing);
		mainFrame.setVisible(true);
		mainFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				System.out.println("\nmainFrame is closing");
				gWindowClosing = true;
			}
		});
	}

	public static void main(String[] args) throws IOException {
		System.out.println("Hello RbTree");
		Main p = new Main();
		//p.run();
		//p.run2();
		p.run3();
		//p.runTests();
	}

	public void run2() throws IOException {
		new Utils.ImageViewer("testdata/puzzles");
	}

	public void run3() throws IOException {
		// Arguments: target, result dir, num. of res. files, first file
		// penalty, penalty quotient, saving interval.
		// Arguments num. of files and penalties are ignored, if some shapes
		// files already exist in result directory.
		Utils.polygonize("testdata/puzzles2/kittens001.jpg", "testdata/puzzles2/puzzle3", 5, 0.005, 0.1, 5000);
	}

	public void run() throws IOException {
		//BufferedImage shapesImg;
		BufferedImage img;

		Area area = new Area(true);
		boolean withReducer = false;
		area.setTarget("testdata/puzzles/castle001.jpg", withReducer);
		String outFile = "testdata/annealing/castle001_test.shapes";
		//area.setFromFile(outFile, withReducer);
		area.setPenaltyPointsCountParam(0.0001);
		area.temp = 1;
		area.diffMin = Double.MAX_VALUE;
		prepareGUI(area.width, area.height);
		int cnt = 0;
		img = Utils.drawArea(area);
		drawing.draw(img);

		long startTime = System.currentTimeMillis();
		int ret;
		boolean cont = true;
		while (cont) {
			cnt++;
			ret = area.doRandomChange(gWindowClosing, Area.DiffIncIfMethod.ITERATE);
			if (gWindowClosing) {
				cont = false;
			}
			if (ret > 0) {
				System.out.print(ret);
					if (cnt == 1 || ret == 3) {
					img = Utils.drawArea(area);
					drawing.draw(img);
				}
			}
			if (cnt % 100 == 0) {
				double diffAll = area.diffTest();
				System.out.println("");
				if(area.temp < 0){
					System.out.println("Diff=" + area.diff + ", cnt=" + cnt + ", cntAll="
							+ area.mutationsTotal + ", polygons=" + area.shapesCount);
					// Incrementally computed difference should fit whole area
					// recomputed difference diffAll.
				}else{
					System.out.println("DiffMin=" + area.diffMin + ", Diff=" + area.diff + ", cnt=" + cnt + ", cntAll="
							+ area.mutationsTotal + ", polygons=" + area.shapesCount + ", temp=" + area.temp);
				}
				assert Math.abs(area.diff - diffAll) < 1.e-12;
				// Diff: incremental diff, own merging of transparent colors.

			}
		}

		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		System.out.println("");
		System.out.println("elapsedTime = " + elapsedTime + " milliseconds");

		area.restoreShapesMin();

		Shape[] exShapes = area.extractShapes();
		assert exShapes.length == area.shapesCount;
		assert area.recalcPointsCount(exShapes) == area.pointsCount;

		if(area.temp < 0){
			System.out.println("Diff=" + area.diff + "cnt=" + cnt + ", cntAll=" + area.mutationsTotal + ", polygons="
					+ area.shapesCount );
		}else{
			System.out.println("DiffMin=" + area.diffMin + "cnt=" + cnt + ", cntAll=" + area.mutationsTotal + ", polygons="
					+ area.shapesCount + ", temp=" + area.temp);
		}
		area.shapesToFile(outFile);
		System.exit(0);
	}

	public void runTests() throws IOException {
		Test.diffIncIfMethodsCompareTest();
		Test.diffIncIfMethodsCompareTest2();
		Test.ccSpeedAndAccuracyTest();
	}

}
