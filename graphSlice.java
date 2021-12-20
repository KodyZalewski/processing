
//import org.rosuda.REngine.REXP;
//import org.rosuda.REngine.REXPMismatchException;
//import org.rosuda.REngine.Rserve.RserveException;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.JRI.Rengine;

// Requires REngine and RServe libraries to run
// Creates and graphs the output of the NIFTI scans

public class graphSlice {
		
	public double total;
	public int width, height;
	// change to double at some point
	public static void graphSliceData(Nifti1Dataset niftiScan, double[][][] data) throws REngineException, REXPMismatchException {
		
		
		//Rengine re=new Rengine();

		//RConnection c = new RConnection();
		//System.out.println(re.eval("R.version.string"));
		
		//re.assign("x", Integer.toString(data.length));
		//re.assign("y", Integer.toString(data[0].length));
		
		//double[] vector = new double[niftiScan.XDIM];
		double[] vector = new double[data.length - 1];
		double[] vector2 = new double[data[0].length - 1];
		double[] vector3 = new double[data[0][0].length - 1];
		String blah = "c( ";
		String blah2 = "c( ";
		String blah3 = "c( ";
		
		int count = 0; int count2 = 0; double val = 0;
		
		// First dimension
		for (int x = 0; x < data.length - 1 ; x++) {
			for (int y = 0; y < data[x].length - 1; y++) {
				for (int z = 0; z < data[x][y].length; z++) {
					val = data[x][y][z];
					count++;
					count2+=val;
				}
			}
			
			vector[x] = count2/count;
			blah += vector[x] + ", ";
			count = 0; count2 = 0;
		}
		
		// Second dimension
		for (int y = 0; y < data[0].length - 1; y++) {
			for (int x = 0; x < data.length - 1 ; x++) {
				for (int z = 0; z < data[0][0].length - 1; z++) {
				
					val = data[x][y][z];
					count++;
					count2+=val;
				}
			}
					
			vector2[y] = count2/count;
			blah2 += vector2[y] + ", ";
			count = 0; count2 = 0;		
		}
		
		// Third dimension
		for (int z = 0; z < data[0][0].length -1; z++) {
			for (int y = 0; y < data[0].length - 1; y++) {
				for (int x = 0; x < data.length - 1 ; x++) {
					val = data[x][y][z];
					count++;
					count2+=val;
				}
			}
							
			vector3[z] = count2/count;
			blah3 += vector3[z] + ", ";
			count = 0; count2 = 0;		
		}
		
		System.out.println("loading historgram from R...");
		//re.eval("library(hist)");
		//re.eval("require(hist)");
		System.out.println("Done.");
		
		String dist = "Distribution of intensity across scan.";
		System.out.println(blah + ")");
		System.out.println(blah2 + ")");
		System.out.println(blah3 + ")");
		
		//re.assign("dist", dist);
		//long e = re.rniParse("hist(vector, dist, )", 1);
		//re.rniEval(e, 0);
		
		//return total/(width*height);
	}
	
	// returns average value of a given slice vector
	/**public double calcAvg(double[] vector) {
		int count = 0; int total = 0;	
		for (int v = 0; v < vector.length -1; v++) {
			total += vector[v];
			count++; 
		}
		return total/count; 
	}**/
}
