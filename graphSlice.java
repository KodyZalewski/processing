//import org.rosuda.REngine.REXP;
//import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

// potentially use Jchart? 


public class graphSlice {
	
	public double total;
	public int width, height;
	//public Graphics g; 
	
	public double graphSliceData(Nifti1Dataset niftiScan, double [][] slice, double[][][] data) throws RserveException {
		
		RConnection c = new RConnection();
		System.out.println(c.eval("R.version.string"));
		
		c.assign("x", Integer.toString(slice.length));
		c.assign("y", Integer.toString(slice.length));
		
		//double[] vector = new double[niftiScan.XDIM];
		
		for (int x = 0; x < slice.length; x++) {
			for (int y = 0; y < slice[x].length; y++) {
				
			}
			
		}
		
		//g.drawLine();
		
		return total/(width*height);
	}
}
