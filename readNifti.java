
import java.io.*;
import java.util.*;

public class readNifti {
	
	public static double data[][][]; // data for storing the information from the nifti scan
	public static double rows[][][]; // 3D matrix stores the moving average for each voxel
	public static double avgVals_lh[][]; // 2D matrix for storing the moving average on the left half of the scan for each row
	public static double avgVals_rh[][]; // 2D matrix for storing the moving average on the right half of the scan for each row
	public static ArrayList<Double> slope = new ArrayList<Double>();
	public static Map<Integer,Double> boundMapLh = new HashMap<Integer,Double>(); // marks the location of the 
	public static Map<Integer,Double> boundMapRh = new HashMap<Integer,Double>();
	//public static Map<Integer, Double> priorMap = new HashMap<Integer, Double>(); will use later for estimating prior distances between edges of the scan the brain itself
	
	public static void readNiftiFinder(Nifti1Dataset inputNifti) throws IOException {
		
		inputNifti.readHeader();
		
		double[][][] rows = new double[inputNifti.XDIM-1][inputNifti.YDIM-1][inputNifti.ZDIM-1];
		
		calcAverages(inputNifti);
		
		// if (lh) {
		avgVals_lh = findMaxAvg(rows, inputNifti, "left");
		// }
		// if (rh) {
		avgVals_rh = findMaxAvg(rows, inputNifti, "right");
		// }
		// if (whole brain) {
		// combine matrices into one data set
		// }
	
		// findLinearEq();
		
		System.out.println("Finished reading data, exiting...");
		System.exit(0);
	}
	
	// calcAverages() takes a nifti data set + dimensions find the moving averages across the entire scan 
	// for the X-dimension and record them in a 3D matrix,  also rounds them to the nearest value. 
	// 
	
	public static void calcAverages(Nifti1Dataset inputNifti) {
		int i = 1; int j = 1; int k = 1; 
		while (i < inputNifti.XDIM - 1) { 
			while (j < inputNifti.YDIM) { 
				while (k < inputNifti.ZDIM) {
					rows[i-1][j-1][k-1] = Math.round(((data[k][j][i-1] + data[k][j][i] + data[k][j][i+1])/3));
					System.out.println(Math.round((data[k][j][i-1] + data[k][j][i] + data[k][j][i+1])/3));
					k++; 
				} 
				j++; 
			} 
			i++; 
		}	
	}
	
	public static Map<Integer, Double> markCoordinates(double[][][] data) {
		
		Map<Integer,Double> boundMap = new HashMap<Integer,Double>();
		
		return boundMap;
	}
	
	// TODO: Find the linear increase in the gradient and compare the moving average to it. This way we aren't looking
	// at changes to the moving average internal to the brain.
	// TODO: Add a private method to return the matrix instead of a public method.
	
	// findMaxAvg() takes the 3D matrix of rows, a 2D matrix of moving average values for each row on the 3D matrix, and
	// a boundary for which half of the scan should be processed. 
	
	
	public static double[][] findMaxAvg(double[][][] rows, Nifti1Dataset inputNifti, String half) {
		
		double[][] avgVals = new double[rows[0].length][rows[0][0].length];
		
		//double[][] coordinates = new double[][]; 
				
		int a = 0, b = 0, c = 0; double max = 0;
		
		while (a < rows[0].length) {
			while (b < rows[0][0].length) {
				if (half.equals("left")) {
					while (c < rows.length/2) {
						if (rows[c][b][a] > max) {
							max = rows[c][b][a];
						}
						c++;
					}
					avgVals[a][b] = max;
				} else { // half equals right by default
					c = rows.length;
					while (c > rows.length/2) {
						if (rows[c][b][a] > max) {
							max = rows[c][b][a];
						}
						c--;
					}
					avgVals[a][b] = max;
				}
				b++;
			}
			a++;
		}
		return avgVals;
	}
	
	/**public static double[][][] traverseMatrix(double[][][] mriData) {
		for () {
			for () {
				for () {
					// do something
				}
			}
		}
	}**/
}
