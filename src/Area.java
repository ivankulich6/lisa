import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class Area {
	public int width;
	public int height;
	public AreaPixel[] pixels;
	private ShapeRange shapeRange;
	BufferedImage tempBufferedImage;

	Area(int width, int height) {
		assert (width > 0 && height > 0);
		this.width = width;
		this.height = height;
		pixels = new AreaPixel[width * height];
		for (int j = 0; j < height * width; j++) {
			pixels[j] = new AreaPixel();
		}
		shapeRange = new ShapeRange();
		tempBufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	}

	private int[] getShapePixels(int[][] shape) {
		Graphics g = tempBufferedImage.getGraphics();
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
		return ((DataBufferInt) tempBufferedImage.getRaster().getDataBuffer()).getData();

	}

	public int getRgbInt(int x, int y) {
		return pixels[y * width + x].getRgbInt();
	}

	private int getShapeOrder(int[][] shape) {
		return shape[0][4];
	}

	public void addShape(int[][] shape) {
		final int[] tmppixels = getShapePixels(shape);
		shapeRange.initialize().add(shape, true);
		int order = getShapeOrder(shape);
		for (int jh = shapeRange.yMin; jh < shapeRange.yMax; jh++) {
			int jhw = jh * width;
			for (int jw = shapeRange.xMin; jw < shapeRange.xMax; jw++) {
				int ind = jhw + jw;
				if ((tmppixels[ind] & 0xff) == 0) {
					AreaPixel p = pixels[ind];
					p.shapes.put(order, shape);
					p.rgbRegen();
				}
			}
		}
	}

	public void removeShape(int[][] shape) {
		shapeRange.initialize().add(shape, true);
		int order = getShapeOrder(shape);
		for (int jh = shapeRange.yMin; jh < shapeRange.yMax; jh++) {
			int jhw = jh * width;
			for (int jw = shapeRange.xMin; jw < shapeRange.xMax; jw++) {
				AreaPixel p = pixels[jhw + jw];
				if (p.shapes.containsKey(order)) {
					p.shapes.remove(order);
					p.rgbRegen();
				}
			}
		}
	}

	public void replaceShape(int[][] oldShape, int[][] newShape) {
		if (oldShape == null && newShape == null) {
			return;
		}
		if (oldShape == null) {
			addShape(newShape);
		} else if (newShape == null) {
			removeShape(oldShape);
		} else {
			final int[] tmppixels = getShapePixels(newShape);
			shapeRange.initialize().add(oldShape).add(newShape, true);

			boolean pixelChange, pixelInOld, pixelInNew;
			int orderOldShape = getShapeOrder(oldShape);
			int orderNewShape = getShapeOrder(newShape);
			AreaPixel p;
			for (int jh = shapeRange.yMin; jh < shapeRange.yMax; jh++) {
				int jhw = jh * width;
				for (int jw = shapeRange.xMin; jw < shapeRange.xMax; jw++) {
					int ind = jhw + jw;
					p = pixels[ind];
					pixelInOld = p.shapes.containsKey(orderOldShape);
					pixelInNew = (tmppixels[ind] & 0xff) == 0;
					pixelChange = false;
					if (pixelInOld && pixelInNew && (orderOldShape == orderNewShape)) {
						p.shapes.put(orderNewShape, newShape);
						// IDEA: small performance tweak: if oldShape and
						// newShape have same color,
						// you don't have to regenerate color of this pixel.
						pixelChange = true;
					} else {
						if (pixelInOld) {
							p.shapes.remove(orderOldShape);
							pixelChange = true;
						}
						if (pixelInNew) {
							p.shapes.put(orderNewShape, newShape);
							pixelChange = true;
						}
					}
					if (pixelChange) {
						p.rgbRegen();
					}
				}
			}
		}
	}

	public double diff(int[][] targetRgb) {
		assert (pixels.length == targetRgb.length);
		double diff = 0;
		int[] res1;
		int[] res2;
		for (int j = 0; j < pixels.length; j++) {
			res1 = pixels[j].rgb;
			res2 = targetRgb[j];
			diff += Math.sqrt(
					Math.pow(res1[0] - res2[0], 2) + Math.pow(res1[1] - res2[1], 2) + Math.pow(res1[2] - res2[2], 2));
		}
		return diff / (width * height);
	}

	// temp. for testing
	double getAverageShapeRangeShare() {
		return shapeRange.averageArea / (width * height);
	}

}
