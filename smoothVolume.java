
public class smoothVolume {
	
	//static double ORIG_DATA[][][];
	static boolean BREAK = false; 
	static boolean BOUNDARIES[][][];
	
	// ------------------------------------------------------------ //
	
	/**
	 * @author KJZ
	 * @param nds is the input nifti dataset volume.
	 * @result Creates an equivalent global variable boolean grid named "BOUNDARIES". 
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
	
	// ------------------------------------------------------------ //
	
	/**
	 * @author KJZ
	 * @param Takes data as double 3D matrix.
	 * @param int a, b, c represent respective dimensions depending on whichdimension (passed as String) is being traversed.
	 * @param length as integer denotes how many voxels parallel to the given dimension to incorporate
	 * into smoothing (default is 1).
	 * @return double 3D matrix to be written to a new scan as a smoothed volume. 
	 * @result The function reduces the number of outliers is reduced across the scan, ergo making thresholding of 
	 * voxels outside of the brain more robust. 
	 * 
	 */

	public static double[][][] movingAverage(double[][][] data, int length) {

		int x = data[0][0].length; int y = data[0].length; int z = data.length; // fix from being [0] at some point

		data = movingAverageHelper(data, z, y, x, length, "x");
		data = movingAverageHelper(data, z, x, y, length, "y");
		data = movingAverageHelper(data, x, y, z, length, "z");
		return data; 
	}

	public static double[][][] movingAverageHelper(double[][][] data, int a, int b, int c, int length, String dimension) {
		for (int i = length; i < a; i++) {
			for (int j = length; j < b; j++) { 
				for (int k = length; k < c - length; k++) {
					int newData = 0;
					if (dimension.equals("x")) {			
						for (int l = 0; l < length; l++) {
							newData += (data[i][j][k-l] + data[i][j][k+l]);
						} 
						data[i][j][k] = (newData + data[i][j][k])/((length*2)+1); 
					} 
					if (dimension.equals("y")) {
						for (int l = 0; l < length; l++) {
							newData += (data[i][k-l][j] + data[i][k+1][j]);
						}
						data[i][k][j] = (newData + data[i][k][j])/((length*2)+1); 				
					} 
					if (dimension.equals("z")) {
						for (int l = 0; l < length; l++) {
							newData += (data[k-l][j][i] + data[k+l][j][i]);
						}
						data[k][j][i] = (newData + data[k][j][i])/((length*2)+1); 
					} 
				}
			}
		}
		return data;
	}
		
	// ------------------------------------------------------------ //
	
	/**
	 * @author KJZ
	 * @param data is nifti intensity data as 3D matrix double[][][]. 
	 * @param erode is the max length (as integer) of voxels to be eroded from the edge of the scan.
	 * @param lowBound is int denoting the lowest intensity threshold to reach before stopping. 
	 * @param doX, doY, doZ as bool passed by the client to denote which dimension are being processed. 
	 * @param half denotes which half of the scan is being traversed (-dim if !half, otherwise +dim).
	 * @return eroded mri intensity data as 3D matrix in double format
	 * @result The number of voxels provided by the client around the edge of the scan are thresholded out
	 * provided they are above the intensity of lowBound. 
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
		
		int distalLength; int incr; // the last dim loop (k) will return to this value after every iteration
		if (half) {
			distalLength = 0; incr = 1;
		} else {
			distalLength = -(c); incr = -1;
		}
		
		int k = distalLength; 
		for (int i = 0; i < a; i++) {
			for (int j = 0; j < b; j++) {
				BREAK = false;
				while(k < c/2 && Math.abs(k) > 0) {
					
					if (dim.equals("x") && data[i][j][Math.abs(k)] == 0 && data[i][j][Math.abs(k)+incr] > 0.99) {
						data = erodeHelper2(data, erode, lowBound, i, j, Math.abs(k), dim, half);
					} else if (dim.equals("y") && data[i][Math.abs(k)][j] == 0 && data[i][Math.abs(k)+incr][j] > 0.99) {
						data = erodeHelper2(data, erode, lowBound, i,  Math.abs(k), j, dim, half);
					} else if (dim.equals("z") && data[Math.abs(k)][j][i] == 0 && data[Math.abs(k)+incr][j][i] > 0.99) {
						data = erodeHelper2(data, erode, lowBound,  Math.abs(k), j, i, dim, half);
					}
					k++;
					if (BREAK) {
						break;
					}
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
			} else if (data[a][b][c] != 0 && data[a][b][c] > lowBound && !BOUNDARIES[a][b][c]) {
				data[a][b][c] = 0;
			}
		}
		BREAK = true;
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
	 * @author KJZ
	 * @param data data is nifti intensity data as 3D matrix double[][][].
	 * @param erode is the max width (as integer) of voxels omitted from thresholding to be cleaned.  
	 * @param doX, doY, doZ as bool passed by the client to denote which dimension are being processed. 
	 * @return mri intensity data as 3D matrix in double format.
	 * @result extraneous streaks of voxels outside of the scan or lone outliers that are on the periphery of the scan are removed. 
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
				while (k < c/2 && Math.abs(k) > 0) {
				
					if (dim.equals("x") && data[i][j][Math.abs(k)] == 0 && data[i][j][Math.abs(k)+incr] != 0) {
						data = cleanUpHelper2(data, erode, i, j, Math.abs(k), incr, dim);
						break;
					} else if (dim.equals("y") && data[i][Math.abs(k)][j] == 0 && data[i][Math.abs(k)+incr][j] != 0) {
						data = cleanUpHelper2(data, erode, i, Math.abs(k), j, incr, dim);
						break;	
					} else if (dim.equals("z") && data[Math.abs(k)][j][i] == 0 && data[Math.abs(k)+incr][j][i] != 0) {
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
	
	// ------------------------------------------------------------ //
	
	/**
	 * @author KJZ 1.7.2020
	 * @param data data is nifti intensity data as 3D matrix double[][][].
	 * @param length is the depth to explore whether or not there is an overshot judging from the surrounding non-zero voxels
	 * @param doX, doY, doZ as bool passed by the client to denote which dimension are being processed. 
	 * @param half denotes which half of the scan is being traversed (-dim if !half, otherwise +dim).
	 * @return mri intensity data as 3D matrix in double[][][] format with any single lines of 0-labeled voxels
	 * overshooting the boundary of the brain filled-in with a value given by the client as "fill".
	 * @result  
	 */
	
	public static double[][][] patchOvershots(double[][][] data, int length, boolean doX, boolean doY, boolean doZ, double fill, boolean half) {
		
		int x = data[0][0].length-2; int y = data[0].length-2; int z = data.length-2;
		
		if (doX) {
			data = patchOvershotsHelper(data, length, z, y, x, "x", half, fill);
			data = patchOvershotsHelper(data, length, z, y, x, "x", !half, fill);
		}
		if (doY) {
			data = patchOvershotsHelper(data, length, z, x, y, "y", half, fill);
			data = patchOvershotsHelper(data, length, z, x, y, "y", !half, fill);
		}
		if (doZ) {
			data = patchOvershotsHelper(data, length, x, y, z, "z", half, fill);
			data = patchOvershotsHelper(data, length, x, y, z, "z", !half, fill);
		}
		return data;
	}
	
	private static double[][][] patchOvershotsHelper(double[][][] data, int length, int a, int b, int c, String dim, boolean half, double fill) {
		
		int distalLength; // the last dim loop (k) will return to this value after every iteration
		int incr;
		if (half) {
			distalLength = 0; incr = 1;
		} else {
			distalLength = -(c); incr = -1;
		}
		
		int k = distalLength; int count = 0;
		for (int i = 0; i < a; i++) {
			for (int j = 0; j < b; j++) {
				while(k < c/2) {
					if (dim.equals("x")) {
						if (data[i][j][Math.abs(k)+incr] > 0.99) {
							break;
						} else if (patchOvershotsHelper2(data[i][j][Math.abs(k)-length], data[i+1][j+1][Math.abs(k)], 
							data[i+1][j-1][Math.abs(k)], data[i-1][j+1][Math.abs(k)], data[i-1][j-1][Math.abs(k)])) {
							
							count++;
						}
						if (count == length) {
							patchOvershotsHelper3(data, i, j, Math.abs(k), length, dim, fill);
							break;
						}
					} else if (dim.equals("y")) {
						if (data[i][Math.abs(k)+incr][j] > 0.99) {
							break;
						} else if (patchOvershotsHelper2(data[i][Math.abs(k)-length][j], data[i+1][Math.abs(k)][c+1], 
							data[i+1][Math.abs(k)][j-1], data[i-1][Math.abs(k)][j+1], data[i-1][Math.abs(k)][j-1])) {
						
							count++;
						}
						if (count == length) {
							patchOvershotsHelper3(data, a, b, c, length, dim, fill);
							break;
						}
					} else if (dim.equals("z")) {
						if (data[Math.abs(k)+incr][j][i] > 0.99) {
							break;
						} else if (patchOvershotsHelper2(data[Math.abs(k)-length][j][i], data[Math.abs(k)][j+1][i+1], 
							data[Math.abs(k)][j+1][i-1], data[Math.abs(k)][j-1][i+1], data[Math.abs(k)][j-1][i-1])) {
							
							count++;
						}
						if (count == length) {
							data = patchOvershotsHelper3(data, a, b, c, length, dim, fill);
							break;
						}
					}
				}
				k++;
			}
		}
		return data;
	}
	
	private static boolean patchOvershotsHelper2(double v, double w, double x, double y, double z) {
		if (v < 0.99 && w > 0.99 && x > 0.99 && y > 0.99 && z > 0.99) {
			return true;
		}
		return false; 
	}
	
	private static double[][][] patchOvershotsHelper3(double[][][] data, int a, int b, int c, int length, String dim, double fill) {
		
		int counter = 0;
		
		if (dim.equals("x")) {
			while (data[a][b][c+counter] < 1) {
				data[a][b][c+counter] = fill;
				counter++;
			}
		}
		if (dim.equals("y")) {
			while (counter < length) {
				if (data[a][c+counter][b] < 1) {
					data[a][c+counter][b] = fill; 
				}
				counter++;
			}
		}
		if (dim.equals("z")) {
			while (counter < length) {
				if (data[c+counter][b][a] < 1) {
					data[c+counter][b][a] = fill;
				}
				counter++;
			}
		}
		return data;
	}
}
