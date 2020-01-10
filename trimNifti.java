
import java.io.IOException;

/**
 * @author KJZ
 *
 * This class organizes the arguments for editing and trimming the exvivo scan. 
 * TODO: Refine this to dynamically adjust the scaling and thresholding of the scan at some point. 
 */ 

public class trimNifti {
	
	public static void runFunctions(Nifti1Dataset inputNifti, boolean smooth, boolean erosion, boolean gradCorr, boolean patchOvershots, boolean clean, int iterations) throws IOException {
		inputNifti.readHeader();
		
		
		/** put default parameters here at some point
		 * int erode; // number of voxels to erode from the edges of the scan
		 * int lowBound; // lowest voxel intensity to stop at
		 * int length // depth to traverse scan in correcting over-shots of the pial surface
		 * int stdev; // standard deviation cut-off for outliers when thresholding
		 * int voxelBound; // same as lowBound for stdev thresholding
		*/
		
		//set boundary matrix
		double average = thresholdStandardDev.findTotalAverage(inputNifti.readDoubleVol((short)0)); // TODO: adjust values below based on average
		
		
		if (smooth) {
			inputNifti.writeVol(smoothVolume.movingAverage(inputNifti.readDoubleVol((short) 0), 2), (short) 0);
		}
		
		int counter = 0;
		while (counter < iterations) {
			if (erosion) {
				inputNifti.writeVol(smoothVolume.erode(inputNifti, inputNifti.readDoubleVol((short) 0), 15, 13, true, true, true, true), (short)0);
			}
		
			if (gradCorr) {
				inputNifti.writeVol(thresholdStandardDev.findGradient(inputNifti.readDoubleVol((short) 0), true, true, true, 2, 3, 17, true, true), (short)0);
			}
			
			if (patchOvershots) {
				inputNifti.writeVol(smoothVolume.patchOvershots(inputNifti.readDoubleVol((short) 0), 3, 2, true, true, true), (short)0);
			}
			
			if (clean) {
				inputNifti.writeVol(smoothVolume.cleanUp(inputNifti.readDoubleVol((short)0), 3, true, true, true, true), (short) 0);
			}
			counter++;
		}
		
		System.out.println("Average non-zero intensity of the scan is: " + thresholdStandardDev.round(average,2)); 
	}
	
}
