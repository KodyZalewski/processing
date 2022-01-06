

import java.awt.*;
import java.awt.event.*;
import javax.swing.JFileChooser;

//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;

/**
 * User interface with 
 * @author kody
 *
 */

public class ExvivoUI extends Frame {
	
	private static final long serialVersionUID = 1L;
	private static Label PATH;
	public static int SMOOTH,EROSION;
	public static int EROSION2;
	public static int THRESHOLD_BOUND;
	public static int THRESHOLD_DEPTH;
	public static int CLEAN;
	public static int PATCH; 

	public ExvivoUI() {
		
		this.setSize(450,350);
		setWindow(this, false, true);
		
		setTitle("Exvivo pre-processing parameters");
		setLayout(new FlowLayout(FlowLayout.LEFT,30,10));
		setVisible(true);
		
		// set buttons 
		Button browse = new Button("Browse:");
		TextField t = new TextField("Input Path:");
		add(browse);  browse.setLocation(50, 100); add(t);
		
		// erosion button properties
		Checkbox ersnBox = new Checkbox("Turn on erosion");
		Button ersn = new Button("Set erosion values");
		add(ersnBox); add(ersn); //ersn.setLocation(50, 125); ersnBox.setLocation(75, 125);
		itemVal(ersnBox, ersn);
		
		// smoothing button properties
		Checkbox smthBox = new Checkbox("Turn on smoothing");
		Button smth = new Button("Set smoothing values");
		add(smthBox); add(smth); //smth.setLocation(50, 200); smthBox.setLocation(75, 200);
		itemVal(smthBox, smth);
		
		// thresholding button properties
		Checkbox thrsBox = new Checkbox("Turn on thresholding");
		Button thrs = new Button("Set thresholding values");
		add(thrsBox); add(thrs); //thrs.setLocation(50,275);  thrsBox.setLocation(75,275);
		itemVal(thrsBox, thrs);
		
		// clean-up button properties
		Checkbox clnBox = new Checkbox("Turn on simple clean-up");
		Checkbox clnBox2 = new Checkbox("Turn on clean-up");
		Button cln = new Button("Change clean-up");
		add(clnBox); add(clnBox2); add(cln);  //cln.setLocation(50,350);   clnBox.setLocation(75,350);
		itemVal(clnBox2, cln);
		
		// patch-up button properties
		Checkbox ptchBox = new Checkbox("Turn on patching");
		Button ptch = new Button("Change patching");
		add(ptchBox); add(ptch); //ptch.setLocation(50, 425); ptchBox.setLocation(75, 425);
		itemVal(ptchBox, ptch);
		
		// run button
		Button run = new Button("Run");
		add(run);
		
		// set button properties for each one
		browse.addActionListener(new ActionListener() {	
			public void actionPerformed(ActionEvent e) {
				
				// automatically generates window
				JFileChooser files = new JFileChooser(); 
				files.setFileSelectionMode(JFileChooser.FILES_ONLY);
				files.setAcceptAllFileFilterUsed(false);
				if (files.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
					PATH.setText(files.getSelectedFile().toString());
				}
			}
		});
		
		ersn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				Frame f = setWindow(new Frame(), true, true);
				TextField fChooser = new TextField("Default");
				fChooser.setName("Average ratio");
				TextField fChooser2 = new TextField("10");
				fChooser2.setName("Boundary length");
				f.add(fChooser); f.add(fChooser2);
				
				EROSION = returnVal(fChooser);
				EROSION2 = returnVal(fChooser2);
			}
		});
		
		smth.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Frame f = setWindow(new Frame(), true, true);
				TextField t = new TextField("Input smoothing distance");
				f.add(t);
				
				SMOOTH = returnVal(t);
			}
		});
		
		thrs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Frame f = setWindow(new Frame(), true, true);
				
				TextField t = new TextField("Set Boundary");
				TextField t2 = new TextField("Set depth");
				f.add(t); f.add(t2);
				
				THRESHOLD_BOUND = returnVal(t);
				THRESHOLD_DEPTH = returnVal(t2);
			}
		});
		
		cln.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			
				Frame f = setWindow(new Frame(), true, true);
				f.add(t);
				
				//CLEAN = 
			}
		});
		
		ptch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Frame f = setWindow(new Frame(), true, true);
				f.add(t);
				
				//PATH = 
			}
		});
		
		run.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				Frame f = setWindow(new Frame(), true, true);
				f.add(t);
				
			}
		});
		
	}
	
	
	// return text field value
	public static int returnVal(TextField t) {
		try {
			return Integer.parseInt(t.getText().toString());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
	
	// pulls current path from ProcessBuilder
	public void fetchPath(ActionEvent ae) {
		ProcessBuilder pb = new ProcessBuilder("pwd");
		pb.inheritIO();
	}
	
	// sets button properties based on checkbox
	public void itemVal(Checkbox c, Button b) {
		
		c.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent event) {
				if (c.getState()) {
					b.setEnabled(true);
				} else {
					b.setEnabled(false);
				}
			}
		});	
	}
	
	// default window settings for frame object
	public Frame setWindow(Frame f, boolean set, boolean set2) {
		
		if (set) { // set parameters
			f.setSize(300,100); f.setVisible(true); 
			f.setLayout(new FlowLayout(FlowLayout.LEFT));
		}
		if (set2) { // allows window to be closed
			f.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent we) {
					f.dispose();
				}
			});
		}
		return f;
	}
	
	// checks if the values is null
	public static boolean checkNull(int s, int e, int e2, int tb, int td, int c, int p) {
		if (s == 0 || e == 0 || e2 == 0 || tb == 0 || td == 0 || c == 0 || p == 0) {
			return false;
		} else {
			return true;
		}
	}
	
	
	public static void readNifti () {
		Nifti1Dataset inputNifti = new Nifti1Dataset(PATH.toString());
		// default values from reading nifti
		/**
		CLEAN = ; EROSION = ; EROSION2 = ; THRESHOLD_BOUND = ; THRESHOLD_DEPTH = 
		
		*/
	}
	
	public static void main(String[] args) {
		
		ExvivoUI ex = new ExvivoUI();
		//if (checkNull(SMOOTH, EROSION, EROSION2, THRESHOLD_BOUND, THRESHOLD_DEPTH, CLEAN, PATCH)) {
		//	TrimScan.runFunctions(EROSION, SMOOTH, EROSION, THRESHOLD_DEPTH, PATCH, CLEAN, 1);
		//}
	}
		
}

