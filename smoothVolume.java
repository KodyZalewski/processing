import java.io.IOException;

public class smoothVolume {
	
	static double data[][][];
	static double data1[][][];
	public static String LOCALPATH = "/home/ns-zalewk/Desktop/memorycube/EXVIVO/TX1275/TX1275_FREESURFER/mri/orig/";
	public static String OUTPUT_PATH = "/home/ns-zalewk/Desktop/memorycube/EXVIVO/TX1275/TX1275_FREESURFER/mri/orig/";
	static boolean boundaries[][][];
	
	// nifti can't be in double format, this sucks! Change writeVolBlob someday
	
	public static void main(String[] args) throws IOException {
			
			Nifti1Dataset nds = new Nifti1Dataset(LOCALPATH + args[0]);
			nds.readHeader();
			
			// set boolean boundaries
			boundaries = new boolean[nds.ZDIM][nds.YDIM][nds.XDIM];
			for (int i = 0; i < boundaries.length; i++) {
				for (int j = 0; j < boundaries[i].length; j++) {
					for (int k = 0; k < boundaries[i][j].length; k++) {
						boundaries[i][j][k] = false;
					}
				}
			}
			
			//String[] newArgs = new String[3]; newArgs[0] = "copy"; newArgs[1] = "flipped.nii"; newArgs[2] = "smoothed.nii";
			//TestNifti1Api.main(newArgs);
			
			Nifti1Dataset smoothNifti = new Nifti1Dataset(LOCALPATH + args[1]);
			smoothNifti.readHeader();
			
			data = nds.readDoubleVol((short) 0);
			//smoothNifti.writeVol(data,(short) 0);
			
			// proof that smoothing works
			//double[][][] smoothed = new double[nds.XDIM][nds.YDIM][nds.ZDIM];
			//smoothed = readNifti.calcAvgThreeDim(data, 1, nds.XDIM, nds.YDIM, nds.ZDIM);
			//smoothNifti.writeVol(smoothed,(short)0);
				
			// testing below here
			
			//smoothNifti.writeVol(erode(nds.readDoubleVol((short) 0), 10, 30, true, true, true, true, true),(short)0);
			smoothNifti.writeVol(cleanUp(smoothNifti.readDoubleVol((short)0), 1, true, true, true), (short) 0);
			//smoothNifti.writeVol(thresholdStandardDev.findGradient(data, true, true, true, 1.75, 2, 9, true, true), (short)0);
			//smoothNifti.writeVol(cleanUp(smoothNifti.readDoubleVol((short)0), 1, true, true, true), (short) 0);
			//smoothNifti.writeVol(erode(nds.readDoubleVol((short) 0), 10, 18, true, true, true, true, true),(short)0);
			//smoothNifti.writeVol(thresholdStandardDev.findGradient(data, false, true, false, 1.0, 4, true, true), (short)0);
			smoothNifti.writeVol(cleanUp(smoothNifti.readDoubleVol((short)0), 1, true, true, true), (short) 0);
			
			//findLinearEq.findLinearEquations(data, 3, true, true, true);
			//data1 = smoothNifti.readDoubleVol((short) 0);	
			
			//smoothNifti.writeVol(writeLines(nds.readDoubleVol((short) 0), 50), (short)0);
			
			//findAvg(smoothNifti.readDoubleVol((short) 0), 7);
			
	}
	
	//works correctly, shows that we can write data along one dimension
	public static double[][][] writeLines(double[][][] data1, int bound) {
		
		boolean cont = false; 
		
		for (int i = 0; i < data1.length; i++) {
			for (int j = 0; j < data1[i].length; j++) {
				cont = false;
				for (int k = 0; k < data1[i][j].length/2; k++) {
					if (data1[i][j][k] > bound) {
						cont = true;
					} else if (cont == false) {
						data1[i][j][k] = (double) 20;
					}
					/** if (data1[i][j][k] > (double)3 && cont == false) {
						lineVal = data1[i][j][k];
						cont = true;
					} else if (cont == true) {
						data1[i][j][k] = lineVal;
					}**/
				}
			}
		}
		return data1;
	}
	
	public static double[][] findAvg(double[][][] data1, int findGradient) {
		
		double[][] storeAvg = new double[data1.length][data1[0].length];
		
		for (int i = 0; i < data1.length; i++) {
			for (int j = 0; j < data1[i].length; j++) {
				for (int k = 0; k < data1[i][j].length; k++) {
					if (data1[i][j][k] > (double) 5) {
						for (int l = 0; l < findGradient; l++) {
							storeAvg[i][j] += data1[i][j][k + l];
						}
						break;
					}	
				}
				storeAvg[i][j] = storeAvg[i][j]/(double)findGradient;
			}
		}
		readNifti.calcAvgOneDim(storeAvg, storeAvg.length, storeAvg[0].length);
		return storeAvg;
	}
	
	public static double[][][] erode(double[][][] data, int erode, double lowBound, 
			boolean xdim, boolean ydim, boolean zdim, boolean firstHalf, boolean secondHalf) {
	
		// x-dimension
		if (xdim == true) {	
			for (int i = 0; i < data.length; i++) {
				for (int j = 0; j < data[i].length; j++) {
					if (firstHalf == true) {
						for (int k = 0; k < data[i][j].length/2; k++) {
							if (data[i][j][k] != 0 && boundaries[i][j][k] == false) {
								for (int l = 0; l < erode; l++) {
									if (data[i][j][k+l] > lowBound && boundaries[i][j][k+l] == false) {
										data[i][j][k+l] = (double) 0;
									} else if (data[i][j][k+l] < lowBound && data[i][j][k+l] != (double) 0) {
										boundaries[i][j][k+l] = true;
										break;
									}
								}
								break;
							}
						}
					}
					if (secondHalf == true) {
						for (int k = data[i][j].length - 1; k > data[i][j].length/2; k--) {
							if (data[i][j][k] != 0 && boundaries[i][j][k] == false) {
								for (int l = 0; l < erode; l++) {
									if (data[i][j][k-l] > lowBound && boundaries[i][j][k-l] == false) {
										data[i][j][k-l] = (double) 0;
									} else if (data[i][j][k-l] < lowBound && data[i][j][k-l] != (double) 0) {
										boundaries[i][j][k-l] = true;
										break;
									}
								}
								break;
							}
						}
					}
				}
			}
		}

		// y-dimension
		if (ydim == true) {
			for (int i = 0; i < data.length; i++) {
				for (int k = 0; k < data[i][0].length; k++) {
					if (firstHalf == true) {
						for (int j = 0; j < data[i].length/2; j++) {
							if (data[i][j][k] != 0 && boundaries[i][j][k] == false) {
								for (int l = 0; l < erode; l++) {
									if (data[i][j+l][k] > lowBound && boundaries[i][j+l][k] == false) {
										data[i][j+l][k] = (double) 0;
									} else if (data[i][j+l][k] < lowBound && data[i][j+l][k] != (double) 0) {
										boundaries[i][j+l][k] = true;
										break;
									}
								}
								break;
							}
						}
					}
					if (secondHalf == true) {
						for (int j = data[i].length - 1; j > data[i].length/2; j--) {
							if (data[i][j][k] != 0 && boundaries[i][j][k] == false) {
								for (int l = 0; l < erode; l++) {
									if (data[i][j-l][k] > lowBound && boundaries[i][j-l][k] == false) {
										data[i][j-l][k] = (double) 0;
									} else if (data[i][j-l][k] < lowBound && data[i][j-l][k] != (double) 0) {
										boundaries[i][j-l][k] = true;
										break;
									}
								}
								break;
							}
						}
					}
				}
			}
		}

		// z-dimension
		if (zdim == true) {
			for (int k = 0; k < data[0][0].length; k++) {
				for (int j = 0; j < data[k].length; j++) {
					if (firstHalf == true) {
						for (int i = 0; i < data.length/2; i++) {
							if (data[i][j][k] != 0 && boundaries[i][j][k] == false) {
								for (int l = 0; l < erode; l++) {
									if (data[i+l][j][k] > lowBound && boundaries[i+l][j][k] == false) {
										data[i+l][j][k] = (double) 0;
									} else if (data[i+l][j][k] < lowBound && data[i+l][j][k] != (double) 0) {
										boundaries[i+l][j][k] = true;
										break;
									}		
								}
								break;
							}
						}
					}
					if (secondHalf == true) {
						for (int i = data.length - 1; i > data.length/2; i--) {
							if (data[i][j][k] != 0 && boundaries[i][j][k] == false) {
								for (int l = 0; l < erode; l++) {
									if (data[i-l][j][k] > lowBound && boundaries[i-l][j][k] == false) {
										data[i-l][j][k] = (double) 0;
									} else if (data[i-l][j][k] < lowBound && data[i-l][j][k] != (double) 0) {
										boundaries[i-l][j][k] = true;
										break;
									}
								}
								break;
							}
						}
					}
				}
			}
		}
		return data;
	}
	
	// cleans up errant voxels
		public static double[][][] cleanUp(double[][][] data1, int strand, boolean xdim, boolean ydim, boolean zdim) {
			// x dimension
			if (xdim == true) {
				for (int i = 0; i < data1.length; i++) {
					for (int j = 0; j < data1[i].length; j++) {
						for (int k = 0; k < data1[i][j].length/2; k++) {
							if (data1[i][j][k] == 0 && data1[i][j][k+1] != 0 && data1[i][j][k+2] == 0) {	
								data1[i][j][k+1] = 0;
							}
						}
						for (int k = data1[i][j].length - 1; k > data1[i][j].length/2; k--) {
							if (data1[i][j][k] == 0 && data1[i][j][k-1] != 0 && data1[i][j][k-2] == 0) {
								data1[i][j][k-1] = 0;
							}
						}
					}
				}
			}
			
			if (ydim == true) {
				// y dimension
				for (int k = 0; k < data1.length/2; k++) {
					for (int i = 0; i < data1[k].length; i++) {
						for (int j = 0; j < data1[k][i].length; j++) {
							if (data1[k][i][j] == 0 && data1[k+1][i][j] != 0 && data1[k+2][i][j]  == 0) {
								data1[k+1][i][j] = 0;
							}
						}
					}
				}
				for (int k = data1.length - 1; k > data1.length/2; k--) {
					for (int i = 0; i < data1[k].length; i++) {
						for (int j = 0; j < data1[k][i].length; j++) {
							if (data1[k][i][j] == 0 && data1[k-1][i][j] != 0 && data1[k-2][i][j]  == 0) {
								data1[k-1][i][j] = 0;
							}
						}
					}
				}
			}		
			if (zdim == true) {
				// z dimension
				for (int i = 0; i < data1.length; i++) {
					for (int k = 0; k < data1[i].length/2; k++) {
						for (int j = 0; j < data1[i][k].length; j++) {
							if (data1[i][k][j] == 0 && data1[i][k+1][j] != 0 && data1[i][k+2][j] == 0) {
								data1[i][k+1][j] = 0;
							}
						}
					}
					for (int k = data1[i].length - 1; k > data1[i].length/2; k--) {
						for (int j = 0; j < data1[i][k].length; j++) {
							if (data1[i][k][j] == 0 && data1[i][k-1][j] != 0 && data1[i][k-2][j] == 0) {
								data1[i][k-1][j] = 0;
							}
						}
					}
				}
			}			
			return data1;
		}
}
