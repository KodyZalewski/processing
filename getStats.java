import java.util.*;

import java.io.*;

import java.nio.file.Paths;

import java.nio.file.Path;

// Concatenates all of the data from multiple stats files into a file to be used for
// volumetric analysis, add the file produced from this script to the spreadsheet (labelled
// according to the study name in its respective folder)

public class getStats {
   
   public static int VERSION; // stores unique int for study version
   
   public static String LOCALPATH = "/home/ns-zalewk/Desktop/memorycube/"; // local path, change as needed
  
   public static String UNIQUE_DIR = "ADNI"; // if path is atypical, change to a list at some point
   
   private static String study; 
	
   private static String subject;
   
   public static void main(String[] args) throws FileNotFoundException, IOException {
   
      Scanner console = new Scanner(System.in);
         
      if (args.length > 0) {
         try {
            study = args[0];
            subject = args[1]; 
         } catch (IllegalArgumentException e) {
            System.out.println("Incorrect number of command line arguments. Study should be first argument, subject should be second.");
            System.exit(1);
         }
      } else { 
         System.out.println("Please input the subject # you would like to process: ");
         subject = console.nextLine();
         
         System.out.println("Please input the study name (e.g. EVOLVE or LRMC etc.) of the subject: ");
         study = console.nextLine();      
      }
      
      VERSION = versionControl(study, subject);
      
      String pathName = thisPath(study, subject, console);
      
      File volFile = new File (pathName + subject + "_stats.txt");
      checkFile(volFile, subject, pathName); 
      PrintStream totalFile = new PrintStream(volFile);
      
      File thicknessFile = new File (pathName + subject + "_thickness.txt");
      checkFile(thicknessFile, subject, pathName);
      PrintStream thickFile = new PrintStream(thicknessFile);      
      
      totalFile.print(subject + " ");
             
      asegStats(pathName, totalFile);
      
      parc(pathName, "lh.aparc.stats", totalFile, thickFile, 87, 52);
      
      parc(pathName, "rh.aparc.stats", totalFile, thickFile, 87, 52);
      
      parc(pathName, "wmparc.stats", totalFile, thickFile, 133, 62);
      
      System.out.println();
      System.out.println("Processing complete!");
      System.out.println();
   }
   
   // takes study name, subject name, and scanner for reading input as arguments
   // outputs the name of the path for this subject as a string
   public static String thisPath(String study, String subject, Scanner console) {
      String pathName; 
      if (study.equals(UNIQUE_DIR)) {
         System.out.println("Big ventricles or not?(Y/n) ");
         String bigvents = console.nextLine();
         if (bigvents.equals("Y")) {
            return pathName = LOCALPATH + study + "/" + subject + "/"  + subject + "_BV" + "/stats/";
         } else {  
            return pathName = LOCALPATH + study + "/" + subject + "/" + subject + "_F6" + "/stats/";
         }    
      } else {
         return pathName = LOCALPATH + study + "/" + subject + "/" + subject + "_FREESURFER" + "/stats/";
      }
   }
   
   // takes a string denoting version number as an argument, assigns a 1 or 0 to
   // the VERSION global variable depending on which version of freesurfer is 
   // specified by the client
   public static int versionControl(String study, String subject) throws IOException {
      Scanner buildFile = new Scanner(Paths.get(LOCALPATH + study + "/" + subject + "/" + subject + "_FREESURFER" + "/scripts/build-stamp.txt"));
      String buildStamp = buildFile.nextLine();
      System.out.println();
      if (buildStamp.contains("v6.0")) {
         System.out.println("This subject was processed with freesurfer version 6.0");
         System.out.println();    
         return 1;
      } else if (buildStamp.contains("v5.3")) {
         System.out.println("This subject was processed with freesurfer version 5.3");
         System.out.println(); 
         return 0;
      } else {
         throw new IllegalArgumentException("Version number provided is not recognized or build stamp is not present in folder: subject_ID/scripts.");
      }
   } 
   
   // Pulls data from the aseg.stats file, includes string 'NULL' as placeholder for non-existent values
   // takes the path name and PrintStream to write out data to as arguments, throws IOException if file
   // is not found. 
   public static void asegStats(String pathName, PrintStream totalFile) throws IOException {
      System.out.println("Writing out aseg.stats...");
      Scanner statsFile = new Scanner(Paths.get(pathName + "aseg.stats"));
      for (int i = 0; i < 124; i++) {
         String line = statsFile.nextLine();
         if (VERSION == 1 && i == 16) { // version 6.0 local atlas has a line of choroid plexus data omitted here, thor does not have this
            line = statsFile.nextLine();
         } 
         if (28 > i && i > 12) {
            Scanner lineRead  = new Scanner(line).useDelimiter(",");
            String name = lineRead.next();
            String abbreviation = lineRead.next();
            String abbreviation2 = lineRead.next();    
            String volume = lineRead.next();
            totalFile.print(volume + " ");
         } else if (i == 33) {
            Scanner lineRead  = new Scanner(line).useDelimiter(",");
            String name = lineRead.next();
            String abbreviation = lineRead.next();
            String abbreviation2 = lineRead.next();     
            String volume = lineRead.next();
            totalFile.print(volume);
            totalFile.print(" NULL ");   
         } else if (124 > i && i > 78) {
            Scanner lineRead  = new Scanner(line);
            printLines(totalFile, lineRead);
            if (i == 96) {
               totalFile.print(" NULL "); 
            }
         }
      }   
   }
   
   // Pulls data from the ?hparc.stats file and writes it out to file. Takes path as string, file name
   // as string, and PrintStreams to output the volumetric and thickness data. Takes two ints denoting
   // lines in the *parc.stats files. Throws IOException if file is not found.
   public static void parc(String pathName, String fileName, PrintStream totalFile, PrintStream thickFile, int lineNumOne, int lineNumTwo) throws IOException {
      System.out.println("Writing out " + fileName + "..."); 
      Scanner statsFile = new Scanner(Paths.get(pathName + fileName));
      for (int i = 0; i < lineNumOne; i++) {
         String line = statsFile.nextLine();
         if (VERSION == 1 && i == 13) { // version 6.0 local atlas has 7 extra lines of hemispheric data here, thor does not have this
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
                     printLines(totalFile, lineRead);
                  }
               }
            } else {
               if (lineRead.hasNext()) {
                  if (i == 53) {
                     // no value for this segmentation, w/ --rip-unknown flag, outputs NULL
                     totalFile.print(" NULL "); 
                  } 
                  printLinesInt(totalFile, lineRead, thickFile);
               }
            }
         }
      } 
   }
   
   // reads double values from stats file, 
   // takes volumetric file name and scanner as arguments 
   public static void printLines(PrintStream totalFile, Scanner lineRead) {
      double throwaway1 = lineRead.nextDouble(); 
      double throwaway2 = lineRead.nextDouble();
      double throwaway3 = lineRead.nextDouble();
      double grayVol = lineRead.nextDouble();
      totalFile.print(grayVol + " ");
   }
   
   // reads int values from stats file and prints them to a file, 
   // takes volumetric file name, scanner to read lines, and 
   // file to store thickness values as arguments 
   public static void printLinesInt(PrintStream totalFile, Scanner lineRead, PrintStream thickFile) {
      String name = lineRead.next();
      int numVert = lineRead.nextInt();
      int surfArea = lineRead.nextInt();
      int grayVol = lineRead.nextInt();
      double thicknessAvg = lineRead.nextDouble();
      double thicknessStd = lineRead.nextDouble();     
      totalFile.print(grayVol + " ");
      thickFile.print(thicknessAvg + " " + thicknessStd + " ");
   }
   
   // Reads in a printstream and checks if it exists, if there is currently a file
   // present, it is renamed in the same directory and the program continues
   public static void checkFile(File file, String subject, String pathName) {
      if(file.exists() && !file.isDirectory()) {
         System.out.println();
         System.out.println("File currently exists! Renaming.");
         System.out.println();
         File file2 = new File(pathName + subject + "_old_stats_file");
         file.renameTo(file2);
//         boolean move = file.renameTo(file2);
         // if (!move) {
//              System.out.println();
//              System.out.println("Stats file is present, but was not moved.");
//              System.out.println();
//              throw new IllegalArgumentException(); 
//          }
      } else {
         System.out.println(file + " does not currently exist for subject " + subject + ".");
         System.out.println(); 
      }
   }        
}