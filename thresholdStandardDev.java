
public class thresholdStandardDev {
	
	public static double LINEVAL = 0; // tracks total intensity for the first half of the scan
	public static double LINEVAL2 = 0;// tracks total intensity opposite half of each dimension of the scan
	public static int COUNT = 0; // keeps track of the number of non-zero voxels
	public static double[] AVGDELTA = new double[6]; // stores avgs of each half: [X lh, X rh, Y sup, Y inf, Z ant, Z post]

	/** 
	 * @author KJZ 12.22.2019
	 * @param Takes nifti scan data as 3D matrix of double[][][]. 
	 * @param Takes x, y, z dimension to be processed (true) or omitted (false) as boolean. 
	 * @param Takes standard deviation as a double val. Lowering the standard dev threshold lowers the boundary 
	 * that the voxels are masked out, assuming that the scan is a T1 weighted volume. TODO: Allow inversion 
	 * to work with T2-weighted scans. 
	 * @param Takes bound as int, corresponds to how the scan should be divided up to calculate the 
	 * stDev. (e.g, 2 = halves, 3 = thirds etc)
	 * @param Takes voxelBound as double, what intensity the gradient should halt calculation at. 
	 * @returns the altered data passed to findGradient() as a matrix of doubles to be written to a new nifti scan.  
	 */

	public static double[][][] findGradient(double[][][] data, boolean x, boolean y, boolean z, 
		double stdev, int bound, double voxelBound, boolean firstHalf, boolean secondHalf) {
		
		int XDIM = data[0][0].length; int YDIM = data[0].length; int ZDIM = data.length;
		
		System.out.println(""); System.out.println
		("X, Y, Z Dimensions are: " + XDIM + " x " + YDIM + " x " + ZDIM); 
		System.out.println("");
		
		if (x) {
			data = thrshldOutliers(data, findGradientHelper(data, ZDIM, YDIM, XDIM, bound, "x"),
					ZDIM, YDIM, 0, 1, 0, bound, "x", stdev, XDIM/bound);
			data = thrshldOutliers(data, findGradientHelper(data, ZDIM, YDIM, XDIM, bound, "x"),
					ZDIM, YDIM, XDIM, -1, 1, bound, "x", stdev, XDIM-(XDIM/bound));
		}
		if (y) {
			data = thrshldOutliers(data, findGradientHelper(data, ZDIM, XDIM, YDIM, bound, "y"),
					ZDIM, XDIM, 0, 1, 0, bound, "y", stdev, YDIM/bound);
			data = thrshldOutliers(data, findGradientHelper(data, ZDIM, XDIM, YDIM, bound, "y"),
					ZDIM, XDIM, YDIM, -1, 1, bound, "y", stdev, YDIM-(YDIM/bound));
		}
		if (z) {
			data = thrshldOutliers(data, findGradientHelper(data, XDIM, YDIM, ZDIM, bound, "z"),
					XDIM, YDIM, 0, 1, 0, bound, "z", stdev, ZDIM/bound);
			data = thrshldOutliers(data, findGradientHelper(data, XDIM, YDIM, ZDIM, bound, "z"),
					XDIM, YDIM, ZDIM, -1, 1, bound, "z", stdev, ZDIM-(ZDIM/bound));
		}
		
		/** matrices for storing the standard deviation for each row in a grid representing both sides of the scan
		if (x) {
			data = traverseData(data, findGradientHelper(data, ZDIM, YDIM, XDIM, bound, "x"), 
				ZDIM, YDIM, XDIM, bound, voxelBound, "x", stdev, firstHalf, secondHalf);
		}
		if (y) {
			data = traverseData(data, findGradientHelper(data, ZDIM, XDIM, YDIM, bound, "y"), 
				ZDIM, XDIM, YDIM, bound, voxelBound, "y", stdev, firstHalf, secondHalf);
		}
		if (z) {
			data = traverseData(data, findGradientHelper(data, XDIM, YDIM, ZDIM, bound, "z"), 
				XDIM, YDIM, ZDIM, bound, voxelBound, "z", stdev, firstHalf, secondHalf);
		} */
		
		// TODO: Double-check that the anatomy UPDATE: anterior and posterior are mixed up, 
		// this could also be b/c the scans are in an 
		// odd orientation. Need to test this on a scan with normal orientation. 
		
		System.out.println("Average gradient delta in each dimension: ");
		System.out.println("	Left = " + round(AVGDELTA[0],3) + "     Right = " + round(AVGDELTA[1],3));
		System.out.println("	Dorsal = " + round(AVGDELTA[2],3) + "   Ventral = " + round(AVGDELTA[3],3));
		System.out.println("	Anterior = " + round(AVGDELTA[4],3) + " Posterior = " + round(AVGDELTA[5],3));
		System.out.println("");
		
		return data;
	}
	
	/** 
	 * @author KJZ
	 * @param Helper method for FindGradient takes scan data as 3-dimensional matrix of doubles. 
	 * @param Takes int as a, b, c, corresponding to the dimensions of the scan. 
	 * @param Takes a string as whether we are traversing the x, y, or z dimensions of the scan.
	 * @returns 3D matrix of nifti data as double[][][]. Columns of data correspond to the first and 
	 * second halves of the scan being processed and contain a grid of gradients corresponding to each row of data.  
	*/

	public static double[][][] findGradientHelper(double[][][] data, int a, int b, int c, int bound, String dimension) {

		// two gradients for moving toward the center of the scan and two measures of averages for each dimension
		double[][][] gradients = new double[2][a][b]; double[][][] averages = new double[2][a][b];

		// for storing residuals to calculate the standard deviation of a row in each dimension
		double[] residuals = new double[c/bound+1]; double[] residuals2 = new double[c/bound+1];
		
		COUNT = 0;
		
		for (int i = 0; i < a; i++) {
			for (int j = 0; j < b; j++) {
				gradients[0][i][j] = findGrdnt(data, residuals, dimension, i, j, 0, 1, c/bound);
				gradients[1][i][j] = findGrdnt(data, residuals2, dimension, i, j, c, -1, (c-(c/bound)) );
				
				/**
				for (int k = 0; k < c/bound; k++) {
					//residuals = calcResiduals(data, residuals, true, i, j, k, k, dimension);
					
					val = pullData(data, dimension, i, j, k);
					if (val == 0) {
						residuals[COUNT] = (pullData(data, dimension, i, j, k+1) - val);
						LINEVAL += residuals[COUNT];
						COUNT++;
					}
				}
				COUNT = 0;
				averages[0][i][j] = takeAverage(residuals);
				gradients[0][i][j] = standardDeviation(residuals, averages[0][i][j]);
	
				for (int k = c - 1; k > (c - c/bound); k--) {
					//residuals2 = calcResiduals(data, residuals2, false, i, j, k-1, (c-k-1), dimension);
					
					val = pullData(data, dimension, i, j, k);
					if (val == 0) {
						residuals[COUNT] = (pullData(data, dimension, i, j, k-1) - val);
						LINEVAL2 += residuals[COUNT];
						COUNT++;
					}
				} 
				COUNT = 0;
				averages[1][i][j] = takeAverage(residuals2);
				gradients[1][i][j] = standardDeviation(residuals2, averages[1][i][j]); */
				
			}
		}
		
		setAvgDelta(a, b, c, dimension, bound);
		LINEVAL = 0;
		LINEVAL2 = 0;
		
		return gradients;
	}
	
	public static double findGrdnt(double[][][] data, double[] resid, String dim, int i, int j, int k, int incr, int len) {
		resid = findResid(data, resid, dim, i, j, k, incr, len);
		COUNT = 0;
		return standardDeviation(resid, takeAvg(resid));
	}
	
	public static double[] findResid(double[][][] data, double[] resid, String dim, int i, int j, int k, int incr, int len) {
		double val;
		while (k != len-incr) {
			val = pullData(data, dim, i, j, k);
			if (val != 0) {
				resid[COUNT] = (pullData(data, dim, i, j, k+incr) - val);
				if (incr > 0) {
					LINEVAL += resid[COUNT];
				} else if (incr < 0) {
					LINEVAL2 += resid[COUNT];
				}
				COUNT++;
			}
			k+=incr;
		}
		return resid;
	}
	
	/** 
	 * @param Nifti scan data as a 3D matrix of double[][][] values. 
	 * @param Array of residuals as double[] for deriving the standard dev and average from. 
	 * @param Forward/backward will equal 0 or 1 as int depending on which half of the scan dimension is being traversed.
	 * @param i, j, k, locate given voxel value on the scan in the 3D matrix at data[i][j][k] e.g..
	 * @param residValue is what half of the scan is where the value is being stored as int.
	 * @param Dimension denotes x, y, or z dimension of the scan as string.  
	 * @returns The gradient of the scan with the standard deviation at each voxel location.  

	public static double[] calcResiduals(double[][][] data, double[] residuals, 
			boolean forward, int i, int j, int k, int residValue, String dimension) {

		if (dimension.equals("x")) { // for x dimension
			if (data[i][j][k] != 0) {	
				if (forward) {
					LINEVAL += (data[i][j][k+1] - data[i][j][k]);
					residuals[COUNT] = (data[i][j][k+1] - data[i][j][k]);
				} else {
					LINEVAL2 += (data[i][j][k] - data[i][j][k-1]);
					residuals[COUNT] = (data[i][j][k] - data[i][j][k-1]);
				}
				COUNT++;
			}
			
		} else if (dimension.equals("y")) { // for y dimension
			if (data[i][k][j] != 0) {
				if (forward) {
					LINEVAL += (data[i][k+1][j] - data[i][k][j]);
					residuals[COUNT] = (data[i][k+1][j] - data[i][k][j]);
				} else {
					LINEVAL2 += (data[i][k][j] - data[i][k-1][j]);
					residuals[COUNT] = (data[i][k][j] - data[i][k-1][j]);
				}
				COUNT++;
			}
			
		} else if (dimension.equals("z")) { // for z dimension
			if (data[k][j][i] != 0 ) {
				if (forward) {
					LINEVAL += (data[k+1][j][i] - data[k][j][i]);
					residuals[COUNT] = (data[k+1][j][i] - data[k][j][i]);
				} else {
					LINEVAL2 += (data[k][j][i] - data[k-1][j][i]);
					residuals[COUNT] = (data[k][j][i] - data[k-1][j][i]);
				}	
				COUNT++;
			}
			
		} else {
			residuals[COUNT] = 0.0;
		}
		return residuals;
	} */

	/** 
	 * @param residuals are single double array of voxels from mri data, 
	 * @param The average intensity of the line of voxel data as double
	 * @returns the standard deviation of the voxel residuals
	 */
	public static double standardDeviation(double[] residuals, double average) {
		
		double standardDev = 0; int power = 1; int n = 0;
		
		for (int counter = 0; counter < residuals.length; counter++) {
			if (residuals[counter] != 0) {
				standardDev += ((residuals[counter]-average)*(residuals[counter]-average));
				n++;
			}
		}
		return Math.sqrt((standardDev/(n - power)));
	}
	
	public static double takeAvg(double[] resid) {
		
		double avg = 0; int n = 0;
		
		for (int counter = 0; counter < resid.length; counter++) {
			if (resid[counter] != 0) {
				avg += avg;
				n++;
			}
		}
		return avg/n;
	}

	/**
	 * @param Takes dimensions x, y, z as integers corresponding to a, b, and c. 
	 * @param Takes which dimension is being measured (x,y,z) as string
	 * @param Bound integer is the dimension by which the scan is divided into (halves = 2, thirds = 3 etc.) as int'
	 * @result Sets the average change in dimension to the global array of averages AVGDELTA.
	 */
	public static void setAvgDelta(int a, int b, int c, String dimension, int bound) {
		
		if (dimension.equals("x")) {
			AVGDELTA[0] = Math.abs(LINEVAL/((c/bound) * b * a));
			AVGDELTA[1] = Math.abs(LINEVAL2/((c/bound) * b * a)); 	
			
		} else if (dimension.equals("y")) {
			AVGDELTA[2] = Math.abs(LINEVAL/(c * (b/bound) * a));
			AVGDELTA[3] = Math.abs(LINEVAL2/(c * (b/bound) * a));
			
		} else if (dimension.equals("z")) {
			AVGDELTA[4] = Math.abs(LINEVAL/(c * b * (a/bound)));
			AVGDELTA[5] = Math.abs(LINEVAL2/(c * b * (a/bound)));
			
		} else {
			System.out.println("No dimension specified");
		}
	}
	
	// simpler way of writing traverseData()
	public static double[][][] thrshldOutliers(double[][][] data, double gradient[][][], int a, int b, int c, 
			int incr, int half, double bnd, String dim, double stdev, int len) {
		
		int k;
		if (half == 0) {
			k = 0;
		} else {
			k = c-1;
		}
		
		for (int i = 0; i < a; i++) {
			for (int j = 0; j < b; j++) {
				while (k != len) { // fix this line

					if (voxVal(data, gradient, half, i, j, k, dim, stdev, incr, bnd)) {
						break;
					} else {
						data = pushData(data, i, j, k, dim, 0);
					}
					k+=incr;
				}
			}
		}
		return data;
	}
	
	// a simpler way to write returnVoxel()	
	private static boolean voxVal(double[][][] data, double[][][] gradient, int half, int i, int j, int k, 
			String dim, double stdev, int incr, double bnd) {

		double val = pullData(data, dim, i, j, k);

		// gradient[][][] might not be right
		double val2 = ((pullData(data, dim, i, j, k+incr) - (val - rtnDelta(dim, half))))/gradient[half][i][j]; 
		return voxValHelper(val, val2, stdev, bnd);
	}
	
	private static boolean voxValHelper(double val, double val2, double stdev, double bnd) {
		
		if (val2 > -stdev && val > bnd) {
			return true;
		} else if (val < 1) {
			return false;
		}
		// find out whatSystem.out.println("ERROR: Value calculated is invalid, returning false.");
		return false; 
	} 

		/** 
		 * @author KJZ
		 * @param data is double 3D matrix of T1 mri data.
		 * @param is the mapping of the avg change in each dimension.
		 * @param a, b, c as int correspond to the desired dimensions to traverse the scan.
		 * @param bound as integer corresponds with how the scan should be broken down (i.e. into 4=quarters, 3=thirds, 2=halves etc).
		 * @param voxelBound as double is the given lowest intensity to stop the thresholding at.
		 * @param dim as string x/y/z as to which dim should be processed.
		 * @param stdev as double is how many standard deviations should be the cutoff for outlier intensities denoting 
		 * the outer boundary of the brain (default=2).
		 * @param firstHalf/secondHalf are boolean as to whether or not to process only one half of a given dimension of the brain.
		 * 
		 * @return double as 3D matrix to be written to a new scan with outlier values thresholded out */
		
		// TODO: add average[][][] grid at some point to arguments to correspond with the stdev grid
		/**
		 public static double[][][] traverseData(double[][][] data, double gradient[][][], int a, int b, int c, int bound, 
				double voxelBound, String dim, double stdev, boolean firstHalf, boolean secondHalf) {

			boolean voxelBoundary = false;

			for (int i = 0; i < a; i++) {
				for (int j = 0; j < b; j++) {
					if (firstHalf) {
						for (int k = 0; k < c/bound; k++) {
							voxelBoundary = returnVoxel(data, gradient, true, i, j, k, dim, stdev, voxelBound, voxelBoundary); 
							if (voxelBoundary) {
								break;
							} else {
								data = pushData(data, i, j, k, dim, 0);
							}
						}
						voxelBoundary = false; 
					}
					if (secondHalf) {
						for (int k = c - 1; k > (c - c/bound); k--) {
							voxelBoundary = returnVoxel(data, gradient, false, i, j, k, dim, stdev, voxelBound, voxelBoundary);
							if (voxelBoundary) {
								break; 
							} else {
								data = pushData(data, i, j, k, dim, 0); 
							}
						}
						voxelBoundary = false; 
					}
				}
			}
			return data;
		}
		// Assuming T1 weighted imaging with dark outer boundary, otherwise should be less-than the standard deviation
		// note that stdev is also given as a negative value,
		
		public static boolean returnVoxel(double[][][] data, double[][][] gradient, boolean forward, int i, int j, int k, 
				String dimension, double stdev, double voxelBound, boolean voxelBoundary) {
			
			
			
			if (dimension.equals("x")) { // for x dimension data[i][j][k] != 0
				if (forward) {
					if (((data[i][j][k+1] - data[i][j][k] - AVGDELTA[0]) / gradient[0][i][j]) > -stdev && data[i][j][k] > voxelBound) {
						voxelBoundary = false; 
					} else if (data[i][j][k] > 1) {
						voxelBoundary = true;  
					}
				} else {
					if (((data[i][j][k-1] - data[i][j][k] - AVGDELTA[1]) / gradient[1][i][j]) > -stdev && data[i][j][k] > voxelBound) {
						voxelBoundary = false;
					} else if (data[i][j][k] > 1) {
						voxelBoundary = true;  
					}
				}
			}

			if (dimension.equals("y")) { // for y dimension data[i][k][j] != 0
				if (forward) {
					if ((data[i][k+1][j] - data[i][k][j] - AVGDELTA[2] / gradient[0][i][j]) > -stdev && data[i][k][j] > voxelBound) {
						voxelBoundary = false;
					} else if (data[i][k][j] > 1) {
						voxelBoundary = true; 
					}
				} else {
					if ((data[i][k-1][j] - data[i][k][j] - AVGDELTA[3] / gradient[1][i][j]) > -stdev && data[i][k][j] > voxelBound) {
						voxelBoundary = false; 
					} else if (data[i][k][j] > 1) {
						voxelBoundary = true; 
					}
				} 

			}

			if (dimension.equals("z")) { // for z dimension data[k][j][i] != 0
				if (forward) {
					if ((data[k+1][j][i] - data[k][j][i] - AVGDELTA[4] / gradient[0][i][j]) > -stdev && data[k][j][i] > voxelBound) {
						voxelBoundary = false; 
					}  else if (data[k][j][i] > 1 || data[k][j][i] < voxelBound) {
						voxelBoundary = true; 
					}
				} else {
					if ((data[k-1][j][i] - data[k][j][i] - AVGDELTA[5] / gradient[1][i][j]) > -stdev && data[k][j][i] > voxelBound) {
						voxelBoundary = false; 
					} else if (data[k][j][i] > 1) {
						voxelBoundary = true; 
					}
				} 
			}
			return voxelBoundary;
		} */
		
	// ------------------------------------------------------------ //

	// sets data to the nifti matrix
	public static double[][][] pushData(double[][][] data, int i, int j, int k, String dimension, double val) {
		
		if (dimension.equals("x")) {
			data[i][j][k] = val; 
		} else if (dimension.equals("y")) {
			data[i][k][j] = val; 
		} else if (dimension.equals("z")) { 
			data[k][j][i] = val;
		} else {
			System.out.println("No valid x, y, z dimension argument provided.");		
		}
		return data;
	}
	
	// retrieves data from the nifti matrix
	private static double pullData(double[][][] data, String dim, int i, int j, int k) {

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

	// retrieves data from global AVGDELTA[] based on dimension 	
	public static double rtnDelta(String dim, int half) {
		
		if (dim.equals("x")) {
			return AVGDELTA[0+half];
		} else if (dim.equals("y")) {
			return AVGDELTA[2+half];
		} else if (dim.equals("z")) {
			return AVGDELTA[4+half];
		} else {
			System.out.println("Error, no value found, returning 0");
			return 0.0;
		}
	}
	
	/**
	 * @param data
	 * @param average1
	 * @param average2
	 */
	public static void phaseEncodeNorm(double[][][] data, double average1, double average2) {
		for (int i = 0; i < data.length; i++) {
			for(int j = 0; j < data[i].length; j++) {
				if (average1 < average2) { 
					for (int k = 0; k < data[i][j].length/2; k++) {
						
					}
				} else {
					//for (int k = c - 1; k > (c - c/bound); k--) {
						
					//}
				}
			}
		}
	}
	
	/**
	 * @author KJZ
	 * @param val as double is the value to be rounded
	 * @param places are the number of places the double should be rounded to
	 * @return truncated double value 
	 */
	public static double round(double val, int places) {
		
	    if (places < 0) { 
	    	throw new IllegalArgumentException();
	    }
	    
	    long factor = (long) Math.pow(10, places);
	    val = val * factor;
	    long tmp = Math.round(val);
	    return (double) tmp / factor;
	}
}
