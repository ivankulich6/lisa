import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class Utils {

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

	public static void printArr(int[] arr) {
		String s = "";
		for (int elem : arr) {
			s += elem + ", ";
		}
		System.out.println(s);
	}

	// TODO fix for Shape class
	private static StringBuilder ShapeToSb(Area.Shape shape) {
		StringBuilder sb = new StringBuilder();
		String delimj, delimk;
		sb.append("[");
		delimj = "";
		for (int j = 0; j < shape.points.length; j++, delimj = ", ") {
			sb.append(delimj).append("[");
			delimk = "";
			for (int k = 0; k < shape.points[j].length; k++, delimk = ", ") {
				sb.append(delimk).append(shape.points[j][k]);
			}
			sb.append("]");
		}
		sb.append("]");
		return sb;
	}

	public static StringBuilder ShapesToSb(int width, int height, Area.Shape[] shapes) {
		StringBuilder sb = new StringBuilder();
		sb.append(width).append(", ").append(height).append("\n");
		sb.append(shapes.length).append("\n");
		for (int i = 0; i < shapes.length; i++) {
			sb.append(i).append(": ").append(ShapeToSb(shapes[i])).append("\n");
		}
		return sb;
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

}
