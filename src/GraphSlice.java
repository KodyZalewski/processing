
//import org.rosuda.REngine.REXP;
//import org.rosuda.REngine.REXPMismatchException;
//import org.rosuda.REngine.Rserve.RserveException;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
//import org.rosuda.JRI.Rengine;

// Requires REngine and RServe libraries to run
// Creates and graphs the output of the NIFTI scans

//  THIS CLASS IS NOT FINISHED - make functional with REngine//

public class GraphSlice {
		
	// change to double at some point
	public static void graphSliceData(Nifti1Dataset niftiScan, double[][][] data) throws REngineException, REXPMismatchException {
		
		//Rengine re=new Rengine();

		//RConnection c = new RConnection();
		//System.out.println(re.eval("R.version.string"));
		
		//re.assign("x", Integer.toString(data.length));
		//re.assign("y", Integer.toString(data[0].length));
		
		System.out.println("loading historgram from R...");
		//re.eval("library(hist)");
		//re.eval("require(hist)");
		System.out.println("Done.");
		
		// TODO: Have to manually input the values to R, why don't they load?
		RStringX = search(data, data.length, data.length[0], data.length[0][0], "x");
		RStringY = search(data, data.length[0], data.length[0][0], data[0].length, "y");
		RStringZ = search(data, data.length[0][0], data[0].length, data.length, "z");
		
		String dist = "Distribution of intensity across scan.";
		//re.assign("dist", dist);
		//long e = re.rniParse("hist(vector, dist, )", 1);
		//re.rniEval(e, 0);
		
		// for testing
		System.out.prinln(RStringX);
		System.out.prinln(RStringY);
		System.out.prinln(RStringZ);

	}

	/**
	 *  Takes data, x/y/z dimension as string, lengths of MRI dimensions and double vector
	 *  Returns a string containing the average intensity for each row of X, this can then
	 *  be graphed using R's histogram function.   
	 */
	public static String search(data double[][][], dim, len1, len2, len3) {
		
		int count = 0; int count2 = 0; double val = 0; String Rstring = "c( ";
		
		for (int dim1 = 0; dim1 < len1 -1; dim1++) {
			for (int dim2 = 0; dim2 < len2 - 1; dim2++) {
				for (int dim3 = 0; dim3 < len3 - 1 ; dim3++) {	
					count++;
					count2+=CalcScan.pullData(data, dim, dim1, dim2, dim3);
				}
			}						
			Rstring += count2/count + ", ";
			count = 0; count2 = 0;		
		}
		return RString + ")";
	}

}
