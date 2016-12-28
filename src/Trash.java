import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

//import java.util.HashSet;
//
//
//public class Trash {
//	HashSet<Integer> all = new HashSet<Integer>();
//	for (int i = 0; i < 1000; i++) {
//		for (int j = 0; j < 1000; j++) {
//			all.add(img1.getRGB(i, j));
//		}
//	}
//	for (int argb : all) {
//		int b = (argb) & 0xFF;
//		int g = (argb >> 8) & 0xFF;
//		int r = (argb >> 16) & 0xFF;
//		int a = (argb >> 24) & 0xFF;
//		System.out.println(r + " " + g + " " + b + " " + a);
//	}
//	System.out.println(all);
//	if (4>3) {
//		return 0;
//	}
//}

//	public void saveToFileStub(BufferedImage img) {
//		try {
//			// retrieve image
//			File outputfile = new File("saved.jpg");
//			ImageIO.write(img, "jpg", outputfile);
//		} catch (IOException e) {
//			System.out.println("Hello " + e.getMessage());
//		}
//	}

//public BufferedImage sampleVectorImage() {
//	BufferedImage img = new BufferedImage(width, height,
//			BufferedImage.TYPE_INT_ARGB);
//	Graphics g = img.getGraphics();
//	g.setColor(new Color(255, 20, 29, 50));
//	int xpoints[] = { 25, 145, 25, 145, 25 };
//	int ypoints[] = { 25, 25, 145, 145, 25 };
//	int npoints = 5;
//	g.fillPolygon(xpoints, ypoints, npoints);
//	g.setColor(new Color(32, 64, 255, 50));
//	int xpoints2[] = { 60, 60, 100, 100 };
//	int ypoints2[] = { 0, 200, 200, 0 };
//	g.fillPolygon(xpoints2, ypoints2, 4);
//	return img;
//}
