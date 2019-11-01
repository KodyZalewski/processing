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
		
	}
	
	private static void writeNiftiHelper(Nifti1Dataset ntf, String outputFile, double[][][] data) 
	throws NumberFormatException, IOException { 
		
		ntf.copyHeader(ntf);
		ntf.setHeaderFilename(outputFile);
		ntf.setDataFilename(outputFile);
		
		int blob_size;
		EndianCorrectOutputStream ecs;
		ByteArrayOutputStream baos;
		
		blob_size = ntf.XDIM * ntf.YDIM * ntf.ZDIM * ntf.bytesPerVoxel(ntf.datatype); // would need to potentially adjust 
		baos = new ByteArrayOutputStream(blob_size);
		
		ecs = new EndianCorrectOutputStream(new ByteArrayOutputStream(HEADER), false); // byte size and big or little endian
		
		for (x = 0; x < ntf.ZDIM; x++) {
			for (y = 0; y < ntf.YDIM; y++) {
				for (z = 0; z < ntf.XDIM; z++) { // add if statement for - scl_inter) / scl_slope at some point
					ecs.writeFloatCorrect((float)(data[x][y][z])); 
				}
			}
		}
		
		// write output file here
		ntf.writeVolBlob(baos,(short)1); // 
		((FilterOutputStream) ecs.newFile).close();
		
		System.out.println("Finished writing data, exiting...");
	}
	
	
	
	public static double[][][] zeroOutVoxels(double data[][][]) {
		return zeroOutVoxelsHelper(data);
	}
	
	private static double[][][] zeroOutVoxelsHelper(double data[][][]) {
		
		for (i = 0; i < data.length; i++) {
			for (j = 0; j < data[i].length; j++) {
				while(data[i][j][k] != 0) {
					data[i][j][k] = 0;
					if (k == data[i][j].length) {
						break;
					}
				}
			}
		}
		return data;
		
	}
}
