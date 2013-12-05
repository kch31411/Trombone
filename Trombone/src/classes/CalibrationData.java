package classes;

import static classes.Constants.*;

public class CalibrationData {
	private int id;
	private String name;
	private double[][][] calib_data = new double[3][12][blockSize + 1]; // 3,4,5 octave
	private String file_path;
	
	public String getFile_path() {
		return file_path;
	}

	public void setFile_path(String file_path) {
		this.file_path = file_path;
	}

	public CalibrationData(int id, String name,
			double[][][] calib_data, String file_path) {
		super();
		this.id = id;
		this.name = name;
		this.calib_data = calib_data;
		this.file_path = file_path;
	}
	public CalibrationData(int id, String name, String file_path) {
		super();
		this.id = id;
		this.name = name;
		this.file_path = file_path;
	}


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double[][][] getCalib_data() {
		return calib_data;
	}

	public void setCalib_data(double[][][] calib_data) {
		this.calib_data = calib_data;
	}
}
