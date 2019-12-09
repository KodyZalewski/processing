package exvivo;

public class FindGradient {
	
	public static int LINEVAL = 0;
	public static int LINEVAL2 = 0;
	public static int COUNT = 0;
	public static double[] AVGDELTA = new double[6];
	public static boolean voxelBoundary; 
	/** 
	 * the second-to-last argument as an integer is how far you want to traverse to find the gradient
	 * e.g. 3 means that it will take only the left and right third of the scan since the brain and actual
	 * data is likely to be in the middle. The last argument is the dimension we're looking for. 
	 */
	
	public static double[] findGradient(Nifti1Dataset data) {
		
		findGradientHelper(data, data.ZDIM, data.YDIM, data.XDIM, 3, x);
		findGradientHelper(data, data.ZDIM, data.XDIM, data.YDIM, 3, y);
		findGradientHelper(data, data.XDIM, data.YDIM, data.ZDIM, 3, z);
		//return AVGDELTA;
		
		traverseData(data, data.ZDIM, data.YDIM, data.XDIM, 3, x);
		traverseData(data, data.ZDIM, data.XDIM, data.YDIM, 3, y);
		traverseData(data, data.XDIM, data.YDIM, data.ZDIM, 3, z); 
	}
	
	// a, b and c are the dimensions used for traversing the scan
	public static double[][][] findGradientHelper(double[][][] data, int a, int b, int c, int bound, String dimension) {
		
		// two gradients for moving toward the center of the scan
		double[][] gradient1 = new double[a][b]; double[][] gradient2 = new double[a][b];
		// for storing the average change and standard deviation of the scan
		double[] avgDelta = new double[6]; double[] stdDev = new double[6];
		
		double[][][] gradients = new double[2][a][b]; 
		gradients[0][0][0] = gradient 1; gradient[1][0][0] = gradient2;  
		
		// for calculating the standard deviation
		double[] residuals = new double[c/bound]; double[] residuals2 = new double[c/bound];
		
		// counter for traversing the scan and counting non-zero voxels
		int COUNT = 0;
		
		for (int i = 0; i < a; i++) {
			for (int j = 0; j < b; j++) {
				for (int k = 0; k < c/bound; k++) {
					residuals = calcResiduals(data, residuals, 1, 0, i, j, k, k, dimension); 
				}
				COUNT = 0;
				gradient1[i][j] = standardDeviation(residuals, LINEVAL);
				
				for (int k = c - 1; k > c/bound; k--) {
					residuals2 = calcResiduals(data, residuals2, 0, 1, i, j, k, (c-k), dimension);
				}
				COUNT = 0;
				gradient2[i][j] = standardDeviation(residuals2, LINEVAL2);
			}
		}
		setAvgDelta(a, b, c, dimension);
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
	
			if (data[i][j][k] != 0 && dimension.equals("x")) { // for x dimension
				COUNT++;
				if (forward == 1 && backward == 0) {
					LINEVAL = LINEVAL + (data[i][j][k+forward] - data[i][j][k-backward]);
				} else if (forward == 0 && backward == 1) {
					LINEVAL2 = LINEVAL2 + (data[i][j][k+forward] - data[i][j][k-backward]);
				}
				residuals[residValue] = data[i][j][k+forward] - data[i][j][k-backward];
			}
			
			if (data[i][k][j] != 0 && dimension.equals("y")) { // for y dimension
				COUNT++;
				if (forward == 1 && backward == 0) {
					LINEVAL = LINEVAL + (data[i][k+forward][j] - data[i][k-backward][j]);
				} else if (forward == 0 && backward == 1) {
					LINEVAL2 = LINEVAL2 + (data[i][k+forward][j] - data[i][k-backward][j]);
				}
				residuals[residValue] = data[i][k+forward][j] - data[i][k-backward][j];
			}
			
			if (data[k][j][i] != 0 && dimension.equals("z")) { // for z dimension
				COUNT++;
				if (forward == 1 && backward == 0) {
					LINEVAL = LINEVAL + (data[k+forward][j][i] - data[k-backward][j][i]);
				} else if (forward == 0 && backward == 1) {
					LINEVAL2 = LINEVAL2 + (data[k+forward][j][i] - data[k-backward][j][i]);
				}
				residuals[residValue] = data[k+forward][j][i] - data[k-backward][j][i];
			}
			
		} else {
			residuals[residValue] = 0;
		}
		return residuals;
	}
		
	
	/** @author kody
	 *  @params data of a line of voxels of mri data,
	 *  the average intensity of the line of voxel data as double
	 *  @returns the standard deviation of the voxel residuals
	*/
	public static double standardDeviation(double[] residuals, double average) {
		
		double standardDev = 0;
		for (int i = 0; i < residuals.length; i++) {
			standardDev += (residuals[i]-average)*(residuals[i]-average);
		}
		return Math.sqrt((standardDev)/residuals.length - 1); 
	}
	
	/**
	*/
	public static setAvgDelta(int a, int b, int c, String dimension) {
		if (dimension.equals("x")) {
			AVGDELTA[0] = LINEVAL/((c/bound) * b * a);
			AVGDELTA[1] = LINEVAL2/((c/bound) * b * a); 	
		} else if (dimension.equals("y")) {
			AVGDELTA[2] = LINEVAL/(c * (b/bound) * a);
			AVGDELTA[3] = LINEVAL2/(c * (b/bound) * a);
		} else if (dimension.equals("z")) {
			AVGDELTA[4] = LINEVAL/(c * b * (a/bound));
			AVGDELTA[5] = LINEVAL2/(c * b * (a/bound));
		} else {
			System.out.println("No dimension specified");
		}
	}
	
	/**
	public static double[][][] traverseData(double[][][] data, int a, int b, int c, int bound, String dimension) {
		
		new double value = 0;
		
		for (int i = 0; i < a; i++) {
			for (int j = 0; j < b; j++) {
				for (int k = 0; k < c/bound; k++) {
					value = returnVoxel(data, i, j, k, k, dimension); 
					if (value is more than 2 standard deviations away from the average) {
						break;
					} else {
						data = replaceVoxel(data);
					}
				}
				voxelBoundary == false; 
				
				for (int k = c - 1; k > c/bound; k--) {
					value = returnVoxel(data, residuals2, 0, 1, i, j, k, (c-k), dimension);
					if (value ) {
						break; 
					} else {
						data = replaceVoxel(data); 
					}
				}
				
				voxelBoundary == false; 
			}
		}
		return data;
	}
	
	
	public static double returnVoxel(double[][][] data, int i, int j, int k, String dimension) {
		
		if (dimension.equals("x")) { // for x dimension data[i][j][k] != 0
			return data[][][]; 
		}

		if (dimension.equals("y")) { // for y dimension data[i][k][j] != 0
			return data[][][]; 
		}

		if (dimension.equals("z")) { // for z dimension data[k][j][i] != 0
			return data[][][];
		} 
	}
	
	public static double[][][] replaceVoxel(double[][][] data, int i, int j, int k, String dimension) {
		
		if (dimension.equals("x")) { // for x dimension data[i][j][k] != 0
			if () {
				data[][][] == 0; 
			} else if {
				data[][][] == 
			}
			return data; 
		}

		if (dimension.equals("y")) { // for y dimension data[i][k][j] != 0
			if () {
				data[][][] == 0; 
			} else if () {
				
			}
			return data;
		}

		if (dimension.equals("z")) { // for z dimension data[k][j][i] != 0
			if () { 
				 data[][][] == 0; 
			} else if {
				
			}
			return data;
		}
		
		return 0;
	}
	*//
	
	
}
