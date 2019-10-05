//import com.sun.star.beans.PropertyValue;
//import com.sun.star.container.XIndexAccess;
//import com.sun.star.frame.XComponentLoader;
//import com.sun.star.frame.XModel;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
//import com.sun.star.table.XCell;
//import com.sun.star.uno.RuntimeException;
//import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import java.io.*;

public class getValues {
	
	private static XComponentContext      maContext;
    private static XMultiComponentFactory maMCFactory;
    private static XSpreadsheetDocument volSpreadSheetDoc;
	public static String LOCALPATH = "/home/ns-zalewk/Desktop/memorycube/";
	
	public static String TESTPATH = "/home/ns-zalewk/R_script_test/";
	
	private static final String LOCALDATA = "00_DATA";
	
	private static String study;
	
	private static String ROI; 
	
	public static void main(String[] args) throws IOException {
		
		getArgs(args);
		System.out.println(LOCALPATH + LOCALDATA + "/" + study + "_FS.ods");
		volSpreadSheetDoc = odsClass.calcOpen(LOCALPATH + LOCALDATA + "/" + study + "_FS.ods", maMCFactory, maContext);
		
		fetchROI(odsClass.getDataSheet(0, volSpreadSheetDoc)); 
		
		System.exit(0);	
	}
	
	// obtains arguments for passing to the study and ROI values
	public static void getArgs(String args[]) {
		if (args.length != 2 ) {
			System.err.println("ERROR: Function must have a study and ROI");
			System.err.println("In the format: makeSheet <study> <ROI>");
			System.exit(1);
		} else {
			study = args[0];
			ROI = args[1];
		}    
	}
	
	// takes the spreadsheet for the desired values as an argument, checks the string value of a cell, 
	// and the name of an ROI as arguments, throws OutOfBounds  exception if end of row is reached unexpectedly 
	// or IO Output error if the path given is incorrect
	public static void fetchROI(XSpreadsheet aSheet) throws IndexOutOfBoundsException, IOException {
		int colCount = 0;
		String newROIValue;	
		try {
			while (!odsClass.getCellValueString(aSheet.getCellByPosition(colCount,0)).isEmpty()) {
				if (odsClass.getCellValueString(aSheet.getCellByPosition(colCount,0)).toLowerCase().contains(ROI)) {
					System.out.println(odsClass.getCellValueString(aSheet.getCellByPosition(colCount,0)));
					newROIValue = odsClass.getCellValueString(aSheet.getCellByPosition(colCount,0));
					pullNumbers(aSheet, newROIValue, colCount);
				}
				colCount++;
			}
		} catch (com.sun.star.lang.IndexOutOfBoundsException e) {e.printStackTrace();}
	}
	
	// takes values for the spreadsheet, the column number and name of ROI as arguments and outputs them to a file, 
	// throws OutOfBounds exception if end of row is reached unexpectedly or IO Output error if the path given is incorrect
	public static void pullNumbers(XSpreadsheet aSheet, String ROI, int colCount) throws IndexOutOfBoundsException, IOException {
		int rowCount = 0;
		PrintStream totalFile = new PrintStream(TESTPATH + ROI.toLowerCase() + ".txt");
		System.out.println("Printing out to the file " + ROI + ".txt on path: " + TESTPATH);
		try {
			while (!odsClass.getCellValueString(aSheet.getCellByPosition(colCount,rowCount)).isEmpty())  {
				totalFile.println(odsClass.getCellValueString(aSheet.getCellByPosition(colCount,rowCount)));
				rowCount++;
			}} catch (com.sun.star.lang.IndexOutOfBoundsException e) {e.printStackTrace();}
		totalFile.close();	
	}
}
