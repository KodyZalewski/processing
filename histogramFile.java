import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.sheet.XSpreadsheet;


public class histogramFile extends makeSheet {
	
	// takes a spreadsheet and a name as an argument, at some point, this should print out a histogram of all 
	// columns contain a text value in the first row equal to the name of the desired ROI
	public static void histogramFile(XSpreadsheet aSheet, String roiName, int binSize, String subject, String study) throws IndexOutOfBoundsException, FileNotFoundException {
		List<Float> histogram = new ArrayList<Float>();
		int columnCounter = 0;
		int rowCounter = 0;
		while (!getCellValueString(aSheet.getCellByPosition(columnCounter,0)).isEmpty()) {
			if (getCellValueString((aSheet.getCellByPosition(columnCounter,0))).toLowerCase().contains(roiName.toLowerCase())) {	    
				while (!getCellValueString(aSheet.getCellByPosition(columnCounter,rowCounter+1)).isEmpty()) { // rowCounter +1 skips the first row
					String cellValue = getCellValueString(aSheet.getCellByPosition(columnCounter,rowCounter+1));
					if (!cellValue.equals("NULL")) {
						try {
							histogram.add(Float.parseFloat(cellValue));
						} catch (NumberFormatException ex) {}
					}		
					rowCounter++;
				}
				rowCounter = 0;
			}
			columnCounter++;
		}
		// writes the data out to a file in the subject directory
		System.out.println("Printing histogram for ROI " + roiName + " in study " + study);
		File fileName = new File("/home/ns-zalewk/" + roiName + subject + ".txt");
		PrintStream printName = new PrintStream(fileName);
		for (float value : histogram) {
			printName.print(value);
		}
		printName.close();
		// printChart(histogram, binSize) print out histogram here somehow, probably display it in default intervals of 100 voxels 

	}
	
	/*public static void printChart(List<Float> histogram, int binSize) {
	int maxValue = 0;
	int minValue = Integer.MAX_VALUE;
	for (float volume : histogram) { // find the largest value in the arrayList to make an appropriate number of bins
		if (volume > maxValue) {
			maxValue = Math.round(volume); 
		}
		if (volume < minValue) {
			minValue = Math.round(volume); 
		}
	}
	Map<Integer, Float> bins = new HashMap<Integer, Float>(); 
	int binNumber = ((maxValue - minValue)/binSize);
	for (int i = minValue; i < maxValue; i += binSize) {
		
	}

	for (float volume : histogram) {
		
	}*/

}
