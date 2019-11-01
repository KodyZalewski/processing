
import java.io.*;
import java.util.*;

/**
 * @author Kody Zalewski 11.1.2019
 * 
 * processExvivo is a program for taking an exvivo scan and pre-processing it to make it available for volumetric analysis 
 * (largely by Freesurfer). Exvivo scans have an extensive amount of non-zero voxels surrounding the tissue from fixation or 
 * otherwise making volumetric analysis extremely troublesome. This can be solved by identifying the boundaries around the tissue 
 * and masking out non-tissue voxels prior-to or after the boundary depending on the hemisphere. The fixation around the brain
 * is generally linear in composition, therefore identifying and marking the departure from the linear gradient is an optimal
 * way to find boundaries around the actual tissue. This also works when processing a single-hemisphere versus a whole-brain. 
 * 
 * The program only requires the input of a single T1 contrast nifti-1 format scan output will be separate from the original scan and 
 * will not overwrite existing data. It is largely assumed that the client is using this on a Linux distribution and the paths and tools 
 * used herein are functional on a Linux OS.
 * 
 * TODO: Incorporate the ability to process other scan formats on multiple OSs too. 
 * 
 * This program extensively relies on the Nifti-1 java libraries written by Kate Fissell at the U. of Pittsburgh and wouldn't 
 * be possible without her contributions. The source of these can be found at: http://niftilib.sourceforge.net/ 
 * The original libraries have been updated where possible from Java 1.4 to run with the current Java version 1.8. 
 * The Freesurfer program that this program is designed for pre-processing can be found at freesurfer.net with the original paper
 * detailing the functionality of the program at: Dale, A.M., Fischl, B., Sereno, M.I., 1999. Cortical surface-based analysis. I. 
 * Segmentation and surface reconstruction. Neuroimage 9, 179-194.
 * 
 * TODO: Prior estimations of whole-brain or hemisphere boundaries can be used to better identify boundaries of other scans
 * in the future. 
 */

public class processExvivo {
	
	public static String inputFile, outputFile;
	public static String LOCALPATH = "/home/ns-zalewk/workspace/ExvivoNifti/src/"; //default, change at some point
	public static String SCANNAME = "";
	public static double data[][][];
	public static Map<Integer,Double> boundMap;
	
	public static void main(String args[]) throws IOException {
		
		inputFile = args[1];		
		outputFile = args[2];

		checkArgs(args);
			
		Nifti1Dataset inputFile = new Nifti1Dataset(LOCALPATH + args[0]);
		
		if (inputFile.exists()) {
			System.out.println("Nifti file has readable header and data.");
		} else {
			System.out.println("Nifti file does not have usable header or data.");
			System.exit(1);
		}		
		
		// TODO: Is time-course *always* necessary for opening nifti scan?
		// data = inputNifti.readDoubleVol(Short.parseShort(args[4])); // timecourse // 
		
		// TODO: read and look for the nifti file, if it's in .gz compressed format, gunzip it, if gunzip isn't installed,
		// prompt the client to download it
		
		readNifti.readNiftiFinder(inputFile);
		boundMap = readNifti.markCoordinates(data); 
		// assign boundMap markings to the data set
		data = writeNifti.zeroOutVoxels(data);
		
		writeNifti.writeNiftiOutput(inputFile, outputFile, data);
		System.exit(0);
	}
		
	/**
	 * @param args are string to give to the program and allow the client input when no other arguments are specified. 
	 * Otherwise the program uses the given nifti file name and path for processing. 
	 */
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
				
			} else if ( response.startsWith("n") || response.contains("no") ) {
				console.close();
				System.out.println("Exiting without arguments.");
				System.exit(0);			
				
			} else {
				System.out.println("Please enter a yes or no answer.");
				checkArgs(args); 
			}
			
		} else {

			System.out.println("Using " + LOCALPATH + " as path.");
			System.out.println("Using " + args[0] + " as scan.");
			return;
		}
	}	
}
