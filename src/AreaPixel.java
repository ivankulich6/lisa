public class AreaPixel {
	/*
	 * Testing methods of pre-finding difference increment: ITERATE - iterate
	 * shapes and apply convexCombine PUTREM - do change in shapes, use reduced
	 * value, revert change in shapes DEFAULT - use reduced from-to value
	 */
	public static final boolean DIFF_INC_IF_ITERATE = false;
	public static final boolean DIFF_INC_IF_PUTREM = false;
	public static final boolean RBTREE_WITHOUT_REDUCER = false;

	public static final int BKGCOLOR[] = { 255, 255, 255 };

	public RbTree<Integer, Area.Shape> shapes;
	public int[] rgb;
	public int[] newRgb;
	public int[] targetRgb;
	public final float[] rgbaWork;

	AreaPixel() {

		Reducer<Area.Shape> reducer = new Reducer<Area.Shape>() {
			@Override
			public Area.Shape reduce(Area.Shape reduced, Area.Shape value) {
				Area.Shape res = new Area().new Shape();
				res.rgba = new float[4];
				convexCombine(value.rgba, reduced.rgba, res.rgba);
				return res;
			}
		};

		if (!RBTREE_WITHOUT_REDUCER) {
			shapes = new RbTree<Integer, Area.Shape>(null, reducer);
		} else {
			shapes = new RbTree<Integer, Area.Shape>();
		}

		rgb = new int[] { 255, 255, 255 };
		newRgb = new int[3];
		rgbaWork = new float[4];
	}

	public int diffIncIfAdded(Area.Shape shape) {
		if (DIFF_INC_IF_ITERATE) {
			return diffIncIfAdded_ITERATE(shape);
		} else if (DIFF_INC_IF_PUTREM) {
			return diffIncIfAdded_PUTREM(shape);
		} else {
			return diffIncIfAdded_RFT(shape);
		}
	}

	public int diffIncIfRemoved(Area.Shape shape) {
		if (DIFF_INC_IF_ITERATE) {
			return diffIncIfRemoved_ITERATE(shape);
		} else if (DIFF_INC_IF_PUTREM) {
			return diffIncIfRemoved_PUTREM(shape);
		} else {
			return diffIncIfRemoved_RFT(shape);
		}
	}


	public int diffIncIfReplaced(Area.Shape oldShape, Area.Shape newShape, int intype, boolean sameRgba) {
		if (DIFF_INC_IF_ITERATE) {
			return diffIncIfReplaced_ITERATE(oldShape, newShape, intype, sameRgba);
		} else if (DIFF_INC_IF_PUTREM) {
			return diffIncIfReplaced_PUTREM(oldShape, newShape, intype, sameRgba);
		} else {
			return diffIncIfReplaced_RFT(oldShape, newShape, intype, sameRgba);
		}
	}

	public int diffIncIfAdded_ITERATE(Area.Shape shape) {
		int diffOld = diff();
		float rgba[] = { 255, 255, 255, 255 };
		RbTree<Integer, Area.Shape>.EntryIterator it = shapes.new EntryIterator(shapes.firstEntry());
		while (it.hasNext()) {
			convexCombine(it.next().getValue().rgba, rgba, rgba);
		}
		convexCombine(shape.rgba, rgba, rgba);
		colorSkipAlpha(rgba, newRgb);
		return diff(newRgb, targetRgb) - diffOld;
	}

	public int diffIncIfAdded_RFT(Area.Shape shape) {
		int diffOld = diff();
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

	public int diffIncIfAdded_PUTREM(Area.Shape shape) {
		int diffOld = diff();
		int order = shape.order;
		shapes.put(order, shape);
		Area.Shape cShape = shapes.getReduced();
		addBkgColor(cShape.rgba, newRgb);
		shapes.remove(order);
		return diff(newRgb, targetRgb) - diffOld;
	}

	public int diffIncIfRemoved_ITERATE(Area.Shape shape) {
		int diffOld = diff();
		int order = shape.order;
		Area.Shape savedShape;
		float rgba[] = { 255, 255, 255, 255 };
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

	public int diffIncIfRemoved_RFT(Area.Shape shape) {
		int diffOld = diff();
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

	public int diffIncIfRemoved_PUTREM(Area.Shape shape) {
		int diffOld = diff();
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

	public int diffIncIfReplaced_ITERATE(Area.Shape oldShape, Area.Shape newShape, int intype, boolean sameRgba) {
		int order = newShape.order;
		assert order == oldShape.order;
		if (intype == 3 && sameRgba) {
			System.arraycopy(rgb, 0, newRgb, 0, 3);
			return 0;
		} else {
			int diffOld = diff();
			Area.Shape savedShape = null;
			float rgba[] = { 255, 255, 255, 255 };
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

	public int diffIncIfReplaced_RFT(Area.Shape oldShape, Area.Shape newShape, int intype, boolean sameRgba) {
		int order = newShape.order;
		assert order == oldShape.order;
		if (intype == 3 && sameRgba) {
			System.arraycopy(rgb, 0, newRgb, 0, 3);
			return 0;
		} else if (intype == 1) {
			return diffIncIfRemoved_RFT(newShape);
		} else {
			int diffOld = diff();
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

	public int diffIncIfReplaced_PUTREM(Area.Shape oldShape, Area.Shape newShape, int intype, boolean sameRgba) {
		int order = newShape.order;
		assert order == oldShape.order;
		if (intype == 3 && sameRgba) {
			System.arraycopy(rgb, 0, newRgb, 0, 3);
			return 0;
		} else {
			int diffOld = diff();
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

	public int getRgbInt() {
		int rgbm[] = { Math.round(this.rgb[0]), Math.round(this.rgb[1]), Math.round(this.rgb[2]) };
		return Utils.getRgbInt(rgbm);
	}

	// IDEA: This seems to be the bottleneck of the whole computation. If you
	// can write this faster, it may improve the speed quite significantly.

	public void rgbRegen(int[] rgb) {
		float rgba[] = { 255, 255, 255, 255 };
		RbTree<Integer, Area.Shape>.EntryIterator it = shapes.new EntryIterator(shapes.firstEntry());
		while (it.hasNext()) {
			convexCombine(it.next().getValue().rgba, rgba, rgba);
		}
		colorSkipAlpha(rgba, newRgb);
	}

	public void rgbRegen() {
		rgbRegen(this.rgb);
	}

	public static void convexCombine(float srcRgba[], float dstRgba[], float outRgba[]) {
		StringBuilder sb = null;
		float srcRgba3 = srcRgba[3];
		float outAlpha = srcRgba3 + (dstRgba[3] * (255 - srcRgba3)) / 255;
		if (outAlpha == 0) {
			for (int j = 0; j < 3; j++) {
				outRgba[j] = 0;
			}
		} else {
			float srcRgba3Compl = outAlpha - srcRgba3;
			for (int j = 0; j < 3; j++) {
				outRgba[j] = (srcRgba[j] * srcRgba3 + dstRgba[j] * srcRgba3Compl) / outAlpha;
			}
		}
		outRgba[3] = outAlpha;
		return;
	}

	public static void convexCombine(int srcRgba[], float dstRgba[], float outRgba[]) {
		float srcRgba3 = (float) srcRgba[3];
		float outAlpha = srcRgba3 + (dstRgba[3] * (255 - srcRgba3)) / 255;
		if (outAlpha == 0) {
			for (int j = 0; j < 3; j++) {
				outRgba[j] = 0;
			}
		} else {
			float srcRgba3Compl = outAlpha - srcRgba3;
			for (int j = 0; j < 3; j++) {
				outRgba[j] = ((float) srcRgba[j] * srcRgba3 + dstRgba[j] * srcRgba3Compl) / outAlpha;
			}
		}
		outRgba[3] = outAlpha;
		return;
	}

	public static void convexCombineInt(int srcRgba[], int dstRgba[], int outRgba[]) {
		double outAlpha = (double) srcRgba[3] + (double) (dstRgba[3] * (255 - srcRgba[3])) / (double) 255;
		if (outAlpha == 0) {
			for (int j = 0; j < 3; j++) {
				outRgba[j] = 0;
			}
		} else {
			for (int j = 0; j < 3; j++) {
				outRgba[j] = (int) Math.round(((double) srcRgba[j] * (double) srcRgba[3] + (double) dstRgba[j]
						* (double) (outAlpha - srcRgba[3]))
						/ outAlpha);
			}

		}
		outRgba[3] = (int) Math.round(outAlpha);
		return;
	}

	public static int diff(int rgb1[], int rgb2[]) {
		return Math.abs(rgb2[0] - rgb1[0]) + Math.abs(rgb2[1] - rgb1[1]) + Math.abs(rgb2[2] - rgb1[2]);
	}

	public static float diff(float rgb1[], float rgb2[]) {
		return Math.abs(rgb2[0] - rgb1[0]) + Math.abs(rgb2[1] - rgb1[1]) + Math.abs(rgb2[2] - rgb1[2]);
	}

	public int diff(int rgb[]) {
		return diff(this.rgb, rgb);
	}

	public int diff() {
		return diff(rgb, targetRgb);
	}

	public static final void colorSkipAlpha(float rgba[], int rgb[]) {
		rgb[0] = Math.round(rgba[0]);
		rgb[1] = Math.round(rgba[1]);
		rgb[2] = Math.round(rgba[2]);
	}

	public static final void setBkgColor(int rgb[]) {
		System.arraycopy(BKGCOLOR, 0, rgb, 0, 3);
	}

	private void addBkgColor(float[] rgbaSrc, int[] rgbOut) {
		float rgba[] = { 255, 255, 255, 255 };
		convexCombine(rgbaSrc, rgba, rgba);
		colorSkipAlpha(rgba, rgbOut);
	}

	private void diffLog(int d, int dTest) {
		if (Math.abs(d - dTest) > 0) {
			Area.doLog = true;
			String s = "d=" + d + " dTest=" + dTest + " cnt=" + Area.cntRandomChange + " mutType="
					+ Area.mutationType + " pixel=" + Area.currPixelOrder;
			System.out.println(s);
			Area.log(s+"\n");
			//assert false;
		}
	}

}
