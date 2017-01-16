import java.util.Map;
import java.util.TreeMap;

public class AreaPixel {
	public int[] rgb;
	public TreeMap<Integer, int[][]> shapes;
	public int[] newRgb;
	public int[] targetRgb;

	AreaPixel() {
		rgb = new int[] { 255, 255, 255 };
		shapes = new TreeMap<Integer, int[][]>();
		newRgb = new int[3];
		targetRgb = new int[3];
	}

	public int getRgbInt() {
		return Utils.getRgbInt(rgb);
	}

	public int prepareAddShape(int[][] shape){
		int diffOld = diff();
		int order = getShapeOrder(shape);
		shapes.put(order, shape);
		rgbRegen(newRgb);
		shapes.remove(order);
		return diff(newRgb, targetRgb) - diffOld;
	}
	
	public int prepareRemoveShape(int[][] shape){
		int diffOld = diff();
		int order = getShapeOrder(shape);
		shapes.remove(order);
		rgbRegen(newRgb);
		shapes.put(order, shape);
		return diff(newRgb, targetRgb) - diffOld;
	}

	public int prepareReplaceShape(int[][] oldShape, int[][] newShape, int intype, boolean sameRgba){
		int orderOld = getShapeOrder(oldShape);
		int orderNew = getShapeOrder(newShape);
		if (intype == 3 && orderOld == orderNew && sameRgba) {
			System.arraycopy(rgb, 0, newRgb, 0, 3);
			return  0;
		}else{
			int diffOld = diff();
			if(intype == 1){
				shapes.remove(orderOld);
			}else if(intype == 2){
				shapes.put(orderNew, newShape);
			}else if(intype == 3){
				if(orderNew != orderOld){
					shapes.remove(orderOld);
				}
				shapes.put(orderNew, newShape);
			}
			rgbRegen(newRgb);
			if(intype == 1){
				shapes.put(orderOld, oldShape);
			}else if(intype == 2){
				shapes.remove(orderNew);
			}else if(intype == 3){
				if(orderNew != orderOld){
					shapes.remove(orderNew);
				}
				shapes.put(orderOld, oldShape);
			}
			return diff(newRgb, targetRgb) - diffOld;
		}
	}
	
	public void useNewRgb(){
		System.arraycopy(newRgb, 0, rgb, 0, 3);
	}
	
	public void addShape(int[][] shape){
		shapes.put(getShapeOrder(shape), shape);
		useNewRgb();
	}
	
	public void removeShape(int[][] shape){
		shapes.remove(getShapeOrder(shape));
		useNewRgb();
	}
	
	public void replaceShape(int[][] oldShape, int[][] newShape, int intype){
		int orderOld = getShapeOrder(oldShape);
		int orderNew = getShapeOrder(newShape);
		if(intype == 1){
			shapes.remove(orderOld);
		}else if(intype == 2){
			shapes.put(orderNew, newShape);
		}else if(intype == 3){
			if(orderNew != orderOld){
				shapes.remove(orderOld);
			}
			shapes.put(orderNew, newShape);
		}
		useNewRgb();
	}
	
	public static int getShapeOrder(int[][] shape) {
		return shape[0][4];
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
				outRgba[j] = ((float) srcRgba[j] * srcRgba3 + dstRgba[j] * srcRgba3Compl) / outAlpha;
			}

		}
		outRgba[3] = outAlpha;
		return;
	}
	
	
	public static int diff(int rgb1[], int rgb2[]) {
		return  Math.abs(rgb2[0] - rgb1[0]) + Math.abs(rgb2[1] - rgb1[1]) + Math.abs(rgb2[2] - rgb1[2]);
	}

	public int diff(int rgb[]) {
		return  diff(this.rgb, rgb);
	}
	
	public int diff() {
		return  diff(rgb, targetRgb);
	}

}
