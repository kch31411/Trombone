package com.example.trombone;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

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
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
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

public class DisplayActivity extends Activity {
	// music sheet information
	private int musicSheetId;
	private int calibId;
	private int pageNum = 1;  // TODO : page related works
	int lastNoteIndex;
	int currentPosition = 0;
	
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
	private CheckForLongPress mPendingCheckForLongPress;
	private Handler mHandler = null;

	private RealDoubleFFT transformer;
	Button startStopButton;
	boolean started = false;
	int sampleSize = 10;
	int sampleCount = 0;

	double[][][] calib_data = new double[3][12][blockSize + 1]; // 3,4,5 octave

	double[] ref_pitches;

	int currentCount = 0;
	int currentError = 0;

	RecordAudio recordTask;

	// TODO : collect view variables in here.
	TextView pageNumView;
	ImageView trackingView;
	ImageView currentSpec;
	Bitmap curBitmap, rec1Bitmap, rec2Bitmap;
	Canvas curCanvas, rec1Canvas, rec2Canvas;
	Paint paint = new Paint();

	int width, height;
	float ratio = 1;
	int bar_length = 12; 
	int keyNumber = 0;
	
	int[] yPositions = yPosition;
	
	int side_padding = 40;

	TextView resultText, debugText;

	double[][] toTransformSample = new double[blockSize * 2 + 1][sampleSize];

	MusicSheet music_sheet;
	ArrayList<ImageView> noteViews =  new ArrayList<ImageView>();

	@Override
	protected void onStop(){
		if (started) {
			started = false;
			recordTask.cancel(true);			
		}
		
		// XXX : This is temporary.
		// This history construction must be done when play is end.
		Date cDate = new Date();
		String fDate = new SimpleDateFormat("yyyy-MM-dd").format(cDate);
		
		Random random = new Random();
		History history = new History(-1, fDate, random.nextInt(100), musicSheetId);
		
		super.onStop();
	}

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);  
		//set up full screen
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_display);

		initialize();

		keyNumber = music_sheet.getKeyNumber();
		if(keyNumber<0)
			yPositions=yPosition_flat;
		bar_length = music_sheet.getBeat();		
		
		drawBackground();
		displayMusicSheet(pageNum);
		
		// start button
		startStopButton = (Button) findViewById(R.id.StartStopButton);
		startStopButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if (started) {
					started = false;
					startStopButton.setText("Start");
					recordTask.cancel(true);
					currentPosition = 0;
					currentCount = 0;
					currentError = 0;
				} else {
					started = true;
					startStopButton.setText("Stop");
					recordTask = new RecordAudio();
					recordTask.execute();		// thread call
				}
			}
		});
		transformer = new RealDoubleFFT(blockSize * 2 + 1);
	}
	
	private void displayMusicSheet(int page) {
		FrameLayout l = (FrameLayout) findViewById(R.id.music_sheet);

		for (ImageView iv : noteViews) {
			l.removeView(iv);
		}
		noteViews.clear();

		// Display music sheet
		int note_index = 0;
		int y = 0;
		int count = 0;
		while (count++ < 3) {
			if (note_index >= 0)
				note_index = DrawNotes(note_index, 120, y, music_sheet.getNotes(pageNum));
			if (note_index >= 0)
				note_index = DrawNotes(note_index,
						(int) ((nexus7_width - side_padding)/2+60), y, music_sheet.getNotes(pageNum));
			y += 150;
		}
		lastNoteIndex = music_sheet.getNotes(pageNum).size()-1;
	}

	
	// set initial data
	private void initialize() {
		// touch handler
		mHandler = new Handler();
		mTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
		
		// spectrum
		currentSpec = (ImageView) findViewById(R.id.CurrentSpectrum);
		curBitmap = Bitmap.createBitmap((int) 256, (int) 100,
				Bitmap.Config.ARGB_8888);
		curCanvas = new Canvas(curBitmap);
		currentSpec.setImageBitmap(curBitmap);
		
		// view binding
		resultText = (TextView) findViewById(R.id.resultText);
		debugText = (TextView) findViewById(R.id.debugText);
		pageNumView = (TextView) findViewById(R.id.page_number);

		// set music sheet
		musicSheetId = getIntent().getIntExtra("musicsheet_id", -1); 
		music_sheet = dbhelper.getMusicSheet(musicSheetId);
		TextView titleView = (TextView) findViewById(R.id.music_sheet_title);
		titleView.setText(music_sheet.getName());

		updatePage(1);	// first page

		// set calibration data
		calibId = getIntent().getIntExtra("calib_id2play", -1);
		try {
			CalibrationData cd = dbhelper.getCalibrationData(calibId);    			
			FileInputStream fis = new FileInputStream(cd.getFile_path());
			ObjectInputStream iis = new ObjectInputStream(fis);
			calib_data = (double[][][]) iis.readObject();
			iis.close();
		}catch (Exception e) {
			Log.d("ccccc", "exception : " + e.toString());
		} 
		
	}
	
	private void drawBackground() {
		// scale layout for multiple devices
		// get dimension of device
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		width = size.x;
		height = size.y;

		getWindow().setLayout(nexus7_width, nexus7_height);

		FrameLayout mainView = (FrameLayout)
				findViewById(R.id.music_sheet_background);

		ratio = (float)width/nexus7_width;

		if (ratio < 1) {
			mainView.setScaleX((float) ratio);
			mainView.setScaleY((float) ratio);
			mainView.setPivotX(0.0f);
			mainView.setPivotY(0.0f);
		}
		
		// now draw background
		FrameLayout l = (FrameLayout) findViewById(R.id.music_sheet);

		Paint paint = new Paint();
		paint.setColor(Color.BLACK);

		// tracking bar
		trackingView = new ImageView(getBaseContext());
		Bitmap trackingBm = Bitmap.createBitmap((int) 40, (int) 120,
				Bitmap.Config.ARGB_8888);
		Canvas trackingCanvas = new Canvas(trackingBm);
		trackingView.setImageBitmap(trackingBm);

		trackingCanvas.drawColor(Color.LTGRAY);

		trackingView.setLayoutParams(new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		trackingView.setScaleType(ScaleType.MATRIX);

		l.addView(trackingView);
		
		// Display music sheet
		int y = 0;
		int count = 0;
		while (count++ < 3) {
			ImageView fiveLine = new ImageView(getBaseContext());
			Bitmap bitmap = Bitmap.createBitmap((int) nexus7_width, (int) 150,
					Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			fiveLine.setImageBitmap(bitmap);

			int startPosition = 19; 
			int interval = 20;
			if(ratio<1) interval = 22;

			for (int i = 0; i <5; i ++)
				canvas.drawLine(side_padding, startPosition+i*interval, 
						nexus7_width - side_padding, startPosition+i*interval, paint);

			canvas.drawLine((int) ((nexus7_width - side_padding + 100) / 2), startPosition,
					(int) ((nexus7_width - side_padding + 100) / 2), 
					startPosition+4*interval, paint);

			canvas.drawLine(nexus7_width - side_padding, startPosition, 
					nexus7_width - side_padding, 
					startPosition+4*interval, paint);

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
			
			if(keyNumber<0)
			{
				int[] flat_position = {7,10,6,9,5,8,4};
				for (int i=0; i<(keyNumber*-1) && i<7; i++)
				{
					ImageView iv = new ImageView(getBaseContext());
					Bitmap btm = BitmapFactory.decodeResource(getResources(),
							R.drawable.flat);
					iv.setImageBitmap(btm);
					iv.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
							LayoutParams.WRAP_CONTENT));
					iv.setPadding(side_padding+8*i+50, y+flat_position[i]*-10 +105, 0, 0);

					Matrix mat = new Matrix();
					mat.postScale((float) 0.17, (float) 0.17);
					iv.setScaleType(ScaleType.MATRIX);
					iv.setImageMatrix(mat);

					l.addView(iv);
				}
				
			}
			else if(keyNumber>0)
			{
				int[] sharp_position = {11,8,12,9,6,10,7};
				for (int i=0; i<(keyNumber) && i<7; i++)
				{
					ImageView iv = new ImageView(getBaseContext());
					Bitmap btm = BitmapFactory.decodeResource(getResources(),
							R.drawable.sharp);
					iv.setImageBitmap(btm);
					iv.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
							LayoutParams.WRAP_CONTENT));
					iv.setPadding(side_padding+8*i+50, y+sharp_position[i]*-10 +110, 0, 0);

					Matrix mat = new Matrix();
					mat.postScale((float) 0.14, (float) 0.14);
					iv.setScaleType(ScaleType.MATRIX);
					iv.setImageMatrix(mat);

					l.addView(iv);
				}
			}
			y += 150;
		}
	}
	
	private void showMemos(ArrayList<Memo> memos) {
		for (Memo memo : memos) {
			FrameLayout f = (FrameLayout) findViewById(R.id.music_sheet_background);

			TextView memoView = new TextView(this);
			memoView.setText(memo.getContent());
			memoView.setLayoutParams(new LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			memoView.setX(memo.getX());
			memoView.setY(memo.getY());
			memoView.setTextColor(Color.argb(memo.getOpacity(), 255, 0, 0));
			memoView.setTextSize(30);
			
			memo.setTv(memoView);
			
			memoView.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					selectedMemo = (TextView) v;
					
					Intent foo = new Intent(DisplayActivity.this, TextEntryActivity.class);
					foo.putExtra("value", ((TextView) v).getText().toString());
					foo.putExtra("deletable", true);
					DisplayActivity.this.startActivityForResult(foo, MEMO_MODIFY);
				}
			});

			f.addView(memoView);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		switch (event.getAction()) {

		case MotionEvent.ACTION_DOWN:
			mLastMotionX = event.getX();
			mLastMotionY = event.getY();   // 시작 위치 저장

			mHasPerformedLongPress = false;   

			postCheckForLongClick(0);     //  Long click message 설정

			break;

		case MotionEvent.ACTION_MOVE:
			final float x = event.getX();
			final float y = event.getY();
			final int deltaX = Math.abs((int) (mLastMotionX - x));
			final int deltaY = Math.abs((int) (mLastMotionY - y));

			// 일정 범위 벗어나면  취소함
			if (deltaX >= mTouchSlop || deltaY >= mTouchSlop) {
				if (!mHasPerformedLongPress) {
					// This is a tap, so remove the longpress check
					removeLongPressCallback();
				}
			}

			break;

		case MotionEvent.ACTION_CANCEL:
			if (!mHasPerformedLongPress) {
				// This is a tap, so remove the longpress check
				removeLongPressCallback();
			}
			break;

		case MotionEvent.ACTION_UP:
			if (!mHasPerformedLongPress) {
				// Long Click을 처리되지 않았으면 제거함.
				removeLongPressCallback();

				// Short Click 처리 루틴을 여기에 넣으면 됩니다.
				performOneClick(); 

			}

			break;

		default:
			break;
		}
		return super.onTouchEvent(event);
	}

	// Long Click을 처리할  Runnable 입니다. 
	class CheckForLongPress implements Runnable {

		public void run() {
			if (performLongClick()) {
				mHasPerformedLongPress = true;
			}
		}
	}

	// Long Click 처리 설정을 위한 함수 
	private void postCheckForLongClick(int delayOffset) {
		mHasPerformedLongPress = false;

		if (mPendingCheckForLongPress == null) {
			mPendingCheckForLongPress = new CheckForLongPress();
		}

		mHandler.postDelayed(mPendingCheckForLongPress,
				ViewConfiguration.getLongPressTimeout() - delayOffset);
		// 여기서  시스템의  getLongPressTimeout() 후에 message 수행하게 합니다.  
		// 추가 delay가 필요한 경우를 위해서  파라미터로 조절가능하게 합니다.
	}


	private void removeLongPressCallback() {
		if (mPendingCheckForLongPress != null) {
			mHandler.removeCallbacks(mPendingCheckForLongPress);
		}
	}

	public boolean performLongClick() {
		Intent foo = new Intent(this, TextEntryActivity.class);
		foo.putExtra("value", "");
		foo.putExtra("deletable", false);
		this.startActivityForResult(foo, MEMO_ADD);

		return true;
	}

	private void performOneClick() {
		// do nothing
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case MEMO_ADD:
			try {
				String value = data.getStringExtra("value");
				int opacity = data.getIntExtra("opacity", 255);

				if (value != null && value.length() > 0) {
					FrameLayout f = (FrameLayout) findViewById(R.id.music_sheet_background);

					TextView memoView = new TextView(this);
					memoView.setText(value);
					memoView.setLayoutParams(new LayoutParams(
							LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
					memoView.setX(mLastMotionX);
					memoView.setY(mLastMotionY);
					memoView.setTextColor(Color.argb(opacity, 255, 0, 0));
					memoView.setTextSize(30);

					Memo memo = new Memo(-1, mLastMotionX, mLastMotionY,
							opacity, pageNum, value, musicSheetId, memoView);
					int id = (int) dbhelper.addMemo(memo);
					memo.setId(id);
					
					memoList.add(memo);
					
					memoView.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View v) {
							selectedMemo = (TextView) v;
							
							Intent foo = new Intent(DisplayActivity.this, TextEntryActivity.class);
							foo.putExtra("value", ((TextView) v).getText().toString());
							foo.putExtra("deletable", true);
							DisplayActivity.this.startActivityForResult(foo, MEMO_MODIFY);
						}
					});

					f.addView(memoView);
				}
			} catch (Exception e) {
			}
			break;
		case MEMO_MODIFY:
			try {
				boolean deleted = data.getBooleanExtra("delete", false);
				String value = data.getStringExtra("value");
				int opacity = data.getIntExtra("opacity", 255);
				FrameLayout f = (FrameLayout) findViewById(R.id.music_sheet_background);
				
				if (deleted) {
					// update memo DB
					for (Memo memo : memoList) {
						if (memo.getTv().equals(selectedMemo)) {
							dbhelper.deleteMemo(memo);
							break;
						}
					}
					f.removeView(selectedMemo);
				} else if (value != null && value.length() > 0) {

					// update display
					selectedMemo.setText(value);
					selectedMemo.setTextColor(Color.argb(opacity, 255, 0, 0));

					// update memo DB
					for (Memo memo : memoList) {
						if (memo.getTv().equals(selectedMemo)) {
							memo.setContent(value);
							memo.setOpacity(opacity);
							dbhelper.updateMemo(memo);
							break;
						}
					}
					
				}
			} catch (Exception e) {
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

	private int getNotePosition(Note note) {
		int umm = note.getPitch()%100;
		int oct = note.getPitch()/100;

		int note_height = yPositions[umm-1] * -10 + 60;

		if(oct==5) note_height-=70;
		if(oct==4&&umm==1) note_height-=10;
		if(oct<=3) {
			note_height-=60;
			note_height/=2;
			note_height+=90;
		}

		return note_height;
	}

	public int DrawNotes(int pt, int x, int y, ArrayList<Note> notes) {
		FrameLayout l = (FrameLayout) findViewById(R.id.music_sheet);

		int beatSum = 0;
		while (pt < notes.size()) {
			Note note = notes.get(pt++);
			beatSum += note.getBeat();

			if (beatSum > bar_length) {
				return pt - 1;
			}

			ImageView noteImage = new ImageView(getBaseContext());
			Bitmap bmNote;
			if (note.isRest()) {
				switch (note.getBeat()){
				case 4:
					bmNote = BitmapFactory.decodeResource(getResources(),
							R.drawable.rest_4);
					break;
				case 8:
					bmNote = BitmapFactory.decodeResource(getResources(),
							R.drawable.rest_8);
					break;
				case 16:
					bmNote = BitmapFactory.decodeResource(getResources(),
							R.drawable.rest_16);
					break;
				default:
					bmNote = BitmapFactory.decodeResource(getResources(),
							R.drawable.rest_4);
				}
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
				case 6:
					bmNote = BitmapFactory.decodeResource(getResources(),
							R.drawable.note_6);
					break;
				case 8:
					bmNote = BitmapFactory.decodeResource(getResources(),
							R.drawable.note_8);
					break;
				case 12:
					bmNote = BitmapFactory.decodeResource(getResources(),
							R.drawable.note_12);
					break;
				case 16:
					bmNote = BitmapFactory.decodeResource(getResources(),
							R.drawable.note_16);
					break;
				default:
					bmNote = BitmapFactory.decodeResource(getResources(),
							R.drawable.note_1);
				}
			}

			paint.setColor(Color.BLACK);
			paint.setStrokeWidth(2f);
			if (note.getPitch() / 100 < 4 || note.getPitch()==401 ) {
				int ummY = yPositions[note.getPitch()%100-1];
				ImageView lineImage = new ImageView(getBaseContext());
				Bitmap bmLine = Bitmap.createBitmap((int) 80, (int) 180,
						Bitmap.Config.ARGB_8888);
				Canvas lineCanvas = new Canvas(bmLine);
				lineImage.setImageBitmap(bmLine); 

				int lineY = 150;
				if(ratio<1) lineY+=7;
				if(ummY%2!=0||note.getPitch()==401) lineY += 6;

				for (int i=ummY; i<8; i+=2){				
					lineCanvas.drawLine(0, lineY, 30, lineY, paint);
					lineY -= 8;
					if(note.getPitch()==401) break;
				}
				lineImage.setLayoutParams(new LayoutParams(
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				lineImage.setPadding(x-5, getNotePosition(note)+y-100, 0, 0);

				l.addView(lineImage);
				noteViews.add(lineImage);
			}
			// noteCanvas.drawLine(0, 0, 0, 0, p);

			noteImage.setImageBitmap(bmNote);
			noteImage.setLayoutParams(new LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			noteImage.setPadding(x+60, getNotePosition(note) + y, 0, 0);
			note.x = x;
			note.y = y;

			Matrix mNote = new Matrix();
			mNote.postScale((float) 0.5, (float) 0.5);
			noteImage.setScaleType(ScaleType.MATRIX);
			noteImage.setImageMatrix(mNote);

			l.addView(noteImage);
			noteViews.add(noteImage);

			if (note.isAccidental()) { 
				ImageView accidental = new ImageView(getBaseContext());
				Bitmap bmA;
				
				if (keyNumber<0)
					bmA = BitmapFactory.decodeResource(getResources(),
							R.drawable.flat);
				else
					bmA = BitmapFactory.decodeResource(getResources(),
							R.drawable.sharp);
												
				accidental.setImageBitmap(bmA);
				accidental.setLayoutParams(new LayoutParams(
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				accidental.setPadding(x+40, getNotePosition(note)+y+35, 0, 0);
				
				Matrix mA = new Matrix();
				mA.postScale((float) 0.17, (float) 0.17);
				accidental.setScaleType(ScaleType.MATRIX);
				accidental.setImageMatrix(mA);
				l.addView(accidental);
				noteViews.add(accidental); 
			}

			x += (int)(14*note.getBeat()/((double)bar_length)*16) ;
		}
		return -1;
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

			while (music_sheet.getNote(pageNum, currentPosition).isRest())
				currentPosition++;

			double MajorF = maxFrequency * frequency / (blockSize * 2 + 1);
			
			Note nextNote = music_sheet.getNote(pageNum, currentPosition+1);
			Note currentNote = music_sheet.getNote(pageNum, currentPosition);

			double errorCurrent = 0;
			double errorNext = 0;
			debugText.setText((currentNote.getPitch()/100-3)+" "+(currentNote.getPitch()%100-1));
			for (int i = 0; i < Magnitude.length; i++) {
				int x = i;
				int downy = (int) (100 - (calib_data[currentNote.getPitch()/100-3]
						[currentNote.getPitch()%100-1][i] * 10));
				int upy = 100;
				paint.setColor(Color.argb(150, 255, 10, 20));
				curCanvas.drawLine(x, downy, x, upy, paint);
				
				double spec_current = calib_data[currentNote.getPitch()/100-3]
						[currentNote.getPitch()%100-1][i];
				double spec_next = calib_data[nextNote.getPitch()/100-3]
						[nextNote.getPitch()%100-1][i];
				
				if (spec_current * 0.8 > Magnitude[i])
					errorCurrent += spec_current * 0.8 - Magnitude[i];
				if (spec_next * 0.8 > Magnitude[i])
					errorNext += spec_current * 0.8 - Magnitude[i];
			}
			resultText.setText(errorCurrent + " " + errorNext);
			
			//debugText.setText(currentCount+"");

			if (maxIntensity < 5)
				currentError++;
			else if (currentError > 3
					&& currentCount > 3
					&& errorNext<150){ // MajorF<pitch2frequency(nextNote.getPitch())*1.04
					//&&  MajorF>pitch2frequency(nextNote.getPitch())/1.04) {
				currentPosition++;
				currentCount = 0;
				currentError = 0;

			} else if (errorCurrent<150){ // (MajorF<pitch2frequency(currentNote.getPitch())*1.04
					//&& MajorF>pitch2frequency(currentNote.getPitch())/1.04) {
				currentCount++;
				currentError = 0;
			} 
			else if (currentError>2 && currentCount>5 
					&& nextNote.isRest())
			{
				currentPosition++;
				currentCount = 0;
				currentError = 0;
			}
			else {
				currentError++;
			}

			trackingView.setX(music_sheet.getNote(pageNum, currentPosition).x+55);
			trackingView.setY(music_sheet.getNote(pageNum, currentPosition).y);

			if (lastNoteIndex >= 0 && currentPosition >= lastNoteIndex) {
				// turn to next page.
				updatePage(pageNum + 1);
			}

			for (int i = 0; i < Magnitude.length; i++) {
				int x = i;
				int downy = (int) (100 - (Magnitude[i] * 10));
				int upy = 100;
				paint.setColor(Color.rgb(250, 100, 255));
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
	
	public double pitch2frequency(int in_pitch){
		int oct = in_pitch/100;
		int umm = in_pitch%100;
		return ref_pitches[umm-1]*Math.pow(2,(oct-4)); 
	}
	
	private void updatePage(int page) {
		if (page > music_sheet.getPages() || page <= 0)
			Log.d("Warning", "unexpected page : " + page);
		else {
			pageNum = page;
			
			// show page number on the bottom
			pageNumView.setText(Integer.toString(page));

			// show existing memos
			memoList = music_sheet.getMemos(page);
			showMemos(memoList);
			
			// update notes
			noteList = music_sheet.getNotes(page);
			displayMusicSheet(page);
			
			// initialize current playing position as 0
			currentPosition = 0;
		}
	}
}
