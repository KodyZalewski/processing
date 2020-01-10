
public class smoothVolume {
	
	static boolean BOUNDARIES[][][];
	
	// ------------------------------------------------------------ //
	
	/**
	 * @author KJZ
	 * @param nds is the input nifti dataset volume.
	 * @result Creates an equivalent global variable boolean grid named "BOUNDARIES". 
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
	 * @author KJZ 12.19
	 * @param Takes data as double 3D matrix.
	 * @param int a, b, c represent respective dimensions depending on which dimension (passed as String) is being traversed.
	 * @param length as integer denotes how many voxels parallel to the given dimension to incorporate
	 * into smoothing (default is 1).
	 * @return double 3D matrix to be written to a new scan as a smoothed volume. 
	 * @result The function reduces the number of outliers is reduced across the scan, ergo making thresholding of 
	 * voxels outside of the brain more robust. 
	 * 
	 */

	public static double[][][] movingAverage(double[][][] data, int length) {

		int x = data[0][0].length; int y = data[0].length; int z = data.length; // fix from being [0] at some point

		data = movingAverageHelper(data, z, y, x, length, "x");
		data = movingAverageHelper(data, z, x, y, length, "y");
		data = movingAverageHelper(data, x, y, z, length, "z");
		return data; 
	}
	
	private static double[][][] movingAverageHelper(double[][][] data, int a, int b, int c, int length, String dim) {
		for ( int i = length; i < a; i++ ) {
			for ( int j = length; j < b; j++ ) { 
				for ( int k = length; k < c - length; k++ ) {
					double newData = 0; 
					for ( int l = 0; l < length; l++ ) {
						newData += (pullData(data, dim, i, j, k, l) + pullData(data, dim, i, j, k, -l));
					}
					data = pushData(data, dim, i, j, k, 0, (newData + pullData(data, dim, i, j, k, 0))/((length * 2) + 1));
				}
			}
		}
		return data; 
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
			data = erodeHelper(data, "x", erode, lowBound, z, y, x, 1, 1);
			data = erodeHelper(data, "x", erode, lowBound, z, y, x, -(x), -1);
		}
		if (doY) {
			data = erodeHelper(data, "y", erode, lowBound, z, x, y, 1, 1);
			data = erodeHelper(data, "y", erode, lowBound, z, x, y, -(y), -1);
		}
		if (doZ) {
			data = erodeHelper(data, "z", erode, lowBound, x, y, z, 1, 1);
			data = erodeHelper(data, "z", erode, lowBound, x, y, z, -(z), -1);
		}
		
		return data; 
		
	}
	
	private static double[][][] erodeHelper(double[][][] data, String dim, int erode, int lowBound, 
			int a, int b, int c, int distalLength, int incr) {
		
		checkParam(erode, c);
		int k; double val; int count = 0;
		
		for ( int i = 0; i < a; i++ ) {
			for ( int j = 0; j < b; j++ ) {
				k = distalLength;			
				while ( k < c/2 - erode && Math.abs(k) > erode ) {
					
					if ( pullData(data, dim, i, j, Math.abs(k), 0) == 0 && pullData(data, dim, i, j, Math.abs(k), incr) > 0.99 ) {
						count = 0;
						while( Math.abs(count) <= erode ) {
							val = pullData(data, dim, i, j, Math.abs(k), count);
							if ( val < lowBound && val > 0.99 ) {
								pushBounds(dim, i, j,  Math.abs(k), count, true);
								break;
							} else if ( val != 0 && val > lowBound && !pullBounds(dim, i, j, Math.abs(k), count) ) {
								data = pushData(data, dim, i, j, Math.abs(k), count, 0);
							}
							count = count;
						}
						break;
					}
					k++;
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
	
	public static double[][][] cleanUp(double[][][] data, int erode, boolean doX, boolean doY, boolean doZ, boolean half) {
		
		int x = data[0][0].length-1; int y = data[0].length-1; int z = data.length-1;
		boolean simpleCleaner = true;
		
		
		if (doX) {
			data = cleanUpHelper(data, "x", erode, simpleCleaner, z, y, x, 1, 1);
			data = cleanUpHelper(data, "x", erode, simpleCleaner, z, y, x, -(x), 1);
		}
		if (doY) {
			data = cleanUpHelper(data, "y", erode, simpleCleaner, z, x, y, 1, 1);
			data = cleanUpHelper(data, "y", erode, simpleCleaner, z, x, y, -(y), 1);
		}
		if (doZ) {
			data = cleanUpHelper(data, "z", erode, simpleCleaner, x, y, z, 1, 1);
			data = cleanUpHelper(data, "z", erode, simpleCleaner, x, y, z, -(z), 1);
		}
		return data; 	
	}
	
	private static double[][][] cleanUpHelper(double[][][] data, String dim, int erode, boolean simpleCleaner, int a, int b, int c,  int distalLength, int incr) {
		
		checkParam(erode, c);
		int k; int l;
		
		for ( int i = 0; i < a; i++ ) {
			for ( int j = 0; j < b; j++ ) {
				k = distalLength;
				while (k < c/2 - erode && Math.abs(k) > erode) {
					if (simpleCleaner) {
						simpleClean(data, dim, i, j, Math.abs(k));
					} else if ( pullData(data, dim, i, j, Math.abs(k), 0) == 0 && pullData(data, dim, i, j, Math.abs(k)) > 0.99 ) { // finds boundary
						for ( int count = incr; Math.abs(count) <= erode; count+=incr ) { // check for strand of voxels w/ width of "erode"
							if (pullData(data, dim, i, j, Math.abs(k+count)) == 0) {
								l = incr;
								while ( Math.abs(l) <= Math.abs(count) ) { // if strand is found, make all voxels on strand = 0
									data = pushData(data, dim, i, j, Math.abs(k+l), 0);
									l = l;
								}
								break;
							}
						}
						break;
					}
					k++;
				}
			}
		}
		return data;
	}
	
	private static double[][][] simpleClean(double[][][] data, String dim, int i, int j, int k) {
		if (pullData(data, dim, i, j, Math.abs(k), 0) == 0 && pullData(data, dim, i, j, Math.abs(k), incr) > 0.99 
				&& pullData(data, dim, i, j, Math.abs(k), incr) == 0) {
			data = pushData(data, dim, i, j, Math.abs(k), incr, 0);
		}
		return data;
	}
	
	// ------------------------------------------------------------ //
	
	/**
	 * @author KJZ 1.7.2020
	 * @param data data is nifti intensity data as 3D matrix double[][][].
	 * @param length is the depth to explore whether or not there is an overshot judging from the surrounding non-zero voxels
	 * @param fill is the intensity that should fill in the overshot artefacts,
	 * @param doX, doY, doZ as bool passed by the client to denote which dimension are being processed. 
	 * @return mri intensity data as 3D matrix in double[][][] format with any single lines of 0-labeled voxels
	 * overshooting the boundary of the brain filled-in with a value given by the client as "fill".
	 * @result  
	 */
	
	public static double[][][] patchOvershots(double[][][] data, int length, double fill, boolean doX, boolean doY, boolean doZ) {
		
		int x = data[0][0].length-length+1; int y = data[0].length-length+1; int z = data.length-length+1;
		
		if (doX) {
			data = patchOvershotsHelper(data, "x", length, fill, z, y, x, 0, 1);
			data = patchOvershotsHelper(data, "x", length, fill, z, y, x, -(x), -1);
		}
		if (doY) {
			data = patchOvershotsHelper(data, "y", length, fill, z, x, y, 0, 1);
			data = patchOvershotsHelper(data, "y", length, fill, z, x, y, -(y), -1);
		}
		if (doZ) {
			data = patchOvershotsHelper(data, "z", length, fill, x, y, z, 0, 1);
			data = patchOvershotsHelper(data, "z", length, fill, x, y, z, -(z)+1, -1);
		}
		return data;
	}
	
	private static double[][][] patchOvershotsHelper(double[][][] data, String dim, int length, double fill, int a, int b, int c,  int distalLength, int incr) {
	
		int k; int count = 0;
		
		for ( int i = 1; i < a; i++ ) {
			for ( int j = 1; j < b; j++ ) {
				k = distalLength;
				while( k < c/2 - length && Math.abs(k) > length) {
					
					if (pullData(data, dim, i, j, Math.abs(k)) > 0.99) {
						break;
					} else if (patchOvershotsHelper2(pullData(data, dim, i, j, Math.abs(k)), pullData(data, dim, i+1, j+1, Math.abs(k)), 
							pullData(data, dim, i+1, j-1, Math.abs(k)), pullData(data, dim, i-1, j+1, Math.abs(k)), pullData(data, dim, i-1, j-1, Math.abs(k)))) {
						count++;
					}
					if (count == length) {
						count = 0;
						while ( pullData(data, dim, i, j, Math.abs(k+count)) < 1 && Math.abs(count) > 0 && Math.abs(count) < c/2-length) { // should have tmp variable as placeholder here
							data = pushData(data, dim, i, j, k, fill) ;
							count+=incr;
						}
					}
				}
				count = 0;
				k++;
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
	
	public static double pullData(double[][][] data, String dim, int i, int j, int k) {
		if (i < 0 || j < 0 || k < 0) {
			System.out.println("ERROR: integer passed to the dimensional array at: dim=" + dim + " [" + i + "][" + j + "][" + k + "] is negative! Exiting...");
			System.exit(1);
		}
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
	
	public static double[][][] pushData(double[][][] data, String dim, int i, int j, int k, double push) {
		if (i < 0 || j < 0 || k < 0) {
			System.out.println("ERROR: integer passed to the dimensional array at: dim=" + dim + " [" + i + "][" + j + "][" + k + "] is negative! Exiting...");
			System.exit(1);
		}
		if (dim.equals("x")) {
			data[i][j][k] = push;
		} else if (dim.equals("y")) {
			data[i][k][j] = push;
		} else if (dim.equals("z")) {
			data[k][j][i] = push;
		}
		return data;
	}
	
	public static boolean pullBounds(String dim, int i, int j, int k) {
		if (i < 0 || j < 0 || k < 0) {
			System.out.println("Error, integer passed to the boundary array is negative!");
			System.exit(1);
		}
		if (dim.equals("x")) {
			return BOUNDARIES[i][j][k];
		} else if (dim.equals("y")) {
			return BOUNDARIES[i][k][j];
		} else if (dim.equals("z")) {
			return BOUNDARIES[k][j][i];
		} else {
			System.out.println("Error, no value found, returning false");
			return false;
		}
	}
	
	public static void pushBounds(String dim, int i, int j, int k, boolean value) {
		if (i < 0 || j < 0 || k < 0) {
			System.out.println("Error, integer passed to the boundary array is negative!");
			System.exit(1);
		}
		if (dim.equals("x")) {
			BOUNDARIES[i][j][k] = value;
		} else if (dim.equals("y")) {
			BOUNDARIES[i][k][j] = value;
		} else if (dim.equals("z")) {
			BOUNDARIES[k][j][i] = value;
		}
	}
	
	public static void checkParam(int param, int length) {
		if (param > length/2) {
			System.out.println("ERROR: Parameter cannot be greater than half of the desired dimension. (" + length/2 + ")");
			System.exit(1);
		}
		if (param < -1) {
			System.out.println("ERROR: Parameter " + param + " passed to the argument cannot be negative.");
			System.exit(1);
		}
	}
	
	// ------------------------------------------------------------ //
	
}
