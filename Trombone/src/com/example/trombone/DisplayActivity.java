package com.example.trombone;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
	private CheckForLongPress mPendingCheckForLongPress;
	private Handler mHandler = null;

	private RealDoubleFFT transformer;
	Button startStopButton;
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
	
	// drawing related.
	int side_padding = 40;
	int top_padding = 80;
	int space_five_line = 150;
	double dx;

	TextView resultText, debugText;

	double[][] toTransformSample = new double[blockSize * 2 + 1][sampleSize];

	MusicSheet music_sheet;
	ArrayList<ImageView> noteViews =  new ArrayList<ImageView>();

	boolean [] matches = new boolean[11];
	double [] scores = new double[11];
	double [] counters = new double[11];
	
	double tracking_velocity;
	double tracking_x;   // XXX : necessary???
	double tracking_y;
	
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
		dbhelper.addHistory(history);
		
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
		dx = ((nexus7_width - 2*side_padding - 100) / 2) / (double) bar_length; 
		
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
					
					Date temp = new Date();
					prevRecognitionTime = temp.getTime();
				}
			}
		});
		transformer = new RealDoubleFFT(blockSize * 2 + 1);
		
		// Capture musicsheet for preview
		String directoryPath = "/storage/emulated/0/DCIM/TROMBONE_PREVIEW/";
		File directory = new File(directoryPath);
		if ( !directory.isDirectory() )
			directory.mkdirs();
		
		Log.d("for captrue", "capture make directry");
		String previewPath = "/storage/emulated/0/DCIM/TROMBONE_PREVIEW/" + musicSheetId + ".png";
		File preview = new File(previewPath);
		if ( preview != null && preview.exists() )
		{
			Log.d("for captrue", "capture file exist");
		}
		else
		{
			Log.d("for captrue", "capture file does not exist");
			capturePreview();
		}
	}
	
	private void FeedbackVelocity(int prev, int curr) {
		Date temp = new Date();
		long currentRecognitionTime = temp.getTime();
		long deltaTime = currentRecognitionTime - prevRecognitionTime;

		double curr_x = trackingView.getX();
		trackingView.setX((float)(curr_x + tracking_velocity * dx * deltaTime));
		
		try {
			int total_beat = 0;
			for (int i = prev; i<curr; i++) {
				total_beat += music_sheet.getNote(pageNum, i).getBeat();
			}
			double modifiedVelocity = total_beat / deltaTime;
			
			tracking_velocity = tracking_velocity * 0.2 + modifiedVelocity * 0.8;
			prevRecognitionTime = currentRecognitionTime;
		} catch (Exception e) {
			tracking_velocity = 1/5000;
		}
	}
	
	private void capturePreview() {
		Log.d("for captrue", "capture capturePreview");
		FrameLayout screen = (FrameLayout) findViewById(R.id.music_sheet);
		screen.setDrawingCacheEnabled(true);
		screen.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
		screen.layout(0, 0, screen.getMeasuredWidth(), screen.getMeasuredHeight()/5*3); 
		screen.buildDrawingCache();
		
		Bitmap bm = Bitmap.createBitmap(screen.getDrawingCache());
		screen.destroyDrawingCache();
		screen.setDrawingCacheEnabled(false);
		
		savePreview(bm);
	}
	
	private void savePreview(Bitmap bm) {
		Log.d("for captrue", "capture savePreview");
		FileOutputStream stream;
		String directoryPath = "/storage/emulated/0/DCIM/TROMBONE_PREVIEW/";
		String path = musicSheetId + ".png";
		
		try {
			File file = new File(directoryPath, path);
			file.createNewFile();
			Log.d("for captrue", file.getAbsolutePath());
	
			stream = new FileOutputStream(file);
			bm.compress(Bitmap.CompressFormat.PNG, 100, stream);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	// set initial data
	private void initialize() {
		// touch handler
		mHandler = new Handler();
		mTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
		
		// spectrum
		currentSpec = (ImageView) findViewById(R.id.CurrentSpectrum);
		curBitmap = Bitmap.createBitmap((int) blockSize, (int) 100,
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
		FrameLayout l = (FrameLayout) findViewById(R.id.music_sheet);
		l.addView(trackingView);
				
		updatePage(1);	// first page

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
		
		tracking_velocity = 1 / 5000; // 珥덇린媛�	
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

		// Display music sheet
		int y = top_padding;
		int count = 0;
		while (count++ < 3) {
			ImageView fiveLine = new ImageView(getBaseContext());
			Bitmap bitmap = Bitmap.createBitmap((int) nexus7_width, (int) space_five_line,
					Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			fiveLine.setImageBitmap(bitmap);

			int startPosition = 19; 
			int interval = 20;
			if(ratio<1) interval = 22;

			for (int i = 0; i <5; i ++)
				canvas.drawLine(side_padding, startPosition+i*interval, 
						nexus7_width - side_padding, startPosition+i*interval, paint);

			canvas.drawLine((int) ((nexus7_width - side_padding + 140) / 2), startPosition,
					(int) ((nexus7_width - side_padding + 140) / 2), 
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
			y += space_five_line;
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
			mLastMotionY = event.getY();   // 占쎌쥙猷욑옙�용쐻占쎌늿��占쎌쥙猷욑옙�덊뒄 占쎌쥙猷욑옙�용쐻占쎌늿��
			mHasPerformedLongPress = false;   

			postCheckForLongClick(0);     //  Long click message 占쎌쥙猷욑옙�용쐻占쎌늿��
			break;

		case MotionEvent.ACTION_MOVE:
			final float x = event.getX();
			final float y = event.getY();
			final int deltaX = Math.abs((int) (mLastMotionX - x));
			final int deltaY = Math.abs((int) (mLastMotionY - y));

			// 占쎌쥙猷욑옙�용쐻占쎌늿��占쎌쥙猷욑옙�용쐻占쎌늿��占쎌쥙猷욑옙�용쐻占쎌꼶援뱄옙醫롫짗占쏙옙 占쎌쥙猷욑옙�용쐻占쎌늿�뺧옙醫묒삕
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
				// Long Click占쎌쥙猷욑옙占쏙㎗�뉖쐻占쎌늿�뺧옙醫롫짗占쎌눨�앾옙�덉굲 占쎌쥙�쀯옙��굲占쎌쥙猷욑옙�용쐻占쎌늿��占쎌쥙猷욑옙�용쐻占쎌늿�뺧옙醫롫짗占쏙옙
				removeLongPressCallback();

				// Short Click 筌ｌ꼪�앾옙�덉굲 占쎌쥙猷욑옙�됰뼖占쎌쥙猷욑옙占쏙옙醫롫짗占쎌눨�앾옙�밸퓠 占쎌쥙猷욑옙�용쐻占쎌늿�뺧옙醫롫짗占쏙옙占쎌쥙�숋옙�덈솇占쏙옙
				performOneClick(); 

			}

			break;

		default:
			break;
		}
		return super.onTouchEvent(event);
	}

	// Long Click占쎌쥙猷욑옙占쏙㎗�뉖쐻占쎌늿�뺧옙醫롫짗占쏙옙 Runnable 占쎌쥙�껓옙�덈솇占쏙옙 
	class CheckForLongPress implements Runnable {

		public void run() {
			if (performLongClick()) {
				mHasPerformedLongPress = true;
			}
		}
	}

	// Long Click 筌ｌ꼪�앾옙�덉굲 占쎌쥙猷욑옙�용쐻占쎌늿�뺧옙醫롫짗占쏙옙占쎌쥙猷욑옙�용쐻占쎌늿��占쎌쥙�껓옙�뚯굲 
	private void postCheckForLongClick(int delayOffset) {
		mHasPerformedLongPress = false;

		if (mPendingCheckForLongPress == null) {
			mPendingCheckForLongPress = new CheckForLongPress();
		}

		mHandler.postDelayed(mPendingCheckForLongPress,
				ViewConfiguration.getLongPressTimeout() - delayOffset);
		// 占쎌쥙猷욑옙�용쐻占썩뫗苑� 占쎌쥙�놅옙�덉굲占쎌쥙猷욑옙�용쐻占쎌늿�� getLongPressTimeout() 占쎌쥙�뉛옙紐꾩굲 message 占쎌쥙猷욑옙�용쐻占쎌늿�뺧옙醫롫뼣�ⓦ끉��占쎌쥙�놅옙�덈솇占쏙옙  
		// 占쎌쥙�ζ��쇱굲 delay占쎌쥙猷욑옙占쏙옙醫롫뼏占쎈챷�뺧옙醫롫짗占쏙옙占쎌쥙猷욑옙�뗣럹�좎뜴�앾옙�덉굲占쎌쥙�륅옙�뚯굲  占쎌쥙�뉛옙怨쀬굲占쎌쥙猷욑옙�용껜占쎌쥜�숋옙醫롫짗占쎌눨�앾옙�덉굲占쎌쥙猷욑옙�곸춷占쎌쥜�숋옙醫롫솁占쎈뜄�뗰옙占�	
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
					// TODO : another font
					memoView.setTextSize(30);  // TODO : get size as like opacity

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
	

	private void displayMusicSheet(int page) {
		FrameLayout l = (FrameLayout) findViewById(R.id.music_sheet);

		for (ImageView iv : noteViews) {
			l.removeView(iv);
		}
		noteViews.clear();

		// Display music sheet
		int note_index = 0;
		int y = top_padding;
		int count = 0;
		while (count++ < 3) {
			if (note_index >= 0)
				note_index = DrawNotes(note_index, side_padding + 100, y, music_sheet.getNotes(pageNum));
			if (note_index >= 0)
				note_index = DrawNotes(note_index,
						(int) ((nexus7_width - side_padding + 140) / 2), y, music_sheet.getNotes(pageNum));
			y += space_five_line;
		}
		lastNoteIndex = music_sheet.getNotes(pageNum).size()-1;
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

				int lineY = space_five_line;
				if(ratio<1) lineY+=7;
				if(ummY%2!=0||note.getPitch()==401) lineY += 6;

				for (int i=ummY; i<8; i+=2){				
					lineCanvas.drawLine(0, lineY, 30, lineY, paint);
					lineY -= 8;
					if(note.getPitch()==401) break;
				}
				lineImage.setLayoutParams(new LayoutParams(
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				lineImage.setPadding(x+5, getNotePosition(note)+y-100, 0, 0);

				l.addView(lineImage);
				noteViews.add(lineImage);
			}
			// noteCanvas.drawLine(0, 0, 0, 0, p);

			noteImage.setImageBitmap(bmNote);
			noteImage.setLayoutParams(new LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			noteImage.setPadding(x+10, getNotePosition(note) + y, 0, 0);
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
				int umm = note.getPitch()%100;
				if(umm!=2&&umm!=4&&umm!=7&&umm!=9&&umm!=11)
					bmA = BitmapFactory.decodeResource(getResources(),
							R.drawable.natural);
				else if (keyNumber<0)
					bmA = BitmapFactory.decodeResource(getResources(),
							R.drawable.flat);
				else
					bmA = BitmapFactory.decodeResource(getResources(),
							R.drawable.sharp);
												
				accidental.setImageBitmap(bmA);
				accidental.setLayoutParams(new LayoutParams(
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				accidental.setPadding(x-5, getNotePosition(note)+y+35, 0, 0);
				
				Matrix mA = new Matrix();
				mA.postScale((float) 0.17, (float) 0.17);
				accidental.setScaleType(ScaleType.MATRIX);
				accidental.setImageMatrix(mA);
				l.addView(accidental);
				noteViews.add(accidental); 
			}

			x += dx*note.getBeat();
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
			}

			Note currentNote = music_sheet.getNote(pageNum, currentPosition);
			int xMax = (int)(calibPitches[currentNote.getPitch()/100-3][currentNote.getPitch()%100-1]
					 / (frequency/(blockSize * 2 + 1)));
			paint.setColor(Color.argb(150,200,255,255));
			curCanvas.drawLine(xMax, 0, xMax, 100, paint);
			
			for (int i = 0; i < Magnitude.length; i++) {
				int x = i;
				int downy = (int) (100 - (calib_data[currentNote.getPitch()/100-3]
						[currentNote.getPitch()%100-1][i] * 10));
				int upy = 100;
				paint.setColor(Color.argb(80, 255, 10, 20));
				curCanvas.drawLine(x, downy, x, upy, paint);
			}
			
			while (music_sheet.getNote(pageNum, currentPosition).isRest())
				currentPosition++;
			
			String s = "";
			for (int j=0; j<scores.length; j++)
			{
				if(currentPosition-5+j>=0){
					Note tempNote = music_sheet.getNote(pageNum, currentPosition-5+j);
					
					double[] tempSpec = calib_data[tempNote.getPitch()/100-3][tempNote.getPitch()%100-1];
					double tempMaxF = calibPitches[tempNote.getPitch()/100-3][tempNote.getPitch()%100-1];
					int tempIdx = (int)Math.round(tempMaxF/(frequency/(blockSize*2+1)));
					
					double mag = 0;
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
					matches[j]= Math.abs(idx)<1 
							&& mag>tempSpec[tempIdx]*0.4 && !tempNote.isRest();
					s+= matches[j]? "t":"f" +"\t";
				}	
				else s+= "f\t";	
			}
			resultText.setText(s);
			
			if(!matches[5]&&matches[6]) currentPosition++;
			else if(!matches[5]&&matches[7]) currentPosition+=2;
			else if(!matches[5]&&matches[8]) currentPosition+=3;
			
			trackingView.setY(music_sheet.getNote(pageNum, currentPosition).y);  // XXX : tracking view should independent to curr position
			FeedbackVelocity(currentPosition - 1, currentPosition); // XXX : prev, curr
			debugText.setText(tracking_velocity+"");
			
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
			
			// tracking bar position
			trackingView.setX(music_sheet.getNote(pageNum, currentPosition).x);
			trackingView.setY(music_sheet.getNote(pageNum, currentPosition).y);
		}
	}
}
