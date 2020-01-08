//import java.io.IOException;

public class smoothVolume {
	
	//static double data[][][];
	static double ORIG_DATA[][][];
	public static String STUDY = ""; 
	public static String SUBJECT = "";
	public static String LOCALPATH = "/home/ns-zalewk/Desktop/memorycube/" ;
	public static String INPUT_PATH = "";
	public static String OUTPUT_PATH = "";
	static boolean BOUNDARIES[][][];
	
	/**
	 * @param nds is the input nifti dataset volume,
	 * creates an equivalent global variable boolean grid called "BOUNDARIES" 
	 */
	
	public static void writeBoundaries(Nifti1Dataset nds) {
		
		BOUNDARIES = new boolean[nds.ZDIM][nds.YDIM][nds.XDIM];
		
		for (int i = 0; i < BOUNDARIES.length; i++) {
			for (int j = 0; j < BOUNDARIES[i].length; j++) {
				for (int k = 0; k < BOUNDARIES[i][j].length; k++) {
					BOUNDARIES[i][j][k] = false;
				}
			}
		}
	}
	
	/**
	 * @param data is nifti intensity data as 3D matrix double[][][]. 
	 * @param erode is the max length (as integer) of voxels to be eroded from the edge of the scan.
	 * @param lowBound is int denoting the lowest intensity threshold to reach before stopping. 
	 * @param doX, doY, doZ as bool passed by the client to denote which dimension are being processed. 
	 * @param half denotes which half of the scan is being traversed (-dim if !half, otherwise +dim).
	 * @return eroded mri intensity data as 3D matrix in double format
	 */
	public static double[][][] erode(Nifti1Dataset inputScan, double[][][] data, int erode, int lowBound, 
			boolean doX, boolean doY, boolean doZ, boolean half) {
		
		writeBoundaries(inputScan);
		
		int x = data[0][0].length-1; int y = data[0].length-1; int z = data.length-1;
		
		if (doX) {
			data = erodeHelper(data, erode, lowBound, z, y, x, "x", half);
			data = erodeHelper(data, erode, lowBound, z, y, x, "x", !half);
		}
		if (doY) {
			data = erodeHelper(data, erode, lowBound, z, x, y, "y", half);
			data = erodeHelper(data, erode, lowBound, z, x, y, "y", !half);
		}
		if (doZ) {
			data = erodeHelper(data, erode, lowBound, x, y, z, "z", half);
			data = erodeHelper(data, erode, lowBound, x, y, z, "z", !half);
		}
		
		return data; 
		
	}
	
	private static double[][][] erodeHelper(double[][][] data, int erode, int lowBound, 
			int a, int b, int c, String dim, boolean half) {
		
		if (erode > c/2) {
			System.out.println("ERROR: Erosion cannot be greater than half of the desired dim ension. (" + c/2 + ")");
			return data;
		}
		
		int distalLength; // the last dim loop (k) will return to this value after every iteration
		if (half) {
			distalLength = 0;
		} else {
			distalLength = -(c);
		}
		
		int k = distalLength; 
		for (int i = 0; i < a; i++) {
			for (int j = 0; j < b; j++) {
				while(k < c/2) {
					if (dim.equals("x")) {
						data = erodeHelper2(data, erode, lowBound, i, j, Math.abs(k), dim, half);
					} else if (dim.equals("y")) {
						data = erodeHelper2(data, erode, lowBound, i,  Math.abs(k), j, dim, half);
					} else if (dim.equals("z")) {
						data = erodeHelper2(data, erode, lowBound,  Math.abs(k), j, i, dim, half);
					}
					k++;
				}
				k = distalLength;
			}
		}
		return data;
	}
	
	private static double[][][] erodeHelper2(double[][][] data, int erode, int lowBound, int a, int b, int c, String dim, boolean half) {

		for (int i = 0; i < erode; i++) {
			if (dim.equals("x")) {
				c = erodeHelper3(c, half);
			} else if (dim.equals("y")) {
				b = erodeHelper3(b, half);
			} else if (dim.equals("z")) {
				a = erodeHelper3(a, half);
			}
	
			if (data[a][b][c] < lowBound && data[a][b][c] > 0.99) {
				BOUNDARIES[a][b][c] = true;
				break; 
			} else if (data[a][b][c] != 0 && !BOUNDARIES[a][b][c]) {
				data[a][b][c] = 0;
			}
		}	
		return data;
	}
	
	private static int erodeHelper3(int val, boolean half) {
		if (half) {
			return val++;
		} else {
			return val--;
		}
	}
	
	// ------------------------------------------------------------ //
	
	/**
	 * 
	 * @param data data is nifti intensity data as 3D matrix double[][][].
	 * @param erode is the max width (as integer) of voxels omitted from thresholding to be cleaned.  
	 * @param doX, doY, doZ as bool passed by the client to denote which dimension are being processed. 
	 * @return mri intensity data as 3D matrix in double format with extraneous streaks of voxels outside of the scan removed. 
	 */
	
	public static double[][][] cleanUp(double[][][] data, int erode, boolean doX, boolean doY, boolean doZ, boolean half) {
		
		int x = data[0][0].length-1; int y = data[0].length-1; int z = data.length-1;
		
		if (doX) {
			data = cleanUpHelper(data, erode, z, y, x, "x", half);
			data = cleanUpHelper(data, erode, z, y, x, "x", !half);
		}
		if (doY) {
			data = cleanUpHelper(data, erode, z, x, y, "y", half);
			data = cleanUpHelper(data, erode, z, x, y, "y", !half);
		}
		if (doZ) {
			data = cleanUpHelper(data, erode, x, y, z, "z", half);
			data = cleanUpHelper(data, erode, x, y, z, "z", !half);
		}
		return data; 	
	}
	
	private static double[][][] cleanUpHelper(double[][][] data, int erode, int a, int b, int c, String dim, boolean half) {
		
		int distalLength; int incr; 
		if (half) {
			distalLength = 0; incr = 1;
		} else {
			distalLength = -(c); incr = -1;
		}
		
		int k = distalLength; 
		for (int i = 0; i < a; i++) {
			for (int j = 0; j < b; j++) {
				while (k < c/2) {
					if (dim.equals("x") && data[a][b][c] == 0 && data[a][b][c+incr] != 0) {
						data = cleanUpHelper2(data, erode, i, j, Math.abs(k), incr, dim);
						break;
					} else if (dim.equals("y") && data[a][c][b] == 0 && data[a][c+incr][b] != 0) {
						data = cleanUpHelper2(data, erode, i, Math.abs(k), j, incr, dim);
						break;	
					} else if (dim.equals("z") && data[c][b][a] == 0 && data[c+incr][b][a] != 0) {
						data = cleanUpHelper2(data, erode, Math.abs(k), j, i, incr, dim);
						break;
					}
					k++;
				}
			}
		}
		return data;
	}
	
	private static double[][][] cleanUpHelper2(double[][][] data, int erode, int i, int j, int k, int incr, String dim) {
		
		int count = 0; int l = 0;
		while (Math.abs(l) < erode) {
			if (dim.equals("x") && data[i][j][k+l+incr] == 0) {
				while(Math.abs(count) < erode) {
					data[i][j][k+count] = 0;
					count=count+incr;
				}
				break;
			} else if (dim.equals("y") && data[i][k+l+incr][j] == 0) {
				while(Math.abs(count) < erode) {
					data[i][k+count][j] = 0;
					count=count+incr;
				}
				break;
			} else if (dim.equals("z") && data[k+l+incr][j][i] == 0) {
				while(Math.abs(count) < erode) {
					data[k+count][j][i] = 0;
					count=count+incr;
				}
				break;
			}
			l = l+incr;
		}	
		return data;
	}
	
	/** this function not finished yet
	public static double[][][] erodeRecursively(double[][][] data, int counter, int erode, int i, int j, int k, String dimension, boolean half) {
		if (counter == erode || data[i][j][k] != 0) {
			return data;
		} else {
			data[i][j][k] = (double) 0;
			counter++;
			if (dimension.equals("x")) {
				if (half) {
					erodeRecursively(data, counter, erode, i, j, k+counter, dimension, half);
				} else {
					erodeRecursively(data, counter, erode, i, j, k-counter, dimension, half);
				}
			} else if (dimension.equals("y")) {
				if (half) {
					erodeRecursively(data, counter, erode, i, k+counter, j, dimension, half);
				} else {
					erodeRecursively(data, counter, erode, i, j, k-counter, dimension, half);
				}
			} else if (dimension.equals("z")) {
				if (half) {
				erodeRecursively(data, counter, erode, k-counter, j, i, dimension, half);
				} else {
					erodeRecursively(data, counter, erode, i, j, k-counter, dimension, half);
				}
			} else {
				System.out.println("We shouldn't reach this statement.");
			}
		}
		return data;
	}*/
	
	// ------------------------------------------------------------ //
	
	/**
	 * @param data
	 * @param length
	 * @param doX, doY, doZ
	 * @param half
	 * @return
	 */
	
	public static double[][][] patchOvershots(double[][][] data, int length, boolean doX, boolean doY, boolean doZ, boolean half) {
		
		int x = data[0][0].length-1; int y = data[0].length-1; int z = data.length-1;
		
		if (doX) {
			data = patchOvershotsHelper(data, length, z, y, x, "x", half);
			data = patchOvershotsHelper(data, length, z, y, x, "x", !half);
		}
		if (doY) {
			data = patchOvershotsHelper(data, length, z, x, y, "y", half);
			data = patchOvershotsHelper(data, length, z, x, y, "y", !half);
		}
		if (doZ) {
			data = patchOvershotsHelper(data, length, x, y, z, "z", half);
			data = patchOvershotsHelper(data, length, x, y, z, "z", !half);
		}
		return data;
	}
	
	private static double[][][] patchOvershotsHelper(double[][][] data, int length, int a, int b, int c, String dim, boolean half) {
		
		int distalLength; // the last dim loop (k) will return to this value after every iteration
		if (half) {
			distalLength = 0;
		} else {
			distalLength = -(c);
		}
		
		int k = distalLength; 
		for (int i = 0; i < a; i++) {
			for (int j = 0; j < b; j++) {
				while(k < c/2) {
					patchOvershotHelper2(data, length);
				}
				k++;
			}
		}
		return data;
	}
	
	private static double[][][] patchOvershotHelper2(double[][][] data, int length) {
		int counter = 0;
	
		while (< length) {
			if () {
				
			}
			counter++;
		}
		return data;
	}
}
