import java.io.IOException;
import java.util.zip.*;
import org.omg.CORBA_2_3.portable.OutputStream;

public class smoothVolume {
	
	static double data[][][];
	static double ORIG_DATA[][][];
	public static String STUDY = ""; 
	public static String SUBJECT = "";
	public static String LOCALPATH = "/home/ns-zalewk/Desktop/memorycube/" ;
	public static String INPUT_PATH = "";
	public static String OUTPUT_PATH = "";
	static boolean boundaries[][][];
	
	public static boolean anterior = false;
	public static boolean posterior = false;
	public static boolean dorsal = false;
	public static boolean ventral = false;
	public static boolean right = false;
	public static boolean left = false;
	public static boolean firstHalf = false;
	public static boolean secondHalf = false;
	public static boolean erode = false; 
	public static boolean findGradient = false; 
	public static boolean cleanUp = false;
	
	
	public static void main(String[] args) throws IOException {
			
		STUDY = args[0];		
		SUBJECT = args[1];
		INPUT_PATH = LOCALPATH + STUDY + "/" + SUBJECT + "/" + SUBJECT + "_FREESURFER/mri/orig/";
		OUTPUT_PATH = LOCALPATH + STUDY + "/" + SUBJECT + "/" + SUBJECT + "_FREESURFER/mri/orig/";
		
		//checkArgs(args); TODO: update helper methods with dimensional booleans
		
		// original nifti-1 dataset
		Nifti1Dataset nds = new Nifti1Dataset(INPUT_PATH + args[2]);
		nds.readHeader();	
		ORIG_DATA = nds.readDoubleVol((short) 0);

		// new nifti-1 dataset to copy to
		Nifti1Dataset smoothNifti = new Nifti1Dataset(OUTPUT_PATH + args[3]);
		smoothNifti.readHeader();
		smoothNifti.writeVol(ORIG_DATA, (short) 0);
		
		// smooth volume
		smoothNifti.writeVol(thresholdStandardDev.movingAverage(smoothNifti.readDoubleVol((short) 0), 1), (short) 0);

		//TODO: set boundary matrix, switch to double[][][] at some point?
		writeBoundaries(nds);

		if (erode == true) {
			smoothNifti.writeVol(erode(smoothNifti.readDoubleVol((short) 0), 20, 7, true, false, true, false, true), (short) 0);
		}
		if (findGradient == true) {
			smoothNifti.writeVol(thresholdStandardDev.findGradient(smoothNifti.readDoubleVol((short) 0), true, false, true, 2, 3, 9, true, true), (short)0);
		}
		if (cleanUp == true) {
			smoothNifti.writeVol(cleanUp(smoothNifti.readDoubleVol((short)0), 2, true, true, true), (short) 0);
		}
	}
	
	/** @params takes array of string of arguments passed to the program.
	 * Sets the defaults to true if all are specified, then erosion, thresholding or clean up
	 * can be chosen as options as boolean. 
	 */
	public static void checkArgs(String[] args) {
		
		for (String arg : args) {
			if (arg.equals("-all")) {
				anterior = true; posterior = true; dorsal = true; ventral = true; 
				right = true; left = true; firstHalf = true; secondHalf = true;
			} else if (arg.equals("-p")) {
				posterior = true;
			} else if (arg.equals("-a")) {
				anterior = true;
			} else if (arg.equals("-d")) {
				dorsal = true;
			} else if (arg.equals("-v")) {
				ventral = true;
			} else if (arg.equals("-r")) {
				right = true;
			} else if (arg.equals("-l")) {
				left = true;
			} else if (arg.equals("-1")) {
				firstHalf = true;
			} else if (arg.equals("-2")) {
				secondHalf = true;
			} else if (arg.equals("-erode")) {
				erode = true;
			} else if (arg.equals("-threshold")) {
				findGradient = true;
			} else if (arg.equals("-cleanup")) {
				cleanUp = true;
			// } else if (arg.equals("-path") {
				// if (next argument != null || next argument != String) { 
				// 	  INPUT_PATH = next argument 
				// } else {
				// 	  System.out.println("Argument is null or not a string, path cannot be set.")
				// }
			// } else if (arg.equals("-outputPath") {
				// if (next argument != null || next argument != String) {
				// 		OUTPUT_PATH = next argument
				// } else { 
				// 		System.out.println("Argument is null or not a string, path cannot be set.")
				// }
			} else {
				System.out.println("No arguments set as to which dimension to process. Exiting...");
				System.exit(0);
			}
		}	
	}
	
	// TODO: Unzipping .nii.gz file attempt, 
	/**
	public void Gunzipper(File f) throws IOException {
		this.in = new FileInputStream(f);
	}
	
	public void unzip(File fileTo) throws IOException {
		OutputStream out = new FileOutputStream(fileTo);
		try {
			in = new GZIPInputStream(in);
			byte[] buffer = new byte[65536];
			int noRead;
			while ((noRead = in.read(buffer)) != 1) {
				out.write(buffer,0,noRead);
			} finally {
				try { 
					out.close(); 
				} catch (Exception e) {}
			}
		}
		
		public void close() {
			try {
				in.close();	
			} catch (Exception e) {}
		}
	} */
	
	// sets the boundaries for a given scan, takes boolean data and uses dimensions of desired scan
	public static void writeBoundaries(Nifti1Dataset nds) {
		boundaries = new boolean[nds.ZDIM][nds.YDIM][nds.XDIM];
		for (int i = 0; i < boundaries.length; i++) {
			for (int j = 0; j < boundaries[i].length; j++) {
				for (int k = 0; k < boundaries[i][j].length; k++) {
					boundaries[i][j][k] = false;
				}
			}
		}
	}
	
	
	
	/** passes data to erode helper method
	 * @return nifit data as double matrix
	 */
	public static double[][][] erode(double[][][] data, int erode, int lowBound, 
			boolean doX, boolean doY, boolean doZ, boolean firstHalf, boolean secondHalf) {
		
		int x = data[0][0].length; int y = data[0].length; int z = data.length; // fix from being [0] at some point
		
		if (doX == true) {
			data = erodeHelper(data, erode, lowBound, z, y, x, "x", firstHalf, secondHalf);
		}
		if (doY == true) {
			data = erodeHelper(data, erode, lowBound, z, x, y, "y", firstHalf, secondHalf);
		}
		if (doZ == true) {
			data = erodeHelper(data, erode, lowBound, x, y, z, "z", firstHalf, secondHalf);
		}
		return data; 
		
	}
	
	/** passes data to cleanUp helper method
	 * @return nifti data as double matrix
	 */
	public static double[][][] cleanUp(double[][][] data, int erode, boolean doX, boolean doY, boolean doZ) {
		
		int x = data[0][0].length; int y = data[0].length; int z = data.length; // fix from being [0] at some point
		
		if (doX == true) {
			data = cleanUpHelper(data, erode, z, y, x, "x");
		}
		if (doY == true) {
			data = cleanUpHelper(data, erode, z, x, y, "y");
		}
		if (doZ == true) {
			data = cleanUpHelper(data, erode, x, y, z, "z");
		}
		return data; 
		
	}
	
	
	/** 
	 * @param data is nifti data being taked as a 3-dimensional matrix of double values.
	 * @param erode is how many voxels should be removed that are outside the bounds of the brain. 
	 * @param lowBound denotes intensity boundary of the erosion algorithm encountering a voxel where it should stop. 
	 * @param a, b, c are dimensions being taken as int, contingent on dimension of scan being processed
	 * @param dimension corresponds to which x, y, z dimension should be processed. 
	 * @param firstHalf and secondHalf are boolean which corresponds to which half of the dimension should be processed. 
	 * @return double matrix of eroded nifti voxels
	 */
	public static double[][][] erodeHelper(double[][][] data, int erode, int lowBound, 
			int a, int b, int c, String dimension, boolean firstHalf, boolean secondHalf) {
		
		if (erode > c/2) {
			System.out.println("ERROR: Erosion cannot be greater than half of the desired dimension. (" + c/2 + ")");
			return data;
		}
		
		for (int i = 0; i < a; i++) {
			for (int j = 0; j < b; j++) {
				if (firstHalf == true) {
					for (int k = 0; k < c/2; k++) {
						if (dimension.equals("x") && data[i][j][k] != 0 && boundaries[i][j][k] == false) {
							for (int l = 0; l < erode; l++) {
								if (data[i][j][k+l] < lowBound && data[i][j][k+l] != (double) 0) {
									boundaries[i][j][k+l] = true;
									break;
								} else if (data[i][j][k+l] != 0 && boundaries[i][j][k+l] == false) {
									data[i][j][k+l] = (double) 0;
								}
							}
							break;
						} else if (dimension.equals("y") && data[i][k][j] != 0 && boundaries[i][k][j] == false) {
							for (int l = 0; l < erode; l++) {
								if (data[i][k+l][j] < lowBound && data[i][k+l][j] != (double) 0) {
									boundaries[i][k+l][j] = true;
									break;
								} else if (data[i][k+l][j] != 0 && boundaries[i][k+l][j] == false) {
									data[i][k+l][j] = (double) 0;
								}
							}
							break;
						} else if (dimension.equals("z") && data[k][j][i] != 0 && boundaries[k][j][i] == false) {
							for (int l = 0; l < erode; l++) {
								 if (data[k+l][j][i] < lowBound && data[k+l][j][i] != (double) 0) {
									boundaries[k+l][j][i] = true;
									break;
								} else if (data[k+l][j][i] != 0 && boundaries[k+l][j][i] == false) {
									data[k+l][j][i] = (double) 0;
								}
							}
							break;	
						} 
					}
				}
				if (secondHalf == true) {
					for (int k = c-1; k > c/2; k--) {
						if (dimension.equals("x") && data[i][j][k] != 0 && boundaries[i][j][k] == false ) {
							for (int l = 0; l < erode; l++) {
								if (data[i][j][k-l] < lowBound && data[i][j][k-l] != (double) 0) {
									boundaries[i][j][k-l] = true;
									break;
								} else if (data[i][j][k-l] != 0 && boundaries[i][j][k-l] == false) {
									data[i][j][k-l] = (double) 0;
								}
							}
							break;
						} else if (dimension.equals("y") && data[i][k][j] != 0 && boundaries[i][k][j] == false) {
							for (int l = 0; l < erode; l++) {
								if (data[i][k-l][j] < lowBound && data[i][k-l][j] != (double) 0) {
									boundaries[i][k-l][j] = true;
									break;
								} else if (data[i][k-l][j] != 0 && boundaries[i][k-l][j] == false) {
									data[i][k-l][j] = (double) 0;
								}
							}
							break;	
						} else if (dimension.equals("z") && data[k][j][i] != 0 && boundaries[k][j][i] == false) {
							for (int l = 0; l < erode; l++) {
								if (data[k-l][j][i] < lowBound && data[k-l][j][i] != (double) 0) {
									boundaries[k-l][j][i] = true;
									break;
								} else if (data[k-l][j][i] != 0 && boundaries[k-l][j][i] == false) {
									data[k-l][j][i] = (double) 0;
								}
							}
							break;
						}
					}
				}
			}
		}
		return data;
	}
	
	// this function not finished yet, (more efficient way to erode)
	public static double[][][] erodeRecursively(double[][][] data, int counter, int erode, int i, int j, int k, String dimension, boolean half) {
		if (counter == erode || data[i][j][k] != 0) {
			return data;
		} else {
			data[i][j][k] = (double) 0;
			counter++;
			if (dimension.equals("x")) {
				if (half == true) {
					erodeRecursively(data, counter, erode, i, j, k+counter, dimension, half);
				} else {
					erodeRecursively(data, counter, erode, i, j, k-counter, dimension, half);
				}
			} else if (dimension.equals("y")) {
				if (half == true) {
					erodeRecursively(data, counter, erode, i, k+counter, j, dimension, half);
				} else {
					erodeRecursively(data, counter, erode, i, j, k-counter, dimension, half);
				}
			} else if (dimension.equals("z")) {
				if (half == true) {
				erodeRecursively(data, counter, erode, k-counter, j, i, dimension, half);
				} else {
					erodeRecursively(data, counter, erode, i, j, k-counter, dimension, half);
				}
			} else {
				System.out.println("We shouldn't reach this statement.");
			}
		}
		return data;
	}
	
	
	/**
	 * 
	 * @param data is nifti data passed as double matrix to function
	 * @param erode is the value of number of suprious voxels which should be removed as int, 
	 * @param a, b, c correspond to the dimensions of the scan
	 * @param dimension is string corresponding to whether we are processing the x, y, or z dimension of the scan
	 * @return the altered nifti data set with spurious lone voxels outside of the brain cleaned up. 
	 */
	public static double[][][] cleanUpHelper(double[][][] data, int erode, int a, int b, int c, String dimension) {
		
		boolean setLength = false; 
		
		for (int i = 0; i < a; i++) {
			for (int j = 0; j < b; j++) {
				for (int k = 0; k < c/2 - erode; k++) {
					for (int l = 0; l < erode; l++) {
						if (dimension.equals("x") && data[i][j][k] == 0 && data[i][j][k+l] != 0 && data[i][j][k+l+1] == 0 || setLength == true) {
							data[i][j][k+l] = 0;
							setLength = true; 
						} else if (dimension.equals("y") && data[i][k][j] == 0 && data[i][k+l][j] != 0 && data[i][k+l+1][j] == 0 || setLength == true) {
							data[i][k+l][j] = 0;
							setLength = true; 
						} else if (dimension.equals("z") && data[k][j][i] == 0 && data[k+l][j][i] != 0 && data[k+l+1][j][i] == 0 || setLength == true ) {
							data[k+l][j][i] = 0;
							setLength = true; 
						} 
					}
					setLength = false;
				}
				for (int k = c - 1; k > c/2 + erode; k--) {
					for (int l = 0; l < erode; l++) {
						if (dimension.equals("x") && data[i][j][k] == 0 && data[i][j][k-l] != 0 && data[i][j][k-l-1] == 0 || setLength == true) {
							data[i][j][k-l] = 0;
							setLength = true; 
						} else if (dimension.equals("y") && data[i][k][j] == 0 && data[i][k-l][j] != 0 && data[i][k-l-1][j] == 0 || setLength == true) {
							data[i][k-l][j] = 0;
							setLength = true; 
						} else if (dimension.equals("z") && data[k][j][i] == 0 && data[k-l][j][i] != 0 && data[k-l-1][j][i] == 0 || setLength == true) {
							data[k-l][j][i] = 0;
							setLength = true; 
						} 
					}
					setLength = false;
				}
			}
		}
		return data; 
	}
}
