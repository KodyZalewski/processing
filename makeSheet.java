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
    private static int rowCountFinal; 
	
	private static final String LOCALPATH = "/home/ns-zalewk/Desktop/memorycube/";	
	
	private static String study; 
	
	private static String subject; 
	
	public static void main(String args[]) throws IOException, URISyntaxException, IndexOutOfBoundsException, BootstrapException {
		
        rowCountFinal = 0;
        
		String spreadsheetPath = LOCALPATH + "/00_DATA/" + "FS_" + study + "_volumetricdata.ods"; 
		String qaSheetPath = LOCALPATH + "/00_LOGS/" + "FS_" + study + "_processingspreadsheet.ods";
		
		if (args.length > 0) {
			try {
	            study = args[0];
	            subject = args[1]; 
	         } catch (IllegalArgumentException e) {
	            System.out.println("Incorrect number of command line arguments. Study should be first argument, subject should be second.");
	            System.exit(1);
	         }
		}
		
		if (study == null || subject == null) {
			System.out.print("ERROR: Function must have both a study and subject.");
			System.out.print("In the format: makeSheet <study> <subjectID>");
			System.exit(1);
		}
				
		volSpreadSheetDoc = calcOpen(spreadsheetPath);
		qaSpreadSheetDoc = calcOpen(qaSheetPath);
		
		insertToSheet(0,volSpreadSheetDoc, subject, study, "_stats", 0);
		insertToSheet(1,volSpreadSheetDoc, subject, study, "_thickness", 1);
		insertToSheet(1,qaSpreadSheetDoc, subject, study, "final_CNR", 1);

		//System.out.println( "There are: " + rowCountFinal + " subjects in "+ study + "'s spreadsheet.");
		System.out.println( subject + " has been added to the spreadsheet successfully!" );
		System.exit(0);
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
			Scanner subjectFile 
				= new Scanner(Paths.get(new URI("file://"+path)));
			readIn(column, 1, subjectFile, sheet, subject, fileName);
			subjectFile.close();
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
				if (fileName.equals("_stats")) {
					rowCountFinal++;
				}
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
