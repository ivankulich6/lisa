import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

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

}
