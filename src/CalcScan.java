
public class CalcScan {
	
	public static double LINEVAL = 0; // tracks total intensity for first half of scan
	public static double LINEVAL2 = 0;// tracks total intensity opposite half of each dimension of the scan
	public static int COUNT = 0; // keeps track of # non-zero voxels
	public static double[] AVGDELTA = new double[6]; // stores avgs of each half: [X lh, X rh, Y sup, Y inf, Z ant, Z post]
	public static float STDEV;
	
	/** 
	 * @author KJZ 12.22.2019
	 * 
	 * Functions search each row in scan to determine where largest change in intensity is, this
	 * denotes a probable location for brain/non-brain boundary. Sets voxel intensity values to 0 until it
	 * reaches voxel labeled with the greatest delta. Moves in six dimensions (+x,-x,+y,-y,+z,-z) toward the 
	 * center of the scan. Variables can be altered to find best match. 
	 * 
	 * TODO: Allow inversion to work with T2-weighted scans. 
	 * 
	 * @param nifti scan data as 3D double[][][] matrix. 
	 * @param x, y, z dimension to be processed(T) or omitted(F) as boolean. 
	 * @param stDev as a double. Lowering the standard deviation threshold lowers boundary 
	 * that the voxels are masked out, assumes scan is a T1 weighted volume. 
	 * @param len as int, corresponds to how scan is divided to calculate the stDev. (e.g, 2 = halves, 3 = thirds)
	 * @param bound as double, intensity boundary the gradient halts calculation at. 
	 * @returns altered data passed to findGrad() as matrix of doubles written to new nifti scan.  
	 */

	public static double[][][] findGrad(double[][][] data, boolean x, boolean y, boolean z, 
		float stdev, int len, float bound, boolean firstHalf, boolean secondHalf) {
		
		int XDIM = data[0][0].length; int YDIM = data[0].length; int ZDIM = data.length; STDEV = stdev;
		
		if (x && firstHalf) { data = threshold(data, 
				findGradHelp(data, ZDIM, YDIM, XDIM, len, "x"), 
				ZDIM, YDIM, 0, 1, 0, bound, "x", XDIM/len);
		}
		
		if (x && secondHalf) { data = threshold(data, 
				findGradHelp(data, ZDIM, YDIM, XDIM, len, "x"), 
				ZDIM, YDIM, XDIM-1, -1, 1, bound, "x", XDIM-(XDIM/len));
		}
		
		if (y && firstHalf) { data = threshold(data, 
				findGradHelp(data, ZDIM, XDIM, YDIM, len, "y"), 
				ZDIM, XDIM, 0, 1, 0, bound, "y", YDIM/len);
		}
		
		if (y && secondHalf) { data = threshold(data, 
				findGradHelp(data, ZDIM, XDIM, YDIM, len, "y"), 
				ZDIM, XDIM, YDIM-1, -1, 1, bound, "y", YDIM-(YDIM/len));
		}
		
		if (z && firstHalf) { data = threshold(data, 
				findGradHelp(data, XDIM, YDIM, ZDIM, len, "z"), 
				XDIM, YDIM, 0, 1, 0, bound, "z", ZDIM/len);
		}
		
		if (z && secondHalf) { data = threshold(data, 
				findGradHelp(data, XDIM, YDIM, ZDIM, len, "z"), 
				XDIM, YDIM, ZDIM-1, -1, 1, bound, "z", ZDIM-(ZDIM/len));
				printStats(XDIM, YDIM, ZDIM);
		}
		
		return data;
	}
	
	// ------------------------------------------------------------ //
	
					// FIND DIMENSIONAL GRADIENTS // 
	
	// ------------------------------------------------------------ //
	
	/** 
	 * @param Helper method for findGrad, takes scan data as 3D double[][][] matrix. 
	 * @param int as a, b, c, correspond to scan dimensions length. 
	 * @param len is how far thresholding is applied
	 * @param dim as string for x, y, or z scan dimensions.
	 * @returns nifti double[][][] matrix. Columns of data correspond to first and 
	 * second scan halves processed containing grid of gradients for each data row.  
	*/

	public static float[][][] findGradHelp(double[][][] data, int a, int b, int c, int len, String dim) {

		// two gradients for moving toward scan center, and two measures of residuals for each dim
		float[][][] gradients = new float[2][a][b]; 
		float[] r = new float[c/len+1]; 
		float[] r2 = new float[c/len+1];

		COUNT = 0;
		
		for (int i = 0; i < a; i++) {
			for (int j = 0; j < b; j++) {
				gradients[0][i][j] = findGradHelp2(data, r, dim, i, j, 0, 1, c/len);
				gradients[1][i][j] = findGradHelp2(data, r2, dim, i, j, c-1, -1, (c-(c/len)));
			}
		}
		
		setAvgDelta(a, b, c, dim, len);
		LINEVAL = 0;
		LINEVAL2 = 0;
		
		return gradients;
	}
	
	public static float findGradHelp2(double[][][] data, float[] r, String dim, 
			int i, int j, int k, int incr, int len) {
		
		r = findResidual(data, r, dim, i, j, k, incr, len);
		COUNT = 0;
		return stDev(r, getAvg(r));
		
	}
	
	/**
	 * @param data of 3-dim matrix of double values w/ mri data
	 * @param r is double array of residuals
	 * @param dim is the dimension as string
	 * @param i,j,k correspond to scan dimensions
	 * @param incr is 1 or -1 depending on moving forward/backward across the scan
	 * @param len is the given length for traversing the residuals
	 * @return double array of all residuals in a given row
	 */
	
	public static float[] findResidual(double[][][] data, float[] r, String dim, 
			int i, int j, int k, int incr, int len) {
		
		float val;
		
		while (k != len-incr) {
			val = (float) pullData(data, dim, i, j, k);
			if (val != 0) {
				r[COUNT] = (float) (pullData(data, dim, i, j, k+incr) - val);
				if (incr > 0) {
					LINEVAL += r[COUNT];
				} else if (incr < 0) {
					LINEVAL2 += r[COUNT];
				}
				COUNT++;
			}
			k+=incr;
		}
		return r;
		
	}
	
	// ------------------------------------------------------------ //
	
					   // STATS CALCULATIONS // 
	
	// ------------------------------------------------------------ //
	
	/** 
	 * @param r residuals are single double array of voxels from mri data, 
	 * @param avg is the average intensity of voxel line data as double
	 * @returns the standard deviation of voxel residuals
	 */
	
	public static float stDev(float[] r, float avg) {
		
		float stDev = 0; int power = 1; int n = 0;
		
		for (int i = 0; i < r.length; i++) {
			if (r[i] != 0) {
				stDev += ((r[i] - avg) * (r[i] - avg));
				n++;
			}
		}
		return (float) Math.sqrt((stDev/(n - power)));
	}
	
	/**
	 * @param r is double array of residuals
	 * @return the average of the array
	 */
	
	public static float getAvg(float[] r) {
		
		float avg = 0; int n = 0;
		
		for (int i = 0; i < r.length; i++) {
			if (r[i] != 0) {
				avg += avg;
				n++;
			}
		}
		return avg/n;
	}

	/**
	 * @param dimensions x, y, z as int corresponding to a, b, and c. 
	 * @param dimension being measured (x,y,z) as string
	 * @param bound is how scan is divided (halves = 2, thirds = 3 etc.)
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
	
				     // THRESHOLDING INTENSITY // 
	
	// ------------------------------------------------------------ //
	
	/**
	 * @param data of 3D matrix of double values w/ mri data
	 * @param gradient(g) as x,y coordinates with [0,1][x][y] denoting which half of scan dimension
	 * @param half as int denotes column of gradient matrix to use (0,1)
	 * @param a, b, c correspond to the dimensions of the scan
	 * @param incr is 1 or -1 for moving forward/backward across the scan
	 * @param bound as double, intensity value boundary gradient stops at. 
	 * @param dim is the dimension given as a string
	 * @param len is the given length of the traversing the residuals
	 * @return edited 3-dim matrix of double mri intensity values
	 */
	
	public static double[][][] threshold(double[][][] data, float g[][][], int a, int b, int c, 
			int incr, int half, float bound, String dim, int len) {
		
		System.out.println("Gradient thresholding all voxels in dimension " 
		+ dim + " with intensity below: " + round(bound,2));
		System.out.println("..."); System.out.println("");
		
		for (int i = 0; i < a; i++) {
			for (int j = 0; j < b; j++) {
				for (int k = c; k != len; k+=incr) {
					
					if (voxVal(data, g, half, i, j, k, dim, incr, bound)) {
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
	 * @param data of 3D matrix of double values w/ mri data
	 * @param gradient(g) as x,y coordinates with [0,1][x][y] denoting which half of scan dimension
	 * @param half as int denotes column of gradient matrix to use (0,1)
	 * @param i, j, k is voxel location
	 * @param dim is dimension as string
	 * @param incr is 1 or -1 for moving forward/backward across the scan
	 * @param bound as double, intensity value boundary gradient stops at
	 * @return boolean if voxel intensity meets criteria for thresholding
	 */
	
	private static boolean voxVal(double[][][] data, float[][][] g, int half, int i, int j, int k, 
			String dim, int incr, float bound) {

		float val = (float) pullData(data, dim, i, j, k);
		// does gradient match up? 
		float delta = (float) pullData(data, dim, i, j, k+incr) - val;
		
		float val2 = (float) (delta - rtnDelta(dim, half))/g[half][i][j];
		
		return voxValHelp(val, val2, bound);
	}
	
	private static boolean voxValHelp(float val, float val2, float bnd) {
		
		if (val2 <= 0 || val < 1) {
			return false;
		} else if (val2 > STDEV || val <= bnd) {
			return true;
		} else {
			return false;
		}
	} 
	
	private static void printStats(int x, int y, int z) {
		// TODO: Double-check that the anatomy 
		// UPDATE 3.21: anterior and posterior are mixed up, 
		// this could be b/c the scans are oddly oriented. check on scan with normal orientation. 
		// UPDATE2 1.22: qform and sform headers are mislabeled on many EXVIVO scans. Not a problem
		// with the program. 
		
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
	
	// ------------------------------------------------------------ //
	
						// EXTRA HELPER METHODS // 
	
	// ------------------------------------------------------------ //

	// sets data to nifti matrix
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
	
	// retrieves data from nifti matrix
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
	 * TODO: UNFINISHED METHOD predicts hyper/hypointense facets
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
	 * @param val as double is rounded
	 * @param number of places the double is rounded
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
