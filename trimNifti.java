
import java.io.IOException;

//import org.rosuda.REngine.REXPMismatchException;
//import org.rosuda.REngine.REngineException;

/**
 * @author KJZ
 *
 * This class organizes the arguments for editing and trimming the exvivo scan. 
 * TODO: Refine this to dynamically adjust the scaling and thresholding of the scan at some point. 
 */ 

public class trimNifti {
	
	static int erode; // number of voxels to erode from the edges of the scan
	static float stdev; // standard deviation cut-off for outliers when thresholding
	static float voxelDiv; // what fraction of the average should be taken to threshold
	static int scanDiv;
	static int depth; 
	static int fill;
	
	// constructor if using UI
	//public static void runFunctions(int s, int e, int e2, int tb, int td, int c, int p, 
	//	boolean smooth, boolean erosion, boolean gradCorr, boolean patchOvershots, boolean clean, boolean iter); {
	//	erode = e; 
	//	runFunctions()
	//}
	
	public static void runFunctions(Nifti1Dataset inputNifti, boolean smooth, boolean erosion, 
			boolean gradCorr, boolean patchOvershots, boolean clean, int iter) throws IOException {
		
		inputNifti.readHeader();
		double[][][] newData = smoothVolume.robustDataCopy(inputNifti.readDoubleVol((short)0));
		
		//set boundary matrix
		double average = smoothVolume.totalAverage(newData, true); // TODO: adjust values below based on average
		
		
		System.out.println("Performing " + iter + " iterations of thresholding on the scan.");
		
		//graphSlice.graphSliceData(inputNifti, newData);
		
		int counter = 0;
		while (counter < iter) {
			
			// *** EDIT THESE LINES TO CHANGE DEFAULT PARAMETERS *** //
			voxelDiv = (float) 1.5; // lowest (intensity/voxelDiv) value to stop at
			scanDiv = 3;
			stdev = (float) 2;
			erode = 11;
			//int depth = 3; // depth to traverse scan in correcting over-shots of the pial surface
			//int fill = 2; // value to fill in overshots with
			
			float[] avgs = smoothVolume.dimAverages(newData, 3);
			
			if (smooth) {
				
				newData = smoothVolume.movingAverage(newData, 2);
			}
			
			if (gradCorr) {
				
				// x-dimension
				newData = thresholdStandardDev.findGrdnt(newData, true, false, false, 3, 3, 80, true, false);
				newData = thresholdStandardDev.findGrdnt(newData, true, false, false, 3, 3, 80, false, true);
				// y-dimension
				newData = thresholdStandardDev.findGrdnt(newData, false, true, false, stdev, 3, 65, true, false);
				newData = thresholdStandardDev.findGrdnt(newData, false, true, false, stdev, 3, 65, false, true);
				// z-dimension
				newData = thresholdStandardDev.findGrdnt(newData, false, false, true, stdev, 3, 65, true, false);
				newData = thresholdStandardDev.findGrdnt(newData, false, false, true, stdev, 3, 65, false, true);
			}
			
			if (erosion) {
				
				//avgs = smoothVolume.dimAverages(newData, 3);
				
				// x-dimension
				newData = smoothVolume.erode(inputNifti, newData, true, false, false, 20, (float) avgs[0]/2, true, false);
				newData = smoothVolume.erode(inputNifti, newData, true, false, false, 20, (float) avgs[1]/4, false, true);
				// y-dimension
				newData = smoothVolume.erode(inputNifti, newData, false, true, false, 10, (float) avgs[2]/2, true, false);
				newData = smoothVolume.erode(inputNifti, newData, false, true, false, 10, (float) avgs[3]/2, false, true);
				// z-dimension
				newData = smoothVolume.erode(inputNifti, newData, false, false, true, 10, (float) avgs[4]/3, true, false);
				newData = smoothVolume.erode(inputNifti, newData, false, false, true, 10, (float) avgs[5]/2, false, true);
			}
			
			if (clean) {
				
				newData = smoothVolume.cleanUp(newData, 3, true, true, true, true, true); // w/ simpleCleaner
				newData = smoothVolume.cleanUp(newData, 3, true, true, true, true, false); // w/o simpleCleaner
			}
			
			if (patchOvershots) {
				
				newData = smoothVolume.patchOvershots(newData);
				newData = smoothVolume.patchOvershots(newData);
				newData = smoothVolume.peripheryClean(newData);
			}
			
			//smoothVolume.histogram(newData, "y", newData.length-1, newData[0][0].length-1, newData[0].length-1, 1, newData[0].length/2);
			
			System.out.println("Iteration complete..."); System.out.println("");
			counter++;
		}
		
		System.out.println("Original average non-zero intensity of the scan was: " + 
		thresholdStandardDev.round(average,2));
		System.out.println("Average non-zero intensity of the scan is now: " + 
		thresholdStandardDev.round(smoothVolume.totalAverage(newData, true),2));
		System.out.println("");
		
		inputNifti.writeVol(newData,(short) 0);
		
	}
	
}
