import java.math.*;

public class findLinearEq {
	
	static double slopes[][]; // 
	static double m; // slope of the gradient
	
	
	/** TODO: Y and Z dimensions are mixed up! Fix at some point! */
	
	// we're only going to take the first third (when bound == 3), but this can be altered
	public static double[] findLinearEquations(double data1[][][], int bound, boolean xdim, boolean ydim, boolean zdim) {
		
		double[] avgDelta = new double[6];
		double[] stdDev = new double[6];
		
		double lineVal = 0;
		double lineVal2 = 0; // for the opposite direction
		int count = 0; // counts non-zero voxels
		
		double[][] Xgradient1 = new double[data1.length][data1[0].length];
		double[][] Xgradient2 = new double[data1.length][data1[0].length];
		double[][] Ygradient1 = new double[data1[0].length][data1[0][0].length];
		double[][] Ygradient2 = new double[data1[0].length][data1[0][0].length];
		double[][] Zgradient1 = new double[data1.length][data1[0][0].length];
		double[][] Zgradient2 = new double[data1.length][data1[0][0].length]; 
		
		// x dimension
		if (xdim == true) {
			lineVal = 0;
			lineVal2 = 0;
			
			double[] residuals = new double[data1[0][0].length/bound]; // residuals for calculating stdev
			
			for (int i = 0; i < data1.length; i++) {
				for (int j = 0; j < data1[i].length; j++) {
					for (int k = 0; k < data1[i][j].length/bound; k++) {
						if (data1[i][j][k] != 0) {
							count++;
							lineVal = lineVal + (data1[i][j][k+1] - data1[i][j][k]);
							residuals[k] = data1[i][j][k+1] - data1[i][j][k];
						} else {
							residuals[k] = 0;
						}
					}
					count = 0;
					Xgradient1[i][j] = standardDeviation(residuals, lineVal);
					
					for (int k = data1[i][j].length - 1; k > data1[i][j].length/bound; k--) {
						if (data1[i][j][k] != 0) {
							count++;
							lineVal2 = lineVal2 + (data1[i][j][k] - data1[i][j][k-1]);
							residuals[data1[i][j].length - k] = data1[i][j][k] - data1[i][j][k-1];
						} else {
							residuals[data1[i][j].length - k] = 0;
						}
					}
					count = 0; 
					Xgradient2[i][j] = standardDeviation(residuals, lineVal2);
				}
			}
			
			avgDelta[0] = lineVal/((data1[0][0].length/bound) * data1[0].length * data1.length);
			avgDelta[1] = lineVal2/((data1[0][0].length/bound) * data1[0].length * data1.length);
		}
		
		// y dimension
		if (ydim == true) {	
			lineVal = 0;
			lineVal2 = 0;
			double[] residuals = new double[data1.length/bound];
			
			// for calculating stdev
			for (int j = 0; j < data1[0][0].length; j++) {
				for (int i = 0; i < data1[0].length; i++) {
					for (int k = 0; k < data1.length/bound; k++) {
						if (data1[k][i][j] != 0) {
							count++;
							lineVal = lineVal + (data1[k+1][i][j] - data1[k][i][j]);
							residuals[k] = data1[k+1][i][j] - data1[k][i][j];
						} else {
							residuals[k] = 0;
						}
					}
					
					Ygradient1[i][j] = standardDeviation(residuals, lineVal/count);
					count = 0;
				}
			}

			for (int j = 0; j < data1[0][0].length; j++) {
				for (int i = 0; i < data1[0].length; i++) {
					for (int k = data1.length - 1; k > data1.length/bound; k--) {
						if (data1[k][i][j] != 0) {
							lineVal2 = lineVal2 + (data1[k][i][j] - data1[k-1][i][j]);
							count++;
							residuals[data1.length - k] = (data1[k][i][j] - data1[k-1][i][j]);
						} else {
							residuals[data1.length - k] = 0;
						}
					}
					
					Ygradient2[i][j] = standardDeviation(residuals, lineVal2/count);
					count = 0;
				}
			}
	
			avgDelta[2] = lineVal/((data1.length/bound) * data1[0].length * data1[0][0].length);
			avgDelta[3] = lineVal2/((data1.length/bound) * data1[0].length * data1[0][0].length);
		}
		
		// z dimension
		if (zdim == true) {
			lineVal = 0;
			lineVal2 = 0;
			double[] residuals = new double[data1[0].length/bound];
			
			
			for (int i = 0; i < data1.length; i++) {
				for (int j = 0; j < data1[0][i].length; j++) {
					for (int k = 0; k < data1[i].length/bound; k++) {
						if (data1[i][k][j] != 0) {
							count++;
							lineVal = lineVal + (data1[i][k+1][j] - data1[i][k][j]);
							residuals[k] = data1[i][k+1][j] - data1[i][k][j];
						} else {
							residuals[k] = 0;
						}
					}
					
					Zgradient1[i][j] = standardDeviation(residuals, lineVal/count);
					count = 0;
					
					for (int k = data1[i].length - 1; k > data1[i].length/bound; k--) {
						if (data1[i][k][j] != 0) {
							count++;
							lineVal2 = lineVal2 + (data1[i][k+1][j] - data1[i][k][j]);
							residuals[data1[i].length - k] = data1[i][k][j] - data1[i][k-1][j];
						} else {
							residuals[data1[i].length - k] = 0;
						}
					}
					
					Zgradient2[i][j] = standardDeviation(residuals, lineVal2/count);
					count = 0;
				}
			}
			
			avgDelta[4] = lineVal/((data1[0].length/bound) * data1[0][0].length * data1.length);
			avgDelta[5] = lineVal2/((data1[0].length/bound) * data1[0][0].length * data1.length);
		}
		
		return avgDelta;
	}
	
	public static double standardDeviation(double[] data, double average) {
				
		double standardDev = 0;
		for (int i = 0; i < data.length; i++) {
			standardDev += (data[i]-average)*(data[i]-average);
		}
		return Math.sqrt((standardDev)/data.length - 1); 
	}
	
	/** // marks the boundary where the standard deviation differs
	public static boolean[][][] boundaryMarker(boolean[][][] boundaries, double[][] stdDevGrid, double threshold, int dim1, int dim2, int dim3) {
		for (int i = 0; i < dim1; i++) {
			for (int j = 0; j < dim2; j++) {
				for (int k = 0; k < dim3; k++) {
					if () {
						
					}
				}
			}
		}
		return boundaries;
	}*/
	
	
	/**for (int i = 0; i < data1.length; i++) {
	for (int k = 0; k < data1[i].length/bound; k++) {
		for (int j = 0; j < data1[i][k].length; j++) {
			if (data1[i][k][j] != 0) {
				lineVal = lineVal + (data1[i][k+1][j] - data1[i][k][j]);
				residuals[j] = data1[i][k+1][j] - data1[i][k][j];
			} else {
				residuals[j] = 0;
			}
		}
	}		
	
	for (int k = data1[i].length - 1; k > data1[i].length/bound; k--) {
		for (int j = 0; j < data1[i][k].length; j++) {
			if (data1[i][k][j] != 0) {
				lineVal2 = lineVal2 + (data1[i][k][j] - data1[i][k-1][j]);
				//Zgradient2[i][k][j] = data1[i][k][j] - data1[i][k-1][j];
			} else {
				//Zgradient2[i][k][j] = 0;
			}
		}
	}
}*/
	/** for (int k = 0; k < data1.length/bound; k++) {
	for (int i = 0; i < data1[k].length; i++) {
		for (int j = 0; j < data1[k][i].length; j++) {
			if (data1[k][i][j] != 0) {
				lineVal = lineVal + (data1[k+1][i][j] - data1[k][i][j]);
				//Ygradient1[k][i][j] = data1[k+1][i][j] - data1[k][i][j];
			} else {
				//Ygradient1[k][i][j] = 0;
			}
		}
	}
}


for (int k = data1.length - 1; k > data1.length/bound; k--) {
	for (int i = 0; i < data1[k].length; i++) {
		for (int j = 0; j < data1[k][i].length; j++) {
			if (data1[k][i][j] != 0) {
				lineVal2 = lineVal2 + (data1[k][i][j] - data1[k-1][i][j]);
				//Ygradient2[k][i][j] = data1[k][i][j] - data1[k-1][i][j];
			} else {
				//Ygradient2[k][i][j] = 0;
			}
		}
	}
}
*/
}
