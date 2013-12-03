package com.example.trombone;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import classes.CalibrationData;
import classes.Memo;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.Intent;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import ca.uol.aig.fftpack.RealDoubleFFT;
import db.DBHelper;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class CalibrationActivity extends Activity {
	public final static int ENTER_NAME = 1;

	public final static String CALIB_PATH = "/storage/sdcard0/Trombone/";
	
	// added
	int frequency = 8000 * 2;
	int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	

	private RealDoubleFFT transformer;
	int blockSize = 256;
	Button startStopButton, buttonC, buttonCs, buttonD, buttonDs, buttonE,
			buttonF, buttonFs, buttonG, buttonGs, buttonA, buttonAs, buttonB,
			RefButton;
	boolean started = false;
	EditText refText;
	
	DBHelper dbhelper = new DBHelper(this);

	int sampleSize = 10;
	int sampleCount = 0;

	double reference = 440.0;

	double[] ref_pitches = { 261.626, 277.183, 293.665, 311.127, 329.628,
			349.228, 369.994, 391.995, 415.305, 440.000, 466.164, 493.883 };
	double[] pitches = { 261.626, 277.183, 293.665, 311.127, 329.628, 349.228,
			369.994, 391.995, 415.305, 440.000, 466.164, 493.883 };

	double[][][] calib_data = new double[3][12][blockSize + 1]; // 3,4,5 octave

	String[] pitchName = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#",
			"A", "A#", "B" };
	int[] yPosition = { 0, 0, 1, 1, 2, 3, 3, 4, 4, 5, 5, 6 };

	double collected_Magnitude[] = new double[blockSize + 1];
	int collected_num = 0;

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
	float ratio = 1;
	int octave = 4;

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

		Bitmap bmNote = BitmapFactory.decodeResource(getResources(),
				R.drawable.note_4);

		for (int i = 0; i < 12; i++) {
			ImageView noteImage = new ImageView(getBaseContext());

			noteImage.setImageBitmap(bmNote);
			noteImage.setLayoutParams(new LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

			if (i == 1 || i == 3 || i == 6 || i == 8 || i == 10) {
				ImageView sharp = new ImageView(getBaseContext());
				Bitmap bm = BitmapFactory.decodeResource(getResources(),
						R.drawable.sharp);
				sharp.setImageBitmap(bm);
				sharp.setLayoutParams(new LayoutParams(
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				sharp.setPadding(100 + i * 55, yPosition[i] * -10 + 95, 0, 0);
				Matrix m = new Matrix();
				m.postScale((float) 0.17, (float) 0.17);
				sharp.setScaleType(ScaleType.MATRIX);
				sharp.setImageMatrix(m);
				l.addView(sharp);
				noteImage.setPadding(120 + i * 55, yPosition[i] * -10 + 60, 0,
						0);
			} else
				noteImage.setPadding(115 + i * 55, yPosition[i] * -10 + 60, 0,
						0);
			Matrix mNote = new Matrix();
			mNote.postScale((float) 0.5, (float) 0.5);
			noteImage.setScaleType(ScaleType.MATRIX);
			noteImage.setImageMatrix(mNote);

			l.addView(noteImage);
			noteViews.add(noteImage);
		}
	}

	@Override
	protected void onStop() {
		if (started) {
			recordTask.cancel(true);
		}
		super.onStop();

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// set up full screen
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_calibration);
		
		// get dimension of device
		Display display = getWindowManager().getDefaultDisplay();

		Point size = new Point();
		display.getSize(size);
		width = size.x;
		height = size.y;

		getWindow().setLayout(nexus7_width, nexus7_height);

		FrameLayout mainView = (FrameLayout) findViewById(R.id.calibration_frame);

		ratio = (float) width / nexus7_width;

		if (ratio < 1) {
			mainView.setScaleX((float) ratio);
			mainView.setScaleY((float) ratio);
			mainView.setPivotX(0.0f);
			mainView.setPivotY(0.0f);
		}

		// get latest calibration data
		try {
			int calib_id = getIntent().getIntExtra("calib_id", -1);
			CalibrationData cd = dbhelper.getCalibrationData(calib_id);
			
			FileInputStream fis = new FileInputStream(cd.getFile_path());
			ObjectInputStream iis = new ObjectInputStream(fis);
			calib_data = (double[][][]) iis.readObject();
			iis.close();
					
		} catch (Exception e) {
			Log.d("ccccc", "exception : " + e.toString());
		}

		// calibDB = openOrCreateDatabase("Calibration",MODE_WORLD_WRITEABLE, null);
		for(int i=0; i<12; i++)
		{
			calibPitches[i] = pitches[i];
		}
		setDefault();		

		progress = (ProgressBar) findViewById(R.id.ProgressBar);
		refText = (EditText)findViewById(R.id.RefEdit);		


		RadioGroup rd = (RadioGroup) findViewById(R.id.radiogroup1);
		rd.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int arg1) {
				// TODO Auto-generated method stub
				switch (arg1) {
				case R.id.radio3:
					octave = 3;
					break;
				case R.id.radio4:
					octave = 4;
					break;
				case R.id.radio5:
					octave = 5;
					break;
				default:
					octave = 4;
				}
				for (int i = 0; i < 12; i++) {
					calibPitches[i] = Math.floor(pitches[i]
							* Math.pow(2, octave - 4) * 1000) / 1000;
				}
				showPitch();
			}
		});

		LinearLayout button_L = (LinearLayout) findViewById(R.id.button_layout);
		button_L.setPadding(100, 0, side_padding, 0);

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
					calibTarget = -1;
					calibCount = 0;
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
				for (int i = 0; i < 12; i++) {
					pitches[i] = Math.floor(ref_pitches[i] * reference / 440
							* 1000) / 1000;
					calibPitches[i] = pitches[i];
				}
				showPitch();

				RadioGroup rd = (RadioGroup) findViewById(R.id.radiogroup1);
				rd.check(R.id.radio4);
				octave = 4;
				calib_data = new double[3][12][blockSize + 1];
				setDefault();
			}});

		Button retButton = (Button) findViewById(R.id.returnButton);
		retButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {				
				Intent foo = new Intent(CalibrationActivity.this, TextEntryActivity.class);
				foo.putExtra("value", "");
				foo.putExtra("deletable", false);
				foo.putExtra("enterOpacity", false);
				foo.putExtra("title", "Instrument Name");
				CalibrationActivity.this.startActivityForResult(foo, ENTER_NAME);

			}
		});

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

			int startPosition = 19;
			int interval = 20;

			if (ratio < 1)
				interval = 22;

			for (int i = 0; i < 5; i++)
				canvas.drawLine(side_padding, startPosition + i * interval,
						nexus7_width - side_padding, startPosition + i
								* interval, paint);

			canvas.drawLine(nexus7_width - side_padding, startPosition,
					nexus7_width - side_padding, startPosition + 4 * interval,
					paint);
			canvas.drawLine(110, startPosition + 5 * interval, 140,
					startPosition + 5 * interval, paint);
			canvas.drawLine(110 + 60, startPosition + 5 * interval, 120 + 60,
					startPosition + 5 * interval, paint);
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

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ENTER_NAME:
			try {
				Log.d("ccccc", "get activity result");
				
				String name =data.getStringExtra("value"); 
				if (name == null || name.length() <= 0) {
					Date cDate = new Date();
					String fDate = new SimpleDateFormat("yyyy-MM-dd").format(cDate);
					name = "tmp_" + fDate;
				}
				
				String path = CALIB_PATH + name + ".clb";
				File file = new File(CALIB_PATH);
				if( !file.exists() )
					file.mkdirs();
				
				// save calibration data as file
				FileOutputStream fos = new FileOutputStream(path);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(calib_data);
				oos.close();
				
				// insert calibration data DB
				CalibrationData calibrationData = new CalibrationData(-1, name, path); // XXX : is filepath correct?
				int id = (int) dbhelper.addCalibrationData(calibrationData);
				
				Intent foo = new Intent();
				foo.putExtra("calib2main", calibPitches);
				foo.putExtra("myData2", "Data 2 value");
				foo.putExtra("calib_id", id);

				// Activity finished ok, return the data
				setResult(RESULT_OK, foo);
				finish();
			} catch (Exception e) {
				Log.d("ccccc", "exception : " + e.toString());
			}
			break;
		default:
			break;
		}
	}
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
	}

	public void calibNote() {
		calibButton.setBackgroundColor(Color.argb(100, 200, 200, 100));
		progress.setProgress(0);
		progress.setVisibility(View.VISIBLE);

		calibCount = 0;
		calibPitches_sum = 0;
		collected_num = 0;
		collected_Magnitude = new double[blockSize + 1];
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

			double[] maxFrequency = { 0, 0, 0 };
			double[] maxIntensity = { 0, 0, 0 };

			double Magnitude[] = new double[blockSize + 1];
			double temp_Magnitude[] = new double[blockSize + 1];
			Magnitude[0] = Math.abs(toTransform[0][0]);
			if (calibTarget >= 0) {
				collected_Magnitude[0] = collected_Magnitude[0]
						* (1 - 1 / (double) (collected_num + 1)) + Magnitude[0]
								/ (double) (collected_num + 1);
			}

			for (int j = 0; j < maxFrequency.length; j++) {
				for (int i = 1; i < toTransform[0].length / 2; i++) {
					if (j == 0) {
						Magnitude[i] = Math.sqrt((toTransform[0][2 * i - 1])
								* (toTransform[0][2 * i - 1])
								+ (toTransform[0][2 * i])
								* (toTransform[0][2 * i]));

						temp_Magnitude[i] = Magnitude[i];

						if (calibTarget >= 0) {
							collected_Magnitude[i] = collected_Magnitude[i]
									* (1 - 1 / (double) (collected_num + 1))
									+ Magnitude[i]
									/ (double) (collected_num + 1);
						}
					}
					if (maxIntensity[j] < temp_Magnitude[i]) {
						maxIntensity[j] = temp_Magnitude[i];
						maxFrequency[j] = i;
					}
					// maxIntensity = Math.max(maxIntensity,
					// Math.abs(toTransform[0][i]));
				}
				for (int i = 0; i < 15; i++) {
					int index = (int) (maxFrequency[j]);
					if (index - i >= 0)
						temp_Magnitude[index - i] = 0;
					if (index + i < Magnitude.length)
						temp_Magnitude[index + i] = 0;
				}
			}

			for (int i = 0; i < Magnitude.length; i++) {
				int x = i;
				int downy = (int) (100 - (Magnitude[i] * 10));

				int upy = 100;


				if (i == maxFrequency[0] || i == maxFrequency[1]
						|| i == maxFrequency[2]) {
					paint.setColor(Color.BLUE);
					curCanvas.drawLine(x, 0, x, downy, paint);
					paint.setColor(Color.GRAY);
				} else
					paint.setColor(Color.rgb(190, 225, 245));
				curCanvas.drawLine(x, downy, x, upy, paint);
			}

			if (calibTarget >= 0) {
				double[] default_spec = calib_data[octave - 3][calibTarget];
				for (int i = 0; i < default_spec.length; i++) {
					int x = i;
					int downy = (int) (100 - (default_spec[i] * 10));
					int upy = 100;
					paint.setColor(Color.argb(150, 255, 10, 20));
					curCanvas.drawLine(x, downy, x, upy, paint);
				}

				for (int j = 0; j < maxFrequency.length; j++) {
					maxFrequency[j] = maxFrequency[j] * frequency
							/ (blockSize * 2 + 1);
				}

				double MajorF = maxFrequency[0];
				double MaxI = maxIntensity[0];

				MajorF = Math.floor(MajorF * 1000) / 1000;
				debugText.setText(MajorF + "\n"
						+ Math.floor(maxFrequency[1] * 1000) / 1000 + "\n"
						+ Math.floor(maxFrequency[2] * 1000) / 1000);

				double targetPitch = pitches[calibTarget]
						* Math.pow(2, octave - 4);

				for (int i = 1; i < 3; i++)
					if (Math.abs(MajorF - targetPitch) > Math
							.abs(maxFrequency[i] - targetPitch)) {
						MajorF = maxFrequency[i];
						MaxI = maxIntensity[i];
					}

				if (MaxI > 3 && MajorF > targetPitch - 50
						&& MajorF < targetPitch + 50 && calibCount < 40) {
					calibPitches_sum += MajorF;
					calibCount++;
					progress.setProgress(calibCount * 100 / 40);
				}
			} else { // not calibrating
				double error = 0;
				for (int i = 0; i < Magnitude.length; i++) {
					int downy = (int) (100 - (collected_Magnitude[i] * 10));
					int upy = 100;
					paint.setColor(Color.argb(100, 255, 200, 100));
					curCanvas.drawLine(i, downy, i, upy, paint);

					if (collected_Magnitude[i] * 1.2 > Magnitude[i])
						error += collected_Magnitude[i] - Magnitude[i];
				}
				resultText.setText(error + " ");
			}

			if (calibCount >= 40) {
				double temp_collected[] = new double[blockSize + 1];

				started = false;
				startStopButton.setText("Start");
				calibButton.setBackgroundColor(Color.argb(0, 0, 0, 0));
				calibPitches[calibTarget] = Math.floor(calibPitches_sum
						/ calibCount * 1000) / 1000;
				showPitch();
				curCanvas.drawColor(Color.BLACK);

				for (int i = 0; i < collected_Magnitude.length; i++) {
					calib_data[octave - 3][calibTarget][i] = collected_Magnitude[i];

					int x = i;
					int downy = (int) (100 - (collected_Magnitude[i] * 10));
					int upy = 100;
					paint.setColor(Color.rgb(255, 200, 0));
					curCanvas.drawLine(x, downy, x, upy, paint);
				}


				double[] entire_maxFrequency = { 0, 0, 0 };
				double[] entire_maxIntensity = { 0, 0, 0 };
				double entire_sum = 0;

				for (int j = 0; j < entire_maxFrequency.length; j++) {
					for (int i = 0; i < collected_Magnitude.length; i++) {
						if (j == 0) {
							entire_sum += collected_Magnitude[i];
							temp_collected[i] = collected_Magnitude[i];
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
						if (index + i < collected_Magnitude.length)
							temp_collected[index + i] = 0;
					}

					entire_maxFrequency[j] = Math.floor(entire_maxFrequency[j]
							* frequency / (blockSize * 2 + 1) * 1000) / 1000;
				}

				resultText
						.setText(entire_maxFrequency[0]
								+ "_"
								+ Math.floor(entire_maxIntensity[0]
										/ entire_sum * 1000)
								/ 1000
								+ " "
								+ entire_maxFrequency[1]
								+ "_"
								+ Math.floor(entire_maxIntensity[1]
										/ entire_sum * 1000)
								/ 1000
								+ " "
								+ entire_maxFrequency[2]
								+ "_"
								+ Math.floor(entire_maxIntensity[2]
										/ entire_sum * 1000) / 1000);
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

	public void showPitch() {
		String s = "";
		for (int i = 0; i < 12; i++) {
			s += pitchName[i] + " : "
					+ Math.floor(pitches[i] * Math.pow(2, octave - 4) * 1000)
					/ 1000;
			if (Math.floor(pitches[i] * Math.pow(2, octave - 4) * 100) / 100 != Math
					.floor(calibPitches[i] * 100) / 100)
				s += "\t -> " + calibPitches[i];
			s += "\n";
		}
		pitchText.setText(s);
	}

	public void setDefault() {
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 12; j++) {
				for (int k = 0; k < 10; k++) {
					int index = (int) (pitches[j] * Math.pow(2, i - 1)
							/ frequency * (blockSize * 2 + 1));
					if (index - k >= 0)
						calib_data[i][j][index - k] = 10 * Math.pow(0.7, k);
					if (index + k < blockSize + 1)
						calib_data[i][j][index + k] = 10 * Math.pow(0.7, k);
				}
			}
		}
	}
}
