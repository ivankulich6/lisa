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
				dValSrc[k][j] = v / 255;
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
		evolutionTest(Area.DiffIncIfMethod.ITERATE, false);
		evolutionTest(Area.DiffIncIfMethod.RFT, true);
		evolutionTest(Area.DiffIncIfMethod.PUTREM, true);
		printMsgResultsFile();
	}

	public static void annealingTest() throws IOException {
		// Note: This tests just annealing support. Annealing in this example is
		// successful only accidentaly. We do not know yet temperature changing
		// strategy, which would give statistically better results than no
		// annealing (temperature -1 or 0)
		Utils.setNewLog("testdata/log_AnnealingTest.txt");
		evolutionTest(Area.DiffIncIfMethod.ITERATE, false, 0, 1.0e-5, 5000);
		evolutionTest(Area.DiffIncIfMethod.ITERATE, false, 0, 0.01, 50000);
		evolutionTest(Area.DiffIncIfMethod.ITERATE, false, 1, 0.01, 50000);
		printMsgResultsFile();
	}

	public static void evolutionTest(Area.DiffIncIfMethod method, boolean withReducer) throws IOException {
		evolutionTest(method, withReducer, -1, 1.0e-5, 5000);
	}

	public static void evolutionTest(Area.DiffIncIfMethod method, boolean withReducer, double temperature,
			double penaltyPointsCountParam, int mutationsCount) throws IOException {
		BufferedImage shapesImg;
		Area area = new Area(true);
		Area.ccCount = 0;
		area.randg = new Random(6543210);
		// area.randg = new Random(3543210);
		area.penaltyPointsCountParam = penaltyPointsCountParam;
		area.setTarget("women_small.jpg", withReducer);
		if (temperature <= 0) {
			area.temperature = temperature;
		}
		int cnt = 0;
		String sMsg;
		if (temperature < 0) {
			sMsg = "Starting test diffIncIfMethodsCompareTest1, method = " + method.toString();
		} else {
			sMsg = "Starting test annealingTest, temperature = " + temperature;
		}
		System.out.print(sMsg);
		long startTime = System.currentTimeMillis();
		if (temperature < 0) {
			while (cnt < mutationsCount) {
				cnt++;
				area.doRandomChange(method);
			}
		} else {
			int nSuccess = 1;
			while (cnt < mutationsCount) {
				cnt++;
				if (cnt % 5000 == 0) {
					System.out.print(".");
				}
				area.temperature = Math.pow(temperature * nSuccess / cnt, 3);
				if (3 == area.doRandomChange(cnt == mutationsCount, method)) {
					nSuccess++;
				}
			}
		}
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		StringBuilder sb = new StringBuilder();

		sb.append("\nelapsedTime = " + elapsedTime + " milliseconds");
		Shape[] exShapes = area.extractShapes();
		assert exShapes.length == area.shapesCount;
		assert area.recalcPointsCount(exShapes) == area.pointsCount;
		sb.append("\nDiff=" + area.diff + ", cnt=" + cnt + ", polygons=" + area.shapesCount + ", temperature="
				+ area.temperature);
		double diffAll = 0, diff2 = 0, avgPolyPerPixel = 0;
		if (temperature <= 0) {
			shapesImg = Utils.drawShapes(area);
			diffAll = area.diffTest();
			diff2 = area.diffTest(Utils.addeDiff(shapesImg, area.target));
			avgPolyPerPixel = area.getAvgNumOfShapesPerPixel();
			sb.append("\nDiffAll=" + diffAll + " Diff2=" + diff2 + " AvgPolyPerPixel=" + avgPolyPerPixel);
			// Diff2: regenerated whole area diff, merging of transparent
			// colors by imported Graphics (fillPolygon)
		}

		sb.append("\nccCount=" + Area.ccCount);
		String anne = (temperature < 0 ? "NoAnne" : (temperature == 1 ? "Anne0" : "Anne1"));
		area.shapesToFile("testdata/diffIncIfMethodsCompareTest1_" + method.toString() + "_" + anne + ".shapes");
		System.out.println(sb.toString());
		System.out.println("");
		sb.insert(0, sMsg).append("\n\n");
		Utils.log(sb);
		// the next depends on implementation of getRandomShape,
		// getRandomMutation, penaltyShape
		if (temperature <= 0 && penaltyPointsCountParam == 1.0e-5 && mutationsCount == 5000) {
			assert area.shapesCount == 95;
			assert Math.abs(area.diff - 0.5343659179119827) < 1.e-15;
			assert Math.abs(diffAll - 0.5343659179119827) < 1.e-15;
			assert Math.abs(diff2 - 0.5334975342648108) < 1.e-15;
			assert Math.abs(avgPolyPerPixel - 6.954266666666666) < 1.e-15;
		}
		if (temperature == 0 && penaltyPointsCountParam == 0.01 && mutationsCount == 50000) {
			assert area.shapesCount == 8;
			assert Math.abs(area.diff - 0.40232216040004554) < 1.e-15;
		}
		if (temperature == 1 && penaltyPointsCountParam == 0.01 && mutationsCount == 50000) {
			assert area.shapesCount == 7;
			assert Math.abs(area.diff - 0.3569308075390611) < 1.e-15;
		}
	}

	public static void diffIncIfMethodsCompareTest2() throws IOException {
		String logFile = "testdata/log_diffIncIfMethodsCompareTest2.txt";
		Utils.setNewLog(logFile);
		evolutionTest2(Area.DiffIncIfMethod.ITERATE, false);
		evolutionTest2(Area.DiffIncIfMethod.RFT, true);
		printMsgResultsFile();
	}

	public static void evolutionTest2(Area.DiffIncIfMethod method, boolean withReducer) throws IOException {
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
		sb.append("\nDiff=" + area.diff + ", cnt=" + cnt + ", polygons=" + area.shapesCount + ", temperature="
				+ area.temperature);
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
