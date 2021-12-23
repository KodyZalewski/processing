
/**
 * @author kody
 * Location where client defined variables are derived. Edit this file exclusive when possible. 
 */

public class Vars {
	
	// local variables
	// *** EDIT THESE LINES FOR CHANGING LOCAL VARIABLES *** //
	
	//paths 1 and 2 will be the same as 3 and 4 if folder where scan is and where scan will be output is the same
	static String path1 = "/work_computer/Desktop/memorycube/"; //path between user ID and subject
	static String path2 = "_FREESURFER/mri/orig/"; //path to location of folder
	static String path3 = "/work_computer/Desktop/memorycube/"; //path between user ID and subject
	static String path4 = "_FREESURFER/mri/orig/"; //path to location of folder
	
	
	// processExvivo variables:
	// *** EDIT THESE LINES FOR CHANGING WHICH FUNCTIONS TO USE *** //
	
	static boolean smooth = false; 
	static boolean erosion = false; 
	static boolean gradCorr = true;
	static boolean clean = true;
	static boolean patchOvershots = true;
	static boolean histogram = false; // test function
	
	// TrimScan variables:
	// *** EDIT THESE LINES FOR CHANGING EDITING PARAMETERS *** //
	
	//used in clean up
	static int erode = 3; // number of voxels to erode from the edges of the scan
	
	//used in smoothing 
	static int smoothlen = 2;
	
	//used in CalcScan
	static int length = 3;
	static float stdev = (float) 2; // standard deviation cut-off for outliers when thresholding
	static float lhbound = 80; // left 
	static float rhbound = 80; // right 
	static float supbound = 65; // superior 
	static float infbound = 65; // inferior
	static float antbound = 65; // anterior
	static float posbound = 65; // posterior
		
	//used in erosion
	static int lherode = 20; // left 
	static int rherode = 20; // right 
	static int superode = 10; // superior 
	static int inferode = 10; // inferior
	static int anterode = 10; // anterior
	static int poserode = 10; // posterior
	
	//used in patching
	static int depth = 3; // depth to traverse scan in correcting over-shots of the pial surface
	
}
