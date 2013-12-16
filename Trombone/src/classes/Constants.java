package classes;

import android.media.AudioFormat;

public final class Constants {
	// sound sampling
	public final static int blockSize = 256;	// sound sample block size
	public final static int frequency = 8000*2;		// frequency range
	public final static int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	public final static int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
		

	// path to store calibration data file
	public final static String CALIB_PATH = "/storage/sdcard0/Trombone/";

	// nexux 7 size
	public final static int nexus7_width = 800;
	public final static int nexus7_height = 1280;
	
	// y position on 5 lines
	public final static int[] yPosition={0,0,1,1,2,3,3,4,4,5,5,6};
	public final static int[] yPosition_flat={0,1,1,2,2,3,4,4,5,5,6,6};

	// request code
	// calibration activity
	public final static int CALIB_ENTER_NAME = 1;
	// display activity
	public static final int MEMO_ADD = 1;
	public static final int MEMO_MODIFY = 2;
	public static final int SCORE = 3;
	// main ui activity
	public final static int CALIBRATION = 3;
}
