import java.io.IOException;

public class smoothVolume {
	
	static double data[][][];
	static double data1[][][];
	public static String LOCALPATH = "/home/ns-zalewk/workspace/ExvivoNifti/src/";
	
	public static void main(String[] args) throws IOException {
			
			Nifti1Dataset nds = new Nifti1Dataset(args[0]);
			nds.readHeader();
			
			String[] newArgs = new String[3]; newArgs[0] = "copy"; newArgs[1] = "flipped.nii"; newArgs[2] = "smoothed.nii";
			TestNifti1Api.main(newArgs);
			
			Nifti1Dataset smoothNifti = new Nifti1Dataset(args[1]);
			smoothNifti.readHeader();
			
			data = nds.readDoubleVol((short) 0);
			double[][][] smoothed = new double[nds.XDIM][nds.YDIM][nds.ZDIM];
			smoothed = readNifti.calcAvgThreeDim(data, 1, nds.XDIM, nds.YDIM, nds.ZDIM);
		
			smoothNifti.writeVol(smoothed,(short)0);
				
			// testing below here
			
			data1 = smoothNifti.readDoubleVol((short) 0);
			
			int count = 0;
			for (int i = 0; i < data1.length; i++) {
				for (int j = 0; j < data1[i].length; j++) {
					count += data1[i][j][128];
				}
				count = count/data1[i].length;
				for (int j = 0; j < data1[i].length; j++) {
					data1[i][j][128] = (double) count;
				}
				count = 0;
			}
			
			smoothNifti.writeVol(data1,(short)0);
			
	}
	// this straight-up doesn't work, "WARNING: neither NIfTI-1 qform or sform are valid, MatrixMultiply: m1 is null!"
	// checked header and it's identical to the copy. Data not being stored? 
	/**String[] newArgs = new String[8]; 
	newArgs[0] = "create"; newArgs[1] = args[1]; newArgs[2] = "16"; 
	newArgs[3] = Short.toString(nds.XDIM); newArgs[4] = Short.toString(nds.YDIM); 
	newArgs[5] = Short.toString(nds.ZDIM); newArgs[6] = "1"; newArgs[7] = "0";	
	TestNifti1Api.main(newArgs);
	**/
}
