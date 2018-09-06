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
*  Otherwise it's expected to be the second sheet in the processing spreadsheet. The insertion of the QA values is the
*  only reason the processing sheet is listed here as a variable. 
*  
*  The Freesurfer data is expected to be pre-processed beforehand with a volumetric data .txt file and a thickness data
*  .txt file that are whitespace separated with corresponding columns in the volumetric spreadsheet for both volumetric 
*  data in the first sheet and thickness values in the second. QA values function in much the same way. 
*  
*  KJZ 8.16.2018
*/

// importing access
import com.sun.star.beans.*;
import com.sun.star.uno.RuntimeException;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import java.io.*;
import java.net.URI;
import java.nio.file.Paths;
import java.util.*;

// application specific classes
import com.sun.star.table.*;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.container.*;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XModel;

// Exceptions
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.XMultiComponentFactory;
import java.net.URISyntaxException;

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
	//private static boolean histogram = false; 
	
	// local non-final variables
	private static String spreadsheetPath;
	private static String qaSheetPath;
	private static int qaValue; 
	
	public static void main(String args[]) throws IOException, URISyntaxException, IndexOutOfBoundsException, BootstrapException {
		
		//getArgs(args);
		checkArgs(args); // checks to make sure arguments and paths are valid, assigns path to spreadsheets if 
		
		// opens the volumetric spreadsheet and the QA processing spreadsheet if it's available
		volSpreadSheetDoc = calcOpen(spreadsheetPath);
		if (qaValue == 1) {
			qaSpreadSheetDoc = calcOpen(qaSheetPath);
		}
		
		// loops through multiple arguments provided for subject numbers following the study at args[0]
		//if (subjectList != null) { for (String x : subjectsList) {
			for (int i = 1; i < args.length; i++) {
				
				subject = args[i];
				
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
		//} }
		//if (histogram != false) {
		//	histogramFile(volSpreadSheetDoc, 100, subject, study); 
		//}	
		System.exit(0);
	}
	
	
	// takes the arguments passed to main() as a list of strings, assigns the study variable to the first argument, 
	// all values after "-s" are assumed to be subject numbers, alongside other arguments that can be passed to the 
	// program for defined behavior
	public static void getArgs(String args[]) throws IOException {
		int counter = 1;
		study = args[0];
		
		for ( String x : args ) {
			if (x.equals("-s")) { // this means the following strings are subject numbers
				for (int i = counter; i < args.length; i++) {
					System.out.println("Adding the statistics for subject " + args[i]);
					subjectList.add(args[i]);
				}
				break;
			}	
			/*if (x.equals("-r")) { //argument 1
				System.out.println("Looking at the values for region " + args[i])
			}
			
			if (x.equals()) { //argument 2
				
			}
			*/
			counter++;
		}
	}
	
	// Takes the arguments passed to main() as an array of strings, assigns the study variable to the first argument.
	// Checks to see if there is a separate log spreadsheet where the QA measures for the subjects would be kept
	// and returns a respective int value to flag that sheet. If not, it is assumed to be the same sheet as 
	// volumetric data. If the QA sheet is not there the program engages in unspecified behavior. 
	public static void checkArgs(String args[]) throws IOException {
		if (args[0] == null) {
			System.err.println("ERROR: Function must have both a study and subject. Study should be first argument, subjects should come after.");
			System.err.println("In the format: makeSheet <study> <subjectID>");
			System.exit(1);
		} else {
			study = args[0];
		}
		
		spreadsheetPath = LOCALPATH + LOCALDATA + "/" + "FS_" + study + "_volumetricdata.ods";
		qaSheetPath = LOCALPATH + LOCALPROC + "/" + "FS_" + study + "_processingspreadsheet.ods";
		
		System.out.println(spreadsheetPath);
		System.out.println(qaSheetPath);
		
		File checkVolFile = new File(spreadsheetPath);
		File checkQaFile = new File(qaSheetPath);
		
		if (!checkVolFile.exists()) {
			System.err.println("ERROR: The given path for the spreadsheets is invalid.");
			System.err.println("Make sure the study is valid and the path to the directory is present.");
			System.exit(1);
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
			XSpreadsheet sheet = getDataSheet(index, data);
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
	
	// retrieves text from a cell and returns it as string
	public static String getCellValueString(XCell newCell) {
		  com.sun.star.text.XText xCellText = (com.sun.star.text.XText) UnoRuntime.queryInterface(com.sun.star.text.XText.class, newCell);
		  return xCellText.getString();
	}
		
	// takes two integers as coordinates, a file to be read into the spreadsheet, and the spreadsheet as an argue
	// throws indexOutOfBounds exception for the getCellByPosition function
	// all values are fed into the spreadsheet as strings until the end of the first line of the file is reached
	public static void readIn(int colCount, int rowCount, Scanner newFile, XSpreadsheet aSheet, String subject, String fileName) throws IndexOutOfBoundsException {		
		try {
			while ( !getCellValueString(aSheet.getCellByPosition(0,rowCount)).equals(subject) && 
					!getCellValueString(aSheet.getCellByPosition(0,rowCount)).isEmpty()) {
					rowCount++;
			}
		} catch (IndexOutOfBoundsException e) {
			e.printStackTrace();
		}
		if (fileName.equals("_thickness") || fileName.equals("final_CNR")) {
			insert(aSheet.getCellByPosition(0, rowCount), subject);
		}
		Scanner lineRead = new Scanner(newFile.nextLine());
		while (lineRead.hasNext()) {
			insert(aSheet.getCellByPosition(colCount,rowCount), lineRead.next());
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
	
	// inserts text into a cell, overwrites existing text if it already exists in the spreadsheet
	// for this reason, please have a record of old data in the subject's stats folder 
	public static void insert(XCell newCell, String text) {
		com.sun.star.text.XText xCellText = (com.sun.star.text.XText) UnoRuntime.queryInterface(com.sun.star.text.XText.class, newCell);
		com.sun.star.text.XTextCursor xTextCursor = xCellText.createTextCursor();
		if (!getCellValueString(newCell).isEmpty()) {
			xCellText.setString(text);
		} else {
			xCellText.insertString(xTextCursor, text, false);
		}	
	}
	
	// connects to the com.sun.star component context and service manager for 
	// interfacing with the necessary spreadsheets 
	public static XSpreadsheetDocument calcOpen(String path) {
        // connect to a running office and get the ServiceManager
        try {
            // get the remote office component context
            maContext = com.sun.star.comp.helper.Bootstrap.bootstrap();
            System.out.println("Connected to a running office ...");
            // get the remote office service manager	
            maMCFactory = maContext.getServiceManager();
            System.out.println("Connected to a service manager ...");
        }
        catch( Exception e) {
            System.out.println( "Couldn't get ServiceManager: " + e );
            e.printStackTrace();
            System.exit(1);
        }
        return getSpreadsheetDocument(path);
    }
	
	// Public method for retrieving requested spreadsheets
    public static XSpreadsheetDocument getSpreadsheetDocument(String path) {
        return (XSpreadsheetDocument) UnoRuntime.queryInterface(XSpreadsheetDocument.class, getDocument(path));
    }
    
    // private method that takes string path as argument and returns the requested spreadsheet
	private static XModel getDocument(String spreadsheetPath) {
        XModel aResult = null;
        try {
            XComponentLoader aLoader = (XComponentLoader) UnoRuntime.queryInterface(XComponentLoader.class, maMCFactory.createInstanceWithContext("com.sun.star.frame.Desktop", maContext));
            aResult = (XModel) UnoRuntime.queryInterface(XModel.class, aLoader.loadComponentFromURL( "file://"+spreadsheetPath,"_blank", 0, new PropertyValue[0]));
        } catch( Exception e ) {
            System.err.println("Oh no. Couldn't fetch document!"+e);
            e.printStackTrace();
            System.exit(0);
        }
        return aResult;
    }    
	
	// takes a spreadsheet as an argument along with the index of the required sheet, returns the sheet to the client
	public static XSpreadsheet getDataSheet(int index, XSpreadsheetDocument theSheets) throws RuntimeException {
        XIndexAccess aSheetIA = (XIndexAccess) UnoRuntime.queryInterface(XIndexAccess.class, theSheets.getSheets());
        XSpreadsheet aSheet = null;
        if ( aSheetIA != null ) {
            try {
                aSheet = (XSpreadsheet) UnoRuntime.queryInterface(XSpreadsheet.class, aSheetIA.getByIndex(index));
            } catch( Exception ex ){}
        }
        return aSheet;
    }
}
