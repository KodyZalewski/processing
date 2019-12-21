
public class thresholdStandardDev {
	
	public static double LINEVAL = 0; // for first half of the scan
	public static double LINEVAL2 = 0;// for the opposite half of each dimension of the scan
	public static int COUNT = 0;
	
	// global variable storing avgs of each half: [X left, X right, Y superior, Y inferior, Z anterior, Z posterior]
	public static double[] AVGDELTA = new double[6]; 

	/** 
	 * the second-to-last argument as an integer is how far you want to traverse to find the gradient
	 * e.g. 3 means that it will take only the left and right third of the scan since the brain and actual
	 * data is likely to be in the middle. The last argument is the dimension we're looking for. 
	 * Lowering the standard dev threshold decreases the boundary at which the voxels are masked out.
	 * Integer "bound" corresponds to how the scan is divided up, e.g. "3" will measure the farthest thirds from the center
	 * "4" is the farther quarters, "2" would be each half, covering the whole scan. 
	 */

	public static double[][][] findGradient(double[][][] data, boolean x, boolean y, boolean z, 
		double stdev, int bound, double voxelBound, boolean firstHalf, boolean secondHalf) {
		
		int XDIM = data[0][0].length;
		int YDIM = data[0].length;
		int ZDIM = data.length;
		
		System.out.println("X, Y, Z Dimensions are: " + XDIM + " " + YDIM + " " + ZDIM);
		
		// matrices for storing the standard deviation for each row in a grid representing both sides of the scan
		if (x == true) {
			data = traverseData(data, findGradientHelper(data, ZDIM, YDIM, XDIM, bound, "x"), 
				ZDIM, YDIM, XDIM, bound, voxelBound, "x", stdev, firstHalf, secondHalf);
		}
		if (y == true) {
			data = traverseData(data, findGradientHelper(data, ZDIM, XDIM, YDIM, bound, "y"), 
				ZDIM, XDIM, YDIM, bound, voxelBound, "y", stdev, firstHalf, secondHalf);
		}
		if (z == true) {
			data = traverseData(data, findGradientHelper(data, XDIM, YDIM, ZDIM, bound, "z"), 
				XDIM, YDIM, ZDIM, bound, voxelBound, "z", stdev, firstHalf, secondHalf);
		}
		// TODO: Double-check that the anatomy corresponds with the x, y, z dimensions
		// UPDATE: Y and Z are swapped I guess, fix at some point, not critical right now as long as it works
		// Will be necessary to address if this is ever published so as not to confuse the client.
		
		// anterior and posterior are mixed up
		
		System.out.println("Average change in each dimension: left = " + AVGDELTA[1] + " right = " + AVGDELTA[1] + " dorsal = " + AVGDELTA[2] + " ventral = " + AVGDELTA[3] + " anterior = " + AVGDELTA[4] + " posterior = " + AVGDELTA[5]);
		return data;
	}

	// a, b and c are the dimensions used for traversing the scan
	public static double[][][] findGradientHelper(double[][][] data, int a, int b, int c, int bound, String dimension) {

		// two gradients for moving toward the center of the scan and two measures of averages for each dimension
		double[][][] gradients = new double[2][a][b]; double[][][] averages = new double[2][a][b];

		// for calculating the standard deviation of the last dimensional row
		double[] residuals = new double[c/bound+1]; double[] residuals2 = new double[c/bound+1];
		
		// counter for traversing the scan and counting non-zero voxels
		COUNT = 0;

		for (int i = 0; i < a; i++) {
			for (int j = 0; j < b; j++) {
				for (int k = 0; k < c/bound; k++) {
					residuals = calcResiduals(data, residuals, 1, 0, i, j, k, k, dimension); 
					
				}
				
				COUNT = 0;
				averages[0][i][j] = Math.abs(takeAverage(residuals));
				gradients[0][i][j] = Math.abs(standardDeviation(residuals, averages[0][i][j]));
				
				
				for (int k = c - 1; k > (c - c/bound); k--) {
					residuals2 = calcResiduals(data, residuals2, 0, 1, i, j, k-1, (c-k-1), dimension);
					
				}
				COUNT = 0;
				averages[1][i][j] = Math.abs(takeAverage(residuals));
				gradients[1][i][j] = Math.abs(standardDeviation(residuals2, averages[1][i][j]));
				
			}
		}
		
		setAvgDelta(a, b, c, dimension, bound);
		LINEVAL = 0;
		LINEVAL2 = 0;
		
		return gradients;
	}
	
	/** @params the original data, the gradient for storing info, forward or backward will equal
	// 0 or 1 depending on which half of the scan dimension is being traversed.
	// i, j, k, locate the voxel value on the scan and a, c are the location
	// resid value is what half of the scan is where the value is being stored
	   @returns the gradient of the scan with the standard deviation at each voxel location 
	 */

	public static double[] calcResiduals(double[][][] data, double[] residuals, 
			int forward, int backward, int i, int j, int k, int residValue, String dimension) {

		// TODO: fix forward/backward nonsense at some point
		if (dimension.equals("x")) { // for x dimension
			if (data[i][j][k] != 0) {	
				if (forward == 1 && backward == 0) {
					LINEVAL += (data[i][j][k+forward] - data[i][j][k-backward]);
				} else if (forward == 0 && backward == 1) {
					LINEVAL2 += (data[i][j][k+forward] - data[i][j][k-backward]);
				}
				residuals[COUNT] = (data[i][j][k+forward] - data[i][j][k-backward]);
				COUNT++;
			}
			
		} else if (dimension.equals("y")) { // for y dimension
			if (data[i][k][j] != 0) {
				if (forward == 1 && backward == 0) {
					LINEVAL += (data[i][k+forward][j] - data[i][k-backward][j]);
				} else if (forward == 0 && backward == 1) {
					LINEVAL2 += (data[i][k+forward][j] - data[i][k-backward][j]);
				}
				residuals[COUNT] = (data[i][k+forward][j] - data[i][k-backward][j]);
				COUNT++;
			}
			
		} else if (dimension.equals("z")) { // for z dimension
			if (data[k][j][i] != 0 ) {
				if (forward == 1 && backward == 0) {
					LINEVAL += (data[k+forward][j][i] - data[k-backward][j][i]);
				} else if (forward == 0 && backward == 1) {
					LINEVAL2 += (data[k+forward][j][i] - data[k-backward][j][i]);
				}
				residuals[COUNT] = (data[k+forward][j][i] - data[k-backward][j][i]);
				COUNT++;
			}
			
		} else {
			residuals[COUNT] = 0.0;
		}
		return residuals;
	}


	/** @author kody
	 *  @params data of a line of voxels of mri data,
	 *  the average intensity of the line of voxel data as double
	 *  @returns the standard deviation of the voxel residuals
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
	
	public static double takeAverage(double[] residuals) {
		
		double average = 0; int n = 0;
		
		for (int counter = 0; counter < residuals.length; counter++) {
			if (residuals[counter] != 0) {
				average += average;
				n++;
			}
		}
		return average/n;
	}

	/**
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

	//TODO: break up everything below this line into it's own separate .java file at some point

	// stdev is how many standard deviations away from the mean the value of the gradient should be at each location
	// before determining that it should be a boundary, default is 2, add average[][][] grid at some point to arguments
	// to correspond with the stdev grid.
	// voxelBound corresponds with the intensity at which the boundary should be set
	// bound corresponds with how the scan should be broken down, i.e. into quarters, thirds, halves etc. 

	public static double[][][] traverseData(double[][][] data, double gradient[][][], int a, int b, int c, int bound, 
			double voxelBound, String dimension, double stdev, boolean firstHalf, boolean secondHalf) {

		boolean voxelBoundary = false;

		for (int i = 0; i < a; i++) {
			for (int j = 0; j < b; j++) {
				if (firstHalf == true) {
					for (int k = 0; k < c/bound; k++) {
						voxelBoundary = returnVoxel(data, gradient, 1, 0, i, j, k, dimension, stdev, voxelBound, voxelBoundary); 
						if (voxelBoundary == true) {
							break;
						} else {
							data = replaceVoxel(data, i, j, k, dimension);
						}
					}
					voxelBoundary = false; 
				}
				if (secondHalf == true) {
					for (int k = c - 1; k > (c - c/bound); k--) {
						voxelBoundary = returnVoxel(data, gradient, 0, 1, i, j, k, dimension, stdev, voxelBound, voxelBoundary);
						if (voxelBoundary == true) {
							break; 
						} else {
							data = replaceVoxel(data, i, j, k, dimension); 
						}
					}
					voxelBoundary = false; 
				}
			}
		}
		return data;
	}


	public static double[][][] replaceVoxel(double[][][] data, int i, int j, int k, String dimension) {
		
		if (dimension.equals("x")) { // for x dimension data[i][j][k] != 0
			data[i][j][k] = 0; 
		} else if (dimension.equals("y")) { // for y dimension data[i][k][j] != 0
			data[i][k][j] = 0; 
		} else if (dimension.equals("z")) { // for z dimension data[k][j][i] != 0
			data[k][j][i] = 0;
		} else {
			System.out.println("No valid x, y, z dimension argument provided.");		
		}
		return data;
	}

	public static boolean returnVoxel(double[][][] data, double[][][] gradient, int forward, int backward, int i, int j, int k, 
			String dimension, double stdev, double voxelBound, boolean voxelBoundary) {

		if (dimension.equals("x")) { // for x dimension data[i][j][k] != 0
			if (forward == 1 && backward == 0) {
				if (Math.abs((Math.abs(data[i][j][k+forward] - data[i][j][k]) - AVGDELTA[0])) / gradient[0][i][j] < stdev && data[i][j][k] > voxelBound) {
					voxelBoundary = false; 
				} else if (data[i][j][k] > 1) {
					voxelBoundary = true;  
				}
			} else if (forward == 0 && backward == 1) {
				if (Math.abs((Math.abs(data[i][j][k-backward] - data[i][j][k]) - AVGDELTA[1]) / gradient[1][i][j]) < stdev && data[i][j][k] > voxelBound) {
					voxelBoundary = false;
				} else if (data[i][j][k] > 1) {
					voxelBoundary = true;  
				}
			} else {
				System.out.println("we shouldn't reach this statement");
			}
		}

		if (dimension.equals("y")) { // for y dimension data[i][k][j] != 0
			if (forward == 1 && backward == 0) {
				if (Math.abs(Math.abs(data[i][k+forward][j] - data[i][k][j]) - AVGDELTA[2]) / gradient[0][i][j] < stdev && data[i][k][j] > voxelBound) {
					voxelBoundary = false;
				} else if (data[i][k][j] > 1) {
					voxelBoundary = true; 
				}
			} else if (forward == 0 && backward == 1) {
				if (Math.abs(Math.abs(data[i][k-backward][j] - data[i][k][j]) - AVGDELTA[3]) / gradient[1][i][j] < stdev && data[i][k][j] > voxelBound) {
					voxelBoundary = false; 
				} else if (data[i][k][j] > 1) {
					voxelBoundary = true; 
				}
			} else {
				System.out.println("we shouldn't reach this statement");
			}

		}

		if (dimension.equals("z")) { // for z dimension data[k][j][i] != 0
			if (forward == 1 && backward == 0) {
				if (Math.abs(Math.abs(data[k+forward][j][i] - data[k][j][i]) - AVGDELTA[4]) / gradient[0][i][j] < stdev && data[k][j][i] > voxelBound) {
					voxelBoundary = false; 
				}  else if (data[k][j][i] > 1 || data[k][j][i] < voxelBound) {
					voxelBoundary = true; 
				}
			} else if (forward == 0 && backward == 1) {
				if (Math.abs(Math.abs(data[k-backward][j][i] - data[k][j][i]) - AVGDELTA[5]) / gradient[1][i][j] < stdev && data[k][j][i] > voxelBound) {
					voxelBoundary = false; 
				} else if (data[k][j][i] > 1) {
					voxelBoundary = true; 
				}
			} else {
				System.out.println("we shouldn't reach this statement");
			}
		}
		return voxelBoundary;
	}
	
	/**
	 * TODO: Need to update to incorporate smoothLength argument
	 * @param data
	 * @param a, b, c represent respective dimensions
	 * @param smoothLength
	 * @param dimension
	 * @return
	 */
	
	public static double[][][] movingAverage(double[][][] data, int smoothLength) {
		
		int x = data[0][0].length; int y = data[0].length; int z = data.length; // fix from being [0] at some point
		
		data = movingAverageHelper(data, z, y, x, smoothLength, "x");
		data = movingAverageHelper(data, z, x, y, smoothLength, "y");
		data = movingAverageHelper(data, x, y, z, smoothLength, "z");
		return data; 
	}
	
	public static double[][][] movingAverageHelper(double[][][] data, int a, int b, int c, int smoothLength, String dimension) {
		for (int i = 1; i < a; i++) {
			for (int j = 1; j < b; j++) { 
				for (int k = 1; k < c - 1; k++) {
					if (dimension.equals("x")) {
						data[i][j][k] = (data[i][j][k-1] + data[i][j][k] + data[i][j][k+1])/3; 
					} else if (dimension.equals("y")) {
						data[i][k][j] = (data[i][k-1][j] + data[i][k][j] + data[i][k+1][j])/3;
					} else if (dimension.equals("z")) {
						data[k][j][i] = (data[k-1][j][i] + data[k][j][i] + data[k+1][j][i])/3;
					} else {
						System.out.println("We shouldn't reach this statement.");
					}
				}
			}
		}
		return data;
	}
}
