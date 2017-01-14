import java.util.Map;
import java.util.TreeMap;

public class AreaPixel {
	public int[] rgb;
	public TreeMap<Integer, int[][]> shapes;
	public int[] newRgb;


	AreaPixel() {
		rgb = new int[] { 255, 255, 255 };
		shapes = new TreeMap<Integer, int[][]>();
		newRgb = new int[3];
	}

	public int getRgbInt() {
		return Utils.getRgbInt(rgb);
	}

	// IDEA: This seems to be the bottleneck of the whole computation. If you
	// can write this faster, it may improve the speed quite significantly.

	public void rgbRegen(int[] rgb) {
		float rgba[] = { 255, 255, 255, 255 };
		for (Map.Entry<Integer, int[][]> entry : shapes.entrySet()) {
			int[][] shape = entry.getValue();
			convexCombine(shape[0], rgba, rgba);
		}
		Utils.colorNoAlpha(rgba, rgb);
	}

	public void rgbRegen() {
		rgbRegen(this.rgb);
	}
	
	public int rgbRegen(int[] targetRgb, int phase) {
	
		int diffOld = diff(targetRgb);
		rgbRegen();
		return diff(targetRgb) - diffOld;
		
//		if(phase == 0){
//			int diffOld = diff(targetRgb);
//			rgbRegen(newRgb);
//			return diff(newRgb, targetRgb) - diffOld;
//		}else if(phase == 1){
//			System.arraycopy(newRgb, 0, rgb, 0, 3);
//			return 0;
//		}
//		return 0;
	}

	public static void convexCombine(int srcRgba[], float dstRgba[], float outRgba[]) {
		float srcRgba3 = (float) srcRgba[3];
		float srcRgba3Compl = 255 - srcRgba3;
		float outAlpha = srcRgba3 + (dstRgba[3] * srcRgba3Compl) / 255;
		if (outAlpha == 0) {
			for (int j = 0; j < 3; j++) {
				outRgba[j] = 0;
			}
		} else {
			for (int j = 0; j < 3; j++) {
				// (srcRgba[j] * srcAlpha + dstRgba[j] * (1 - srcAlpha))/outAlpha
				outRgba[j] = ((float) srcRgba[j] * srcRgba3 + dstRgba[j] * srcRgba3Compl) / outAlpha;
			}

		}
		outRgba[3] = outAlpha;
		return;
	}
	
	public void rgbRegenInt() {
		// TODO: work with floats, only when you combine all the shapes, convert
		// to int.
		int rgba[] = { 255, 255, 255, 255 };
		for (Map.Entry<Integer, int[][]> entry : shapes.entrySet()) {
			int[][] shape = entry.getValue();
			convexCombineInt(shape[0], rgba, rgba);
		}
		Utils.colorNoAlpha(rgba, rgb);
	}

	public static void convexCombineInt(int srcRgba[], int dstRgba[], int outRgba[]) {
		double outAlpha = (double) srcRgba[3] + (double) (dstRgba[3] * (255 - srcRgba[3])) / (double) 255;
		if (outAlpha == 0) {
			for (int j = 0; j < 3; j++) {
				outRgba[j] = 0;
			}
		} else {
			for (int j = 0; j < 3; j++) {
				outRgba[j] = (int) Math.round(
						((double) srcRgba[j] * (double) srcRgba[3] + (double) dstRgba[j] * (double) (255 - srcRgba[3]))
								/ outAlpha);
			}

		}
		outRgba[3] = (int) Math.round(outAlpha);
		return;
	}
	
	public static int diff(int rgb1[], int rgb2[]) {
		return  Math.abs(rgb2[0] - rgb1[0]) + Math.abs(rgb2[1] - rgb1[1]) + Math.abs(rgb2[2] - rgb1[2]);
	}

	public int diff(int rgb[]) {
		return  diff(this.rgb, rgb);
	}


}
