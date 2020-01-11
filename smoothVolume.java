
public class smoothVolume {
	
	static boolean BOUNDARIES[][][];
	
	// TODO: alter public methods to handle opposite dimensions independently
	// TODO: don't set all values of k to Math.abs() every single time, needlessly convoluted
	// TODO: double-check loop counters
	
	// ------------------------------------------------------------ //
	
	/**
	 * @author KJZ
	 * @param nds is the input nifti dataset volume.
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
	
	/**
	 * @author KJZ 12.19
	 * @param Takes data as double 3D matrix.
	 * @param int a, b, c represent respective dimensions depending on which dimension (as string) is being traversed.
	 * @param length as integer denotes how many voxels parallel to the given dimension to incorporate into smoothing (default is 1).
	 * @return double 3D matrix to be written to a new scan as a smoothed volume. 
	 * @result The function reduces the number of outliers is reduced across the scan, ergo making thresholding of 
	 * voxels outside of the brain more robust. 
	 */

	public static double[][][] movingAverage(double[][][] data, int length) {

		int x = data[0][0].length; int y = data[0].length; int z = data.length; // fix from being [0] at some point

		data = movingAverageHelper(data, z, y, x, length, "x");
		data = movingAverageHelper(data, z, x, y, length, "y");
		data = movingAverageHelper(data, x, y, z, length, "z");
		return data; 
	}
	
	private static double[][][] movingAverageHelper(double[][][] data, int a, int b, int c, int length, String dim) {
		double newData;
		for ( int i = length; i < a; i++ ) {
			for ( int j = length; j < b; j++ ) { 
				for ( int k = length; k < c - length; k++ ) {
					newData = 0; 
					for ( int l = 0; l < length; l++ ) {
						newData += (pullData(data, dim, i, j, k+l) + pullData(data, dim, i, j, k-l));
					}
					data = pushData(data, dim, i, j, k, (newData + pullData(data, dim, i, j, k))/((length * 2) + 1));
				}
			}
		}
		return data; 
	}
	
	// ------------------------------------------------------------ //
	
	/**
	 * @author KJZ 1.10.20
	 * @param Takes data as double 3D matrix.
	 * @param int a, b, c represent respective dimensions depending on which dimension (as string) is being traversed.
	 * @param depth is integer as to how many voxels deep should be taken on the periphery of the scan to 
	 * obtain an average estimation. (3 is probably a robust number)
	 * @result finds the average of the most peripheral voxels in dimension
	 * passed to the function. This aids with normalizing each dimension appropriately. 
	 */
	public static double outerAverage(double[][][] data, String dim, int depth, int a, int b, int c) {
		
		int voxCount = 0; double avg = 0; double tmp;
		
		for (int i = 0; i < a; i++) {
			for(int j = 0; j < b; j++) {
				for (int k = c; k < c/2 && Math.abs(k) > 0; k++) {
					if (checkBoundary(data, dim, 0, 0.99, i, j, k)) {
						for (int l = 0; l < depth; l++) {
							tmp = pullData(data, dim, i, j, k+l);
							if (tmp > 0.99) {
								voxCount++;
								avg+=tmp;
							}
						}
					}
				}
			}
		}
		return avg/voxCount;
	}
	
	// ------------------------------------------------------------ //
	
	/**
	 * @author KJZ 12.19
	 * @param data is nifti intensity data as 3D matrix double[][][]. 
	 * @param erode is the max length (as integer) of voxels to be eroded from the edge of the scan.
	 * @param lowBound is int denoting the lowest intensity threshold to reach before stopping. 
	 * @param doX, doY, doZ as bool passed by the client to denote which dimension are being processed. 
	 * @param half denotes which half of the scan is being traversed (-dim if !half, otherwise +dim).
	 * @return eroded mri intensity data as 3D matrix in double format
	 * @result The number of voxels provided by the client around the edge of the scan are thresholded out
	 * provided they are above the intensity of lowBound. 
	 */
	public static double[][][] erode(Nifti1Dataset inputScan, double[][][] data, int erode, int lowBound, 
			boolean doX, boolean doY, boolean doZ, boolean half) {
		
		writeBoundaries(inputScan);
		
		int x = data[0][0].length-1; int y = data[0].length-1; int z = data.length-1;
		
		if (doX) {
			data = erodeHelper(data, "x", erode, lowBound, z, y, x, 0);
			data = erodeHelper(data, "x", erode, lowBound, z, y, x, -(x));
		}
		if (doY) {
			data = erodeHelper(data, "y", erode, lowBound, z, x, y, 0);
			data = erodeHelper(data, "y", erode, lowBound, z, x, y, -(y));
		}
		if (doZ) {
			data = erodeHelper(data, "z", erode, lowBound, x, y, z, 0);
			data = erodeHelper(data, "z", erode, lowBound, x, y, z, -(z));
		}
		
		return data; 
		
	}
	
	private static double[][][] erodeHelper(double[][][] data, String dim, int erode, int lowBound, 
			int a, int b, int c, int distalLength) {
		
		checkParam(erode, c); double val; int count;
		
		for ( int i = 0; i < a; i++ ) {
			for ( int j = 0; j < b; j++ ) {			
				for ( int k = distalLength; k < c/2 - erode && Math.abs(k)+erode >= erode; k++ ) {
					
					if (checkBoundary(data, dim, 0, 0.99, i, j, k) ) {
						count = 0;
						while( count <= erode ) {
							val = pullData(data, dim, i, j, Math.abs(k+count));
							if ( val < lowBound && val > 0.99 ) {
								pushBounds(dim, i, j,  Math.abs(k+count), true);
								break;
							} else if ( val != 0 && val > lowBound && !pullBounds(dim, i, j, Math.abs(k+count)) ) {
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
	
	/**
	 * @author KJZ 12.19
	 * @param data data is nifti intensity data as 3D matrix double[][][].
	 * @param erode is the max width (as integer) of voxels omitted from thresholding to be cleaned.  
	 * @param doX, doY, doZ as bool passed by the client to denote which dimension are being processed. 
	 * @return mri intensity data as 3D matrix in double format.
	 * @result extraneous streaks of voxels outside of the scan or lone outliers that are on the periphery of the scan are removed. 
	 */
	
	public static double[][][] cleanUp(double[][][] data, int erode, boolean doX, boolean doY, boolean doZ, boolean half, boolean simpleCleaner) {
		
		int x = data[0][0].length-erode; int y = data[0].length-erode; int z = data.length-erode;
		
		if (doX) {
			data = cleanUpHelper(data, "x", erode, simpleCleaner, z, y, x, 0);
			data = cleanUpHelper(data, "x", erode, simpleCleaner, z, y, x, -(x));
		}
		if (doY) {
			data = cleanUpHelper(data, "y", erode, simpleCleaner, z, x, y, 0);
			data = cleanUpHelper(data, "y", erode, simpleCleaner, z, x, y, -(y));
		}
		if (doZ) {
			data = cleanUpHelper(data, "z", erode, simpleCleaner, x, y, z, 0);
			data = cleanUpHelper(data, "z", erode, simpleCleaner, x, y, z, -(z));
		}
		
		return data; 	
	}
	
	private static double[][][] cleanUpHelper(double[][][] data, String dim, int erode, boolean simpleCleaner, int a, int b, int c,  int distalLength) {
		
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
	
	/**
	 * @author KJZ 1.7.2020
	 * @param data data is nifti intensity data as 3D matrix double[][][].
	 * @param depth is the depth to explore whether or not there is an overshot judging from the surrounding non-zero voxels
	 * @param fill is the intensity that should fill in the overshot artefacts,
	 * @param doX, doY, doZ as bool passed by the client to denote which dimension are being processed. 
	 * @return mri intensity data as 3D matrix in double[][][] format with any single lines of 0-labeled voxels
	 * @result  overshooting the boundary of the brain filled-in with a value given by the client as "fill" when there are streaks
	 * defined by "length".
	 */
	
	public static double[][][] patchOvershots(double[][][] data, int depth, double fill, boolean doX, boolean doY, boolean doZ) {
		
		int x = data[0][0].length-depth+1; int y = data[0].length-depth+1; int z = data.length-depth+1;
		
		if (doX) {
			data = patchOvershotsHelper(data, "x", depth, fill, z, y, x, 0);
			data = patchOvershotsHelper(data, "x", depth, fill, z, y, x, -(x));
		}
		if (doY) {
			data = patchOvershotsHelper(data, "y", depth, fill, z, x, y, 0);
			data = patchOvershotsHelper(data, "y", depth, fill, z, x, y, -(y));
		}
		if (doZ) {
			data = patchOvershotsHelper(data, "z", depth, fill, x, y, z, 0);
			data = patchOvershotsHelper(data, "z", depth, fill, x, y, z, -(z));
		}
		
		return data;
	}
	
	private static double[][][] patchOvershotsHelper(double[][][] data, String dim, int depth, double fill, int a, int b, int c,  int distalLength) {
	
		int count = 0;
		
		for ( int i = 1; i < a; i++ ) {
			for ( int j = 1; j < b; j++ ) {		
				for (int k = distalLength+1; k < c/2 - depth && Math.abs(k)+depth > depth; k++) {
					
					if (pullData(data, dim, i, j, Math.abs(k)) > 0.99) {
						break;
					} else if (patchOvershotsHelper2(pullData(data, dim, i, j, Math.abs(k)), pullData(data, dim, i+1, j, Math.abs(k)), 
							pullData(data, dim, i, j+1, Math.abs(k)), pullData(data, dim, i-1, j, Math.abs(k)), pullData(data, dim, i, j-1, Math.abs(k)))) {
						count++;
					}
					if (count == depth) {
						count = 0;
						while (pullData(data, dim, i, j, Math.abs(k+count)) < 1 && Math.abs(k+count) < c/2) { 
							data = pushData(data, dim, i, j, Math.abs(k+count), fill) ;
							count++;
						}
						break;
					}
				}
				count = 0;
			}
		}
		return data;
	}
	
	private static boolean patchOvershotsHelper2(double v, double w, double x, double y, double z) {
		if (v < 0.99 && w > 0.99 && x > 0.99 && y > 0.99 && z > 0.99) {
			return true;
		}
		return false; 
	}

	// ------------------------------------------------------------ //
	
	/**
	 * @author KJZ
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
			System.out.println("Error, no value found, returning 0");
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
	private static void pushBounds(String dim, int i, int j, int k, boolean value) {
		
		checkNegative(i, j, k);
		if (dim.equals("x")) {
			BOUNDARIES[i][j][k] = value;
		} else if (dim.equals("y")) {
			BOUNDARIES[i][k][j] = value;
		} else if (dim.equals("z")) {
			BOUNDARIES[k][j][i] = value;
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
