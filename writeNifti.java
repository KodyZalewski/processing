import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;

/** Class used in conjuction with readNifti in pre-processing exvivo nifti scans.
 * @param: Takes a Nifti-1 dataset and double matrix of data as an argument.
 *  
 *  The matrix is written first to a data output stream object. If any boundaries are found the output to the stream is 0 
 * (will be 0 before or after at some point too), otherwise the original data is just reassigned to the output stream. 
 * This data is then reassigned to the Nifti-1 dataset provided in blob (Binary Large OBject) format and the stream is closed.  
 *  
 * @exception throws IOException or number format exception. 
 *  TODO: copied from Nifti1Dataset only start by working with short/int, can extrapolate to other formats later
	   	
    TODO: Don't need to redundantly assign intensity to most of the voxels, only the ones we're masking out. Find a way to 
	only re-assign masked voxels to the Nifti-1. **/

public class writeNifti {
	
	public static final int HEADER = 348; // 352 in other formats? we're working exclusive in NIFTI-1+ for now

	public static int x,y,z,i,j,k; //nifti dimension counters
	
	public static void writeNiftiOutput(Nifti1Dataset ntf, String outputFile, double[][][] data) throws IOException {
		writeNiftiHelper(ntf, outputFile, data);
		//writeNiftiHelper(ntf, outputFile, zeroOutVoxels(data, dimBound));
	}
	
	private static void writeNiftiHelper(Nifti1Dataset ntf, String outputFile, double[][][] data) 
	throws NumberFormatException, IOException { 
		
		int blob_size;
		EndianCorrectOutputStream ecs;
		ByteArrayOutputStream baos;
		
		blob_size = ntf.XDIM * ntf.YDIM * ntf.ZDIM * ntf.bytesPerVoxel(ntf.datatype); // would need to potentially adjust 
		baos = new ByteArrayOutputStream(blob_size);	
		//ecs = new EndianCorrectOutputStream(new ByteArrayOutputStream(HEADER), false); // byte size and big or little endian
		ecs = new EndianCorrectOutputStream(baos, ntf.big_endian);
		
		for (x = 0; x < ntf.ZDIM; x++) {
			for (y = 0; y < ntf.YDIM; y++) {
				for (z = 0; z < ntf.XDIM; z++) {  // 1 is only for testing
					if (ntf.scl_slope == 0) {
						ecs.writeFloatCorrect((float)(data[x][y][z])); 
					} else {
						ecs.writeFloatCorrect((float)(data[x][y][z] - ntf.scl_inter) / ntf.scl_slope); 
					}
				}
			}
		}		
		/** testing statement 
		double sum = 0.0;
		for (x = 0; x < ntf.ZDIM; x++) {
			for (y = 0; y < ntf.YDIM; y++) {
				for (z = 0; z < ntf.XDIM - 1; z++) { // 1 is only for testing
					if (((data[x][y][z] - ntf.scl_inter) / ntf.scl_slope) != 0) {
						counter++;
						sum += data[x][y][z];
					}
				}
			}
		}
		System.out.println(counter);
		System.out.println(sum/counter);*/
		
		ntf.readHeader();
		ntf.setHeaderFilename(outputFile);
		ntf.setDataFilename(outputFile);
		ntf.writeHeader();
		
		// write output file here	
		ntf.writeVolBlob(baos,(short)1); 
		((FilterOutputStream) ecs.newFile).close();
		
		System.out.println("Finished writing data.");
		System.out.println("Exiting ... ");
	}
	
	
	public static double[][][] zeroOutVoxels(double data[][][], int dimBound[][]) {
		return zeroOutVoxelsHelper(data, dimBound);
	}
	
	private static double[][][] zeroOutVoxelsHelper(double data[][][], int[][] dimBound) {
	
		for (i = 0; i < dimBound.length; i++) {
			for (j = 0; j < dimBound[i].length; j++) {
				int counter = 0;
				if (dimBound[i][j] != 0) {
					while (counter < data[i][j].length) {
						data[i][j][counter] = 0;
						counter++;
					}
				} else {
					while(counter < dimBound[i][j]) {
						data[i][j][counter] = 0;
						counter++;
					}
				}
			}
		}
		return data;
	}
	
	/** public static void copyNifti(String outputName, Nifti1Dataset nds) {
		try {
			nds.readHeader();
			byte[] b = nds.readData();
			nds.setHeaderFilename(outputName);
			nds.setDataFilename(outputName);
			nds.writeHeader();
			nds.writeData(b);
		} catch (IOException ex) {
			System.out.println("\nCould not copy to " + outputName + ": " + ex.getMessage());
		}
		return;
	}**/
}
