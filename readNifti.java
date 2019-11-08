
import java.io.*;
//import java.util.*;

public class readNifti {
	
	public static double avgVals_lh[][]; // 2D matrix for storing the moving average on the left half of the scan for each row
	public static double avgVals_rh[][]; // 2D matrix for storing the moving average on the right half of the scan for each row

	//public static Map<Integer, Double> priorMap = new HashMap<Integer, Double>(); will use later for estimating prior distances between edges of the scan the brain itself
	
	public static int[][] readNiftiFinder(Nifti1Dataset inputNifti, double[][][] data) throws IOException {
			
		int x = inputNifti.XDIM; int y = inputNifti.YDIM; int z = inputNifti.ZDIM; // scan dimensions need to match, can adjust in future
		
		double[][][] movingAvg = calcAvgThreeDim(data, 1, x, y, z);
		double [][] avgGrid = calcAvgTwoDim(movingAvg, x, y, z);
		double[][] stDevGrid = calcStDev(data, avgGrid, x, y, z);
		int[][] dimBound = calcBoundary(movingAvg, stDevGrid, x, y, z);
		
		System.out.println("Finished reading data.");
		return dimBound;
	}
	
	/**
	 * @param inputNifti, timeCourse, the inputNifti is the data to retrieve the matrix from, the timeCourse
	 * is for multi-volume scans (this value will likely be 0).
	 * @returns the nifti data a double matrix format @throws IOException
	 */
	
	public static double[][][] dataParse(Nifti1Dataset inputNifti, int timeCourse) throws IOException {
		return inputNifti.readDoubleVol((short) timeCourse); // adjust if different time course is needed
	}
	
	/**
	 * calcBoundary() takes a matrix of moving averages, the original matrix of data and the nifti dimensions. 
	 * The function looks for the boundary of the brain and marks the voxel in 3-dimensional space. 
	 * Averages are originally in double format and rounded to the nearest value.
	 * 
	 * @params inputNifti, dim_1/dim_2/dim3, the desired nifti scan and the parameters therein are arguments.
	 * Usually these will be nifti.XDIM, nifti.YDIM, nifti.ZDIM respectively, but some scans are oriented otherwise. 
	 * @throws IOException in-case boundaries of array are exceeded. 
	 **/
	
	public static int[][] calcBoundary(double[][][] movingAvg, double[][] stDevGrid, int dim_1, int dim_2, int dim_3) throws IOException {
		
		// will have to do this for both hemispheres
		double residual = 0.0;
		boolean boundary = false; 
		int[][] dimBound = new int[dim_3][dim_2];
		
		for (int i = 0; i < dim_3; i++) { // maybe subtract from Z? 
			for (int j = 0; j < dim_2; j++) {
				for (int k = 0; k < dim_1 - 1; k++) {
					residual = (movingAvg[i][j][k]*movingAvg[i][j][k])/(movingAvg[i][j].length - 1);
					// test statement here
					if (residual > 0.5 && boundary == false) {		
						dimBound[i][j] = (int) Math.round(movingAvg[i][j][k]); 
						boundary = true;
					}
				}
				boundary = false;
			}
		}
		return dimBound;
	}
	
	/** The following three functions of calcAvgDim() take either a matrix of values corresponding to
	 * the mri nifti-1 data, or a moving average in one dimension of this data. The result is eventually
	 * the average of all values in a single row on one hemisphere of the brain. 
	 * This data can be used to determine where the boundary of the brain is on the scan. 
	 * @param movingAvg or data[][][], dimensions dim_1, dim_2, dim_3 corresponding the mri dimensions
	 * in integer format. 
	 * @returns the average of the slice provided to the function
	 */
	
	public static double[] calcAvgOneDim(double[][] twoDimSliceAvg, int dim_1, int dim_2) {
		
		double[] sliceAvg = new double[dim_1];
		int lengthCounter = 0;
		
		for (int i = 0; i < twoDimSliceAvg.length; i++) {
			for (int j = 0; j < twoDimSliceAvg[i].length; j++) {
				sliceAvg[i] += twoDimSliceAvg[i][j];
				if (twoDimSliceAvg[i][j] != 0) {
					lengthCounter++;
				}
			}
			if (sliceAvg[i] != 0 && lengthCounter != 0) {
				sliceAvg[i] = sliceAvg[i]/lengthCounter;
			} else {
				sliceAvg[i] = 0;
			}
		}
		return sliceAvg;
	}
	
	public static double[][] calcAvgTwoDim(double[][][] movingAvg, int dim_1, int dim_2, int dim_3) {
		
		double[][] avgGrid = new double[dim_3][dim_2];
		int lengthCounter = 0; //only counting non-zero voxels	
		
		for (int i = 0; i < dim_3; i++) { 
			for (int j = 0; j < dim_2; j++) { 
				for (int k = 0; k < dim_1 - 1; k++) {
					if (movingAvg[i][j][k] != 0) { // allow the option for zero-voxels to be counted at some point
						lengthCounter++;
					}
					avgGrid[i][j] += movingAvg[i][j][k];
				}
				if (avgGrid[i][j] != 0 && lengthCounter != 0) {
					avgGrid[i][j] = avgGrid[i][j]/lengthCounter;			
				} else {
					avgGrid[i][j] = 0;
				}
				lengthCounter = 0;
			}
			calcAvgOneDim(avgGrid, dim_3, dim_2);
		}
		return avgGrid;
	}
	
	// TODO: Incorporate the width of the moving average into the calculation and allow adjustments
	public static double[][][] calcAvgThreeDim(double[][][] data, int width, int dim_1, int dim_2, int dim_3) {
		
		double[][][] movingAvg = new double[dim_3][dim_2][dim_1]; 
			
		for (int i = 1; i < dim_3; i++) {
			for (int j = 1; j < dim_2; j++) { 
				for (int k = 1; k < dim_1 - 1; k++) {
					movingAvg[i-1][j-1][k-1] = ((data[i][j][k-1] + data[i][j][k] + data[i][j][k+1])/3);
				}
			}
		}
		return movingAvg;
	}
	
	public static double[][] calcStDev(double[][][] data, double[][] avgGrid, int dim_1, int dim_2, int dim_3) {
	  
		double stDevTotal = 0;
		double[][] stDevGrid = new double[dim_3][dim_2]; 
	   
		for (int i = 0; i < dim_3; i++) {
			for (int j = 0; j < dim_2; j++) { 
				for (int k = 0; k < dim_1 - 1; k++) {
					if (data[i][j][k] != 0) {
						stDevTotal += data[i][j][k] - avgGrid[i][j];
					} 
				}
				// calculate standard deviation for each row
				stDevGrid[i][j] = Math.sqrt(stDevTotal*stDevTotal)/(data[i][j].length - 1);
				stDevTotal = 0;	
			}
		}
		
	    return stDevGrid; 
	}
	
	// TODO: Find the linear increase in the gradient and compare the moving average to it. This way we aren't looking
	// at changes to the moving average internal to the brain.
	// TODO: Add a private method to return the matrix instead of a public method.

	/**
	 * findMaxAvg() takes the 3D matrix of rows and a boundary for which half of the scan should be processed. 
	 * @param data, half, the data are double values that correspond with the mri nifti data, the half
	 * corresponds with whether the right or left hemisphere is being processed. 
	 * @returns average values for each row and creates a normalized vector for determining the gradient
	 * change over one dimension of the scan.
	 */
	
	public static double[][] findMaxAvg(double[][][] data, String half) {
		
		double[][] avgVals = new double[data[0].length][data[0][0].length];
		double max = 0;
		
		for (int a = 0; a < data.length; a++) {
			for(int b = 0; b < data[a].length; b++) {
				if (half.equals("left")) {
					for (int c = 0; c < data.length/2; c++) {
						if (data[a][b][c] > max) {
							max = data[a][b][c];
						}
					}
				} else { // half equals right by default		
					for (int c = data.length; c > data.length/2; c--) {
						if (data[a][b][c] > max) {
							max = data[a][b][c];
						}				
					}
					
				}
				avgVals[a][b] = max;
			}
		}
		return avgVals;
	}
	
	
	// maybe perform the calculation recursively rather than using a bajillion for loops? could also use a map
	// for the bounary marker
	
	/** public static Map<Integer, Double> markCoordinates(double[][][] data) {
	
	Map<Integer,Double> boundMap = new HashMap<Integer,Double>();
	return boundMap;
	}  **/	
	
	
	
	/**public static double[][][] traverseMatrix(double[][][] mriData, int counter, int boundary) {
	 * while () {
		 *  if (counter == boundary) {
		 *  	return mriData;
			} else {
				movingAvg[i-1][j-1][k-1] = ((data[k][j][i-1] + data[k][j][i] + data[k][j][i+1])/3);
				counter++;
				traverseMatrix(mriData);
				mri[][][]
			}
		}
	}**/
}
