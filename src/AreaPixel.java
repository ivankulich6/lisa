import java.util.HashSet;
import java.util.Set;


public class AreaPixel {
	public int[] rgb;
	public Set<int[][]> shapes;
	
	AreaPixel(){
		rgb = new int[]{255, 255, 255};
		shapes = new HashSet<int[][]>();
	}
	
	public int getRgbInt(){
		return Utils.getRgbInt(rgb);
	}
	
	public void rgbRegen(int[][][] gShapes){
		int rgba[] = {255, 255, 255, 255};
		for(int j = 0; j < gShapes.length; j++){
			if(shapes.contains(gShapes[j])){
				convexCombine(gShapes[j][0], rgba, rgba);
			}
	    }
		Utils.colorNoAlpha(rgba, rgb);
	}

	public static void convexCombine(int srcRgba[], int dstRgba[], int outRgba[]){
		double outAlpha = (double)srcRgba[3] + (double)(dstRgba[3] * (255 - srcRgba[3]))/(double)255;
		//int outAlpha = srcRgba[3] + dstRgba[3] * (255 - srcRgba[3]);
		if(outAlpha == 0){
			for(int j = 0; j < 3; j++){
				outRgba[j] = 0;
			}
		}else{
			for(int j = 0; j < 3; j++){
				outRgba[j] = (int)Math.round(((double)srcRgba[j] * (double) srcRgba[3]  + (double)dstRgba[j] * (double)(255 - srcRgba[3]))/outAlpha);
			}

		}
		outRgba[3] = (int)Math.round(outAlpha);
		return;
	}

	
	


}
