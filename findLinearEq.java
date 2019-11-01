
public class findLinearEq {
	
	static double slopes[][]; // 
	static double m; // slope of the gradient
	
	public static double[][] findLinearEquations(double dataset[][][]) {
			
		int ydimLength = dataset[0][0].length; // we don't need to calculate the length of the array every time
		
		for (int i = 0; i < dataset.length ; i++) {
			for (int j = 0; j < dataset[i].length; j++) {
				for (int k = 0; k < ydimLength; k++) {
					m+=dataset[i][j][k]; // average, change at somepoint 
				}
				slopes[i][j] = m/ydimLength;
				m = 0;
			}
			
		}
		return slopes; 
	}
}
