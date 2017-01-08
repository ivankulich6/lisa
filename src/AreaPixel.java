import java.util.Map;
import java.util.TreeMap;

public class AreaPixel {
	public int[] rgb;
	public TreeMap<Integer, int[][]> shapes;

	AreaPixel() {
		rgb = new int[] { 255, 255, 255 };
		shapes = new TreeMap<Integer, int[][]>();
	}

	public int getRgbInt() {
		return Utils.getRgbInt(rgb);
	}

	// IDEA: This seems to be the bottleneck of the whole computation. If you
	// can write this faster, it may improve the speed quite significantly.

	public void rgbRegen() {
		// TODO: work with floats, only when you combine all the shapes, convert
		// to int.
		int rgba[] = { 255, 255, 255, 255 };
		for (Map.Entry<Integer, int[][]> entry : shapes.entrySet()) {
			int[][] shape = entry.getValue();
			convexCombine(shape[0], rgba, rgba);
		}
		Utils.colorNoAlpha(rgba, rgb);
	}

	public static void convexCombine(int srcRgba[], int dstRgba[], int outRgba[]) {
		// srcAlpha + dstAlha * (1 - srcAlpha)
		double outAlpha = (double) srcRgba[3] + (double) (dstRgba[3] * (255 - srcRgba[3])) / (double) 255;
		if (outAlpha == 0) {
			for (int j = 0; j < 3; j++) {
				outRgba[j] = 0;
			}
		} else {
			for (int j = 0; j < 3; j++) {
				// (srcRgba[j] * srcAlpha +
				// + dstRgba[j] * (1 - srcAlpha))/outAlpha
				outRgba[j] = (int) Math.round(
						((double) srcRgba[j] * (double) srcRgba[3] + (double) dstRgba[j] * (double) (255 - srcRgba[3]))
								/ outAlpha);
			}

		}
		outRgba[3] = (int) Math.round(outAlpha);
		return;
	}

}
