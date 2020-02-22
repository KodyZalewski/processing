
import java.util.*;

public class smoothVolume {
	
	static boolean BOUNDARIES[][][];
	
	// TODO: alter public methods to handle opposite dimensions independently
	// TODO: don't set all values of k to Math.abs() every single time, needlessly convoluted
	// TODO: double-check loop counters, why does patch create a line of intensity "2"?
	
	// ------------------------------------------------------------ //
	
					   // BOOLEAN BOUNDARY MAP // 
	
	// ------------------------------------------------------------ //
	
	/**
	 * @author KJZ
	 * @param nds is the input nifti dataset volume, can also handle taking double matrix.
	 * @result Creates an equivalent global variable boolean grid named "BOUNDARIES".
	 * Boundaries of the brain can be marked later as "true". 
	 */
	
	public static void writeBoundaries(Nifti1Dataset nds) {
		
		BOUNDARIES = new boolean[nds.ZDIM][nds.YDIM][nds.XDIM];
		
		for (int i = 0; i < BOUNDARIES.length; i++) {
			for (int j = 0; j < BOUNDARIES[i].length; j++) {
				for (int k = 0; k < BOUNDARIES[i][j].length; k++) {
					BOUNDARIES[i][j][k] = false;
				}
			}
		}
	}
	
	// ------------------------------------------------------------ //
	
						// ZERO MATRIX //
	
	// ------------------------------------------------------------ //
	
	/**
	 * @author KJZ
	 * @param takes z, y, xDim to set matrix boundaries
	 * @return double matrix of zeros
	 */
	
	public static double[][][] writeZeros(int z, int y, int xDim) {
	
		double[][][] dataBound = new double[z][y][xDim];
		
		for ( int i = 0; i < z; i++ ) {
			for ( int j = 0; j < y; j++ ) {
				for ( int k = 0; k < xDim; k++ ) {
					dataBound[i][j][k] = 0; 
				}
			}
		}
		
		return dataBound;
		
	}
	
	// ------------------------------------------------------------ //
	
						// TOTAL AVERAGE // 
	
	// ------------------------------------------------------------ //
	
	/**
	 * @author KJZ 1.7.2020
	 * @param Takes 3D data as double 3D matrix.
	 * @param Takes boolean nonZero, if true then only non-zero voxels are included
	 * @result Finds the total average of a nifti dataset for given sets of voxels
	 */
	
	public static double totalAverage(double[][][] data, boolean nonZero) {
		double avg = 0; int counter = 0;
		
		for (int i = 0; i < data.length; i++) {		
			for (int j = 0; j < data[i].length; j++) {
				for (int k = 0; k < data[i][j].length; k++) {
					if (data[i][j][k] != 0 && nonZero) {
						avg = avg+=data[i][j][k];
						counter++;
					} else if (!nonZero) {
						avg = avg+=data[i][j][k];
						counter++;
					}
				}
			}
		}
		
		return (avg/counter);
		
	}
	
	// ------------------------------------------------------------ //
	
					// MOVING AVERAGE SMOOTHING // 
	
	// ------------------------------------------------------------ //
	
	/**
	 * @author KJZ 12.19
	 * @param Takes data as double 3D matrix.
	 * @param length as int len, denotes how many voxels parallel to the given dim to smooth (default is 1).
	 * @return double 3D matrix to be written to a new scan as a smoothed volume. 
	 * @result The function reduces the number of outliers is reduced across the scan, ergo making thresholding of 
	 * voxels outside of the brain more robust. 
	 */

	public static double[][][] movingAverage(double[][][] data, int len) {

		System.out.println("Smoothing volume... ");
				
		int x = data[0][0].length; int y = data[0].length; int z = data.length; 
		System.out.println("Finished smoothing."); System.out.println("");
		
		return movingAverageHelper(movingAverageHelper(movingAverageHelper(data, 
				z, y, x, len, "x"), 
				z, x, y, len, "y"),
				x, y, z, len, "z");
	}
	
	private static double[][][] movingAverageHelper(double[][][] data, 
			int a, int b, int c, int len, String dim) {
		
		double newData;
		
		for ( int i = len; i < a; i++ ) {
			for ( int j = len; j < b; j++ ) { 
				for ( int k = len; k < c - len; k++ ) {
					newData = 0; 
					for ( int l = 0; l < len; l++ ) {
						newData += (pullData(data, dim, i, j, k+l) + pullData(data, dim, i, j, k-l));
					}
					data = pushData(data, dim, i, j, k, (newData + pullData(data, dim, i, j, k))/((len * 2) + 1));
				}
			}
		}
		
		return data; 
		
	}
	
	// ------------------------------------------------------------ //
	
					// PERIPHREAL VOXEL AVERAGING // 
	
	// ------------------------------------------------------------ //
	
	/**
	 * @author KJZ 1.10.20
	 * @param Takes data as double 3D matrix.
	 * @param dim is the dimension given as a string
	 * @param int a, b, c are respective dimensions depending on which dim (as string) is being traversed.
	 * @param depth is integer as to how many voxels deep should be taken on the periphery of the scan to 
	 * obtain an average estimation. (3 is probably a robust number)
	 * @result finds the average of the most peripheral voxels in dimension
	 * passed to the function. This aids with normalizing each dimension appropriately. 
	 */
	
	public static float outerAverage(double[][][] data, String dim, 
			int depth, int a, int b, int c, int incr, int len) {
		
		int voxCount = 0; float avg = 0; double tmp;
		
		for (int i = 0; i < a; i++) {
			for(int j = 0; j < b; j++) {
				for (int k = c; k != len; k+=incr) {
					if (checkBoundary(data, dim, 0, 0.99, i, j, k)) {
						for (int l = 0; l < depth; l++) {
							if (k+l == c-incr) {
								break;
							} else {
								tmp = pullData(data, dim, i, j, k+l);
								if (tmp > 0.99) {
									voxCount++;
									avg+=tmp;
								}
							}
						}
						break;
					}
				}
			}
		}
		
		return avg/voxCount;
		
	}
	
	// finds the average of voxel intensity for each of the six dimensional facets
	// depth denotes the number of voxels included in the calculation for each point
	// at coordinate (x,y)
	public static float[] dimAverages(double[][][] data, int depth) {
		
		float[] avgs = new float[6];
		
		avgs[0] = outerAverage(data, "x", depth, 
				data.length-1, data[0].length-1, 0, 1, data[0][0].length/2);
		avgs[1] = outerAverage(data, "x", depth, 
				data.length-1, data[0].length-1, data[0][0].length-2, -1, data[0][0].length/2);		
		avgs[2] = outerAverage(data, "y", 
				depth, data.length-1, data[0][0].length-1, 0, 1, data[0].length/2);		
		avgs[3] = outerAverage(data, "y", 
				depth, data.length-1, data[0][0].length-1, data[0].length-2, -1, data[0].length/2);		
		avgs[4] = outerAverage(data, "z", 
				depth, data[0][0].length-1, data[0].length-1, 0, 1, data.length/2);	
		avgs[5] = outerAverage(data, "z", 
				depth, data[0][0].length-1, data[0].length-1, data.length-2, -1, data.length/2);
		System.out.println("X+ outer average is: " + thresholdStandardDev.round(avgs[0],3) + 
				" X- outer average is: " + thresholdStandardDev.round(avgs[1],3));
		System.out.println("Y+ outer average is: " + thresholdStandardDev.round(avgs[2],3) +  
				" Y- outer average is: " + thresholdStandardDev.round(avgs[3],3)); 
		System.out.println("Z+ outer average is: " + thresholdStandardDev.round(avgs[4],3) + 
				" Z- outer average is: " + thresholdStandardDev.round(avgs[5],3));
		System.out.println("");
		
		return avgs;
		
	}
	
	// ------------------------------------------------------------ //
	
				      // HISTOGRAM GENERATION // 
	
	// ------------------------------------------------------------ //
	
	/**
	 * @author KJZ 2.20.20
	 * @param data data as double 3D matrix.
	 * @param dim is the dimension given as a string
	 * @param a, b, c are respective dimensions depending on which dim (as string) is being traversed
	 * @param incr is 1 or -1 depending on moving forward/backward across the scan
	 * @param len denotes number of voxels in dim k to include in analysis
	 * @return array of integers for each bin of the histogram dataset 
	 * the first column of the array is the value of the bin, the second
	 * column is the total count of the number of values in that bin 
	 */
	
	public static float[][] histogram(double[][][] data, String dim, 
			int a, int b, int c, int incr, int len) {
		
		int count = 0; double total = 0; double val; 
		ArrayList<Float> totalVals = new ArrayList<Float>();
		
		for ( int i = 0; i < a; i++ ) {
			for ( int j = 0; j < b; j++ ) {
				for ( int k = 0; k != len; k+=incr ) {
					val = pullData(data, dim, i, j, k);
					if (val > 0.99) {
						totalVals.add(count,(float) val);
						count++;
						total+=val;
					}
				}
			}
		}

		return histCalc((float) total/count, copyArray(totalVals), totalVals);
		
	}
	
	private static float[][] histCalc(float avg, float[] floatArray, ArrayList<Float> totalVals) {
		
		float stDev =  thresholdStandardDev.standardDeviation(floatArray, avg);
		float[][] histBins = new float[2][12]; float startVal = avg-(stDev*3)-(stDev/2); // change these values to a variable later
		
		for (int i = 0; i < histBins[0].length; i++) {
			startVal += stDev/2;
			histBins[0][i] = startVal;
			histBins[1][i] = 0.0f;
		}
		
		for (float i : totalVals) {
			if (i > 0.99) {
				for (int j = 1; j < histBins[1].length; j++) {
					if (i < histBins[0][j]) {
						histBins[1][j-1]+=1;
						break;
					}
				}
			}
		}
		
		for (int i = 0; i < histBins[0].length; i++) {
			System.out.println("Bin " + i + " with intensity min " + 
		histBins[0][i] + " has " + histBins[1][i] + " voxels");		
		}
		
		return histBins;
		
	}
	
	// ------------------------------------------------------------ //
	
						// EROSION ALGORITHM //
	
	// ------------------------------------------------------------ //
	
	/**
	 * @author KJZ 12.19
	 * @param data is nifti intensity data as 3D matrix double[][][]. 
	 * @param erode is the max length (as integer) of voxels to be eroded from the edge of the scan.
	 * @param lowBnd is int denoting the lowest intensity threshold to reach before stopping. 
	 * @param x, y, z as bool passed by the client to denote which dimension are being processed. 
	 * @param half denotes which half of the scan is being traversed (-dim if !half, otherwise +dim).
	 * @return eroded mri intensity data as 3D matrix in double format
	 * @result The number of voxels provided by the client around the edge of the scan are thresholded out
	 * provided they are above the intensity of lowBnd. 
	 */
	
	public static double[][][] erode(Nifti1Dataset inputScan, double[][][] data, boolean x, boolean y, boolean z, 
			int erode, float lowBnd, boolean firstHalf, boolean secondHalf) {
		
		String dimensions = "";
		
		System.out.println("Eroding " + erode + " voxels from the surface with "
				+ "intensities greater than " + thresholdStandardDev.round(lowBnd,2) + "...");
		
		writeBoundaries(inputScan);
		
		int XDIM = data[0][0].length-1; int YDIM = data[0].length-1; int ZDIM = data.length-1;
		
		if (x && firstHalf) {
				data = erodeHelper(data, "x", erode, lowBnd, ZDIM, YDIM, XDIM, 0);
				dimensions = " +x,";
		}
		if (x && secondHalf) {
				data = erodeHelper(data, "x", erode, lowBnd, ZDIM, YDIM, XDIM, -(XDIM));
				dimensions = " -x,";
		}
		if (y && firstHalf) {
				data = erodeHelper(data, "y", erode, lowBnd, ZDIM, XDIM, YDIM, 0);
				dimensions = " +y,";
		}
		if (y && secondHalf) {
				data = erodeHelper(data, "y", erode, lowBnd, ZDIM, XDIM, YDIM, -(YDIM));
				dimensions = " -y,";
		}
		if (z && firstHalf) {
				data = erodeHelper(data, "z", erode, lowBnd, XDIM, YDIM, ZDIM, 0);
				dimensions = " +z,";
		}
		if (z && secondHalf) {
				data = erodeHelper(data, "z", erode, lowBnd, XDIM, YDIM, ZDIM, -(ZDIM));
				dimensions = " -z";
		}
		
		System.out.println("Finished erosion for dimensions:" + dimensions); System.out.println("");
		return data; 
		
	}
	
	private static double[][][] erodeHelper(double[][][] data, String dim, int erode, float lowBnd, 
			int a, int b, int c, int distalLen) {
		
		checkParam(erode, c); double val; int count;
		
		for ( int i = 0; i < a; i++ ) {
			for ( int j = 0; j < b; j++ ) {			
				for ( int k = distalLen; k < c/2 - erode && Math.abs(k)+erode >= erode; k++ ) {
					
					if (checkBoundary(data, dim, 0, 0.99, i, j, k) ) {
						count = 0;
						while( count <= erode ) {
							val = pullData(data, dim, i, j, Math.abs(k+count));
							if ( val < lowBnd && val > 0.99 ) {
								pushBounds(dim, i, j,  Math.abs(k+count), true);
								break;
							} else if ( val != 0 && val > lowBnd && !pullBounds(dim, i, j, Math.abs(k+count)) ) {
								data = pushData(data, dim, i, j, Math.abs(k+count), 0);
							}
							count++;
						}
						break;
					}
				}
			}
		}
		
		return data;
		
	}
	
	// ------------------------------------------------------------ //
	
						// VOXEL CLEAN-UP //
	
	// ------------------------------------------------------------ //
	
	/**
	 * @author KJZ 12.19
	 * @param data is nifti intensity data as 3D matrix double[][][].
	 * @param erode is the max width (as integer) of voxels omitted from thresholding to be cleaned.  
	 * @param x, y, z as bool passed by the client to denote which dimension are being processed. 
	 * @param half as bool denotes which half of the scan is being processed
	 * @param simpleCleaner as bool whether or not to only clean 1 voxel wide segments
	 * @return mri intensity data as 3D matrix in double format.
	 * @result extraneous streaks of voxels outside of the scan or lone outliers are removed
	 */
	
	public static double[][][] cleanUp(double[][][] data, int erode, 
			boolean x, boolean y, boolean z, boolean half, boolean simpleClean) {
		
		System.out.println("Cleaning up voxel strands...");
		
		if (simpleClean) {
			System.out.println("Running simple cleaner.");
		} else {
			System.out.println("Not running simple cleaner.");
		}
		
		int XDIM = data[0][0].length-erode; int YDIM = data[0].length-erode; int ZDIM = data.length-erode;
		
		if (x) {
			data = cleanUpHelper(data, "x", erode, simpleClean, ZDIM, YDIM, XDIM, 0);
			data = cleanUpHelper(data, "x", erode, simpleClean, ZDIM, YDIM, XDIM, -(XDIM));
		}
		if (y) {
			data = cleanUpHelper(data, "y", erode, simpleClean, ZDIM, XDIM, YDIM, 0);
			data = cleanUpHelper(data, "y", erode, simpleClean, ZDIM, XDIM, YDIM, -(YDIM));
		}
		if (z) {
			data = cleanUpHelper(data, "z", erode, simpleClean, XDIM, YDIM, ZDIM, 0);
			data = cleanUpHelper(data, "z", erode, simpleClean, XDIM, YDIM, ZDIM, -(ZDIM));
		}
		
		System.out.println("Finished clean-up."); System.out.println("");
		return data; 	
		
	}
	
	private static double[][][] cleanUpHelper(double[][][] data, String dim, int erode, 
			boolean simpleCleaner, int a, int b, int c,  int distalLength) {
		
		checkParam(erode, c);
		
		for ( int i = 0; i < a; i++ ) {
			for ( int j = 0; j < b; j++ ) {
				for (int k = distalLength; k < c/2 - erode && Math.abs(k) > erode; k++) {					
					if (simpleCleaner) {
						data = simpleClean(data, dim, i, j, k);
					} else if (checkBoundary(data, dim, 0, 0.99, i, j, k)) { // finds boundary
						data = erodeClean(data, dim, erode, i, j, k);
						break;
					}
				}
			}
		}
		
		return data;
		
	}
	
	private static double[][][] simpleClean(double[][][] data, String dim, int i, int j, int k) {
		
		if (checkBoundary(data, dim, 0, 0.99, i, j, k) && pullData(data, dim, i, j, Math.abs(k+2)) == 0) {
			data = pushData(data, dim, i, j, Math.abs(k+1), 0);
		}
		return data;
	}
	
	private static double[][][] erodeClean(double[][][] data, String dim, int erode, int i, int j, int k) {
	
		for (int count = 1; count <= erode; count++) { // check for strand of voxels w/ width of "erode"
			if (pullData(data, dim, i, j, Math.abs(k+count)) == 0) {
				for (int l = 1; l <= count; l++) { // if strand is found, make all voxels on strand = 0
					data = pushData(data, dim, i, j, Math.abs(k+l), 0);
				}
				break;
			}
		}
		
		return data;
		
	}
	
	// ------------------------------------------------------------ //
	
					   // PERIPHERY CLEAN-UP //
	
	// ------------------------------------------------------------ //
	
	/**
	 * @author KJZ 2.7.2020
	 * @param data is nifti intensity data as 3D matrix double[][][].
	 * @return mri intensity data as 3D matrix in double[][][] format with any single lines of 0-labeled voxels
	 * @result the periphery of the brain will likely have a non-zero after patchOvershots is run, since
	 * it only works in 2-dimensions, this zeros out all voxels on the outer periphery of the scan,
	 * can probably write patchOvershots more elegantly at some point to render this function deprecated
	 */
	
	public static double[][][] peripheryClean(double[][][] data) {
		
		int XDIM = data[0][0].length-1; int YDIM = data[0].length-1; int ZDIM = data.length-1;
		
		data = peripheryCleanHelper(data, "x", ZDIM, YDIM, 0);
		data = peripheryCleanHelper(data, "x", ZDIM, YDIM, XDIM);
		data = peripheryCleanHelper(data, "y", ZDIM, XDIM, 0);
		data = peripheryCleanHelper(data, "y", ZDIM, XDIM, YDIM);
		data = peripheryCleanHelper(data, "z", XDIM, YDIM, 0);
		data = peripheryCleanHelper(data, "z", XDIM, YDIM, ZDIM);
		
		return data;
		
	}
	
	public static double[][][] peripheryCleanHelper(double[][][] data, String dim, int a, int b, int c) {
		
		for ( int i = 0; i < a; i++ ) {
			for ( int j = 0; j < b; j++ ) {
				data = pushData(data, dim, i, j, c, 0);
			}
		}
		
		return data;
		
	}
	
	// ------------------------------------------------------------ //
	
						// SLICE PATCHING //
	
	// ------------------------------------------------------------ //
	
	/**
	 * @author KJZ 1.7.2020
	 * @param data data is nifti intensity data as 3D matrix double[][][].
	 * @return mri intensity data as 3D matrix in double[][][] format with any single lines of 0-labeled voxels
	 * @result  overshooting the boundary of the brain filled-in with a value (default = 2) and marked as part of the
	 * brain to include in the mask. 
	 */
	
	public static double[][][] patchOvershots(double[][][] data) {
		
		System.out.println("Patching overshots of the pial surface...");
		
		int z = data.length; int y = data[0].length; int x = data[0][0].length;
		double[][][] dataBound = writeZeros(z, y, x);
		
		// TODO: Can maybe cut the second length in half later on?
		// traverses each dimension of the scan
		dataBound = patchSlice(data, patchSlice(data, patchSlice(data, dataBound, 
				"z", x, y, x), "x", z, y, x), "y", z, x, y);
		
		// remaining 0 voxels are in the interior of the brain -> 2
        // outside of the brain "1" voxels are returned to intensity 0.
		for ( int i = 1; i < z; i++ ) {
			for ( int j = 0; j < y; j++ ) {
				for ( int k = 0; k < x; k++ ) {
					if (dataBound[i][j][k] == 0) {
						data[i][j][k] = 2;
					} else if (dataBound[i][j][k] == 1) {
						data[i][j][k] = 0;
					}
				}
			}
		}
		System.out.println("Finished patching."); System.out.println("");
		
		return data;
		
	}
	
	// TODO: might need to come back and implement firstHalf/secondHalf function
	// traverses scan from the opposite corners of each slice
	private static double[][][] patchSlice(double[][][] data, double[][][] dataBound, 
			String dim, int a, int b, int c) {
		
		for ( int i = 0; i < a; i++ ) {
			dataBound = patchSliceHelper(data, patchSliceHelper(data, dataBound, 
					dim, 1, i, 0, 0, b, c), 
					dim, -1, i, b-1, c-1, 0, 0);
			
			dataBound = patchSliceHelper2(data, dim, i, b, c);
		}
		
		return dataBound;
	}
	
	// data <2 -> 1, otherwise reaches boundary and stops traversing the scan
	private static double[][][] patchSliceHelper(double[][][] data, double[][][] dataBound, 
			String dim, int incr, int i, int j, int k, int len1, int len2) {
		
		for (int b = j; b != len1; b+=incr) {
			for (int c = k; c != len2; c+=incr) {
				if (pullData(data, dim, i, b, c) < 2) {
					pushData(dataBound, dim, i, b, c, 1);
				} else if (pullData(data, dim, i, b, c) >= 2) { 
					break;
				}
			}
		}
		
		return dataBound;
		
	}
	
	// 0 voxels inside of the brain -> 2, ignores voxels outside the brain labeled "1"
	private static double[][][] patchSliceHelper2(double[][][] dataBound, String dim, int a, int j, int k) {
		
		for (int b = 0; b < j; b++) {
			for (int c = 0; c < k; c++) {
				if (pullData(dataBound, dim, a, b, c) == 0) {
					dataBound = pushData(dataBound, dim, a, b, c, 2); 
				} 
			}
		}
		
		return dataBound; 
		
	}
	
	// ------------------------------------------------------------ //
	
					   // HELPER METHODS //
	
	// ------------------------------------------------------------ //
	
	/**
	 * Below are helper methods for checking for edge cases and retrieving and 
	 * setting values for data sets regardless of dimensional orientation.
	 */
	
	// retrieves data from the nifti matrix
	private static double pullData(double[][][] data, String dim, int i, int j, int k) {
		
		checkNegative(i, j, k);
		if (dim.equals("x")) {
			return data[i][j][k];
		} else if (dim.equals("y")) {
			return data[i][k][j];
		} else if (dim.equals("z")) {
			return data[k][j][i];
		} else {
			System.out.println("ERROR: no value found, returning 0");
			return 0.0;
			
		}
	}
	
	// sets data to the nifti matrix
	private static double[][][] pushData(double[][][] data, String dim, int i, int j, int k, double push) {
		
		checkNegative(i, j, k);
		if (dim.equals("x")) {
			data[i][j][k] = push;
		} else if (dim.equals("y")) {
			data[i][k][j] = push;
		} else if (dim.equals("z")) {
			data[k][j][i] = push;
		}
		
		return data;
		
	}
	
	// pulls boolean value from the boundary matrix
	private static boolean pullBounds(String dim, int i, int j, int k) {
		
		checkNegative(i, j, k);
		if (dim.equals("x")) {
			return BOUNDARIES[i][j][k];
		} else if (dim.equals("y")) {
			return BOUNDARIES[i][k][j];
		} else if (dim.equals("z")) {
			return BOUNDARIES[k][j][i];
		} else {
			System.out.println("ERROR: No value found, returning false.");
			return false;
		}
	}
	
	// sets boolean value to the boundary matrix
	private static void pushBounds(String dim, int i, int j, int k, boolean val) {
		
		checkNegative(i, j, k);
		if (dim.equals("x")) {
			BOUNDARIES[i][j][k] = val;
		} else if (dim.equals("y")) {
			BOUNDARIES[i][k][j] = val;
		} else if (dim.equals("z")) {
			BOUNDARIES[k][j][i] = val;
		} else {
			System.out.println("We shouldn't reach this statement, no correct dimension passed.");
		}
	}
	
	// checks if the following voxel matches boundary properties
	private static boolean checkBoundary(double[][][] data, String dim, double val1, double val2, int i, int j, int k) {
		// usually val1 == 0, val2 == 0.99
		if (pullData(data, dim, i, j, Math.abs(k)) == val1 && pullData(data, dim, i, j, Math.abs(k+1)) > val2) { 
			return true; 
		} else {
			return false; 
		}
	}
	
	// checks parameters passed to the function (** Update at some point to handle null, varying sizes etc. **)
	private static void checkParam(int param, int length) {
		if (param > length/2) {
			System.out.println("ERROR: Parameter cannot be greater than half of the desired dimension. (" + length/2 + ")");
			System.exit(1);
		}
		if (param < -1) {
			System.out.println("ERROR: Parameter " + param + " passed to the argument cannot be negative.");
			System.exit(1);
		}
	}
	
	// allows for copying and returning nifti intensity data without mutating original data
	public static double[][][] robustDataCopy(double[][][] data) {
		
		double[][][] newData = new double[data.length][data[0].length][data[0][0].length];
		
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[0].length; j++) {
				for (int k = 0; k < data[0][0].length; k++) {
					newData[i][j][k] = data[i][j][k];
				}
			}
		}
		
		return newData;
		
	}
	
	// used for troubleshooting
	private static void printGrid(Double[][] data) {
		for (int i = 0; i < data.length; i++) {
			System.out.println(data[i][0] + " ");
			for (int j = 0; j < data[0].length; j++) {
				System.out.print(data[i][j] + " ");
			}
		}
	}
	
	//copying Float[] array objects to float[]
	private static float[] copyArray(ArrayList<Float> fltObj) {
		
		float[] floatArray = new float[fltObj.size()]; int count = 0;	
		for (Float x : fltObj) {
			floatArray[count++] = x;
		}
		
		return floatArray;
	}
	
	/** // checks dimensions of the matrix passed to the function
	private static boolean checkLen(int i, int j, int k) {
		if (i < 4 || j < 4 || k < 4) {
			System.out.println("ERROR: Integers + " + i + "][" + j + "][" + k + "] matrix needs minimum of 4x4x4 dimensions. Exiting...");
			System.exit(1);
		}
		return true;
	}*/
	
	// ensures values passed to an array are not negative, throws error and exits otherwise
	private static boolean checkNegative(int i, int j, int k) {
		if (i < 0 || j < 0 || k < 0) {
			System.out.println("ERROR: Integers + " + i + "][" + j + "][" + k + "] passed to the array are negative! Exiting...");
			System.exit(1);
		}
		
		return true;
		
	}
	
	// ------------------------------------------------------------ //
	
}
