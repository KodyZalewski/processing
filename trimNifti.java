
import java.io.IOException;

/**
 * @author KJZ
 *
 * This class organizes the arguments for editing and trimming the exvivo scan. 
 * TODO: Refine this to dynamically adjust the scaling and thresholding of the scan at some point. 
 */ 

public class trimNifti {
	
	public static void runFunctions(Nifti1Dataset inputNifti, boolean smooth, boolean erosion, 
			boolean gradCorr, boolean patchOvershots, boolean clean, int iterations) throws IOException {
		
		inputNifti.readHeader();
		double[][][] newData = smoothVolume.robustDataCopy(inputNifti.readDoubleVol((short)0));
		
		//set boundary matrix
		double average = smoothVolume.totalAverage(newData, true); // TODO: adjust values below based on average
		
		// *** EDIT THESE LINES TO CHANGE DEFAULT PARAMETERS *** //
		//int lowBound = (int) average/2; // lowest voxel intensity to stop at
		//int lowBound = 11;
		//int depth = 3; // depth to traverse scan in correcting over-shots of the pial surface
		//int fill = 2; // value to fill in overshots with
		
		/** put default parameters here at some point
		 * int erode; // number of voxels to erode from the edges of the scan
		 * int stdev; // standard deviation cut-off for outliers when thresholding
		 * int voxelBound; // same as lowBound for stdev thresholding
		 * 
		*/
		
		smoothVolume.dimAverages(newData, 3);
		
		int counter = 0;
		while (counter < iterations) {
			if (smooth) {
				newData = smoothVolume.movingAverage(newData, 2);
				System.out.println("Finished smoothing."); System.out.println("");
			}
			
		
			if (gradCorr) {
				newData = thresholdStandardDev.findGradient(newData, true, true, true, 2, 3, 11, true, true);
			}
			if (erosion) {
				//newData = smoothVolume.erode(inputNifti, newData, 15, lowBound, true, true, true, true, true);
				newData = smoothVolume.erode(inputNifti, newData, 15, 9, true, true, true, true, true);
				newData = smoothVolume.erode(inputNifti, newData, 15, 6, false, true, false, false, true);
			}
			if (clean) {
				newData = smoothVolume.cleanUp(newData, 3, true, true, true, true, true); // w/ simpleCleaner
				newData = smoothVolume.cleanUp(newData, 3, true, true, true, true, false); // w/o simpleCleaner
			}
			if (patchOvershots) {
				newData = smoothVolume.patchOvershots(newData);
			}
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
