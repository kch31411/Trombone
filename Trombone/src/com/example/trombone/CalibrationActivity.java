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
import android.widget.Button;
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
	Button startStopButton, buttonC, buttonD, buttonE, buttonF, 
		buttonG, buttonA, buttonB, buttonC2;
	boolean started = false;

	int sampleSize = 10;
	int sampleCount = 0;
	
	double[] pitches = { 523.25, 587.32, 659.25, 698.45,
			783.99, 880.00, 987.76, 1046.50, 1174.66 };
	String[] pitchName = { "C", "D", "E", "F", "G", "A", "B", "C6", "D6" };
	String[] musicSheet_code = { "C", "C", "G", "G", "A", "A", "G", "F", "F",
			"E", "E", "D", "D", "C", "end" };
	
	double calibPitches_sum = 0;
	int calibCount = 0;
	int calibTarget = 8;
	Button calibButton;
	double[] calibPitches = new double[8];
	
	int currentCount = 0;
	int currentError = 0;
	int currentPosition = 0;

	RecordAudio recordTask;

	ImageView currentSpec;
	Bitmap curBitmap, rec1Bitmap, rec2Bitmap;
	Canvas curCanvas, rec1Canvas, rec2Canvas;
	Paint paint;
	
	int width, height;
	int lastNoteIndex;
	int side_padding = 40;

	TextView resultText, debugText, pitchText;
	ProgressBar progress;
	
	double[][] toTransformSample = new double[blockSize * 2 + 1][sampleSize];

	ArrayList<Note> music_sheet;
	ArrayList<ImageView> noteViews;

	SQLiteDatabase calibDB;
	
	private void displayMusicSheet(int start) {
		FrameLayout l = (FrameLayout) findViewById(R.id.music_sheet);
		
		for (ImageView iv : noteViews) {
			l.removeView(iv);
		}
		noteViews.clear();
		
		// Display music sheet
		int note_index = start;
		int y = 0;
		int count = 0;
		while (count++ < 3) {
			if (note_index >= 0)
				note_index = DrawNotes(note_index, 150, y, music_sheet);
			if (note_index >= 0)
				note_index = DrawNotes(note_index,
						(int) ((width - side_padding) / 2) + 50, y, music_sheet);

			y += 150;
		}
		lastNoteIndex = note_index-1;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_calibration);
		
		// calibDB = openOrCreateDatabase("Calibration",MODE_WORLD_WRITEABLE, null);

		for(int i=0; i<8; i++)
		{
			calibPitches[i] = pitches[i];
		}
		progress = (ProgressBar) findViewById(R.id.ProgressBar);
				
		buttonC = (Button) findViewById(R.id.buttonC);
		buttonC.setBackgroundColor(Color.WHITE);
		calibButton = buttonC;
		buttonC.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				calibButton = buttonC;
				calibTarget = 0;
				calibNote();
			}
		});
		buttonD = (Button) findViewById(R.id.buttonD);
		buttonD.setBackgroundColor(Color.WHITE);
		buttonD.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				calibButton = buttonD;
				calibTarget = 1;
				calibNote();
			}
		});
		buttonE = (Button) findViewById(R.id.buttonE);
		buttonE.setBackgroundColor(Color.WHITE);
		buttonE.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				calibButton = buttonE;
				calibTarget = 2;
				calibNote();
			}
		});
		buttonF = (Button) findViewById(R.id.buttonF);
		buttonF.setBackgroundColor(Color.WHITE);
		buttonF.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				calibButton = buttonF;
				calibTarget = 3;
				calibNote();
			}
		});
		buttonG = (Button) findViewById(R.id.buttonG);
		buttonG.setBackgroundColor(Color.WHITE);
		buttonG.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				calibButton = buttonG;
				calibTarget = 4;
				calibNote();
			}
		});
		buttonA = (Button) findViewById(R.id.buttonA);
		buttonA.setBackgroundColor(Color.WHITE);
		buttonA.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				calibButton = buttonA;
				calibTarget = 5;
				calibNote();
			}
		});
		buttonB = (Button) findViewById(R.id.buttonB);
		buttonB.setBackgroundColor(Color.WHITE);
		buttonB.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				calibButton = buttonB;
				calibTarget = 6;
				calibNote();
			}
		});
		buttonC2 = (Button) findViewById(R.id.buttonC2);
		buttonC2.setBackgroundColor(Color.WHITE);
		buttonC2.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				calibButton = buttonC2;
				calibTarget = 7;
				calibNote();
			}
		});		
		// added
		resultText = (TextView) findViewById(R.id.resultText);
		debugText = (TextView) findViewById(R.id.debugText);
		pitchText = (TextView) findViewById(R.id.pitchText);
		showPitch();
		
		
		startStopButton = (Button) findViewById(R.id.StartStopButton);
		startStopButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if (started) {
					started = false;
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
		transformer = new RealDoubleFFT(blockSize * 2 + 1);

		paint = new Paint();
		paint.setColor(Color.GREEN);

		currentSpec = (ImageView) findViewById(R.id.CurrentSpectrum);
		curBitmap = Bitmap.createBitmap((int) 256, (int) 100,
				Bitmap.Config.ARGB_8888);
		curCanvas = new Canvas(curBitmap);
		currentSpec.setImageBitmap(curBitmap);

		noteViews = new ArrayList<ImageView>();
		 
		// temporary music sheet
		// TODO : consider beat
		// 4/4 beat. hak gyo jong E DDangDDANGADNAGDSNGADSf
		
		music_sheet = new ArrayList<Note>();
		
		music_sheet.add(new Note(-4));
		music_sheet.add(new Note(-3));
		music_sheet.add(new Note(-2));
		music_sheet.add(new Note(-1));
		music_sheet.add(new Note(0));
		music_sheet.add(new Note(1));
		music_sheet.add(new Note(2));
		music_sheet.add(new Note(3));
			
		// get dimension of device
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		width = size.x;
		height = size.y;
		height = height / 4; // added

		FrameLayout l = (FrameLayout) findViewById(R.id.music_sheet);

		musicSheet_code = new String[music_sheet.size()];
		for (int pt = 0; pt < music_sheet.size(); pt++) {
			Note note = music_sheet.get(pt);
			if (!note.isRest)
				musicSheet_code[pt] = pitchName[note.pitch + 4]; // /// 0 for G
			else
				musicSheet_code[pt] = "_";
		}

		Paint paint = new Paint();
		paint.setColor(Color.BLACK);

		// Display music sheet
		int y = 0;
		int count = 0;
		while (count++ < music_sheet.size()/8) {
			ImageView fiveLine = new ImageView(getBaseContext());
			Bitmap bitmap = Bitmap.createBitmap((int) width, (int) 150,
					Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			fiveLine.setImageBitmap(bitmap);

			for (int i = 20; i <= 100; i += 20)
				canvas.drawLine(side_padding, i, width - side_padding, i, paint);

			canvas.drawLine((int) ((width - side_padding) / 2), 20,
					(int) ((width - side_padding) / 2), 100, paint);
			canvas.drawLine(width - side_padding, 20, width - side_padding,
					100, paint);

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


	class Note {
		private int pitch; // temporary set 0 as G
		private int beat; // no saenggak yet
		private boolean isRest;
		public int x;
		public int y;

		// TODO : sharp? flat?

		public Note(int pitch, int beat, boolean rest) {
			this.pitch = pitch;
			this.beat = beat;
			this.isRest = rest;
			this.x = -100;
			this.y = -100;
		}

		public Note(int pitch, int beat) {
			this.pitch = pitch;
			this.beat = beat;
			this.isRest = false;
			this.x = -100;
			this.y = -100;
		}

		public Note(int pitch) {
			this.pitch = pitch;
			this.beat = 1;
			this.isRest = false;
			this.x = -100;
			this.y = -100;
		}

		public int getPitch() {
			return pitch;
		}

		public void setPitch(int pitch) {
			this.pitch = pitch;
		}

		public int getBeat() {
			return beat;
		}

		public void setBeat(int beat) {
			this.beat = beat;
		}

		public boolean isRest() {
			return isRest;
		}

		public void setRest(boolean isRest) {
			this.isRest = isRest;
		}
	}

	private int getNotePosition(Note note) {
		return note.getPitch() * -10 + 20;
	}

	class MusicSheet {
		// C major, G minor ...
		// overall beat
	}

	public int DrawNotes(int pt, int x, int y, ArrayList<Note> notes) {
		FrameLayout l = (FrameLayout) findViewById(R.id.music_sheet);

		int beatSum = 0;
		while (pt < notes.size()) {
			Note note = notes.get(pt++);
			beatSum += note.getBeat();

			if (beatSum > 4) {
				return pt - 1;
			}

			ImageView noteImage = new ImageView(getBaseContext());
			Bitmap bmNote;
			if (note.isRest) {
				bmNote = BitmapFactory.decodeResource(getResources(),
						R.drawable.rest_1);
			} else {
				switch (note.getBeat()) {
				case 1:
					bmNote = BitmapFactory.decodeResource(getResources(),
							R.drawable.note_1);
					break;
				case 2:
					bmNote = BitmapFactory.decodeResource(getResources(),
							R.drawable.note_2);
					break;
				case 3:
					bmNote = BitmapFactory.decodeResource(getResources(),
							R.drawable.note_3);
					break;
				case 4:
					bmNote = BitmapFactory.decodeResource(getResources(),
							R.drawable.note_4);
					break;
				default:
					bmNote = BitmapFactory.decodeResource(getResources(),
							R.drawable.note_1);
				}
			}

			noteImage.setImageBitmap(bmNote);
			noteImage.setLayoutParams(new LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			noteImage.setPadding(x, getNotePosition(note) + y, 0, 0);
			note.x = x;
			note.y = y;

			Matrix mNote = new Matrix();
			mNote.postScale((float) 0.5, (float) 0.5);
			noteImage.setScaleType(ScaleType.MATRIX);
			noteImage.setImageMatrix(mNote);

			l.addView(noteImage);
			noteViews.add(noteImage);

			x += 60 * note.getBeat();
		}
		return -1;
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
				// maxIntensity = Math.max(maxIntensity,
				// Math.abs(toTransform[0][i]));
			}

			while (musicSheet_code[currentPosition].equals("_"))
				currentPosition++;

			double MajorF = maxFrequency * frequency / (blockSize * 2 + 1);
			
			if(maxIntensity>5 && MajorF>pitches[calibTarget]-50 && 
				MajorF<pitches[calibTarget]+50 && calibCount<40){
				calibPitches_sum += MajorF;
				calibCount++;
				debugText.setText(calibCount+"");

				progress.setProgress(calibCount*100/40);	
				}
			
			if(calibCount>=40){
				started = false;
				startStopButton.setText("Start");
				calibButton.setBackgroundColor(Color.argb(0,0,0,0));
				calibPitches[calibTarget] = calibPitches_sum/calibCount;
				showPitch();
			}
			
			for (int i = 0; i < Magnitude.length; i++) {
				int x = i;
				int downy = (int) (100 - (Magnitude[i] * 10));
				int upy = 100;

				curCanvas.drawLine(x, downy, x, upy, paint);
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
		for (int i=0; i<8; i++)
		{
			s+=pitchName[i] + " : "+pitches[i];
			if(pitches[i]!=calibPitches[i])
				s+=" -> "+calibPitches[i];
			s+="\n";
		}
		pitchText.setText(s);
	}

}