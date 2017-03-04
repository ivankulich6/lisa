import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class Utils {

	public static BufferedImage readImage(String filename) throws IOException {
		BufferedImage _img = ImageIO.read(new File(filename));
		BufferedImage img = new BufferedImage(_img.getWidth(), _img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		img.getGraphics().drawImage(_img, 0, 0, null);
		return img;
	}

	public static int colorNoAlpha(int rgb, int alpha) {
		double a = (double) alpha / (double) 255;
		return (int) Math.round(a * rgb + (1 - a) * 255);
	}

	public static void getPixel(int pixel, int[] res) {
		int c1 = (int) pixel & 0xff; // b
		int c2 = (int) pixel >> 8 & 0xff; // g
		int c3 = (int) pixel >> 16 & 0xff; // r
		int c0 = (int) pixel >> 24 & 0xff; // alpha
		res[0] = colorNoAlpha(c3, c0);
		res[1] = colorNoAlpha(c2, c0);
		res[2] = colorNoAlpha(c1, c0);
	}

	public static int[][] getPixelsRgb(BufferedImage img) {
		int width = img.getWidth();
		int height = img.getHeight();
		int wh = width * height;
		int[][] pixelsRgb = new int[wh][];

		final int[] pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
		assert (pixels.length == wh);
		for (int j = 0; j < wh; j++) {
			int[] res = new int[3];
			Utils.getPixel(pixels[j], res);
			pixelsRgb[j] = res;
		}
		return pixelsRgb;
	}

	public static int getRgbInt(int rgb[]) {
		return ((rgb[0] & 0x0ff) << 16) | ((rgb[1] & 0x0ff) << 8) | (rgb[2] & 0x0ff) | 255 << 24;
	}

	public static double addeDiff(BufferedImage img1, BufferedImage img2) {
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

	private static void _drawShapes(Shape[] shapes, BufferedImage img) {
		Graphics g = img.getGraphics();
		for (Shape shape : shapes) {
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

	public static BufferedImage drawShapes(Area area) {
		BufferedImage tmp = new BufferedImage(area.width, area.height, BufferedImage.TYPE_INT_ARGB);
		Graphics g = tmp.getGraphics();
		g.setColor(new Color(255, 255, 255));
		g.fillRect(0, 0, area.width, area.height);
		_drawShapes(area.shapes, tmp);
		return tmp;
	}

	public static BufferedImage drawArea(Area area) {
		BufferedImage tmp = new BufferedImage(area.width, area.height, BufferedImage.TYPE_INT_ARGB);
		for (int jh = 0; jh < area.height; jh++) {
			for (int jw = 0; jw < area.width; jw++) {
				tmp.setRGB(jw, jh, area.getRgbInt(jw, jh));
			}
		}
		return tmp;
	}

	public static void printArr(int[] arr) {
		String s = "";
		for (int elem : arr) {
			s += elem + ", ";
		}
		System.out.println(s);
	}

	private static StringBuilder ShapeToSb(Shape shape) {
		StringBuilder sb = new StringBuilder("[");
		sb.append(arrayToSb(shape.rgba));
		for (int j = 0; j < shape.points.length; j++) {
			sb.append(", ").append(arrayToSb(shape.points[j]));
		}
		sb.append("]");
		return sb;
	}

	public static StringBuilder ShapesToSb(Area area) {
		StringBuilder sb = new StringBuilder();
		sb.append("TargetPath: ").append(area.targetPath).append("\n");
		sb.append("Width: ").append(area.width).append("\n");
		sb.append("Height: ").append(area.height).append("\n");
		sb.append("PenaltyPointsCountParam: ").append(area.penaltyPointsCountParam).append("\n");
		sb.append("PointsCount: ").append(area.pointsCount).append("\n");
		sb.append("DistancePerPixel: ").append(getMinMaxColorDistanceToTargetPerPixel(area)).append("\n");
		sb.append("MutationsTotal: ").append(area.mutationsTotal).append("\n");
		sb.append("MutationsAccepted: ").append(area.mutationsAccepted).append("\n");
		sb.append("MutationsAdd: ").append(area.mutationsAdd).append("\n");
		sb.append("MutationsRemove: ").append(area.mutationsRemove).append("\n");
		sb.append("MutationsReplace: ").append(area.mutationsReplace).append("\n");
		sb.append("ShapesCount: ").append(area.shapes.length).append("\n");
		for (int i = 0; i < area.shapes.length; i++) {
			sb.append("Shape_").append(i).append(": ").append(ShapeToSb(area.shapes[i])).append("\n");
		}
		return sb;
	}

	public static double getMinMaxColorDistanceToTargetPerPixel(Area area) {
		return area.penaltyRgb(addeDiff(drawShapes(area), area.target));
	}

	public static StringBuilder textToSb(String text) {
		return (new StringBuilder()).append(text);
	}

	public static StringBuilder arrayToSb(int values[], int count) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		String delim = "";
		for (int j = 0; j < count; j++) {
			sb.append(delim).append(values[j]);
			delim = ", ";
		}
		sb.append("]");
		return sb;
	}

	public static StringBuilder arrayToSb(int[] values) {
		return arrayToSb(values, values.length);
	}

	public static StringBuilder arrayToSb(double[] values, int count) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		String delim = "";
		for (int j = 0; j < count; j++) {
			sb.append(delim).append(values[j]);
			delim = ", ";
		}
		sb.append("]");
		return sb;
	}

	public static StringBuilder arrayToSb(double[] values) {
		return arrayToSb(values, values.length);
	}

	public static StringBuilder arrayToSb(float[] values, int count) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		String delim = "";
		for (int j = 0; j < count; j++) {
			sb.append(delim).append(values[j]);
			delim = ", ";
		}
		sb.append("]");
		return sb;
	}

	public static StringBuilder arrayToSb(float[] values) {
		return arrayToSb(values, values.length);
	}

	public static StringBuilder arrayToSb(String[] values, int count) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		String delim = "";
		for (int j = 0; j < count; j++) {
			sb.append(delim).append('"').append(values[j]).append('"');
			delim = ", ";
		}
		sb.append("]");
		return sb;
	}

	public static StringBuilder arrayToSb(String[] values) {
		return arrayToSb(values, values.length);
	}

	public static String[] decodeKeyVal(String code) {
		String[] keyVal = null;
		int pos = code.indexOf(':');
		if (pos > 0) {
			keyVal = new String[2];
			keyVal[0] = code.substring(0, pos).trim();
			keyVal[1] = code.substring(pos + 1).trim();
		}
		return keyVal;
	}

	public static String[] decodeStringArray(String code) {
		String[] sArray = null;
		ArrayList<String> values = null;
		if (code.charAt(0) == '[') {
			values = new ArrayList<String>();
			int level = 0;
			int posPrev = 1;
			for (int pos = 1; pos < code.length(); pos++) {
				char c = code.charAt(pos);
				if (c == '[') {
					++level;
				} else if (c == ']') {
					if (level == 0) {
						values.add(code.substring(posPrev, pos).trim());
						break;
					} else {
						--level;
					}
				} else if (c == ',' && level == 0) {
					values.add(code.substring(posPrev, pos).trim());
					posPrev = pos + 1;
				}
			}
		}
		if (values != null && values.size() > 0) {
			sArray = new String[values.size()];
			values.toArray(sArray);
		}
		return sArray;
	}

	public static double[][] decodeShapeData(String code) {
		double shapeData[][] = null;
		String sData[] = decodeStringArray(code);
		if (sData != null) {
			shapeData = new double[sData.length][];
			for (int j = 0; j < sData.length; j++) {
				String sDataj[] = decodeStringArray(sData[j]);
				if (sDataj != null) {
					shapeData[j] = new double[sDataj.length];
					for (int k = 0; k < sDataj.length; k++) {
						shapeData[j][k] = Double.parseDouble(sDataj[k]);
					}
				} else {
					return null;
				}
			}
		}
		return shapeData;
	}

	public static boolean doLog = false;
	public static String logPath = null;

	public static void setLogPath(String logPath) {
		Utils.logPath = logPath;
	}

	public static void setNewLog(String logPath) {
		Utils.doLog = true;
		Utils.setLogPath(logPath);
		Utils.logClean();
	}

	public static String getLogPath() {
		return logPath == null ? "testdata/log.txt" : logPath;
	}

	public static void log(String text) {
		if (doLog) {
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(getLogPath(), true));
				writer.append(text);
				writer.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public static void log2(String text) {
		System.out.println(text);
		log(text + "\n");
	}

	public static void log(StringBuilder sb) {
		log(sb.toString());
	}

	public static void logNewline() {
		log("\n");
	}

	public static void logClean() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(getLogPath()));
			writer.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void polygonize(String targetPath, String shapesDirPath, int filesCount, double pointsPenalty,
			double pointsPenaltyQuotient, int minMutations) throws IOException {
		new Polygonizer(targetPath, shapesDirPath, filesCount, pointsPenalty, pointsPenaltyQuotient, minMutations)
				.polygonize();
	}

	public static class Polygonizer {
		String shapesDirPath;
		String targetPath;
		String fileNameBase;
		int filesCount;
		List<String> filePaths;
		double penaltyPointsCountParams[];
		double penaltyPointsCountParam;
		JFrame mainFrame;
		DrawingPanel drawing;
		boolean windowClosing = false;
		BufferedImage img;
		int minMutations;
		double pointsPenalty;
		double pointsPenaltyQuotient;
		int nRepeat;

		Polygonizer(String targetPath, String shapesDirPath, int filesCount, double pointsPenalty,
				double pointsPenaltyQuotient, int minMutations) throws IOException {
			this.targetPath = targetPath;
			this.shapesDirPath = shapesDirPath;
			this.filesCount = filesCount;
			this.pointsPenalty = pointsPenalty;
			this.pointsPenaltyQuotient = pointsPenaltyQuotient;
			this.minMutations = minMutations;

			File directory = new File(shapesDirPath);
			if (directory.exists()) {
				assert directory.isDirectory();
				filePaths = getShapesFilePaths(shapesDirPath);
				if (filePaths.size() > 0) {
					this.filesCount = filePaths.size();
				} else {
					generateFileSpecs();
				}
			} else {
				generateFileSpecs();
			}
			prepareGUI();
		}

		private void generateFileSpecs() {
			filePaths = new ArrayList<String>(filesCount);
			penaltyPointsCountParams = new double[filesCount];
			File fTargetPath = new File(targetPath);
			assert fTargetPath.exists() && !fTargetPath.isDirectory();
			fileNameBase = fTargetPath.getName();
			int pos = fileNameBase.lastIndexOf(".");
			if (pos > 0) {
				fileNameBase = fileNameBase.substring(0, pos);
			}
			for (int j = 0; j < filesCount; j++) {
				String fileName = fileNameBase + String.format("_%03d", j + 1) + ".shapes";
				filePaths.add(Paths.get(shapesDirPath, fileName).toString());
			}
			double penalty = pointsPenalty;
			for (int j = 0; j < filesCount; j++) {
				penaltyPointsCountParams[j] = penalty;
				penalty *= pointsPenaltyQuotient;
			}
		}

		private void prepareGUI() throws IOException {
			mainFrame = new JFrame("Polygonizer");
			mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			mainFrame.setSize(10, 10);
			drawing = new DrawingPanel();

			mainFrame.add(drawing);
			mainFrame.setVisible(false);
			mainFrame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent we) {
					System.out.println("\nmainFrame is closing");
					windowClosing = true;
				}
			});
		}

		public void polygonize() throws IOException {
			double successQuotients[] = new double[filesCount];
			Arrays.fill(successQuotients, 1);
			double successQuotientStop = 1;
			nRepeat = 0;
			while (successQuotientStop > 0) {
				boolean done = true;
				boolean changeStop = true;
				nRepeat++;
				for (int j = 0; j < filesCount; j++) {
					if (successQuotients[j] >= (double) 0.0005) {
						done = false;
						successQuotients[j] = polygonize1(j, minMutations, successQuotientStop);
						if (successQuotients[j] > successQuotientStop) {
							changeStop = false;
						}
					}
				}
				if (done) {
					break;
				}
				if (changeStop) {
					successQuotientStop *= 0.5;
				}
			}
			System.exit(0);
		}

		public double polygonize1(int index, int cntMin, double successQuotientStop) throws IOException {
			double successQuotient = 1;
			String resultPath = filePaths.get(index);
			Area area = new Area(true);
			File file = new File(resultPath);
			assert !file.isDirectory();
			if (file.exists()) {
				area.setFromFile(resultPath, false);
			} else {
				if (index > 0) { // use index - 1 shapes if possible
					String resultPathPrev = filePaths.get(index - 1);
					File filePrev = new File(resultPathPrev);
					if (filePrev.exists()) {
						area.setFromFile(resultPathPrev, false);
					}
				}
				if (area.targetPath == null) {
					area.setTarget(targetPath, false);
				}
				area.setPenaltyPointsCountParam(penaltyPointsCountParams[index]);
			}
			mainFrame.setSize(area.width, area.height);
			mainFrame.setVisible(true);
			int cnt = 0;
			int cntSuccess = 0;
			img = Utils.drawArea(area);
			drawing.draw(img);
			long startTime = System.currentTimeMillis();
			boolean taskDone = false;
			while (true) {
				cnt++;
				int success = area.doRandomChange(Area.DiffIncIfMethod.ITERATE);
				if (success > 0) {
					System.out.print("+");
					cntSuccess++;
					if (cntSuccess % 10 == 0 || cnt % 100 == 0) {
						img = Utils.drawArea(area);
						drawing.draw(img);
					}
				}
				if (cnt % 100 == 0) {
					System.out.println("");
					System.out.println("File=" + (index + 1) + ", Repeat=" + nRepeat + ", Diff=" + area.diff + ", cnt="
							+ cnt + ", cntAll=" + area.mutationsTotal + ", polygons=" + area.shapesCount);

				}
				successQuotient = (double) cntSuccess / (double) cnt;
				if (cnt >= cntMin && successQuotient <= successQuotient) {
					taskDone = true;
				}
				if (taskDone || windowClosing) {
					long stopTime = System.currentTimeMillis();
					long elapsedTime = stopTime - startTime;
					System.out.println("");
					System.out.println("elapsedTime = " + elapsedTime + " milliseconds");
					Shape[] exShapes = area.extractShapes();
					assert exShapes.length == area.shapesCount;
					assert area.recalcPointsCount(exShapes) == area.pointsCount;
					System.out.println("File=" + (index + 1) + ", Repeat=" + nRepeat + ", Diff=" + area.diff + ", cnt="
							+ cnt + ", cntAll=" + area.mutationsTotal + ", polygons=" + area.shapesCount);
					double diffAll = area.diffTest();
					System.out.println("DiffAll=" + diffAll + ", Distance="
							+ getMinMaxColorDistanceToTargetPerPixel(area) + ", AvgPolyPerPixel="
							+ area.getAvgNumOfShapesPerPixel());
					area.shapesToFile(resultPath);
					if (windowClosing) {
						System.exit(0);
						break;
					}
					break;
				}
			}
			return successQuotient;
		}
	}

	public static List<String> getShapesFilePaths(String shapesDirPath) {
		List<String> filePaths = new ArrayList<>();
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get(shapesDirPath), "*.shapes")) {
			for (Path p : ds) {
				filePaths.add(p.toString());
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		Collections.sort(filePaths);
		return filePaths;
	}

	public static List<String> getShapesDirPaths(String parentDirPath) {
		List<String> dirPaths = new ArrayList<>();
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get(parentDirPath))) {
			for (Path p : ds) {
				File dir = p.toFile();
				if (dir.isDirectory()) {
					if (getShapesFilePaths(dir.toString()).size() > 0) {
						dirPaths.add(p.toString());
					}
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		Collections.sort(dirPaths);
		return dirPaths;
	}

	public static class ImageViewer {
		String topDirPath;
		JFrame mainFrame;
		DrawingPanel drawing;
		JButton btnStart;
		JButton btnNextImage;
		JButton btnPrevImage;
		JComboBox<Object> cmbShapesDirs;
		String shapesDirPath;
		List<String> filePaths;
		int fileIndex;
		Point mainFrameLocation;
		int hExt;
		int frameStateID;
		Area area;

		ImageViewer(String topDirPath) throws IOException {
			this.topDirPath = topDirPath;
			mainFrameLocation = null;
			frameStateID = -1;
			shapesDirPath = null;
			actionUp();
		}

		void setFileIndexNext() {
			int nFiles = filePaths.size() + 1;
			if (nFiles == 0) {
				fileIndex = -1;
			} else {
				fileIndex = Math.min(fileIndex + 1, nFiles - 1);
			}
		}

		void setFileIndexPrev() {
			int nFiles = filePaths.size() + 1;
			if (nFiles == 0) {
				fileIndex = -1;
			} else {
				fileIndex = Math.max(fileIndex - 1, 0);
			}
		}

		void showImage() throws IOException {
			int nFiles = filePaths.size() + 1;
			btnStart.setVisible(fileIndex == -1);
			cmbShapesDirs.setVisible(fileIndex == -1);
			if (fileIndex < 0) {
			} else if (fileIndex < nFiles) {
				BufferedImage img;
				area = new Area(true);
				if (fileIndex < nFiles - 1) {
					area.setFromFile(filePaths.get(fileIndex), false, false);
					img = drawShapes(area);
				} else {
					area.setFromFile(filePaths.get(fileIndex - 1), false, false);
					img = readImage(area.targetPath);
				}
				mainFrame.setSize(area.width, area.height + hExt);
				mainFrame.setTitle("What's this? (press -> for next)");
				drawing.draw(img);
			}
		}

		void actionFirstImage() throws IOException {
			if (frameStateID == 0) {
				shapesDirPath = (String) cmbShapesDirs.getSelectedItem();
				filePaths = getShapesFilePaths(shapesDirPath);
				fileIndex = 0;
				frameStateID = 1;
				showImage();
			}
		}

		void actionNext() throws IOException {
			if (frameStateID == 0) {
				actionFirstImage();
			} else if (frameStateID == 1) {
				setFileIndexNext();
				showImage();
			}
		}

		void actionPrev() throws IOException {
			if (frameStateID == 0) {
				actionExit();
			} else if (frameStateID == 1) {
				setFileIndexPrev();
				showImage();
			}
		}

		void actionExit() {
			mainFrame.dispose();
			System.exit(0);
		}

		void actionUp() throws IOException {
			if (frameStateID == 0) {
				actionExit();
				return;
			}
			frameStateID = 0;
			fileIndex = -1;
			int width = 350;
			int height = 200;
			btnStart = new JButton("Click to start!");
			btnStart.addActionListener(btnStartActionListener);
			cmbShapesDirs = new JComboBox<Object>(getShapesDirPaths(topDirPath).toArray());
			cmbShapesDirs.setFocusable(false);
			if (shapesDirPath != null) {
				cmbShapesDirs.setSelectedItem(shapesDirPath);
			}
			hExt = 0;
			if (mainFrame != null) {
				mainFrameLocation = mainFrame.getLocation();
				mainFrame.dispose();
				hExt = 28;
			}
			mainFrame = new JFrame("Polygonized pictures viewer");
			mainFrame.setSize(width, height + hExt);
			mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			drawing = new DrawingPanel();
			mainFrame.add(drawing);
			drawing.add(new JLabel("Select folder:"));
			drawing.add(cmbShapesDirs);
			char[] aspace = new char[100];
			Arrays.fill(aspace, ' ');
			String space = new String(aspace);
			for (int j = 0; j < 3; j++) {
				drawing.add(new JLabel(space));
			}
			drawing.add(btnStart);
			if (mainFrameLocation != null) {
				mainFrame.setLocation(mainFrameLocation);
			}
			mainFrame.setVisible(true);
			mainFrame.setFocusable(true);
			mainFrame.addKeyListener(new KeyListener() {
				@Override
				public void keyTyped(KeyEvent evt) {
				}

				@Override
				public void keyPressed(KeyEvent evt) {
					try {
						action(evt);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				@Override
				public void keyReleased(KeyEvent evt) {
				}

				void action(KeyEvent evt) throws IOException {
					if (isIn(evt.getKeyCode(), KeyEvent.VK_RIGHT, KeyEvent.VK_KP_RIGHT, KeyEvent.VK_ENTER)) {
						actionNext();
					} else if (isIn(evt.getKeyCode(), KeyEvent.VK_LEFT, KeyEvent.VK_KP_LEFT, KeyEvent.VK_BACK_SPACE)) {
						actionPrev();
					} else if (isIn(evt.getKeyCode(), KeyEvent.VK_UP, KeyEvent.VK_KP_UP, KeyEvent.VK_ESCAPE)) {
						actionUp();
					} else if (isIn(evt.getKeyCode(), KeyEvent.VK_I)) {
						actionInfo();
					}

				}
			});
		}

		void actionInfo() throws IOException {
			if (frameStateID == 1) {
				if (fileIndex == filePaths.size()) {
					System.out.println("(i) Target file=" + area.targetPath + ", Width=" + area.width + ", Height="
							+ area.height);
				} else {
					area.setTarget(area.targetPath, false);
					System.out.println("(i) File=" + filePaths.get(fileIndex).toString() + ", polygons="
							+ area.shapesCount + ", points=" + area.pointsCount + ",\nPenaltyPointsCountParam="
							+ area.penaltyPointsCountParam + ", DistancePerPixel="	+ getMinMaxColorDistanceToTargetPerPixel(area));
				}
			}
		}

		boolean isIn(int val, int... args) {
			for (int arg : args) {
				if (val == arg) {
					return true;
				}
			}
			return false;
		}

		ActionListener btnStartActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				try {
					actionFirstImage();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

	}

}
