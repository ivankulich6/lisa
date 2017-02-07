public class AreaPixel {
	/*
	 * Testing methods of pre-finding difference increment: ITERATE - iterate
	 * shapes and apply convexCombine PUTREM - do change in shapes, use reduced
	 * value, revert change in shapes DEFAULT - use reduced from-to value
	 */
	public static final boolean DIFF_INC_IF_ITERATE = true;
	public static final boolean DIFF_INC_IF_PUTREM = false;
	public static final boolean RBTREE_WITHOUT_REDUCER = true;

	public static final double BKGCOLOR[] = { 1, 1, 1 };

	public RbTree<Integer, Area.Shape> shapes;
	public double[] rgb;
	public double[] newRgb;
	public double[] targetRgb;
	public final double[] rgbaWork;

	AreaPixel() {

		Reducer<Area.Shape> reducer = new Reducer<Area.Shape>() {
			@Override
			public Area.Shape reduce(Area.Shape reduced, Area.Shape value) {
				Area.Shape res = new Area().new Shape();
				res.rgba = new double[4];
				convexCombine(value.rgba, reduced.rgba, res.rgba);
				return res;
			}
		};

		if (!RBTREE_WITHOUT_REDUCER) {
			shapes = new RbTree<Integer, Area.Shape>(null, reducer);
		} else {
			shapes = new RbTree<Integer, Area.Shape>();
		}

		rgb = new double[] { 1, 1, 1 };
		newRgb = new double[3];
		targetRgb = new double[3];
		rgbaWork = new double[4];
	}

	public double diffIncIfAdded(Area.Shape shape) {
		if (DIFF_INC_IF_ITERATE) {
			return diffIncIfAdded_ITERATE(shape);
		} else if (DIFF_INC_IF_PUTREM) {
			return diffIncIfAdded_PUTREM(shape);
		} else {
			return diffIncIfAdded_RFT(shape);
		}
	}

	public double diffIncIfRemoved(Area.Shape shape) {
		if (DIFF_INC_IF_ITERATE) {
			return diffIncIfRemoved_ITERATE(shape);
		} else if (DIFF_INC_IF_PUTREM) {
			return diffIncIfRemoved_PUTREM(shape);
		} else {
			return diffIncIfRemoved_RFT(shape);
		}
	}

	public double diffIncIfReplaced(Area.Shape oldShape, Area.Shape newShape, int intype, boolean sameRgba) {
		if (DIFF_INC_IF_ITERATE) {
			return diffIncIfReplaced_ITERATE(oldShape, newShape, intype, sameRgba);
		} else if (DIFF_INC_IF_PUTREM) {
			return diffIncIfReplaced_PUTREM(oldShape, newShape, intype, sameRgba);
		} else {
			return diffIncIfReplaced_RFT(oldShape, newShape, intype, sameRgba);
		}
	}

	public double diffIncIfAdded_ITERATE(Area.Shape shape) {
		double diffOld = diff();
		double rgba[] = { 1, 1, 1, 1 };
		RbTree<Integer, Area.Shape>.EntryIterator it = shapes.new EntryIterator(shapes.firstEntry());
		while (it.hasNext()) {
			convexCombine(it.next().getValue().rgba, rgba, rgba);
		}
		convexCombine(shape.rgba, rgba, rgba);
		colorSkipAlpha(rgba, newRgb);
		return diff(newRgb, targetRgb) - diffOld;
	}

	public double diffIncIfAdded_RFT(Area.Shape shape) {
		double diffOld = diff();
		// Area.Shape cShape = shapes.getReduced();
		Area.Shape cShape = shapes.getReduced(0, Integer.MAX_VALUE);
		if (cShape != null) {
			convexCombine(shape.rgba, cShape.rgba, rgbaWork);
			addBkgColor(rgbaWork, newRgb);
		} else {
			addBkgColor(shape.rgba, newRgb);
		}
		return diff(newRgb, targetRgb) - diffOld;
	}

	public double diffIncIfAdded_PUTREM(Area.Shape shape) {
		double diffOld = diff();
		int order = shape.order;
		shapes.put(order, shape);
		Area.Shape cShape = shapes.getReduced();
		addBkgColor(cShape.rgba, newRgb);
		shapes.remove(order);
		return diff(newRgb, targetRgb) - diffOld;
	}

	public double diffIncIfRemoved_ITERATE(Area.Shape shape) {
		double diffOld = diff();
		int order = shape.order;
		Area.Shape savedShape;
		double rgba[] = { 1, 1, 1, 1 };
		RbTree<Integer, Area.Shape>.EntryIterator it = shapes.new EntryIterator(shapes.firstEntry());
		while (it.hasNext()) {
			savedShape = it.next().getValue();
			if (savedShape.order != order) {
				convexCombine(savedShape.rgba, rgba, rgba);
			}
		}
		colorSkipAlpha(rgba, newRgb);
		return diff(newRgb, targetRgb) - diffOld;
	}

	public double diffIncIfRemoved_RFT(Area.Shape shape) {
		double diffOld = diff();
		int order = shape.order;
		Area.Shape cShape1 = shapes.getReduced(-1, order);
		Area.Shape cShape2 = shapes.getReduced(order + 1, Integer.MAX_VALUE);
		if (cShape1 == null && cShape2 == null) {
			setBkgColor(newRgb);
		} else if (cShape1 == null) {
			addBkgColor(cShape2.rgba, newRgb);
		} else if (cShape2 == null) {
			addBkgColor(cShape1.rgba, newRgb);
		} else {
			convexCombine(cShape2.rgba, cShape1.rgba, rgbaWork);
			addBkgColor(rgbaWork, newRgb);
		}
		return diff(newRgb, targetRgb) - diffOld;

	}

	public double diffIncIfRemoved_PUTREM(Area.Shape shape) {
		double diffOld = diff();
		int order = shape.order;
		shapes.remove(order);
		Area.Shape cShape = shapes.getReduced();
		if (cShape != null) {
			addBkgColor(cShape.rgba, newRgb);
		} else {
			setBkgColor(newRgb);
		}
		shapes.put(order, shape);
		return diff(newRgb, targetRgb) - diffOld;
	}

	public double diffIncIfReplaced_ITERATE(Area.Shape oldShape, Area.Shape newShape, int intype, boolean sameRgba) {
		int order = newShape.order;
		assert order == oldShape.order;
		if (intype == 3 && sameRgba) {
			System.arraycopy(rgb, 0, newRgb, 0, 3);
			return 0;
		} else {
			double diffOld = diff();
			Area.Shape savedShape = null;
			double rgba[] = { 1, 1, 1, 1 };
			if (intype == 1) {
				RbTree<Integer, Area.Shape>.EntryIterator it = shapes.new EntryIterator(shapes.firstEntry());
				while (it.hasNext()) {
					savedShape = it.next().getValue();
					if (savedShape.order != order) {
						convexCombine(savedShape.rgba, rgba, rgba);
					}
				}
			} else if (intype == 2) {
				boolean add = true;
				RbTree<Integer, Area.Shape>.EntryIterator it = shapes.new EntryIterator(shapes.firstEntry());
				while (it.hasNext()) {
					savedShape = it.next().getValue();
					if (savedShape.order > order) {
						if (add) {
							convexCombine(newShape.rgba, rgba, rgba);
							add = false;
						}
						convexCombine(savedShape.rgba, rgba, rgba);
					} else {
						convexCombine(savedShape.rgba, rgba, rgba);
					}
				}

				if (add) {
					convexCombine(newShape.rgba, rgba, rgba);
				}
			} else if (intype == 3) {
				RbTree<Integer, Area.Shape>.EntryIterator it = shapes.new EntryIterator(shapes.firstEntry());
				while (it.hasNext()) {
					savedShape = it.next().getValue();
					if (savedShape.order != order) {
						convexCombine(savedShape.rgba, rgba, rgba);
					} else {
						convexCombine(newShape.rgba, rgba, rgba);
					}
				}
			}
			colorSkipAlpha(rgba, newRgb);
			return diff(newRgb, targetRgb) - diffOld;
		}
	}

	public double diffIncIfReplaced_RFT(Area.Shape oldShape, Area.Shape newShape, int intype, boolean sameRgba) {
		int order = newShape.order;
		assert order == oldShape.order;
		if (intype == 3 && sameRgba) {
			System.arraycopy(rgb, 0, newRgb, 0, 3);
			return 0;
		} else if (intype == 1) {
			return diffIncIfRemoved_RFT(newShape);
		} else {
			double diffOld = diff();
			Area.Shape cShape1 = shapes.getReduced(-1, order);
			Area.Shape cShape2 = shapes.getReduced(order + 1, Integer.MAX_VALUE);
			if (cShape1 == null && cShape2 == null) {
				addBkgColor(newShape.rgba, newRgb);
			} else if (cShape1 == null) {
				convexCombine(cShape2.rgba, newShape.rgba, rgbaWork);
				addBkgColor(rgbaWork, newRgb);
			} else if (cShape2 == null) {
				convexCombine(newShape.rgba, cShape1.rgba, rgbaWork);
				addBkgColor(rgbaWork, newRgb);
			} else {
				convexCombine(newShape.rgba, cShape1.rgba, rgbaWork);
				convexCombine(cShape2.rgba, rgbaWork, rgbaWork);
				addBkgColor(rgbaWork, newRgb);
			}
			return diff(newRgb, targetRgb) - diffOld;

		}
	}

	public double diffIncIfReplaced_PUTREM(Area.Shape oldShape, Area.Shape newShape, int intype, boolean sameRgba) {
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

			Area.Shape cShape = shapes.getReduced();
			if (cShape != null) {
				addBkgColor(cShape.rgba, newRgb);
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

	public void addShape(Area.Shape shape) {
		shapes.put(shape.order, shape);
		useNewRgb();
	}

	public void removeShape(Area.Shape shape) {
		shapes.remove(shape.order);
		useNewRgb();
	}

	public void replaceShape(Area.Shape oldShape, Area.Shape newShape, int intype) {
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
			for (int j = 0; j < 3; j++) {
				outRgba[j] = 0;
			}
		} else {
			double aa = srcRgba[3] / outAlpha;
			for (int j = 0; j < 3; j++) {
				outRgba[j] = aa * srcRgba[j] + (1 - aa) * dstRgba[j];
			}
		}
		outRgba[3] = outAlpha;
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

	public static final void colorSkipAlpha(double rgba[], double rgb[]) {
		System.arraycopy(rgba, 0, rgb, 0, 3);
	}

	public static final void setBkgColor(double rgb[]) {
		System.arraycopy(BKGCOLOR, 0, rgb, 0, 3);
	}
//zzz
	private void addBkgColor(double[] rgbaSrc, double[] rgbOut) {
		double rgba[] = { 1, 1, 1, 1 };
		convexCombine(rgbaSrc, rgba, rgba);
		colorSkipAlpha(rgba, rgbOut);
	}

	private void diffLog(int d, int dTest) {
		if (Math.abs(d - dTest) > 0) {
			Area.doLog = true;
			String s = "d=" + d + " dTest=" + dTest + " cnt=" + Area.cntRandomChange + " mutType=" + Area.mutationType
					+ " pixel=" + Area.currPixelOrder;
			System.out.println(s);
			Area.log(s + "\n");
			// assert false;
		}
	}

}
