import java.io.*;
//import java.util.zip.*;

/**
 *  EndianCorrectOutputStream extends DataOutputStream; it will optionally flip
 *  bytes to write little endian data correctly. 
 */

public class EndianCorrectOutputStream {
	
	public static DataOutput newFile;
	private boolean bigendian = true;

	/**
	 * Constructor for a disk file.
	 * @param filename filename for datafile
	 * @param be -- endian flag: if be (big endian) is false bytes will be flipped on write
	 * @exception FileNotFoundException
	 */
	public EndianCorrectOutputStream(String filename, boolean be) throws FileNotFoundException, SecurityException {
		
		//FileOutputStream newFile = new FileOutputStream(filename);
		bigendian = be;
		
	}

	/**
	 * Constructor for an OutputStream.
	 * @param is OutputStream to write data to
	 * @param be -- endian flag: if be (big endian) is false bytes will be flipped on write
	 * @exception FileNotFoundException
	 */
	public EndianCorrectOutputStream(OutputStream os, boolean be)  {
		super();
		bigendian = be;
	}



	/**
	 *  writeShortCorrect will write a short to the stream
	 */
	public void writeShortCorrect(short val) throws IOException {

		if (bigendian) {
			newFile.writeShort(val);
		}
		else {	
			int byte0 = (int) val & 0xff;
			int byte1 = ((int)val>>8) & 0xff;
			// swap the byte order
			newFile.writeShort((short) ((byte0<<8) | (byte1))) ;
		}
		return;
	}
	/**
	 *  flipShort will byte flip a short
	 */
	public short flipShort(short val) {

		int byte0 = (int) val & 0xff;
		int byte1 = ((int)val>>8) & 0xff;
		// swap the byte order
		return (short) ((byte0<<8) | (byte1)) ;
	}


	/**
	 *  writeIntCorrect will write an int to the stream
	 */
	public void writeIntCorrect(int val) throws IOException {

		if (bigendian){
			newFile.writeInt(val);
		}

		else {
			int byte0 = val & 0xff;
			int byte1 = (val>>8) & 0xff;
			int byte2 = (val>>16) & 0xff;
			int byte3 = (val>>24) & 0xff;
			// swap the byte order
			newFile.writeInt((byte0<<24) | (byte1<<16) | (byte2<<8) | byte3);
		}
		return;
	}

	/**
	 *  flipInt will flip the byte order of an int
	 */
	public int flipInt(int val) {

		int byte0 = val & 0xff;
		int byte1 = (val>>8) & 0xff;
		int byte2 = (val>>16) & 0xff;
		int byte3 = (val>>24) & 0xff;
		// swap the byte order
		return (byte0<<24) | (byte1<<16) | (byte2<<8) | byte3;
	}

	/**
	 *  writeLongCorrect will write a long to the stream
	 */
	public void writeLongCorrect(long val) throws IOException {

		if (bigendian){
			newFile.writeLong(val);
		}

		else {
			newFile.writeLong(flipLong(val));
		}
		return;
	}

	/**
	 *  flipLong will flip the byte order of a long
	 */
	public long flipLong(long val) {

		long byte0 = val & 0xff;
		long byte1 = (val>>8) & 0xff;
		long byte2 = (val>>16) & 0xff;
		long byte3 = (val>>24) & 0xff;
		long byte4 = (val>>32) & 0xff;
		long byte5 = (val>>40) & 0xff;
		long byte6 = (val>>48) & 0xff;
		long byte7 = (val>>56) & 0xff;
		// swap the byte order
		return (long) ((byte0<<56) | (byte1<<48) | (byte2<<40) | (byte3<<32) | (byte4<<24) | (byte5<<16)| (byte6<<8) | byte7);
	}


	/**
	 *  writeFloatCorrect will write a float to the stream
	 */ 
	public void writeFloatCorrect(float val) throws IOException {

		if (bigendian){
			newFile.writeFloat(val);
		}

		else {
			newFile.writeFloat(flipFloat(val));
		}
		return;
	}

	/**
	 *  flipFloat will flip the byte order of a float
	 */ 
	public float flipFloat(float val) throws IOException {

		int x = Float.floatToIntBits(val);
		int y = flipInt(x);
		return Float.intBitsToFloat(y);
	}

	/**
	 *  writeDoubleCorrect will write a double to the stream
	 */
	public void writeDoubleCorrect(double val) throws IOException {

		if (bigendian){
			newFile.writeDouble(val);
		}
		else {
			newFile.writeDouble(flipDouble(val));
		}
		return;
	}

	/**
	 *  flipDouble will flip the byte order of a double
	 */ 
	public double flipDouble(double val) {

		long x = Double.doubleToLongBits(val);
		long y = flipLong(x);
		return Double.longBitsToDouble(y);
	}

}
