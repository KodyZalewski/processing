
public class thresholdStandardDev {
	
	public static double LINEVAL = 0; // tracks total intensity for the first half of the scan
	public static double LINEVAL2 = 0;// tracks total intensity opposite half of each dimension of the scan
	public static int COUNT = 0; // keeps track of the number of non-zero voxels
	public static double[] AVGDELTA = new double[6]; // stores avgs of each half: [X lh, X rh, Y sup, Y inf, Z ant, Z post]
	public static float STDEV;
	
	/** 
	 * @author KJZ 12.22.2019
	 * @param Takes nifti scan data as 3D matrix of double[][][]. 
	 * @param Takes x, y, z dimension to be processed (true) or omitted (false) as boolean. 
	 * @param Takes standard deviation as a double val. Lowering the standard dev threshold lowers the boundary 
	 * that the voxels are masked out, assuming that the scan is a T1 weighted volume. 
	 * TODO: Allow inversion to work with T2-weighted scans. 
	 * @param Takes len as int, corresponds to how the scan should be divided up to calculate the 
	 * stDev. (e.g, 2 = halves, 3 = thirds etc)
	 * @param Takes bnd as double, what intensity the gradient should halt calculation at. 
	 * @returns the altered data passed to findGradient() as a matrix of doubles to be written to a new nifti scan.  
	 */

	public static double[][][] findGrdnt(double[][][] data, boolean x, boolean y, boolean z, 
		float stdev, int len, float bnd, boolean firstHalf, boolean secondHalf) {
		
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
				printStats(XDIM, YDIM, ZDIM);
		}
		
		return data;
	}
	
					// FIND DIMENSIONAL GRADIENTS // 
	
	// ------------------------------------------------------------ //
	
	/** 
	 * @param Helper method for findGradient, takes scan data as 3-dimensional matrix of doubles. 
	 * @param int as a, b, c, correspond to length of the dimensions of the scan. 
	 * @param len is how far thresholding should be applied
	 * @param Takes string dim for x, y, or z dimensions of the scan.
	 * @returns 3D matrix of nifti data as double[][][]. Columns of data correspond to the first and 
	 * second halves of the scan being processed and contain a grid of gradients corresponding to each row of data.  
	*/

	public static float[][][] findGrdntHelper(double[][][] data, int a, int b, int c, int len, String dim) {

		// two gradients for moving toward the center of the scan and two measures of residuals for each dim
		float[][][] gradients = new float[2][a][b]; 
		float[] resid = new float[c/len+1]; 
		float[] resid2 = new float[c/len+1];

		COUNT = 0;
		
		for (int i = 0; i < a; i++) {
			for (int j = 0; j < b; j++) {
				gradients[0][i][j] = findGrdntHelper2(data, resid, dim, i, j, 0, 1, c/len);
				gradients[1][i][j] = findGrdntHelper2(data, resid2, dim, i, j, c-1, -1, (c-(c/len)));
			}
		}
		
		setAvgDelta(a, b, c, dim, len);
		LINEVAL = 0;
		LINEVAL2 = 0;
		
		return gradients;
	}
	
	public static float findGrdntHelper2(double[][][] data, float[] resid, String dim, 
			int i, int j, int k, int incr, int len) {
		
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
	
	public static float[] findResid(double[][][] data, float[] resid, String dim, 
			int i, int j, int k, int incr, int len) {
		
		float val;
		
		while (k != len-incr) {
			val = (float) pullData(data, dim, i, j, k);
			if (val != 0) {
				resid[COUNT] = (float) (pullData(data, dim, i, j, k+incr) - val);
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
	
					   // STATS CALCULATIONS // 
	
	// ------------------------------------------------------------ //
	
	/** 
	 * @param risid are residuals are single double array of voxels from mri data, 
	 * @param avg is the average intensity of the line of voxel data as double
	 * @returns the standard deviation of the voxel residuals
	 */
	
	public static float standardDeviation(float[] resid, float avg) {
		
		float stDev = 0; int power = 1; int n = 0;
		
		for (int counter = 0; counter < resid.length; counter++) {
			if (resid[counter] != 0) {
				stDev += ((resid[counter] - avg) * (resid[counter] - avg));
				n++;
			}
		}
		return (float) Math.sqrt((stDev/(n - power)));
	}
	
	/**
	 * @param takes double array of residuals as resid
	 * @return the average of the array
	 */
	
	public static float takeAvg(float[] resid) {
		
		float avg = 0; int n = 0;
		
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
	 * @param Boundary int bnd is the dimension by which the scan is divided into (halves = 2, thirds = 3 etc.)
	 * @result Sets the average change in dimension to the global array of averages AVGDELTA.
	 */
	
	public static void setAvgDelta(int a, int b, int c, String dim, int bnd) {
		
		if (dim.equals("x")) {
			AVGDELTA[0] = Math.abs(LINEVAL/((c/bnd) * b * a));
			AVGDELTA[1] = Math.abs(LINEVAL2/((c/bnd) * b * a)); 	
			
		} else if (dim.equals("y")) {
			AVGDELTA[2] = Math.abs(LINEVAL/(c * (b/bnd) * a));
			AVGDELTA[3] = Math.abs(LINEVAL2/(c * (b/bnd) * a));
			
		} else if (dim.equals("z")) {
			AVGDELTA[4] = Math.abs(LINEVAL/(c * b * (a/bnd)));
			AVGDELTA[5] = Math.abs(LINEVAL2/(c * b * (a/bnd)));
			
		} else {
			System.out.println("No dimension specified");
		}
	}
	
				     // THRESHOLDING INTENSITY // 
	
	// ------------------------------------------------------------ //
	
	/**
	 * @param data of 3-dim matrix of double values w/ mri data
	 * @param gradient(grndt) as x,y coordinates with [0,1][x][y] denoting which half of scan dimension it is
	 * @param half as int denotes column of gradient matrix to use (0,1)
	 * @param a, b, c correspond to the dimensions of the scan
	 * @param incr is 1 or -1 depending on moving forward/backward across the scan
	 * @param bnd as double, what intensity the gradient should halt calculation at. 
	 * @param dim is the dimension given as a string
	 * @param len is the given length of the traversing the residuals
	 * @return edited 3-dim matrix of double mri intensity values
	 */
	
	public static double[][][] thrshldOutliers(double[][][] data, float grdnt[][][], int a, int b, int c, 
			int incr, int half, float bnd, String dim, int len) {
		
		System.out.println("Gradient thresholding all voxels in dimension " 
		+ dim + " with intensity below: " + round(bnd,2));
		System.out.println("..."); System.out.println("");
		
		for (int i = 0; i < a; i++) {
			for (int j = 0; j < b; j++) {
				for (int k = c; k != len; k+=incr) {
					
					if (voxVal(data, grdnt, half, i, j, k, dim, incr, bnd)) {
						break;
					} else { //
						data = pushData(data, i, j, k, dim, 0);
					}
					
				}
			}
		}
		
		return data;
	}
	
	/**
	 * 
	 * @param data of 3-dim matrix of double values w/ mri data
	 * @param gradient(grndt) as x,y coordinates with [0,1][x][y] denoting which half of scan dimension it is
	 * @param half as int denotes column of gradient matrix to use (0,1)
	 * @param i, j, k correspond to the dimensions of the scan
	 * @param dim is the dimension given as a string
	 * @param incr is 1 or -1 depending on moving forward/backward across the scan
	 * @param bnd as double, what intensity the gradient should halt calculation at. 
	 * @return boolean if voxel intensity meets criteria for thresholding
	 */
	
	private static boolean voxVal(double[][][] data, float[][][] grdnt, int half, int i, int j, int k, 
			String dim, int incr, float bnd) {

		float val = (float) pullData(data, dim, i, j, k);
		// does gradient match up? 
		float delta = (float) pullData(data, dim, i, j, k+incr) - val;
		
		float val2 = (float) (delta - rtnDelta(dim, half))/grdnt[half][i][j];
		
		return voxValHelper(val, val2, bnd);
	}
	
	private static boolean voxValHelper(float val, float val2, float bnd) {
		
		if (val2 <= 0 || val < 1) {
			return false;
		} else if (val2 > STDEV || val <= bnd) {
			return true;
		} else {
			return false;
		}
	} 
	
	private static void printStats(int x, int y, int z) {
		// TODO: Double-check that the anatomy UPDATE: anterior and posterior are mixed up, 
		// this could be b/c the scans are oddly oriented. check on scan with normal orientation. 
		
		System.out.println(""); System.out.println
		("X, Y, Z Dimensions are: " + x + " x " + y + " x " + z);
		System.out.println("Using standard deviation threshold of " + STDEV);
		System.out.println("");
		System.out.println("Average gradient delta in each dimension: ");
		System.out.println("	Left = " + round(AVGDELTA[0],3) + "     Right = " + round(AVGDELTA[1],3));
		System.out.println("	Dorsal = " + round(AVGDELTA[2],3) + "   Ventral = " + round(AVGDELTA[3],3));
		System.out.println("	Anterior = " + round(AVGDELTA[4],3) + " Posterior = " + round(AVGDELTA[5],3));
		System.out.println("");
	}
	
						// HELPER METHODS // 
	
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
	public static float rtnDelta(String dim, int half) {
		
		if (dim.equals("x")) {
			return (float) AVGDELTA[0+half];
		} else if (dim.equals("y")) {
			return (float) AVGDELTA[2+half];
		} else if (dim.equals("z")) {
			return (float) AVGDELTA[4+half];
		} else {
			System.out.println("Error, no value found, returning 0");
			return 0;
		}
	}
	
	/**
	 * TODO: UNFINISHED METHOD
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
