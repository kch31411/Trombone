package com.example.trombone;

import java.util.ArrayList;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.TextView;
import ca.uol.aig.fftpack.RealDoubleFFT;
/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class CalibrationActivity extends Activity {
	// added
	int frequency = 8000*2;
	int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

	private RealDoubleFFT transformer;
	int blockSize = 256;
	Button startStopButton, buttonC, buttonCs, buttonD, buttonDs,
		buttonE, buttonF, buttonFs, buttonG, buttonGs,
		buttonA, buttonAs, buttonB, RefButton;
	boolean started = false;
	EditText refText;
	 
	int sampleSize = 10;
	int sampleCount = 0;
	
	double reference=440.0;
	
	double[] ref_pitches = {261.626, 277.183, 293.665, 311.127, 329.628, 
			349.228, 369.994, 391.995, 415.305, 440.000, 466.164,
			493.883};
	double[] pitches = {261.626, 277.183, 293.665, 311.127, 329.628, 
			349.228, 369.994, 391.995, 415.305, 440.000, 466.164,
			493.883};
	
	String[] pitchName={"C","C#","D","D#","E","F","F#","G","G#",
			"A","A#","B"
	};
	int[] yPosition={0,0,1,1,2,3,3,4,4,5,5,6};
	
	double calibPitches_sum = 0;
	int calibCount = 0;
	int calibTarget = -1;
	Button calibButton;
	double[] calibPitches = new double[12];
	
	int currentCount = 0;
	int currentError = 0;
	int currentPosition = 0;

	RecordAudio recordTask;

	ImageView currentSpec;
	Bitmap curBitmap, rec1Bitmap, rec2Bitmap;
	Canvas curCanvas, rec1Canvas, rec2Canvas;
	Paint paint;
	
	int width, height;
	int nexus7_width = 800;
	int nexus7_height = 1280;
	
	int lastNoteIndex;
	int side_padding = 40;

	TextView resultText, debugText, pitchText;
	ProgressBar progress;
	
	double[][] toTransformSample = new double[blockSize * 2 + 1][sampleSize];
	SQLiteDatabase calibDB;

	ArrayList<ImageView> noteViews;

	private void displayMusicSheet(int start) {
		FrameLayout l = (FrameLayout) findViewById(R.id.music_sheet);
		
		for (ImageView iv : noteViews) {
			l.removeView(iv);
		}
		noteViews.clear();
		
		Bitmap bmNote = BitmapFactory.decodeResource
				(getResources(),R.drawable.note_1);

		for(int i=0; i<12; i++){
			ImageView noteImage = new ImageView(getBaseContext());

			noteImage.setImageBitmap(bmNote);
			noteImage.setLayoutParams(new LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
						
			if(i==1||i==3||i==6||i==8||i==10){
				ImageView sharp = new ImageView(getBaseContext());
				Bitmap bm = BitmapFactory.decodeResource(getResources(),
						R.drawable.sharp);
				sharp.setImageBitmap(bm);
				sharp.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
						LayoutParams.WRAP_CONTENT));
				sharp.setPadding(100+i*55, yPosition[i]*-10+95, 0, 0);
				Matrix m = new Matrix();
				m.postScale((float) 0.17, (float) 0.17);
				sharp.setScaleType(ScaleType.MATRIX);
				sharp.setImageMatrix(m);
				l.addView(sharp);
				noteImage.setPadding(120+i*55, yPosition[i]*-10+60, 0, 0);
			}
			else
				noteImage.setPadding(115+i*55, yPosition[i]*-10+60, 0, 0);
			Matrix mNote = new Matrix();
			mNote.postScale((float) 0.5, (float) 0.5);
			noteImage.setScaleType(ScaleType.MATRIX);
			noteImage.setImageMatrix(mNote);

			l.addView(noteImage);						
			noteViews.add(noteImage);	
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		 
		requestWindowFeature(Window.FEATURE_NO_TITLE);  
	    //set up full screen
	    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
	                WindowManager.LayoutParams.FLAG_FULLSCREEN);  
	        
		setContentView(R.layout.activity_calibration);
		//getWindow().addFlags(WindowManager.LayoutParams. FLAG_LAYOUT_NO_LIMITS)
		//getWindow().setLayout(852, 1280)

		// get dimension of device
		Display display = getWindowManager().getDefaultDisplay();
				
		Point size = new Point();
		display.getSize(size);
		width = size.x;
		height = size.y;

		getWindow().setLayout(nexus7_width, nexus7_height);
		
		FrameLayout mainView = (FrameLayout)
				findViewById(R.id.calibration_frame);
		
		float ratioW = (float)width/nexus7_width;
		float ratioH = (float)height/nexus7_height;

		if (ratioW < 1 || ratioH < 1) {
			if(ratioW>ratioH) ratioW = ratioH;
			mainView.setScaleX((float) ratioW);
			mainView.setScaleY((float) ratioW);
			mainView.setPivotX(0.0f);
			mainView.setPivotY(0.0f);
		}

		// calibDB = openOrCreateDatabase("Calibration",MODE_WORLD_WRITEABLE, null);
		for(int i=0; i<12; i++)
		{
			calibPitches[i] = pitches[i];
		}
		progress = (ProgressBar) findViewById(R.id.ProgressBar);
		refText = (EditText)findViewById(R.id.RefEdit);		
		buttonC = (Button) findViewById(R.id.buttonC);
		buttonC.setBackgroundColor(Color.WHITE);
		calibButton = buttonC;
		buttonC.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				calibButton.setBackgroundColor(Color.WHITE);
				calibButton = buttonC;
				calibTarget = 0;
				calibNote();
			}
		});
		buttonCs = (Button) findViewById(R.id.buttonCs);
		buttonCs.setBackgroundColor(Color.WHITE);
		buttonCs.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				calibButton.setBackgroundColor(Color.WHITE);
				calibButton = buttonCs;
				calibTarget = 1;
				calibNote();
			}
		});
		buttonD = (Button) findViewById(R.id.buttonD);
		buttonD.setBackgroundColor(Color.WHITE);
		buttonD.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				calibButton.setBackgroundColor(Color.WHITE);
				calibButton = buttonD;
				calibTarget = 2;
				calibNote();
			}
		});
		buttonDs = (Button) findViewById(R.id.buttonDs);
		buttonDs.setBackgroundColor(Color.WHITE);
		buttonDs.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				calibButton.setBackgroundColor(Color.WHITE);
				calibButton = buttonDs;
				calibTarget = 3;
				calibNote();
			}
		});
		buttonE = (Button) findViewById(R.id.buttonE);
		buttonE.setBackgroundColor(Color.WHITE);
		buttonE.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				calibButton.setBackgroundColor(Color.WHITE);
				calibButton = buttonE;
				calibTarget = 4;
				calibNote();
			}
		});
		buttonF = (Button) findViewById(R.id.buttonF);
		buttonF.setBackgroundColor(Color.WHITE);
		buttonF.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				calibButton.setBackgroundColor(Color.WHITE);
				calibButton = buttonF;
				calibTarget = 5;
				calibNote();
			}
		});
		buttonFs = (Button) findViewById(R.id.buttonFs);
		buttonFs.setBackgroundColor(Color.WHITE);
		buttonFs.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				calibButton.setBackgroundColor(Color.WHITE);
				calibButton = buttonFs;
				calibTarget = 6;
				calibNote();
			}
		});
		buttonG = (Button) findViewById(R.id.buttonG);
		buttonG.setBackgroundColor(Color.WHITE);
		buttonG.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				calibButton.setBackgroundColor(Color.WHITE);
				calibButton = buttonG;
				calibTarget = 7;
				calibNote();
			}
		});
		buttonGs = (Button) findViewById(R.id.buttonGs);
		buttonGs.setBackgroundColor(Color.WHITE);
		buttonGs.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				calibButton.setBackgroundColor(Color.WHITE);
				calibButton = buttonGs;
				calibTarget = 8;
				calibNote();
			}
		});
		buttonA = (Button) findViewById(R.id.buttonA);
		buttonA.setBackgroundColor(Color.WHITE);
		buttonA.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				calibButton.setBackgroundColor(Color.WHITE);
				calibButton = buttonA;
				calibTarget = 9;
				calibNote();
			}
		});
		buttonAs = (Button) findViewById(R.id.buttonAs);
		buttonAs.setBackgroundColor(Color.WHITE);
		buttonAs.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				calibButton.setBackgroundColor(Color.WHITE);
				calibButton = buttonAs;
				calibTarget = 10;
				calibNote();
			}
		});
		buttonB = (Button) findViewById(R.id.buttonB);
		buttonB.setBackgroundColor(Color.WHITE);
		buttonB.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				calibButton.setBackgroundColor(Color.WHITE);
				calibButton = buttonB;
				calibTarget = 11;
				calibNote();
			}
		});
		
		resultText = (TextView) findViewById(R.id.resultText);
		debugText = (TextView) findViewById(R.id.debugText);
		pitchText = (TextView) findViewById(R.id.pitchText);
		showPitch();
		
		
		startStopButton = (Button) findViewById(R.id.StartStopButton);
		startStopButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if (started) {
					started = false;
					calibTarget = -1;
					calibButton.setBackgroundColor(Color.WHITE);
					startStopButton.setText("Start");
					recordTask.cancel(true);
					currentPosition = 0;
					currentCount = 0;
					currentError = 0;
				} else {
					started = true;
					startStopButton.setText("Stop");
					recordTask = new RecordAudio();
					recordTask.execute();
				}
			}
		});
		
		RefButton = (Button) findViewById(R.id.RefButton);
		RefButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				reference = Double.parseDouble(refText.getText().toString());
				for(int i=0;i<12;i++){
					pitches[i]= Math.floor(
							ref_pitches[i]*reference/440*100)/100;
					calibPitches[i]=pitches[i];
				}
				showPitch();
			}});
		
		transformer = new RealDoubleFFT(blockSize * 2 + 1);

		paint = new Paint();
		paint.setColor(Color.GREEN);

		currentSpec = (ImageView) findViewById(R.id.CurrentSpectrum);
		curBitmap = Bitmap.createBitmap((int) 256, (int) 100,
				Bitmap.Config.ARGB_8888);
		curCanvas = new Canvas(curBitmap);
		currentSpec.setImageBitmap(curBitmap);
		noteViews = new ArrayList<ImageView>();
		 
		
		FrameLayout l = (FrameLayout) findViewById(R.id.music_sheet);

		Paint paint = new Paint();
		paint.setColor(Color.BLACK);

		// Display music sheet
		int y = 0;
		int count = 0;
		while (count++ < 1) {
			ImageView fiveLine = new ImageView(getBaseContext());
			Bitmap bitmap = Bitmap.createBitmap((int) nexus7_width, (int) 150,
					Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			fiveLine.setImageBitmap(bitmap);

			for (int i = 25; i <= 105; i += 20)
				canvas.drawLine(side_padding, i, nexus7_width - side_padding, i, paint);

			canvas.drawLine(nexus7_width - side_padding, 25, 
					nexus7_width - side_padding,
					105, paint);
			canvas.drawLine(110, 125, 140, 125, paint);
			canvas.drawLine(110+60, 125, 120+60, 125, paint);

			fiveLine.setLayoutParams(new LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			fiveLine.setPadding(0, y, 0, 0);
			fiveLine.setScaleType(ScaleType.MATRIX);

			l.addView(fiveLine);

			ImageView clef = new ImageView(getBaseContext());
			Bitmap bm = BitmapFactory.decodeResource(getResources(),
					R.drawable.high);
			clef.setImageBitmap(bm);
			clef.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT));
			clef.setPadding(side_padding, y, 0, 0);

			Matrix m = new Matrix();
			m.postScale((float) 0.24, (float) 0.24);
			clef.setScaleType(ScaleType.MATRIX);
			clef.setImageMatrix(m);

			l.addView(clef);
			y += 150;
		}
		displayMusicSheet(0);	
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
	}

	public void calibNote (){
		calibButton.setBackgroundColor(Color.argb(100, 200, 200, 100));
		progress.setProgress(0);
		progress.setVisibility(View.VISIBLE);
		
		calibCount = 0;
		calibPitches_sum = 0;
		
		started = true;
		startStopButton.setText("Stop");
		recordTask = new RecordAudio();
		recordTask.execute();
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
			curCanvas.drawColor(Color.BLACK);
			// imaginary)
			double[] maxFrequency ={0,0,0};
			double[] maxIntensity = {0,0,0};			
			
			double Magnitude[] = new double[blockSize + 1];
			double temp_Magnitude[] = new double[blockSize + 1];	
			Magnitude[0] = Math.abs(toTransform[0][0]);

			for (int j = 0; j < maxFrequency.length; j++) {
				for (int i = 1; i < toTransform[0].length / 2; i++) {
					if (j == 0) {
						Magnitude[i] = Math.sqrt((toTransform[0][2 * i - 1])
								* (toTransform[0][2 * i - 1])
								+ (toTransform[0][2 * i])
								* (toTransform[0][2 * i]));
						temp_Magnitude[i]=Magnitude[i];
					}
					if (maxIntensity[j] < temp_Magnitude[i]) {
						maxIntensity[j] = temp_Magnitude[i];
						maxFrequency[j] = i;
					}
					// maxIntensity = Math.max(maxIntensity,
					// Math.abs(toTransform[0][i]));
				}
				for (int i=0; i<15; i++) {
					int index = (int)(maxFrequency[j]);
					if(index-i>=0)
						temp_Magnitude[index-i]=0;
					if(index+i<Magnitude.length)
						temp_Magnitude[index+i]=0;
				}
			}
			
			for (int i = 0; i < Magnitude.length; i++) {
				int x = i;
				
				int scale=1;
				if(maxIntensity[0]>10)
					scale = (int) Math.ceil(maxIntensity[0]/10);

				int downy = (int) (100 - (Magnitude[i] * 10/scale));
				int upy = 100;

				if(i==maxFrequency[0]||i==maxFrequency[1]
						||i==maxFrequency[2]){
						paint.setColor(Color.BLUE);
						curCanvas.drawLine(x, 0, x, downy, paint);
						paint.setColor(Color.GRAY);
				}
				else paint.setColor(Color.rgb(190,225,245));
				curCanvas.drawLine(x, downy, x, upy, paint);
			}
			
			for (int j=0;j<maxFrequency.length;j++){
				maxFrequency[j] = maxFrequency[j]
						*frequency / (blockSize * 2 + 1);
			}
			
			double MajorF = maxFrequency[0];
			MajorF = Math.floor(MajorF*100)/100;
			resultText.setText(MajorF+" "+
					Math.floor(maxFrequency[1]*100)/100
					+" "+Math.floor(maxFrequency[2]*100)/100);
			
			if (calibTarget >= 0) {
				for (int i = 1; i < 3; i++)
					if (Math.abs(MajorF - pitches[calibTarget]) > Math
							.abs(maxFrequency[i] - pitches[calibTarget]))
						MajorF = maxFrequency[i];

				if (maxIntensity[0] > 5 && MajorF > pitches[calibTarget] - 50
						&& MajorF < pitches[calibTarget] + 50
						&& calibCount < 40) {
					calibPitches_sum += MajorF;
					calibCount++;
					debugText.setText(calibCount + "");

					progress.setProgress(calibCount * 100 / 40);
				}
			}
			
			if(calibCount>=40){
				started = false;
				startStopButton.setText("Start");
				calibButton.setBackgroundColor(Color.argb(0,0,0,0));
				calibPitches[calibTarget] = Math.floor
						(calibPitches_sum/calibCount*100)/100;
				showPitch();
			}

			if (true) {
				if (sampleCount < sampleSize) {
					for (int i = 0; i < toTransform[0].length; i++) {
						toTransformSample[i][sampleCount] = toTransform[0][i];
					}
					sampleCount++;
				} else {
					sampleCount = 0;
				}
			}
			currentSpec.invalidate();
		}
	}
	
	public void showPitch()
	{
		String s = "";
		for (int i=0; i<12; i++)
		{
			s+=pitchName[i] + " : "+pitches[i];
			if(pitches[i]!=calibPitches[i])
				s+=" -> "+calibPitches[i];
			s+="\n";
		}
		pitchText.setText(s);
	}
}
