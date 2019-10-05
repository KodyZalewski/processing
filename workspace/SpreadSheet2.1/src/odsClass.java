import com.sun.star.beans.PropertyValue;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.container.XIndexAccess;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XModel;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.XCell;
import com.sun.star.uno.RuntimeException;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

public class odsClass {
	
		// takes a spreadsheet as an argument along with the index of the required sheet, returns the sheet to the client
		public static XSpreadsheet getDataSheet(int index, XSpreadsheetDocument theSheets) throws RuntimeException {
			XIndexAccess aSheetIA = (XIndexAccess) UnoRuntime.queryInterface(XIndexAccess.class, theSheets.getSheets());
			XSpreadsheet aSheet = null;
			if ( aSheetIA != null ) { try {
				aSheet = (XSpreadsheet) UnoRuntime.queryInterface(XSpreadsheet.class, aSheetIA.getByIndex(index)); } catch( Exception ex ){}
			}
			return aSheet;
		}
			
			// private method that takes string path as argument and returns the requested spreadsheet
		public static XModel getDocument(String spreadsheetPath, XMultiComponentFactory maMCFactory, XComponentContext maContext) {
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
		// retrieves text from a cell and returns it as string
		public static String getCellValueString(XCell newCell) {
			com.sun.star.text.XText xCellText = (com.sun.star.text.XText) UnoRuntime.queryInterface(com.sun.star.text.XText.class, newCell);
			return xCellText.getString();
		}

		// Public method for retrieving requested spreadsheets
		public static XSpreadsheetDocument getSpreadsheetDocument(String path, XMultiComponentFactory maMCFactory, XComponentContext maContext) {
			
			return (XSpreadsheetDocument) UnoRuntime.queryInterface(XSpreadsheetDocument.class, getDocument(path, maMCFactory, maContext));
		}

		public static XSpreadsheetDocument calcOpen(String path, XMultiComponentFactory maMCFactory, XComponentContext maContext) {	
			try { //maContext = calcRead();
			maMCFactory = calcRead().getServiceManager(); // get the remote office service manager
			System.out.println("Connected to a service manager ...");
			} catch(Exception e) {
				System.out.println( "Couldn't get ServiceManager: " + e );
				e.printStackTrace();
				System.exit(1);
			}
			return getSpreadsheetDocument(path, maMCFactory, maContext);
		}
		
		// connect to a running office and get the ServiceManager, get the remote office component context
		public static XComponentContext calcRead() throws BootstrapException {
			System.out.println("Connecting to a running office ...");
			return com.sun.star.comp.helper.Bootstrap.bootstrap();
		}
}
