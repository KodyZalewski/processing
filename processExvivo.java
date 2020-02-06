
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
//import java.util.zip.GZIPOutputStream;

/**
 * @author KJZ 11.1.2019
 * 
 * processExvivo is a program for taking an exvivo scan and pre-processing it to make it available for volumetric analysis 
 * (largely by Freesurfer). Exvivo scans have an extensive amount of non-zero voxels surrounding the tissue from fixation or 
 * otherwise making volumetric analysis extremely troublesome. This can be solved by identifying the boundaries around the tissue 
 * and masking out non-tissue voxels prior-to or after the boundary depending on the hemisphere. The fixation around the brain
 * is generally linear in composition, therefore identifying and marking the departure from the gradient is an optimal
 * way to find boundaries around the actual tissue. This also works when processing a single-hemisphere versus a whole-brain. 
 * 
 * The program only requires the input of a single T1 contrast Nifti-1 format scan output will be separate from the original scan and 
 * will not overwrite existing data. It is largely assumed that the client is using this on a Linux distribution and the paths and tools 
 * used herein are functional on a Linux OS. 
 * TODO: Incorporate the ability to process other scan formats on multiple OSs too. 
 * 
 * This program extensively relies on the Nifti-1 java libraries written by Kate Fissell at the U. of Pittsburgh and wouldn't 
 * be possible without her contributions. The source of these can be found at: http://niftilib.sourceforge.net/ 
 * The original libraries have been updated where possible from Java 1.4 to run with the current Java version 1.8. 
 * The Freesurfer program that this program is designed for pre-processing can be found at freesurfer.net with the original paper
 * detailing the functionality of the program at: Dale, A.M., Fischl, B., Sereno, M.I., 1999. Cortical surface-based analysis. I. 
 * Segmentation and surface reconstruction. Neuroimage 9, 179-194.
 * 
 * TODO: Change default format from FLOAT to UCHAR or INT, something that won't randomly zero out voxels
 * 
 * TODO: Prior estimations of whole-brain or hemisphere boundaries can be used to better identify boundaries of other scans
 * in the future. 
 */

public class processExvivo {
	
	public static String STUDY, SUBJECT; 
	public static String USERID = System.getenv("USER");
	public static String LOCALPATH, OUTPUT_PATH;
	public static String SCANNAME = "";
	
	public static void setGlobal(String[] args) {
		STUDY = args[0];
		SUBJECT = args[1];
		LOCALPATH = "/home/" + USERID + "/Desktop/memorycube/" + STUDY + "/" + SUBJECT + "/" + SUBJECT + "_FREESURFER/mri/orig/";
		OUTPUT_PATH =  "/home/" + USERID + "/Desktop/memorycube/" + STUDY + "/" + SUBJECT + "/" + SUBJECT + "_FREESURFER/mri/orig/";
	}
	
	public static void main(String args[]) throws IOException {
		
		setGlobal(args);
		String inputFile = args[2];
		String outputFile = args[3];
		checkArgs(args);
			
		if (!checkPackage()) {
			System.out.println("Gunzip is not installed! ");
			System.out.println("From command line for Debian/Ubuntu users: 'sudo apt install gunzip' ");
			System.out.println("Exiting program.");
			System.exit(1);
		}
		
		// read the input nifti1, copy data, unzip if needed
		Nifti1Dataset inputNifti = new Nifti1Dataset(LOCALPATH + inputFile);
		
		if (inputNifti.exists()) {
			System.out.println("Input NIFTI-1 file has readable header and data.");
			System.out.println("");
		} else {
			System.out.println("Nifti file does not have usable header or data.");
			System.out.println("Exiting...");
			System.exit(1);
		}
		
		inputNifti.readHeader();
		
		if (inputFile.endsWith(".gz")) {
			
			/**TODO: Is time-course *always* necessary for opening nifti scan? only do so if it's in .gz format
			// data = inputNifti.readDoubleVol(Short.parseShort(args[4])); // timecourse // 
			**/
			
			String inputPath = LOCALPATH + inputFile;
			inputFile = inputFile.substring(0, inputFile.length() - 3);  //removes ".gz" extension
			String unzippedPath = LOCALPATH + inputFile;
			
			gunzip(inputNifti, inputPath, unzippedPath);
			inputNifti = new Nifti1Dataset(LOCALPATH + inputFile);
			inputNifti.readHeader();
			
		}
		
		// new nifti-1 dataset to copy to
		Nifti1Dataset outputNifti = new Nifti1Dataset(OUTPUT_PATH + outputFile);
		
		if (!outputNifti.exists()) {
			copyNifti(inputNifti, OUTPUT_PATH + outputFile);
		}
		
		outputNifti.readHeader();
		
		// *** EDIT THESE LINE FOR CHANGING WHICH FUNCTIONS TO USE *** //
		boolean smooth = false; 
		boolean erosion = false; 
		boolean gradientCorrection = true;
		boolean clean = false;
		boolean patchOvershots = false;
		
		// perform data manipulation
		trimNifti.runFunctions(outputNifti, smooth, erosion, gradientCorrection, patchOvershots, clean, 1);
		
		if (outputNifti.exists()) {
			System.out.print("File sucessfully written.");
		} else {
			System.out.print("File hasn't been written to the folder successfully, exiting...");
		}
	
		System.exit(0);
	}
	
	/**
	 * @param args are string to give to the program and allow the client input when no other arguments are specified. 
	 * Otherwise the program uses the given nifti file name and path for processing. 
	 */
	
	public static void checkArgs(String[] args) {
		
		if (args.length == 4) {
			
			System.out.println("The local path is: " + LOCALPATH);
			System.out.println("The output path is: " + OUTPUT_PATH);
			System.out.println("");
			
		} else if (args.length > 0 && args.length < 4) { 
			
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
				
			} else if ( response.startsWith("n") || response.contains("no") ) {
				console.close();
				System.out.println("Exiting without arguments.");
				System.exit(0);			
				
			} else {
				System.out.println("Please enter a yes or no answer.");
				checkArgs(args); 
			}
			
		} else {
			System.out.println("Illegal number of arguments.");
			System.out.println("processExvivo <STUDY> <SUBJECT> <input scan> <output scan>");
			System.exit(1);
		}
	}
	
	/**
	 * @param inputNifti
	 * @param niftiCopy
	 */
	public static void copyNifti(Nifti1Dataset inputNifti, String niftiCopy) {
		
		byte[] b;
		
		try {
			inputNifti.readHeader();
			b = inputNifti.readData();
			inputNifti.setHeaderFilename(niftiCopy);
			inputNifti.setDataFilename(niftiCopy);
			inputNifti.writeHeader();
			inputNifti.writeData(b);
			
		} catch (IOException ex) {
			System.out.println("\nCould not copy nifti to "+niftiCopy+": "+ex.getMessage());
		}
	}
	
	/**
	 * @param inputFile is .nii.gz passed to be unzipped
	 * @param outputFile is the unzipped .nii/nifti1 format file 
	 * @throws IOException
	 */
	public static void gunzip(Nifti1Dataset inputFile, String inputPath, String outputFile) throws IOException {
		try {
			
			File newFile = new File(outputFile);
			
			if (!newFile.exists()) {
				copyNifti(inputFile, outputFile);
			}
			
			FileOutputStream fos = new FileOutputStream(newFile);
			FileInputStream fis = new FileInputStream(inputPath);
			GZIPInputStream gis = new GZIPInputStream(fis);
				
			int len;
			byte[] buffer = new byte[1024]; 		
			while ((len = gis.read(buffer)) != -1) {
				fos.write(buffer, 0, len);
			}
			
			fos.close();
			gis.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// We can only check if a class from a package exists, not the package itself
	private static boolean checkPackage() {
		
		try {
			Class.forName("java.util.zip.GZIPInputStream");
			return true;
		} catch (Exception e) {
			return false; 
		}
	}
}
