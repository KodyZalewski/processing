import java.util.*;

import java.io.*;

import java.nio.file.Paths;

//import java.nio.file.Path;

// Concatenates all of the data from multiple stats files into a file to be used for
// volumetric analysis, add the file produced from this script to the spreadsheet (labeled
// according to the study name in its respective folder)

public class getStats {
   
   public static int VERSION; // stores unique int for study version
   public static String LOCALPATH = "/home/ns-zalewk/Desktop/memorycube/"; // local path, change as needed
   public static String UNIQUE_DIR = ""; // if path is atypical, change to a list at some point
   
   private static String study; 
   private static String subject;
   private static List<String> subjectList = new ArrayList<String>();
   
   public static void main(String[] args) throws FileNotFoundException, IOException {
   
      Scanner console = new Scanner(System.in);
         
      if (args.length > 0) {
    	  
         try {
            study = args[0];
            getArgs(args); 
         } catch (IllegalArgumentException e) {
            System.out.println("Incorrect number of command line arguments. Study should be first argument, subject should be second.");
            System.exit(1);
         }
         
      } else {
    	  
         System.out.println("Please input the study name (e.g. EVOLVE or LRMC etc.) of the subject: ");
         study = console.nextLine();
         
         System.out.println("Please input the subject # you would like to process: ");
         subject = console.nextLine();
         subjectList.add(subject);
         
      }
      
      if (subjectList.size() > 0) {
    	  
	      for (String subject : subjectList) {
	    	  
		      VERSION = versionControl(study, subject);
		      
		      String pathName = LOCALPATH + study + "/" + subject + "/" + subject + "_FREESURFER" + "/stats/";
		      
		      File volFile = new File (pathName + subject + "_stats.txt");
		      checkFile(volFile, subject, pathName, 1);
		      PrintStream totalFile = new PrintStream(volFile);
		      
		      File thicknessFile = new File (pathName + subject + "_thickness.txt");
		      checkFile(thicknessFile, subject, pathName, 2);
		      PrintStream thickFile = new PrintStream(thicknessFile);      
		      
		      totalFile.print(subject + " ");
		             
		      asegStats(pathName, totalFile);
		      
		      parc(pathName, "lh.aparc.stats", totalFile, thickFile, 87, 52);
		      
		      parc(pathName, "rh.aparc.stats", totalFile, thickFile, 87, 52);
		      
		      parc(pathName, "wmparc.stats", totalFile, thickFile, 133, 62);
		      
		      subfields(pathName, "hippocampal_lh.stats", totalFile);
		      
		      subfields(pathName, "hippocampal_rh.stats", totalFile); 
		      
		      subfields(pathName, "brainstem.stats", totalFile);
		      
		      System.out.println("\n" + "Processing complete for subject " + subject + "!" + "\n");
		      
	      }
	      
      } else {
    	  
    	  System.out.println("\n" + "No subjects specified... quitting stats retrieval." + "\n");
      }
      console.close();
   }
   
   // cycles through the arguments for subject value extraction and adds any client-specified values to the subject list
   // can currently change the local path or add a successive subjects to a subject list
   public static void getArgs(String args[]) {
	   int counter = 1;
	   if (args.length > 1) {
		   for (String x : args) {
				if (x.equals("-s")) { // this means the following strings are subject numbers
					counter++;
					for (int i = counter; i < args.length; i++) {
						if (args[i].equals("-p")) { // or any other future arguments
							break;
						}
						System.out.println("Pulling volumetric values for subject " + args[i]);
						subjectList.add(args[i]);
					}
				}
				if (x.equals("-p")) { // changes LOCALPATH path variable
					// change this at some point to something more efficient
					for (int i = counter; i < args.length; i++) {
						if (args[i].equals("-p")) {
							LOCALPATH = args[i+1];
						}
					}
				}
				counter = 1; // reset counter
		   }
	   } else {
		   System.out.println("Only one argument. For a single subject, don't specify any arguments.");
	   }
   }
   
   // takes a string denoting version number as an argument, assigns a 1 or 0 to
   // the VERSION global variable depending on which version of Freesurfer is specified by the client
   public static int versionControl(String study, String subject) throws IOException {
      Scanner buildFile = new Scanner(Paths.get(LOCALPATH + study + "/" + subject + "/" + subject + "_FREESURFER" + "/scripts/build-stamp.txt"));
      String buildStamp = buildFile.nextLine();
      buildFile.close();
      if (buildStamp.contains("v6.0")) {
         System.out.println("\n" + "This subject was processed with freesurfer version 6.0" + "\n"); 
         return 1;
      } else if (buildStamp.contains("v5.3")) {
         System.out.println("\n" + "This subject was processed with freesurfer version 5.3" + "\n");
         return 0;
      } else {
         throw new IllegalArgumentException("Version number provided is not recognized or build stamp is not present in folder: subject_ID/scripts.");
      }
   } 
   
   // Reads in a printstream and checks if it exists, if there is currently a file
   // present, it is renamed in the same directory and the program continues. The fileNumber
   // variable is for whether or not the file is the volume/stats file (1), or the thickness file (2)
   public static void checkFile(File file, String subject, String pathName, int fileNumber) {
      if (file.exists() && !file.isDirectory()) {
    	 String fileSuffix = ""; 
    	 if (fileNumber == 1) {
    		 fileSuffix = "_stats.txt";
    	 } else if (fileNumber == 2) {
    		 fileSuffix = "_thickness.txt";
    	 } else {
    		 System.out.println("Unknown file type submitted.");
    	 }
    	 int counter = 1;
    	 File thisFile = new File (pathName + subject + fileSuffix + "_" + counter);
    	 while (thisFile.exists()) {
	         counter++;
	         thisFile = new File (pathName + subject + fileSuffix + "_" + counter);
    	 }
    	 System.out.println(counter + " file(s) currently exist!");
         boolean move = file.renameTo(thisFile);
         if (!move) {
              System.out.println("\n" + "Stats file is present, but was not moved." + "\n");
              throw new IllegalArgumentException(); 
         }
      } else {
         System.out.println(file + " does not currently exist for subject " + subject + "." + "\n");
      }
   }
   
   // Pulls data from the aseg.stats file, includes string 'NULL' as placeholder for non-existent values
   // takes the path name and PrintStream to write out data to as arguments, throws IOException if file
   // is not found. 
   public static void asegStats(String pathName, PrintStream totalFile) throws IOException {
      System.out.println("Writing out aseg.stats...");
      Scanner statsFile = new Scanner(Paths.get(pathName + "aseg.stats"));
      int i = 0;
      while (statsFile.hasNextLine()) {
         String line = statsFile.nextLine();
         if (VERSION == 1 && i == 16) { // version 6.0 line of choroid plexus data omitted here, not in 5.3
            line = statsFile.nextLine();
         } 
         if (28 > i && i > 12) {
            printAsegLines(line, totalFile); 
            //totalFile.print(" ");
         } else if (i == 33) {
        	printAsegLines(line, totalFile);
	        if (!study.equals("EVOLVE10")) { // for the hemispheric cerebral white matter
	            totalFile.print(" NULL ");   
	        } else {
	        	totalFile.print(" ");
	        }
         } else if (124 > i && i > 78) {
            Scanner lineRead  = new Scanner(line);
            if (i == 96 && !study.equals("EVOLVE10")) {
               printLines(totalFile, lineRead, i);
               totalFile.print(" NULL "); 
            } else {
            	printLines(totalFile, lineRead, i);
            }
            lineRead.close();
         }
         i++;
      }
      statsFile.close();
   }
   
   // Pulls data from the ?hparc.stats file and writes it out to file. Takes path as string, file name
   // as string, and PrintStreams to output the volumetric and thickness data. Takes two integers denoting
   // lines in the *parc.stats files. Throws IOException if file is not found.
   public static void parc(String pathName, String fileName, PrintStream totalFile, PrintStream thickFile, int lineNumOne, int lineNumTwo) throws IOException {
      System.out.println("Writing out " + fileName + "..."); 
      Scanner statsFile = new Scanner(Paths.get(pathName + fileName));
      for (int i = 0; i < lineNumOne; i++) {
         String line = statsFile.nextLine();
         if (VERSION == 1 && i == 13) { // version 6.0 local atlas has 7 extra lines of hemispheric data here, 5.3 does not have this
            if (fileName.equals("wmparc.stats")) {
               line = statsFile.nextLine();
            } else { 
               for (int j = 0; j < 7; j++) {
                  line = statsFile.nextLine();
               }
            }
         } 
         Scanner lineRead = new Scanner(line);
         if (i > lineNumTwo) {
            if (fileName.equals("wmparc.stats")) {
               if (i > 62) {
                  if (lineRead.hasNextDouble()) {
                     printLines(totalFile, lineRead, i);
                  }
               }
            } else {
               if (lineRead.hasNext()) {
                  if (i == 53) {       
                     totalFile.print(" NULL "); // no value for "unknown" segmentation, w/ --rip-unknown flag, outputs NULL
                  } 
                  printLinesInt(totalFile, lineRead, thickFile);
               }
            }
         }
      }
      statsFile.close();
   }
   
   // reads values from subfields and prints them to file. Checks if the subfield and brainstem files exists
   // for studies where brainstem might not exist or where subfields might be too noisy, there will be no values here
   // otherwise, prints the final subfield/brainstem values to the stats file
   public static void subfields(String pathName, String fileName, PrintStream totalFile) throws IOException {
	   System.out.println("Writing out " + fileName + "...");   
	   File checkFile = new File(pathName + fileName);
	   if (!checkFile.exists()) {
		   System.out.println("An error has occured, or this file isn't available. File: " + fileName + " doesn't exist.");
		   return;
	   } 
	   Scanner statsFile = new Scanner(Paths.get(pathName + fileName));
	   String line = statsFile.nextLine();
	   int counter = 0;
	   if (fileName.equals("hippocampal_lh.stats") || fileName.equals("hippocampal_rh.stats") || fileName.equals("brainstem.stats")) {
		  while (statsFile.hasNextLine()) {
			  line = statsFile.nextLine();
				   counter++;
				   if (counter > 55 && counter < 69) {
					   Scanner lineRead = new Scanner(line);
					   printLines(totalFile, lineRead, counter);
				   }
			}     
	   } else {
			System.out.println("File is unrecognized.");
	   }
	   statsFile.close();
   }
	   
   // reads double values from stats file, takes volumetric file name and scanner as arguments 
   public static void printLines(PrintStream totalFile, Scanner lineRead, int i) {
      double throwaway1 = lineRead.nextDouble(); double throwaway2 = lineRead.nextDouble(); double throwaway3 = lineRead.nextDouble(); 
      double grayVol = lineRead.nextDouble();
      totalFile.print(grayVol + " ");
   }
   
   // uses a comma as a delimiter to process lines as strings, throws away unneeded values and
   // prints volumes to a new file
   public static void printAsegLines(String line, PrintStream totalFile) {
	   Scanner lineRead  = new Scanner(line).useDelimiter(",");
       String name = lineRead.next(); String abbreviation = lineRead.next(); String abbreviation2 = lineRead.next(); String volume = lineRead.next();        
       totalFile.print(volume);
   }
   
   // reads int values from stats file and prints them to a file, takes volumetric file name, scanner to read lines, and
   // file to store thickness values as arguments 
   public static void printLinesInt(PrintStream totalFile, Scanner lineRead, PrintStream thickFile) {
      String name = lineRead.next(); int numVert = lineRead.nextInt(); int surfArea = lineRead.nextInt();
      int grayVol = lineRead.nextInt();
      double thicknessAvg = lineRead.nextDouble(); double thicknessStd = lineRead.nextDouble();       
      totalFile.print(grayVol + " ");
      thickFile.print(thicknessAvg + " " + thicknessStd + " ");
   }
   
   // takes study name, subject name, and scanner for reading input as arguments
   // outputs the name of the path for this subject as a string
   //public static String thisPath(String study, String subject, Scanner console) {
      //String pathName; 
      /*if (study.equals(UNIQUE_DIR)) {
         System.out.println("Big ventricles or not?(Y/n) ");
         String bigvents = console.nextLine();
         if (bigvents.equals("Y")) {
            return pathName = LOCALPATH + study + "/" + subject + "/"  + subject + "_BV" + "/stats/";
         } else {  
            return pathName = LOCALPATH + study + "/" + subject + "/" + subject + "_F6" + "/stats/";
         }    
      } else { */
      //   return LOCALPATH + study + "/" + subject + "/" + subject + "_FREESURFER" + "/stats/";
      //}
   //}
}
