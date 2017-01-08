import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class Utils {
	
	public static int colorNoAlpha(int rgb, int alpha) {
		double a = (double) alpha / (double) 255;
		return (int) Math.round(a * rgb + (1 - a) * 255);
	}
	
	public static void colorNoAlpha(int rgba[], int rgb[]) {
		double a = (double) rgba[3] / (double) 255;
		for(int j = 0; j < 3; j++){
			rgb[j] = (int) Math.round(a * rgba[j] + (1 - a) * 255);
		}
		return;
	}


	public static void getPixel(int pixel, int[] res) {
		int c1 = (int) pixel & 0xff; // b
		int c2 = (int) pixel >> 8 & 0xff; // g
		int c3 = (int) pixel >> 16 & 0xff; // r
		int c0 = (int) pixel >> 24 & 0xff; // alpha
		// System.out.println("raw: " + c0 + " " + c1 + " " + c2 + " " + c3);
		res[0] = colorNoAlpha(c3, c0);
		res[1] = colorNoAlpha(c2, c0);
		res[2] = colorNoAlpha(c1, c0);
	}
	
	public static int[][] getPixelsRgb(BufferedImage img){
		int width = img.getWidth();
		int height = img.getHeight();
		int wh = width * height;
		int[][] pixelsRgb = new int[wh][];
		
		final int[] pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
		assert (pixels.length == wh);
		for(int j = 0; j < wh; j++){
			int[] res = new int[3];
			Utils.getPixel(pixels[j], res);
			pixelsRgb[j] = res;
		}
		return pixelsRgb;
	}

	public static int getRgbInt(int rgb[]){
		return ((rgb[0]&0x0ff)<<16) | ((rgb[1]&0x0ff)<<8) | (rgb[2]&0x0ff) | 255<<24;
	}

	private void printArr(int[] arr) {
		String s = "";
		for (int elem : arr) {
			s += elem + ", ";
		}
		System.out.println(s);
	}


}
