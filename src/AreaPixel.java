import java.util.LinkedHashSet;
import java.util.Set;


public class AreaPixel {
	public int[] rgb;
	public Set<int[][]> shapes;
	
	AreaPixel(){
		rgb = new int[]{0, 0, 0};
		shapes = new LinkedHashSet<int[][]>();
	}
	
	public int getRgbInt(){
		return Utils.getRgbInt(rgb);
	}
	
	


}
