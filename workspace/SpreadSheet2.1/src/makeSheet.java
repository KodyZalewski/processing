/* 
* Program Description:
* 
* This is a short program that takes the desired study as the first argument, followed by successive subject
* numbers as the following argument in the form: java makeSheet <STUDY> <SUBJECT_ID> <SUBJECT_ID> ... in 
* keeping with the Freesurfer format of having a set study directory and subject folders as children.
*  
* LOCALPATH is is path to this subject directory on this particular computer, for any other user this should 
* be changed, likely to the $SUBJECT_DIR environmental variable. LOCALPATH will also be the site of LOCALPROC
* and LOCALDATA, which are the suffixes of the processing and volumetric data respectively alongside the 
* spreadsheets themselves.
* 
* 			       root
*  		            |
*  			    LOCALPATH
*  			   / 	|	  \
*      LOCALDATA LOCALPROC study
*      /            |	        \
*  data_sheet  processing_sheet  subjects
* 
*  Upon loading the OpenOffice component and accessing the XSpreadsheet object, (assuming the path is valid and the 
*  files present in the subject folder are correct) the program searches the first sheet of the file for a subject 
*  number corresponding to the name provided by the client. If no strings stored in the cells of the first column 
*  are matches, the values and the subject number is placed at the end of a list of values (also assumed to be 
*  subject numbers).
*  
*  The QA sheet in the processing folder may or may not exist. It's location is variable depending on the study
*  so it can be treated as being the third sheet in the spreadsheet following the volumetric data and the thickness file.
*  Otherwise it's expected to be the second sheet in the processing spreadsheet. The odsClass.insertion of the QA values is the
*  only reason the processing sheet is listed here as a variable. 
*  
*  The Freesurfer data is expected to be pre-processed beforehand with a volumetric data .txt file and a thickness data
*  .txt file that are whitespace separated with corresponding columns in the volumetric spreadsheet for both volumetric 
*  data in the first sheet and thickness values in the second. QA values function in much the same way. 
*  
*  KJZ 8.16.2018
*/

// importing access
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

//import com.sun.star.beans.PropertyValue;
import com.sun.star.comp.helper.BootstrapException;
//import com.sun.star.container.XIndexAccess;
//import com.sun.star.frame.XComponentLoader;
//import com.sun.star.frame.XModel;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
//import com.sun.star.table.XCell;
//import com.sun.star.uno.RuntimeException;
//import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

public class makeSheet {
	
	 // __________ private members __________   
    private static XComponentContext      maContext;
    private static XMultiComponentFactory maMCFactory;
    private static XSpreadsheetDocument volSpreadSheetDoc;
    private static XSpreadsheetDocument qaSpreadSheetDoc;
    
    // counter variables
    private static int rowCountFinalQa = 0;     
    private static int rowCountFinalVol = 0; 
	
    // local final variables
	private static final String LOCALPATH = "/home/ns-zalewk/Desktop/memorycube/";	
	private static final String LOCALDATA = "00_DATA";
	private static final String LOCALPROC = "00_LOGS";
	
	// client-specified variables
	private static String study; 
	private static String subject;
	private static List<String> subjectList = new ArrayList<String>();
	private static boolean histogram = false;
	private static boolean leftHemi = false;
	private static boolean rightHemi = false;
	
	// local non-final variables
	private static String spreadsheetPath;
	private static String qaSheetPath;
	private static int qaValue; 
	
	public static void main(String args[]) throws IOException, URISyntaxException, IndexOutOfBoundsException, BootstrapException {
		
		getArgs(args);
		checkArgs(args); // checks to make sure arguments and paths are valid, assigns path to spreadsheets if 
		
		// opens the volumetric spreadsheet and the QA processing spreadsheet if it's available
		volSpreadSheetDoc = odsClass.calcOpen(spreadsheetPath,  maMCFactory, maContext);
		if (qaValue == 1) {
			qaSpreadSheetDoc = odsClass.calcOpen(qaSheetPath,  maMCFactory, maContext);
		}
		
		// loops through multiple arguments provided for subject numbers following the study at args[0]
		if (subjectList != null) { 
			for (String x : subjectList) {
				
				subject = x;
				
				insertToSheet(0, volSpreadSheetDoc, subject, study, "_stats", 0);
				insertToSheet(1, volSpreadSheetDoc, subject, study, "_thickness", 1);
				if (qaValue == 1) {
					insertToSheet(qaValue, qaSpreadSheetDoc, subject, study, "final_CNR", 1);
				} else {
					insertToSheet(qaValue, volSpreadSheetDoc, subject, study, "final_CNR", 1);
				}
				
				System.out.println( "Subject " + subject + " is located at: " + rowCountFinalVol + " row in the " + study + " volumetric spreadsheet.");
				System.out.println( "Subject " + subject + " is located at: " + rowCountFinalQa + " row in the " + study + " processing spreadsheet.");
				System.out.println( subject + " has been added to the spreadsheet successfully!" );
			}
		}
		else {
			System.out.println("No subjects provided with the '-s' flag.");
		}
		if (histogram != false) {
			//histogramFile(volSpreadSheetDoc, 100, subject, study); 
		}	
		System.exit(0);
	}
	
	
	// takes the arguments passed to main() as a list of strings, assigns the study variable to the first argument, 
	// all values after "-s" are assumed to be subject numbers, alongside other arguments that can be passed to the 
	// program for defined behavior
	public static void getArgs(String args[]) throws IOException {
		
		if (args.length == 0) {
			System.err.println("ERROR: Function must have both a study and subject. Study should be first argument, subjects should come after.");
			System.err.println("In the format: makeSheet <study> <subjectID>");
			System.exit(1);
		} else {
			study = args[0];
		}
		
		int counter = 1;
		for ( String x : args ) {
			if (x.equals("-s")) { // this means the following strings are subject numbers
				for (int i = counter; i < args.length; i++) {
					if (x.equals("-r")) {
						break;
					}
					System.out.println("Adding the statistics for subject " + args[i]);
					subjectList.add(args[i]);
				}
				break;
			}	
			if (x.equals("-r")) { // TODO: check all the following strings and create histograms for those variables
				System.out.println("Looking at the values for region " + args[counter]);
				histogram = true; 
			}
			if (x.equals("-lh")) {
				System.out.println("Subjects are designated as left hemisphere volumes");
				leftHemi = true;
			}
			if (x.equals("-rh")) {
				System.out.println("Subjects are designated as right hemisphere volumes");
				rightHemi = true;
			}
			if (x.equals("-path")) {
				System.out.println("Changing the local path...");
				//LOCALPATH = args[i];
			}
			counter++;
		}
	}
	
	// Takes the arguments passed to main() as an array of strings, assigns the study variable to the first argument.
	// Checks to see if there is a separate log spreadsheet where the QA measures for the subjects would be kept
	// and returns a respective int value to flag that sheet. If not, it is assumed to be the same sheet as 
	// volumetric data. If the QA sheet is not there the program engages in unspecified behavior. 
	public static void checkArgs(String args[]) throws IOException {
		
		spreadsheetPath = LOCALPATH + LOCALDATA + "/" + study + "_FS.ods";
		qaSheetPath = LOCALPATH + LOCALPROC + "/" + "FS_" + study + "_processingspreadsheet.ods";
		
		System.out.println(spreadsheetPath);
		System.out.println(qaSheetPath);
		
		File checkVolFile = new File(spreadsheetPath);
		File checkQaFile = new File(qaSheetPath);
		
		if (!checkVolFile.exists()) {
			System.out.println("The given path for the spreadsheets is invalid.");
			System.out.println("Make sure the study is valid and the path to the directory is present.");
			System.out.println("Would you like to create a new spreadsheet for this study? (Y/n): ");
			Scanner console = new Scanner(System.in);
			String response = console.nextLine();
			if (response.equalsIgnoreCase("Y") || response.equalsIgnoreCase("Yes")) {
				System.out.println("Creating a new spreadsheet for study "+study);
				odsClass.getDocument(spreadsheetPath, maMCFactory, maContext); // TODO: make sure this works
			} else {
				System.out.println("Subject was not added to the spreadsheet, exiting.");	
				System.exit(1);
			}
			console.close();
		}
		if (checkQaFile.exists()) {			
			qaValue = 1; 
		} else { // TODO: At some point, should loop through sheets and check for name of qameasures
			System.out.println("QA measures are probably located in the same file as volumetric measures.");	
			qaSheetPath = spreadsheetPath;
			qaValue = 2; 
		}
	}
	
	// Takes a sheet index number as int, a spreadsheet of data, subject number as string, study value as string, file suffix/prefix,
	// and column number as int as arguments.
	public static void insertToSheet(int index, XSpreadsheetDocument data, String subject, String study, String fileName, int column) 
			throws IOException, URISyntaxException, IndexOutOfBoundsException {
		String path = "";
		if (!fileName.equals("final_CNR")) {
			path = LOCALPATH+study+"/"+subject+"/"+subject+"_FREESURFER/stats/"+subject+fileName+".txt";
		} else {
			path = LOCALPATH+study+"/"+subject+"/"+subject+"_FREESURFER/stats/"+fileName+"_"+subject+".txt";			
		} 
		File file = new File(path); 
		if (file.exists()) {
			XSpreadsheet sheet = odsClass.getDataSheet(index, data);
			if (sheet != null) {
				Scanner subjectFile 
					= new Scanner(Paths.get(new URI("file://"+path)));
				readIn(column, 1, subjectFile, sheet, subject, fileName);
				subjectFile.close();
			} else {
				System.out.println("");
				System.out.println("The sheet does not exist! Skipping input.");
				System.out.println("");
			}
		} else {
			System.out.println("");
			System.out.println("The " + fileName + " file does not exist! Skipping input.");
			System.out.println("");
		}
	}
	
	// takes two integers as coordinates, a file to be read into the spreadsheet, and the spreadsheet as an argument
	// throws indexOutOfBounds exception for the getCellByPosition function
	// all values are fed into the spreadsheet as strings until the end of the first line of the file is reached
	// hemisphere-specific volumetric values are included for exvivo scans 
	public static void readIn(int colCount, int rowCount, Scanner newFile, XSpreadsheet aSheet, String subject, String fileName) throws IndexOutOfBoundsException {		
		try {
			while ( !odsClass.getCellValueString(aSheet.getCellByPosition(0,rowCount)).equals(subject) && 
					!odsClass.getCellValueString(aSheet.getCellByPosition(0,rowCount)).isEmpty()) {
					rowCount++;
			}
		} catch (IndexOutOfBoundsException e) {
			e.printStackTrace();
		}
		if (fileName.equals("_thickness") || fileName.equals("final_CNR")) {
				odsClass.insert(aSheet.getCellByPosition(0, rowCount), subject);			
		}
		Scanner lineRead = new Scanner(newFile.nextLine());
		while (lineRead.hasNext()) {
			if (leftHemi == false && rightHemi == false) {
				odsClass.insert(aSheet.getCellByPosition(colCount,rowCount), lineRead.next());
			} else if (leftHemi == true && rightHemi == false) {
				leftHemisphereInsert(colCount, rowCount, lineRead, aSheet, fileName);
			} else if (leftHemi == false && rightHemi == true) {
				rightHemisphereInsert(colCount, rowCount, lineRead, aSheet, fileName);
			} else {
				odsClass.insert(aSheet.getCellByPosition(colCount,rowCount), lineRead.next());
			}
			colCount++;
		}
		if (fileName.equals("_stats")) {	
			rowCountFinalVol = rowCount;
			rowCountFinalVol++;
		} else if (fileName.equals("final_CNR")) {
			rowCountFinalQa = rowCount;
			rowCountFinalQa++;
		}
		lineRead.close();
	}
	
	// includes all of the same variables as the normal odsClass.insert function, with custom values to overwrite with "N/A" if they
	// are located on the dummy hemisphere and should not be included in analysis, also throws IndexOutOfBoundsException
	public static void leftHemisphereInsert(int colCount, int rowCount, Scanner lineRead, XSpreadsheet aSheet, String fileName) 
			throws IndexOutOfBoundsException {
		if (fileName.equals("_stats")) {
			if ( 0 < colCount && colCount < 4 || 4 < colCount && colCount < 7 || 7 < colCount && colCount < 15 || 36 < colCount && colCount < 51 || 
			99 < colCount && colCount < 134 || 167 < colCount && colCount < 202 || colCount == 203 ) {
				odsClass.insert(aSheet.getCellByPosition(colCount,rowCount), "N/A");
				lineRead.next();
				if (colCount == 51) {
					lineRead.next();
				}
			} else {
				odsClass.insert(aSheet.getCellByPosition(colCount,rowCount), lineRead.next());
			}
		} else if (fileName.equals("_thickness")) {
			if (colCount > 69) {
				odsClass.insert(aSheet.getCellByPosition(colCount,rowCount), "N/A");
				lineRead.next();
			} else {
				odsClass.insert(aSheet.getCellByPosition(colCount,rowCount), lineRead.next());
			}
		}
		/* needs to be made consistent with the format of CNR files 
		 * if (fileName.equals("final_CNR") {
		 * 		if (colCount == 3 || colCount == 5 || colCount > 39) {
					odsClass.insert(aSheet.getCellByPosition(colCount,rowCount), "N/A");
					lineRead.next();
				}
		}*/
		else {
			odsClass.insert(aSheet.getCellByPosition(colCount,rowCount), lineRead.next());
		}
	}
	
	// includes all of the same variables as the normal odsClass.insert function, with custom values to overwrite with "N/A" if they
	// are located on the dummy hemisphere and should not be included in analysis, also throws IndexOutOfBoundsException
	public static void rightHemisphereInsert(int colCount, int rowCount, Scanner lineRead, XSpreadsheet aSheet, String fileName) 
			throws IndexOutOfBoundsException {
		if (fileName.equals("_stats")) {
			if (0 < colCount && colCount < 5 || 5 < colCount && colCount < 8 || 8 < colCount && colCount < 15 || 16 < colCount && colCount < 26 || 26 < colCount && colCount < 31 || 
			35 > colCount && colCount > 31 || 99 > colCount && colCount > 63 || 168 > colCount && colCount > 133 || colCount == 202 ) {
				if (colCount == 17 || colCount == 64) {
					odsClass.insert(aSheet.getCellByPosition(colCount,rowCount), "NULL");
					if ( colCount == 64 ) {
						lineRead.next();
					}
				} else {
					odsClass.insert(aSheet.getCellByPosition(colCount,rowCount), "N/A");
					lineRead.next();
				}
			} else {
				odsClass.insert(aSheet.getCellByPosition(colCount,rowCount), lineRead.next());
			}
		} else if (fileName.equals("_thickness")) {
			if (colCount < 68) {
				odsClass.insert(aSheet.getCellByPosition(colCount,rowCount), "N/A");
				lineRead.next();
			} else {
				odsClass.insert(aSheet.getCellByPosition(colCount,rowCount), lineRead.next());
			}
		}
		/* needs to be made consistent with the format of CNR files 
		 * if (fileName.equals("final_CNR") {
		 *    if (colCount == 2 || colCount == 4 || < colCount && colCount < 40) {
				odsClass.insert(aSheet.getCellByPosition(colCount,rowCount), "N/A");
				lineRead.next();
			  }
		}*/
		else {
			odsClass.insert(aSheet.getCellByPosition(colCount,rowCount), lineRead.next());
		}
	}
}
