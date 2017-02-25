import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;

public class Test {
	/*
	 * Speed and accuracy test of Convex combine operator (CC). Compares double
	 * and float versions. Applies CC on the same sequence of colors in
	 * different, randomly generated associations, calculates average result and
	 * average deviation of results from average result.
	 */

	static void ccSpeedAndAccuracyTest() {
		int nCyc = 100; // number of repeating of the same task
		int nVal = 2000; // number of randomly generated source rgba colors
		int nAssoc = 1000; // number of randomly generated associations of
							// source color sequence
		double dValSrc[][] = new double[nVal][4];
		double dVal[][] = new double[nVal][4];
		double dRes[][] = new double[nAssoc][4];
		double dAvg[] = new double[4];
		double dDist = 0;
		float fValSrc[][] = new float[nVal][4];
		float fVal[][] = new float[nVal][4];
		float fRes[][] = new float[nAssoc][4];
		float fAvg[] = new float[4];
		float fDist = 0;
		System.out.println("Test ccSpeedAndAccuracy: preparing data");
		Random randg = new Random(76543210);
		for (int j = 0; j < 4; j++) {
			for (int k = 0; k < nVal; k++) {
				double v = randg.nextInt(255);
				if (v > 2 && v < 250) {
					v -= (Math.sqrt(2) - 1); // making full decimal places
				}
				dValSrc[k][j] = v / (double) 255;
				fValSrc[k][j] = (float) dValSrc[k][j];
			}
		}
		LinkedList<Integer> inds = new LinkedList<Integer>();
		int assoc[][][] = new int[nAssoc][nVal - 1][2];
		for (int i = 0; i < nAssoc; i++) {
			inds.clear();
			for (int j = 0; j < nVal; j++) {
				inds.add(j);
			}
			for (int k = 0; k < nVal - 1; k++) {
				int iAss = randg.nextInt(inds.size() - 1);
				assoc[i][k][0] = inds.get(iAss);
				assoc[i][k][1] = inds.get(iAss + 1);
				inds.remove(iAss);
			}
		}

		System.out.println("Test ccSpeedAndAccuracy: start");
		long startTime = System.currentTimeMillis();

		for (int jc = 0; jc < nCyc; jc++) {
			for (int ja = 0; ja < nAssoc; ja++) {
				for (int jv = 0; jv < nVal; jv++) {
					for (int ji = 0; ji < 4; ji++) {
						dVal[jv][ji] = dValSrc[jv][ji];
					}
				}
				int[][] assoc1 = assoc[ja];
				for (int jo = 0; jo < nVal - 1; jo++) {
					convexCombine(dVal[assoc1[jo][0]], dVal[assoc1[jo][1]], dVal[assoc1[jo][1]]);
					if (jo == nVal - 2) {
						System.arraycopy(dVal[assoc1[jo][1]], 0, dRes[ja], 0, 4);
					}
				}
			}
			for (int ji = 0; ji < 4; ji++) {
				dAvg[ji] = 0;
				for (int ja = 0; ja < nAssoc; ja++) {
					dAvg[ji] += dRes[ja][ji];
				}
				dAvg[ji] /= nAssoc;
			}
			dDist = 0;
			for (int ja = 0; ja < nAssoc; ja++) {
				for (int ji = 0; ji < 4; ji++) {
					dDist += Math.abs(dRes[ja][ji] - dAvg[ji]);
				}
			}
			dDist /= nAssoc;
		}

		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;

		Utils.setNewLog("testdata/log_ccSpeedAndAccuracyTest.txt");
		Utils.log2("double: elapsedTime= " + elapsedTime + " milliseconds" + " avg=" + Utils.arrayToSb(dAvg) + " dist="
				+ dDist);

		startTime = System.currentTimeMillis();

		for (int jc = 0; jc < nCyc; jc++) {
			for (int ja = 0; ja < nAssoc; ja++) {
				for (int jv = 0; jv < nVal; jv++) {
					for (int ji = 0; ji < 4; ji++) {
						fVal[jv][ji] = fValSrc[jv][ji];
					}
				}
				int[][] assoc1 = assoc[ja];
				for (int jo = 0; jo < nVal - 1; jo++) {
					convexCombine(fVal[assoc1[jo][0]], fVal[assoc1[jo][1]], fVal[assoc1[jo][1]]);
					if (jo == nVal - 2) {
						System.arraycopy(fVal[assoc1[jo][1]], 0, fRes[ja], 0, 4);
					}
				}
			}
			for (int ji = 0; ji < 4; ji++) {
				fAvg[ji] = 0;
				for (int ja = 0; ja < nAssoc; ja++) {
					fAvg[ji] += fRes[ja][ji];
				}
				fAvg[ji] /= nAssoc;
			}
			fDist = 0;
			for (int ja = 0; ja < nAssoc; ja++) {
				for (int ji = 0; ji < 4; ji++) {
					fDist += Math.abs(fRes[ja][ji] - fAvg[ji]);
				}
			}
			fDist /= nAssoc;
		}

		stopTime = System.currentTimeMillis();
		elapsedTime = stopTime - startTime;
		Utils.log2("float: elapsedTime= " + elapsedTime + " milliseconds" + " avg=" + Utils.arrayToSb(fAvg) + " dist="
				+ fDist);
		printMsgResultsFile();
	}

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

	public static void convexCombine(float srcRgba[], float dstRgba[], float outRgba[]) {
		float outAlpha = 1 - (1 - srcRgba[3]) * (1 - dstRgba[3]);
		if (Math.abs(outAlpha) < 1.e-10) {
			for (int j = 0; j < 3; j++) {
				outRgba[j] = 0;
			}
		} else {
			float aa = srcRgba[3] / outAlpha;
			for (int j = 0; j < 3; j++) {
				outRgba[j] = aa * srcRgba[j] + (1 - aa) * dstRgba[j];
			}
		}
		outRgba[3] = outAlpha;
		return;
	}

	public static void diffIncIfMethodsCompareTest() throws IOException {
		Utils.setNewLog("testdata/log_diffIncIfMethodsCompareTest.txt");
		diffIncIfMethodsCompareTest1(Area.DiffIncIfMethod.ITERATE, false);
		diffIncIfMethodsCompareTest1(Area.DiffIncIfMethod.RFT, true);
		diffIncIfMethodsCompareTest1(Area.DiffIncIfMethod.PUTREM, true);
		printMsgResultsFile();
	}

	public static void diffIncIfMethodsCompareTest1(Area.DiffIncIfMethod method, boolean withReducer)
			throws IOException {
		BufferedImage shapesImg;
		Area area = new Area(true);
		Area.ccCount = 0;
		area.randg = new Random(6543210);
		area.penaltyPointsCountParam = 1.0e-5;
		area.setTarget("women_small.jpg", withReducer);
		int cnt = 0;
		String sMsg = "Starting test diffIncIfMethodsCompareTest1, method = " + method.toString();
		System.out.print(sMsg);
		long startTime = System.currentTimeMillis();
		while (cnt < 5000) {
			cnt++;
			area.doRandomChange(method);
		}
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		StringBuilder sb = new StringBuilder();

		sb.append("\nelapsedTime = " + elapsedTime + " milliseconds");
		Shape[] exShapes = area.extractShapes();
		assert exShapes.length == area.shapesCount;
		assert area.recalcPointsCount(exShapes) == area.pointsCount;
		sb.append("\nDiff=" + area.diff + ", cnt=" + cnt + ", polygons=" + area.shapesCount + ", temp=" + area.temp);
		shapesImg = Utils.drawShapes(area);
		double diffAll = area.diffTest();
		double diff2 = area.diffTest(Utils.addeDiff(shapesImg, area.target));
		double avgPolyPerPixel = area.getAvgNumOfShapesPerPixel();
		sb.append("\nDiffAll=" + diffAll + " Diff2=" + diff2 + " AvgPolyPerPixel=" + avgPolyPerPixel);
		// Diff2: regenerated whole area diff, merging of transparent
		// colors by imported Graphics (fillPolygon)
		sb.append("\nccCount=" + Area.ccCount);
		area.shapesToFile("testdata/diffIncIfMethodsCompareTest1_" + method.toString() + ".shapes");
		System.out.println(sb.toString());
		System.out.println("");
		sb.insert(0, sMsg).append("\n\n");
		Utils.log(sb);
		// the next depends on implementation of getRandomShape,
		// getRandomMutation, penaltyShape
		assert area.shapesCount == 95;
		assert Math.abs(area.diff - (double) 0.5343659179119827) < (double) 1.e-15;
		assert Math.abs(diffAll - (double) 0.5343659179119827) < (double) 1.e-15;
		assert Math.abs(diff2 - (double) 0.5334975342648108) < (double) 1.e-15;
		assert Math.abs(avgPolyPerPixel - (double) 6.954266666666666) < (double) 1.e-15;
	}

	public static void diffIncIfMethodsCompareTest2() throws IOException {
		String logFile = "testdata/log_diffIncIfMethodsCompareTest2.txt";
		Utils.setNewLog(logFile);
		diffIncIfMethodsCompareTest21(Area.DiffIncIfMethod.ITERATE, false);
		diffIncIfMethodsCompareTest21(Area.DiffIncIfMethod.RFT, true);
		printMsgResultsFile();
	}

	public static void diffIncIfMethodsCompareTest21(Area.DiffIncIfMethod method, boolean withReducer)
			throws IOException {
		BufferedImage shapesImg;
		Area area = new Area(true);
		Area.ccCount = 0;
		area.randg = new Random(6543210);
		area.penaltyPointsCountParam = 0;
		area.setFromFile("testdata/diffIncIfMethodsCompareTest2.shapes", withReducer);
		area.setPenaltyPointsCountParam(0);
		int cnt = 0;
		String sMsg = "Starting test diffIncIfMethodsCompareTest2, method = " + method.toString();
		System.out.print(sMsg);
		long startTime = System.currentTimeMillis();
		while (cnt < 5000) {
			cnt++;
			area.doRandomChange(method);
		}
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		StringBuilder sb = new StringBuilder();

		sb.append("\nelapsedTime = " + elapsedTime + " milliseconds");
		Shape[] exShapes = area.extractShapes();
		assert exShapes.length == area.shapesCount;
		assert area.recalcPointsCount(exShapes) == area.pointsCount;
		sb.append("\nDiff=" + area.diff + ", cnt=" + cnt + ", polygons=" + area.shapesCount + ", temp=" + area.temp);
		shapesImg = Utils.drawShapes(area);
		double diffAll = area.diffTest();
		double diff2 = area.diffTest(Utils.addeDiff(shapesImg, area.target));
		double avgPolyPerPixel = area.getAvgNumOfShapesPerPixel();
		sb.append("\nDiffAll=" + diffAll + " Diff2=" + diff2 + " AvgPolyPerPixel=" + avgPolyPerPixel);
		// Diff2: regenerated whole area diff, merging of transparent
		// colors by imported Graphics (fillPolygon)
		sb.append("\nccCount=" + Area.ccCount);
		System.out.println(sb.toString());
		System.out.println("");
		sb.insert(0, sMsg).append("\n\n");
		Utils.log(sb);
	}

	public static void printMsgResultsFile() {
		System.out.println("Find results in file " + Utils.logPath + "\n");
	}

}
