
public class ShapeRange {
	
	public int xMin;
	public int xMax;
	public int yMin;
	public int yMax;
	
	//temporary for testing
	public int nRunCount;
	public double averageArea;
	
	public ShapeRange(){
		initialize();
		nRunCount = 0;
		averageArea = 0;
	}

	public ShapeRange initialize(){
		xMin = yMin = Integer.MAX_VALUE;
		xMax = yMax = Integer.MIN_VALUE;
		return this;
	}
	
	public ShapeRange add(int[][] shape){
		if(shape != null){
			for(int j = 1; j < shape.length; j++){
				int xy[] = shape[j];
				xMin = Math.min(xMin, xy[0]);
				xMax = Math.max(xMax, xy[0]);
				yMin = Math.min(yMin, xy[1]);
				yMax = Math.max(yMax, xy[1]);
			}
		}
		return this;
	}

	//temporary for testing
	public ShapeRange add(int[][] shape, boolean addToAverage){
		add(shape);
		if(shape != null && addToAverage){
			averageArea = ((double)1/(double)(nRunCount + 1)) * (nRunCount * averageArea + Math.max(0, (xMax - xMin)) * Math.max(0, (yMax - yMin)));
			++nRunCount;
		}
		return this;
	}
	

}
