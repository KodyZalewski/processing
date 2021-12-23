
import java.io.IOException;

//import org.rosuda.REngine.REXPMismatchException;
//import org.rosuda.REngine.REngineException;

/**
 * @author KJZ
 *
 * This class organizes the arguments for editing and trimming the exvivo scan.
 *  
 * TODO: Refine this to dynamically adjust the scaling and thresholding of the scan at some point. 
 */ 

public class TrimScan {
	
	// constructor if using UI
	//public static void runFunctions(int s, int e, int e2, int tb, int td, int c, int p, 
	//	boolean smooth, boolean erosion, boolean gradCorr, boolean patchOvershots, boolean clean, boolean iter); {
	//	erode = e; 
	//	runFunctions()
	//}
	
	public static void runFunctions(Nifti1Dataset inputNifti, int iter) throws IOException {
		
		inputNifti.readHeader();
		double[][][] newData = EditScan.robustDataCopy(inputNifti.readDoubleVol((short)0));
		
		//set boundary matrix
		double average = EditScan.totalAvg(newData, true); // TODO: adjust values below based on average
		
		System.out.println("Performing " + iter + " iterations of thresholding on the scan.");
		
		//GraphSlice.graphSliceData(inputNifti, newData);
		
		int counter = 0;
		while (counter < iter) {
			
			if (Vars.smooth) {
				
				newData = EditScan.movingAvg(newData, Vars.smoothlen);
			}
			
			if (Vars.gradCorr) {
				
				// x-dimension
				newData = CalcScan.findGrad(newData, true, false, false, Vars.stdev, Vars.length, Vars.lhbound, true, false);
				newData = CalcScan.findGrad(newData, true, false, false, Vars.stdev, Vars.length, Vars.rhbound, false, true);
				// y-dimension
				newData = CalcScan.findGrad(newData, false, true, false, Vars.stdev, Vars.length, Vars.supbound, true, false);
				newData = CalcScan.findGrad(newData, false, true, false, Vars.stdev, Vars.length, Vars.infbound, false, true);
				// z-dimension
				newData = CalcScan.findGrad(newData, false, false, true, Vars.stdev, Vars.length, Vars.antbound, true, false);
				newData = CalcScan.findGrad(newData, false, false, true, Vars.stdev, Vars.length, Vars.posbound, false, true);
			}
			
			if (Vars.erosion) {
				
				float[] avgs = EditScan.dimAvg(newData, Vars.depth);
				
				// x-dimension
				newData = EditScan.erode(inputNifti, newData, true, false, false, Vars.lherode, (float) avgs[0]/2, true, false);
				newData = EditScan.erode(inputNifti, newData, true, false, false, Vars.rherode, (float) avgs[1]/4, false, true);
				// y-dimension
				newData = EditScan.erode(inputNifti, newData, false, true, false, Vars.superode, (float) avgs[2]/2, true, false);
				newData = EditScan.erode(inputNifti, newData, false, true, false, Vars.inferode, (float) avgs[3]/2, false, true);
				// z-dimension
				newData = EditScan.erode(inputNifti, newData, false, false, true, Vars.anterode, (float) avgs[4]/3, true, false);
				newData = EditScan.erode(inputNifti, newData, false, false, true, Vars.poserode, (float) avgs[5]/2, false, true);
			}
			
			if (Vars.clean) {
				
				newData = EditScan.cleanUp(newData, Vars.erode, true, true, true, true, true); // w/ simpleCleaner
				newData = EditScan.cleanUp(newData, Vars.erode, true, true, true, true, false); // w/o simpleCleaner
			}
			
			if (Vars.patchOvershots) {
				
				newData = EditScan.patchOvershots(newData);
				newData = EditScan.patchOvershots(newData);
				newData = EditScan.distalClean(newData);
			}
			
			// generates intensity histogram, uncomment if needed
			if (Vars.histogram) {
				EditScan.histogram(newData, "y", newData.length-1, newData[0][0].length-1, newData[0].length-1, 1, newData[0].length/2);
			}
			
			System.out.println("Iteration complete..."); System.out.println("");
			counter++;
		}
		
		System.out.println("Original average non-zero intensity of the scan was: " + 
		CalcScan.round(average,2));
		System.out.println("Average non-zero intensity of the scan is now: " + 
		CalcScan.round(EditScan.totalAvg(newData, true),2));
		System.out.println("");
		
		inputNifti.writeVol(newData,(short) 0);
		
	}
	
}
