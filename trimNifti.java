
import java.io.IOException;

/**
 * @author ns-zalewk
 *
 * This class organizes the arguments for editing and trimming the exvivo scan. 
 * TODO: Refine this to dynamically adjust the scaling and thresholding of the scan at some point. 
 */ 

public class trimNifti {
	
	public static void runFunctions(Nifti1Dataset inputNifti, boolean smooth, boolean erosion, boolean gradCorr, boolean clean, int iterations) throws IOException {
		inputNifti.readHeader();
		
		//set boundary matrix
		smoothVolume.writeBoundaries(inputNifti);
		double average = thresholdStandardDev.findTotalAverage(inputNifti.readDoubleVol((short)0)); // TODO: adjust values below based on average
		
		
		if (smooth) {
			inputNifti.writeVol(thresholdStandardDev.movingAverage(inputNifti.readDoubleVol((short) 0), 2), (short) 0);
		}
		
		int counter = 0;
		while (counter < iterations) {
			if (erosion) {
				inputNifti.writeVol(smoothVolume.erode(inputNifti, inputNifti.readDoubleVol((short) 0), 15, 13, true, true, true, true), (short)0);
			}
		
			if (gradCorr) {
				inputNifti.writeVol(thresholdStandardDev.findGradient(inputNifti.readDoubleVol((short) 0), true, true, true, 2, 3, 17, true, true), (short)0);
			}
			
			if (clean) {
				inputNifti.writeVol(smoothVolume.cleanUp(inputNifti.readDoubleVol((short)0), 1, true, true, true, true), (short) 0);
			}
			counter++;
		}
		
		System.out.println("Average non-zero intensity of the scan is: " + thresholdStandardDev.round(average,2)); 
	}
	
}
