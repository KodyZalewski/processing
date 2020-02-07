
public class thresholdStandardDev {
	
	public static double LINEVAL = 0; // tracks total intensity for the first half of the scan
	public static double LINEVAL2 = 0;// tracks total intensity opposite half of each dimension of the scan
	public static int COUNT = 0; // keeps track of the number of non-zero voxels
	public static double[] AVGDELTA = new double[6]; // stores avgs of each half: [X lh, X rh, Y sup, Y inf, Z ant, Z post]
	public static double STDEV;
	/** 
	 * @author KJZ 12.22.2019
	 * @param Takes nifti scan data as 3D matrix of double[][][]. 
	 * @param Takes x, y, z dimension to be processed (true) or omitted (false) as boolean. 
	 * @param Takes standard deviation as a double val. Lowering the standard dev threshold lowers the boundary 
	 * that the voxels are masked out, assuming that the scan is a T1 weighted volume. TODO: Allow inversion 
	 * to work with T2-weighted scans. 
	 * @param Takes len as int, corresponds to how the scan should be divided up to calculate the 
	 * stDev. (e.g, 2 = halves, 3 = thirds etc)
	 * @param Takes bnd as double, what intensity the gradient should halt calculation at. 
	 * @returns the altered data passed to findGradient() as a matrix of doubles to be written to a new nifti scan.  
	 */

	public static double[][][] findGrdnt(double[][][] data, boolean x, boolean y, boolean z, 
		double stdev, int len, double bnd, boolean firstHalf, boolean secondHalf) {
		
		int XDIM = data[0][0].length; int YDIM = data[0].length; int ZDIM = data.length; STDEV = stdev;
		
		if (x && firstHalf) { data = thrshldOutliers(data, 
				findGrdntHelper(data, ZDIM, YDIM, XDIM, len, "x"), 
				ZDIM, YDIM, 0, 1, 0, bnd, "x", XDIM/len);
		}
		if (x && secondHalf) { data = thrshldOutliers(data, 
				findGrdntHelper(data, ZDIM, YDIM, XDIM, len, "x"), 
				ZDIM, YDIM, XDIM-1, -1, 1, bnd, "x", XDIM-(XDIM/len));
		}
		if (y && firstHalf) { data = thrshldOutliers(data, 
				findGrdntHelper(data, ZDIM, XDIM, YDIM, len, "y"), 
				ZDIM, XDIM, 0, 1, 0, bnd, "y", YDIM/len);
		}
		if (y && secondHalf) { data = thrshldOutliers(data, 
				findGrdntHelper(data, ZDIM, XDIM, YDIM, len, "y"), 
				ZDIM, XDIM, YDIM-1, -1, 1, bnd, "y", YDIM-(YDIM/len));
		}
		if (z && firstHalf) { data = thrshldOutliers(data, 
				findGrdntHelper(data, XDIM, YDIM, ZDIM, len, "z"), 
				XDIM, YDIM, 0, 1, 0, bnd, "z", ZDIM/len);
		}
		if (z && secondHalf) { data = thrshldOutliers(data, 
				findGrdntHelper(data, XDIM, YDIM, ZDIM, len, "z"), 
				XDIM, YDIM, ZDIM-1, -1, 1, bnd, "z", ZDIM-(ZDIM/len));
		}
		
		printStats(XDIM, YDIM, ZDIM);
		
		return data;
	}
	
	/** 
	 * @author KJZ
	 * @param Helper method for findGradient, takes scan data as 3-dimensional matrix of doubles. 
	 * @param Takes int as a, b, c, corresponding to the dimensions of the scan. 
	 * @param Takes a string as whether we are traversing the x, y, or z dimensions of the scan.
	 * @returns 3D matrix of nifti data as double[][][]. Columns of data correspond to the first and 
	 * second halves of the scan being processed and contain a grid of gradients corresponding to each row of data.  
	*/

	public static double[][][] findGrdntHelper(double[][][] data, int a, int b, int c, int bound, String dimension) {

		// two gradients for moving toward the center of the scan and two measures of residuals for each dim
		double[][][] gradients = new double[2][a][b]; 
		double[] residuals = new double[c/bound+1]; 
		double[] residuals2 = new double[c/bound+1];

		COUNT = 0;
		
		for (int i = 0; i < a; i++) {
			for (int j = 0; j < b; j++) {
				gradients[0][i][j] = findGrdntHelper2(data, residuals, dimension, i, j, 0, 1, c/bound);
				gradients[1][i][j] = findGrdntHelper2(data, residuals2, dimension, i, j, c-1, -1, (c-(c/bound)) );
			}
		}
		
		setAvgDelta(a, b, c, dimension, bound);
		LINEVAL = 0;
		LINEVAL2 = 0;
		
		return gradients;
	}
	
	public static double findGrdntHelper2(double[][][] data, double[] resid, String dim, int i, int j, int k, int incr, int len) {
		resid = findResid(data, resid, dim, i, j, k, incr, len);
		COUNT = 0;
		return standardDeviation(resid, takeAvg(resid));
	}
	
	
	/**
	 * @param data of 3-dim matrix of double values w/ mri data
	 * @param resid in an array of residuals as doubles
	 * @param dim is the dimension given as a string
	 * @param i,j,k correspond to the dimensions of the scan
	 * @param incr is 1 or -1 depending on moving forward/backward across the scan
	 * @param len is the given length of the traversing the residuals
	 * @return double array of all residuals in a given row
	 */
	
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
	 * @param residuals are single double array of voxels from mri data, 
	 * @param The average intensity of the line of voxel data as double
	 * @returns the standard deviation of the voxel residuals
	 */
	
	public static double standardDeviation(double[] resid, double avg) {
		
		double stDev = 0; int power = 1; int n = 0;
		
		for (int counter = 0; counter < resid.length; counter++) {
			if (resid[counter] != 0) {
				stDev += ((resid[counter] - avg) * (resid[counter] - avg));
				n++;
			}
		}
		return Math.sqrt((stDev/(n - power)));
	}
	
	/**
	 * @param takes double array of residuals 
	 * @return the average of the array
	 */
	
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
	
	public static void setAvgDelta(int a, int b, int c, String dim, int bound) {
		
		if (dim.equals("x")) {
			AVGDELTA[0] = Math.abs(LINEVAL/((c/bound) * b * a));
			AVGDELTA[1] = Math.abs(LINEVAL2/((c/bound) * b * a)); 	
			
		} else if (dim.equals("y")) {
			AVGDELTA[2] = Math.abs(LINEVAL/(c * (b/bound) * a));
			AVGDELTA[3] = Math.abs(LINEVAL2/(c * (b/bound) * a));
			
		} else if (dim.equals("z")) {
			AVGDELTA[4] = Math.abs(LINEVAL/(c * b * (a/bound)));
			AVGDELTA[5] = Math.abs(LINEVAL2/(c * b * (a/bound)));
			
		} else {
			System.out.println("No dimension specified");
		}
	}
	
	// ------------------------------------------------------------ //
	
	// simpler way of writing traverseData()
	public static double[][][] thrshldOutliers(double[][][] data, double gradient[][][], int a, int b, int c, 
			int incr, int half, double bnd, String dim, int len) {
		for (int i = 0; i < a; i++) {
			for (int j = 0; j < b; j++) {
				for (int k = c; k != len; k+=incr) {
					
					if (voxVal(data, gradient, half, i, j, k, dim, incr, bnd)) {
						break;
					} else {
						data = pushData(data, i, j, k, dim, 0);
					}
				}
				
			}
		}
		
		return data;
	}
	
	// a simpler way to write returnVoxel()	
	private static boolean voxVal(double[][][] data, double[][][] gradient, int half, int i, int j, int k, 
			String dim, int incr, double bnd) {

		double val = pullData(data, dim, i, j, k);
		// does gradient match up? 
		double val2 = ((pullData(data, dim, i, j, k+incr) - (val - rtnDelta(dim, half) ) ) )/gradient[half][i][j]; 
		
		return voxValHelper(val, val2, bnd);
	}
	
	private static boolean voxValHelper(double val, double val2, double bnd) {
		
		if (val2 > STDEV && val > bnd) {
			return true;
		} else if (val < 1) {
			return false;
		}
		return false; 
	} 
	
	private static void printStats(int XDIM, int YDIM, int ZDIM) {
		// TODO: Double-check that the anatomy UPDATE: anterior and posterior are mixed up, 
		// this could also be b/c the scans are in an 
		// odd orientation. Need to test this on a scan with normal orientation. 
		
		System.out.println(""); System.out.println
		("X, Y, Z Dimensions are: " + XDIM + " x " + YDIM + " x " + ZDIM);
		System.out.println("Using standard deviation threshold of " + STDEV);
		System.out.println("");
		System.out.println("Average gradient delta in each dimension: ");
		System.out.println("	Left = " + round(AVGDELTA[0],3) + "     Right = " + round(AVGDELTA[1],3));
		System.out.println("	Dorsal = " + round(AVGDELTA[2],3) + "   Ventral = " + round(AVGDELTA[3],3));
		System.out.println("	Anterior = " + round(AVGDELTA[4],3) + " Posterior = " + round(AVGDELTA[5],3));
		System.out.println("");
	}
		
	// ------------------------------------------------------------ //

	// sets data to the nifti matrix
	public static double[][][] pushData(double[][][] data, int i, int j, int k, String dim, double val) {
		
		if (dim.equals("x")) {
			data[i][j][k] = val; 
		} else if (dim.equals("y")) {
			data[i][k][j] = val; 
		} else if (dim.equals("z")) { 
			data[k][j][i] = val;
		} else {
			System.out.println("No valid x, y, z dim argument provided.");		
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
	public static void phaseEncodeNorm(double[][][] data, double avg1, double avg2) {
		for (int i = 0; i < data.length; i++) {
			for(int j = 0; j < data[i].length; j++) {
				if (avg1 < avg2) { 
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
