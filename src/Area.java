import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Iterator;

public class Area {
	public int width;
	public int height;
	public AreaPixel[] pixels;
	private int[] xRange;
	private int[] yRange;
	
	Area(int width, int height){
		assert(width > 0 && height > 0);
		this.width = width;
		this.height = height;
		pixels = new AreaPixel[width * height];
		for(int j = 0; j < height * width; j++){
			pixels[j] = new AreaPixel();
		}
		xRange = new int[2];
		yRange = new int[2];
	}
	
	public int getRgbInt(int x, int y){
		return pixels[y * width + x].getRgbInt();
	}
	
	public void addShape(int[][] shape){
		BufferedImage tmp = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_ARGB);
		Graphics g = tmp.getGraphics();
		g.setColor(new Color(255, 255, 255));
		g.fillRect(0, 0, width, height);
		g.setColor(new Color(0, 0, 0, 255));
		int npoints = shape.length - 1;
		int xpoints[] = new int[npoints];
		int ypoints[] = new int[npoints];
		for (int i = 0; i < npoints; i++) {
			// 0-th element of shape is color
			xpoints[i] = shape[i + 1][0];
			ypoints[i] = shape[i + 1][1];
		}
		g.fillPolygon(xpoints, ypoints, npoints);
		final int[] tmppixels = ((DataBufferInt) tmp.getRaster().getDataBuffer())
				.getData();
		
		Utils.findShapeRanges(shape, xRange, yRange);
		
		for(int jh = yRange[0]; jh < yRange[1]; jh++){
			int jhw = jh * width;
			for(int jw = xRange[0]; jw <xRange[1]; jw++){
				int ind = jhw + jw;
				if((tmppixels[ind] & 0xff) == 0){
					AreaPixel p = pixels[ind];
					p.shapes.add(shape);
					int rgba[] = {0, 0, 0, 255};
			        Iterator<int[][]> itr = p.shapes.iterator();
			        while(itr.hasNext()){
			        	convexCombine(itr.next()[0], rgba, rgba);
			        }
					Utils.colorNoAlpha(rgba, p.rgb);
				}
			}
		}
	}
	
	public void removeShape(int[][] shape){
		Utils.findShapeRanges(shape, xRange, yRange);
		for(int jh = yRange[0]; jh < yRange[1]; jh++){
			int jhw = jh * width;
			for(int jw = xRange[0]; jw <xRange[1]; jw++){
				AreaPixel p = pixels[jhw + jw];
				if(p.shapes.contains(shape)){
					p.shapes.remove(shape);
					int rgba[] = {0, 0, 0, 255};
			        Iterator<int[][]> itr = p.shapes.iterator();
			        while(itr.hasNext()){
			        	convexCombine(itr.next()[0], rgba, rgba);
			        }
					Utils.colorNoAlpha(rgba, p.rgb);
				}
			}
		}
		
	}
	
	public void removeAddShape(int[][][] raShapes, boolean revert){
		if(revert){
			if(raShapes[1] != null){
				removeShape(raShapes[1]);
			}
			if(raShapes[0] != null){
				addShape(raShapes[0]);
			}
		}else{
			if(raShapes[0] != null){
				removeShape(raShapes[0]);
			}
			if(raShapes[1] != null){
				addShape(raShapes[1]);
			}
		}
	}

	public static void convexCombine(int srcRgba[], int dstRgba[], int outRgba[]){
		int outAlpha = srcRgba[3] + dstRgba[3] * (255 - srcRgba[3]);
		if(outAlpha == 0){
			for(int j = 0; j < 3; j++){
				outRgba[j] = 0;
			}
		}else{
			for(int j = 0; j < 3; j++){
				outRgba[j] = (int)Math.round(((double)srcRgba[j] * (double) srcRgba[3]  + (double)dstRgba[j] * (double)(255 - srcRgba[3]))/(double) outAlpha);
			}

		}
		outRgba[3] = outAlpha;
		return;
	}
	
	public double diff(int[][] targetRgb) {
		assert (pixels.length == targetRgb.length);
		double diff = 0;
		int[] res1;
		int[] res2;
		for (int j = 0; j < pixels.length; j++) {
			res1 = pixels[j].rgb;
			res2 = targetRgb[j];
			diff += Math.sqrt(Math.pow(res1[0] - res2[0], 2)
					+ Math.pow(res1[1] - res2[1], 2)
					+ Math.pow(res1[2] - res2[2], 2));
		}

		return diff / (width * height);
	}




}
