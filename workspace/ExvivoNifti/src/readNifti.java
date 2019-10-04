
import java.io.*;
import java.util.*;

public class readNifti {
	
	public static double data[][][]; // data for storing the information from the nifti scan
	public static double rows[][][]; // 3D matrix stores the moving average for each voxel
	public static double avgVals_1[][]; // 2D matrix for storing the moving average on the left half of the scan for each row
	public static double avgVals_2[][]; // 2D matrix for storing the moving average on the right half of the scan for each row
	public static String LOCALPATH = "/home/ns-zalewk/workspace/ExvivoNifti/src/"; //default, change at some point
	public static String SCANNAME = "";
	public static ArrayList<Double> slope = new ArrayList<Double>();
	public static Map<Integer,Double> boundMapLh = new HashMap<Integer,Double>(); // marks the location of the 
	public static Map<Integer,Double> boundMapRh = new HashMap<Integer,Double>();
	//public static Map<Integer, Double> priorMap = new HashMap<Integer, Double>(); will use later for estimating prior distances
	
	public static void main(String args[]) throws IOException {
		
		checkArgs(args);
		
		Nifti1Dataset inputNifti = new Nifti1Dataset(LOCALPATH + args[0]);
		
		inputNifti.readHeader();
		
		data = inputNifti.readDoubleVol(Short.parseShort(args[4])); // timecourse
		
		double[][][] rows = new double[inputNifti.XDIM-1][inputNifti.YDIM-1][inputNifti.ZDIM-1];
		
		calcAverages(inputNifti);
		
		avgVals_1 = findMaxAvg(rows, inputNifti, "left");
		avgVals_2 = findMaxAvg(rows, inputNifti, "right");
		
		// simply for testing
		int x = 0;
		while (x < avgVals_1.length) {
			System.out.println(avgVals_1[x][0]);
			x++;
		}
		
		// writeToScan();
		// findLinearEqu();
		
		System.out.println("Exiting...");
		System.exit(0);
	}
	
	// TODO: Organize it a bit better? 
	
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
	
	// TODO: Find the linear increase in the gradient and compare the moving average to it. This way we aren't looking
	// at changes to the moving average internal to the brain.
	// TODO: Add a private method to return the matrix instead of a public method.
	
	// findMaxAvg() takes the 3D matrix of rows, a 2D matrix of moving average values for each row on the 3D matrix, and
	// a boundary for which half of the scan should be processed. The l
	
	public static double[][] findMaxAvg(double[][][] rows, Nifti1Dataset inputNifti, String half) {
		double[][] avgVals = new double[rows[0].length][rows[0][0].length];
		int a = 0; int b = 0; int c = 0; double max = 0;
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
	
	// TODO: Is time-course necessary for opening nifti scan? 
	
	// checkArgs() takes arguments give to the program as a string and allows for client input when no arguments are specified 
	// for the program otherwise uses given nifti file name and path for processing. Exits program if nothing is specified.
	
	public static void checkArgs(String[] args) {
		if (args.length < 1) {
			Scanner console = new Scanner(System.in);
			System.out.println("Would you like to input a new scan/path to denote for processing? (yes/no) : ");
			String response = console.nextLine().toLowerCase();
			
			if ( response.startsWith("y") || response.contains("yes") ) {
			
				System.out.println("Input path here: ");
				LOCALPATH = console.nextLine();
				
				System.out.println("Input name of the scan here: ");
				SCANNAME = console.nextLine();
				
				console.close();
				return;
			} else {
				console.close();
				System.out.println("Exiting without arguments.");
				System.exit(0);
			}
		} else {
			
			System.out.println("Using " + LOCALPATH + " as path.");
			System.out.println("Using " + args[0] + " as scan.");
			return;
		}
	}
}
