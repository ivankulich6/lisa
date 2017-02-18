public class AreaPixel {

	public static final double BKGCOLOR[] = { 1, 1, 1, 1 };

	public RbTree<Integer, Shape> shapes;
	public double[] rgb;
	public double[] newRgb;
	public double[] targetRgb;

	AreaPixel(boolean withReducer) {

		initShapes(withReducer);
		rgb = new double[] { 1, 1, 1, 1 };
		newRgb = new double[4];
		targetRgb = new double[3];
	}

	public void initShapes(boolean withReducer) {

		Reducer<Shape> reducer = new Reducer<Shape>() {
			@Override
			public Shape reduce(Shape reduced, Shape value) {
				Shape res = new Shape();
				res.rgba = new double[4];
				convexCombine(value.rgba, reduced.rgba, res.rgba);
				return res;
			}
		};

		if (withReducer) {
			shapes = new RbTree<Integer, Shape>(null, reducer);
		} else {
			shapes = new RbTree<Integer, Shape>();
		}

	}

	public double diffIncIfAdded_ITERATE(Shape shape) {
		double diffOld = diff();
		System.arraycopy(BKGCOLOR, 0, newRgb, 0, 4);
		RbTree<Integer, Shape>.EntryIterator it = shapes.new EntryIterator(shapes.firstEntry());
		while (it.hasNext()) {
			convexCombine(it.next().getValue().rgba, newRgb, newRgb);
		}
		convexCombine(shape.rgba, newRgb, newRgb);
		return diff(newRgb, targetRgb) - diffOld;
	}

	public double diffIncIfAdded_RFT(Shape shape) {
		double diffOld = diff();
		// Shape cShape = shapes.getReduced();
		Shape cShape = shapes.getReduced(0, Integer.MAX_VALUE);
		if (cShape != null) {
			convexCombine(shape.rgba, cShape.rgba, newRgb);
			convexCombine(newRgb, BKGCOLOR, newRgb);
		} else {
			convexCombine(shape.rgba, BKGCOLOR, newRgb);
		}
		return diff(newRgb, targetRgb) - diffOld;
	}

	public double diffIncIfAdded_PUTREM(Shape shape) {
		double diffOld = diff();
		int order = shape.order;
		shapes.put(order, shape);
		Shape cShape = shapes.getReduced();
		convexCombine(cShape.rgba, BKGCOLOR, newRgb);
		shapes.remove(order);
		return diff(newRgb, targetRgb) - diffOld;
	}

	public double diffIncIfRemoved_ITERATE(Shape shape) {
		double diffOld = diff();
		int order = shape.order;
		Shape savedShape;
		System.arraycopy(BKGCOLOR, 0, newRgb, 0, 4);
		RbTree<Integer, Shape>.EntryIterator it = shapes.new EntryIterator(shapes.firstEntry());
		while (it.hasNext()) {
			savedShape = it.next().getValue();
			if (savedShape.order != order) {
				convexCombine(savedShape.rgba, newRgb, newRgb);
			}
		}
		return diff(newRgb, targetRgb) - diffOld;
	}

	public double diffIncIfRemoved_RFT(Shape shape) {
		double diffOld = diff();
		int order = shape.order;
		Shape cShape1 = shapes.getReduced(-1, order);
		Shape cShape2 = shapes.getReduced(order + 1, Integer.MAX_VALUE);
		if (cShape1 == null && cShape2 == null) {
			setBkgColor(newRgb);
		} else if (cShape1 == null) {
			convexCombine(cShape2.rgba, BKGCOLOR, newRgb);
		} else if (cShape2 == null) {
			convexCombine(cShape1.rgba, BKGCOLOR, newRgb);
		} else {
			convexCombine(cShape2.rgba, cShape1.rgba, newRgb);
			convexCombine(newRgb, BKGCOLOR, newRgb);
		}
		return diff(newRgb, targetRgb) - diffOld;

	}

	public double diffIncIfRemoved_PUTREM(Shape shape) {
		double diffOld = diff();
		int order = shape.order;
		shapes.remove(order);
		Shape cShape = shapes.getReduced();
		if (cShape != null) {
			convexCombine(cShape.rgba, BKGCOLOR, newRgb);
		} else {
			setBkgColor(newRgb);
		}
		shapes.put(order, shape);
		return diff(newRgb, targetRgb) - diffOld;
	}

	public double diffIncIfReplaced_ITERATE(Shape oldShape, Shape newShape, int intype, boolean sameRgba) {
		int order = newShape.order;
		assert order == oldShape.order;
		if (intype == 3 && sameRgba) {
			System.arraycopy(rgb, 0, newRgb, 0, 3);
			return 0;
		} else {
			double diffOld = diff();
			Shape savedShape = null;
			System.arraycopy(BKGCOLOR, 0, newRgb, 0, 4);
			if (intype == 1) {
				RbTree<Integer, Shape>.EntryIterator it = shapes.new EntryIterator(shapes.firstEntry());
				while (it.hasNext()) {
					savedShape = it.next().getValue();
					if (savedShape.order != order) {
						convexCombine(savedShape.rgba, newRgb, newRgb);
					}
				}
			} else if (intype == 2) {
				boolean add = true;
				RbTree<Integer, Shape>.EntryIterator it = shapes.new EntryIterator(shapes.firstEntry());
				while (it.hasNext()) {
					savedShape = it.next().getValue();
					if (savedShape.order > order) {
						if (add) {
							convexCombine(newShape.rgba, newRgb, newRgb);
							add = false;
						}
						convexCombine(savedShape.rgba, newRgb, newRgb);
					} else {
						convexCombine(savedShape.rgba, newRgb, newRgb);
					}
				}

				if (add) {
					convexCombine(newShape.rgba, newRgb, newRgb);
				}
			} else if (intype == 3) {
				RbTree<Integer, Shape>.EntryIterator it = shapes.new EntryIterator(shapes.firstEntry());
				while (it.hasNext()) {
					savedShape = it.next().getValue();
					if (savedShape.order != order) {
						convexCombine(savedShape.rgba, newRgb, newRgb);
					} else {
						convexCombine(newShape.rgba, newRgb, newRgb);
					}
				}
			}
			return diff(newRgb, targetRgb) - diffOld;
		}
	}

	public double diffIncIfReplaced_RFT(Shape oldShape, Shape newShape, int intype, boolean sameRgba) {
		int order = newShape.order;
		assert order == oldShape.order;
		if (intype == 3 && sameRgba) {
			System.arraycopy(rgb, 0, newRgb, 0, 3);
			return 0;
		} else if (intype == 1) {
			return diffIncIfRemoved_RFT(newShape);
		} else {
			double diffOld = diff();
			Shape cShape1 = shapes.getReduced(-1, order);
			Shape cShape2 = shapes.getReduced(order + 1, Integer.MAX_VALUE);
			if (cShape1 == null && cShape2 == null) {
				convexCombine(newShape.rgba, BKGCOLOR, newRgb);
			} else if (cShape1 == null) {
				convexCombine(cShape2.rgba, newShape.rgba, newRgb);
				convexCombine(newRgb, BKGCOLOR, newRgb);
			} else if (cShape2 == null) {
				convexCombine(newShape.rgba, cShape1.rgba, newRgb);
				convexCombine(newRgb, BKGCOLOR, newRgb);
			} else {
				convexCombine(newShape.rgba, cShape1.rgba, newRgb);
				convexCombine(cShape2.rgba, newRgb, newRgb);
				convexCombine(newRgb, BKGCOLOR, newRgb);
			}
			return diff(newRgb, targetRgb) - diffOld;

		}
	}

	public double diffIncIfReplaced_PUTREM(Shape oldShape, Shape newShape, int intype, boolean sameRgba) {
		int order = newShape.order;
		assert order == oldShape.order;
		if (intype == 3 && sameRgba) {
			System.arraycopy(rgb, 0, newRgb, 0, 3);
			return 0;
		} else {
			double diffOld = diff();
			if (intype == 1) {
				shapes.remove(order);
			} else {
				shapes.put(order, newShape);
			}

			Shape cShape = shapes.getReduced();
			if (cShape != null) {
				convexCombine(cShape.rgba, BKGCOLOR, newRgb);
			} else {
				setBkgColor(newRgb);
			}
			if (intype == 2) {
				shapes.remove(order);
			} else {
				shapes.put(order, oldShape);
			}
			return diff(newRgb, targetRgb) - diffOld;
		}
	}

	public void addShape(Shape shape) {
		shapes.put(shape.order, shape);
		useNewRgb();
	}

	public void removeShape(Shape shape) {
		shapes.remove(shape.order);
		useNewRgb();
	}

	public void replaceShape(Shape oldShape, Shape newShape, int intype) {
		int orderOld = oldShape.order;
		int orderNew = newShape.order;
		if (intype == 1) {
			shapes.remove(orderOld);
		} else if (intype == 2) {
			shapes.put(orderNew, newShape);
		} else if (intype == 3) {
			if (orderNew != orderOld) {
				shapes.remove(orderOld);
			}
			shapes.put(orderNew, newShape);
		}
		useNewRgb();
	}

	public void rgbRegen(double[] rgba) {
		System.arraycopy(BKGCOLOR, 0, rgba, 0, 4);
		RbTree<Integer, Shape>.EntryIterator it = shapes.new EntryIterator(shapes.firstEntry());
		while (it.hasNext()) {
			convexCombine(it.next().getValue().rgba, rgba, rgba);
		}
	}

	public void rgbRegen() {
		rgbRegen(this.rgb);
	}

	private void useNewRgb() {
		System.arraycopy(newRgb, 0, rgb, 0, 3);
	}

	public int getRgb1Int(double rgb1) {
		return (int) Math.round(255 * rgb1);
	}

	public int getRgbInt() {
		int rgbm[] = { getRgb1Int(this.rgb[0]), getRgb1Int(this.rgb[1]), getRgb1Int(this.rgb[2]) };
		return Utils.getRgbInt(rgbm);
	}

	// IDEA: This seems to be the bottleneck of the whole computation. If you
	// can write this faster, it may improve the speed quite significantly.

	public static void convexCombine(double srcRgba[], double dstRgba[], double outRgba[]) {
		double outAlpha = 1 - (1 - srcRgba[3]) * (1 - dstRgba[3]);
		if (Math.abs(outAlpha) < 1.e-10) {
			outRgba[0] = outRgba[1] = outRgba[2] = 0;
		} else {
			double aa1 = srcRgba[3] / outAlpha;
			double aa2 = 1 - aa1;
			outRgba[0] = aa1 * srcRgba[0] + aa2 * dstRgba[0];
			outRgba[1] = aa1 * srcRgba[1] + aa2 * dstRgba[1];
			outRgba[2] = aa1 * srcRgba[2] + aa2 * dstRgba[2];
		}
		outRgba[3] = outAlpha;
		Area.ccCount++;
		return;
	}

	public static double diff(int rgb1[], int rgb2[]) {
		return (1 / (double) 255)
				* (Math.abs(rgb2[0] - rgb1[0]) + Math.abs(rgb2[1] - rgb1[1]) + Math.abs(rgb2[2] - rgb1[2]));
	}

	public static double diff(double rgb1[], double rgb2[]) {
		return Math.abs(rgb2[0] - rgb1[0]) + Math.abs(rgb2[1] - rgb1[1]) + Math.abs(rgb2[2] - rgb1[2]);
	}

	public double diff(double rgb[]) {
		return diff(this.rgb, rgb);
	}

	public double diff() {
		return diff(rgb, targetRgb);
	}

	public static final void setBkgColor(double rgb[]) {
		System.arraycopy(BKGCOLOR, 0, rgb, 0, 3);
	}

}
