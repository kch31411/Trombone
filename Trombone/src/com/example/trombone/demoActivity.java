package com.example.trombone;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import javax.xml.datatype.Duration;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import android.widget.Toast;
import ca.uol.aig.fftpack.RealDoubleFFT;
import classes.CalibrationData;
import classes.History;
import classes.Memo;
import classes.MusicSheet;
import classes.Note;
import db.DBHelper;

import static classes.Constants.*;

public class demoActivity extends Activity {
	// music sheet information
	private int musicSheetId;
	private int calibId;
	private int pageNum = 1;  // TODO : page related works
	int lastNoteIndex;
	int currentPosition = 0;
	private long prevRecognitionTime;
	
	private TextView selectedMemo;	// for memo modify
	private ArrayList<Memo> memoList = new ArrayList<Memo>();
	private ArrayList<Note> noteList = new ArrayList<Note>();

	// db helper
	DBHelper dbhelper = new DBHelper(this);

	// handling long touch for memo addition
	private float mLastMotionX = 0;
	private float mLastMotionY = 0;
	private int mTouchSlop;
	private boolean mHasPerformedLongPress;
	private Handler mHandler = null;

	private RealDoubleFFT transformer;
	Button startStopButton;
	Button prevButton, nextButton;
	boolean started = false;
	int sampleSize = 10;
	int sampleCount = 0;

	double[][][] calib_data = new double[3][12][blockSize + 1]; // 3,4,5 octave
	double[][] calibPitches = new double[3][12]; // 3,4,5 octave

	double[] ref_pitches;

	int currentCount = 0;
	int currentError = 0;

	RecordAudio recordTask;

	// TODO : collect view variables in here.
	TextView pageNumView;
	ImageView trackingView, trackingDebugView;
	ImageView currentSpec;
	Bitmap curBitmap, rec1Bitmap, rec2Bitmap;
	Canvas curCanvas, rec1Canvas, rec2Canvas;
	Paint paint = new Paint();

	int width, height;
	float ratio = 1;
	int bar_length = 12; 
	int keyNumber = 0;
	
	int[] yPositions = yPosition;
	
	// drawing related.
	int side_padding = 40;
	int top_padding = 80;
	int space_five_line = 150;
	double dx;

	TextView resultText, debugText;

	double[][] toTransformSample = new double[blockSize * 2 + 1][sampleSize];

	boolean [] matches = new boolean[11];
	double [] scores = new double[11];
	double [] errors = new double[11];
	double [] factors = {0,0,0,0,0,1,0.7,0.4,0.2,0.1,0.1};
	
	double tracking_velocity;
	double tracking_x;   // XXX : necessary???
	double tracking_y;
	long tracking_prev_time = -1;
	long matched_time = -1;
	
	
	Note[] testNotes = new Note[5];
	double[][] results = new double[5][4];

	@Override
	protected void onStop(){
		if (started) {
			started = false;
			recordTask.cancel(true);			
		}
		super.onStop();
	}

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);  
		//set up full screen
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_demo);

		initialize();

		testNotes[0] = new Note(408);
		testNotes[1] = new Note(409);
		testNotes[2] = new Note(410);
		testNotes[3] = new Note(411);
		testNotes[4] = new Note(412);
		
		// start button
		startStopButton = (Button) findViewById(R.id.StartStopButton);
		startStopButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if (started) {
					started = false;
					startStopButton.setText("Start");
					recordTask.cancel(true);
					currentPosition = 0;
					scores = new double[11];
					errors = new double[11];
					currentCount = 0;
					currentError = 0;
					tracking_velocity = 0;
				} else {
					started = true;
					startStopButton.setText("Stop");
					recordTask = new RecordAudio();
					recordTask.execute();		// thread call
					
					//Date temp = new Date();
					prevRecognitionTime = 0;// temp.getTime();
				}
			}
		});
		transformer = new RealDoubleFFT(blockSize * 2 + 1);
	}
	
	// set initial data
	private void initialize() {

		// spectrum
		currentSpec = (ImageView) findViewById(R.id.CurrentSpectrum);
		curBitmap = Bitmap.createBitmap((int) 2*blockSize, (int) 400,
				Bitmap.Config.ARGB_8888);
		curCanvas = new Canvas(curBitmap);
		currentSpec.setImageBitmap(curBitmap);
		
		// view binding
		debugText = (TextView) findViewById(R.id.debugText);

		// set calibration data
		calibId = getIntent().getIntExtra("calib_id2play", -1);
		try {
			CalibrationData cd = dbhelper.getCalibrationData(calibId);    			
			FileInputStream fis = new FileInputStream(cd.getFile_path());
			ObjectInputStream iis = new ObjectInputStream(fis);
			calib_data = (double[][][]) iis.readObject();
			iis.close();
			
			FileInputStream fis2 = new FileInputStream(cd.getFile_path2());
			ObjectInputStream iis2 = new ObjectInputStream(fis2);
			calibPitches = (double[][]) iis2.readObject();
			iis2.close();
			
		}catch (Exception e) {
			Log.d("ccccc", "exception : " + e.toString());
		} 
		
		tracking_velocity = 1 / 5000; // �λ뜃由겼첎占�
		}
	
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
	}

	private class RecordAudio extends AsyncTask<Void, double[], Void> {
		@Override
		protected Void doInBackground(Void... params) {
			try {
				int bufferSize = AudioRecord.getMinBufferSize(frequency,
						channelConfiguration, audioEncoding);
				AudioRecord audioRecord = new AudioRecord(
						MediaRecorder.AudioSource.MIC, frequency,
						channelConfiguration, audioEncoding, bufferSize);

				short[] buffer = new short[blockSize * 2 + 1];
				double[] toTransform = new double[blockSize * 2 + 1];
				// n is even, the real of part of (n/2)-th complex FFT
				// coefficients is x[n];
				// (n-k)-th complex FFT coeffient is the conjugate of n-th
				// complex FFT coeffient.

				audioRecord.startRecording();
				while (started) {
					int bufferReadResult = audioRecord.read(buffer, 0,
							blockSize * 2 + 1);

					for (int i = 0; i < blockSize * 2 + 1
							&& i < bufferReadResult; i++) {
						toTransform[i] = (double) buffer[i] / Short.MAX_VALUE;
					}

					transformer.ft(toTransform);
					publishProgress(toTransform);
				}
				audioRecord.stop();

			} catch (Throwable t) {
				Log.e("AudioRecord", "Recording Failed");
			}
			return null;
		}

		// not in background
		@Override
		protected void onProgressUpdate(double[]... toTransform) {
			// tracking bar moving
			Date temp = new Date();
			long currentRecognitionTime = temp.getTime();
			if (tracking_prev_time <= 0) tracking_prev_time = currentRecognitionTime;
			long deltaTime = currentRecognitionTime - tracking_prev_time;
			tracking_prev_time = currentRecognitionTime; 
   
	
			curCanvas.drawColor(Color.BLACK);
			double maxIntensity = Math.abs(toTransform[0][0]); // first real (0
			// imaginary)
			double maxFrequency = 0;
			double Magnitude[] = new double[blockSize + 1];
			Magnitude[0] = maxIntensity;
			for (int i = 1; i < toTransform[0].length / 2; i++) {
				Magnitude[i] = Math.sqrt((toTransform[0][2 * i - 1])
						* (toTransform[0][2 * i - 1]) + (toTransform[0][2 * i])
						* (toTransform[0][2 * i]));
				if (maxIntensity < Magnitude[i]) {
					maxIntensity = Magnitude[i];
					maxFrequency = i;
				}
			}
			double mag = 0;
			String s = "";
			for (int k=0; k<5; k++)
			{
				Note tempNote =testNotes[k];
				
				if( tempNote != null ){ //currentPosition-5+j>=0||pageNum>1 &&        currentPosition <= lastNoteIndex ||pageNum>1){
					double[] tempSpec = calib_data[tempNote.getPitch()/100-3][tempNote.getPitch()%100-1];
					double tempMaxF = calibPitches[tempNote.getPitch()/100-3][tempNote.getPitch()%100-1];
					int tempIdx = (int)Math.round(tempMaxF/(frequency/(blockSize*2+1)));
					
					double temp_collected[] = new double[blockSize + 1];
					int[] entire_maxFrequency = { 0, 0, 0 };
					double[] entire_maxIntensity = { 0, 0, 0 };
					double entire_sum = 0;
					double temp_score = 0;

					for (int j = 0; j < entire_maxFrequency.length; j++) {
						for (int i = 0; i < tempSpec.length; i++) {
							if (j == 0) {
								entire_sum += tempSpec[i];
								temp_collected[i] = tempSpec[i];
							}
							if (entire_maxIntensity[j] < temp_collected[i]) {
								entire_maxIntensity[j] = temp_collected[i];
								entire_maxFrequency[j] = i;
							}
						}
						for (int i = 0; i < 15; i++) {
							int index = (int) (entire_maxFrequency[j]);
							if (index - i >= 0)
								temp_collected[index - i] = 0;
							if (index + i < tempSpec.length)
								temp_collected[index + i] = 0;
						}
					}
					
					
					int idx = 0;
					for (int i=-5;i<=5; i++)
					{
						if(tempIdx+i>=0 && tempIdx+i<Magnitude.length)
						{
							if(mag<Magnitude[tempIdx+i]) {
								mag=Magnitude[tempIdx+i];
								idx = i;
								
							}
						}					
					}
					results[k][0] = idx;
					temp_score += 100*Math.pow(0.5,Math.abs(idx))*mag/tempMaxF;
										
					//s+=idx+",";
					for (int ii = 0; ii<3; ii++){
						double mag2 = 0;
						int idx2 = 0;
						for (int i=-5;i<=5; i++)
						{
							if(entire_maxIntensity[ii]+i>=0 && entire_maxIntensity[ii]+i<Magnitude.length)
							{
								if(mag2<Magnitude[(int) (entire_maxIntensity[ii]+i)]) {
									mag2=Magnitude[(int) (entire_maxIntensity[ii]+i)];
									idx2 = i;
								}
							}					
						}
						results[k][ii+1] = idx2;
						temp_score += 10 * Math.pow(0.25,Math.abs(idx2)) * entire_maxIntensity[ii]/tempMaxF;
						//s+=idx2+",";
						
					}
					s+=temp_score+"  ";
					
				}	
				else ; 
				
			}
			debugText.setText(s);
			
			s+= "demo" ;
			Log.d("demo",s);
			// initializing
			if(matches[5]==true && prevRecognitionTime ==0){								
				prevRecognitionTime = temp.getTime();
			}
			for (int i = 0; i < Magnitude.length; i++) {
				int x = 2*i;
				int downy = (int) (400 - (Magnitude[i] * 40/mag));
				int upy = 400;
				paint.setColor(Color.rgb(190, 225, 245));
				curCanvas.drawLine(x, downy, x, upy, paint);
				curCanvas.drawLine(x+1, downy, x+1, upy, paint);
			}
		
			currentSpec.invalidate();
		}
	}
	
}
