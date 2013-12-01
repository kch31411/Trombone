package com.example.trombone;

import java.util.ArrayList;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
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
import android.os.Handler;
import android.text.Html;
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

import classes.Memo;
import classes.Note;
import db.DBMemoHelper;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class DisplayActivity extends Activity {
	// added
	int frequency = 8000*2;
	int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	
	// constants
	public static final int NUM_FREQUENCIES = 3;  // calibration 혹은 tracking 할 때 사용할 주파수의 개수
	public static final double[] SCORING_WEIGHT = {0.8, 0.5, 0.3};
	public static final double SCORE_THRESHOLD = 1.0;  // have to be adjusted. 
	
	// request code
	public static final int MEMO_ADD = 1;
	public static final int MEMO_MODIFY = 2;
	 
    // 시작 위치를 저장을 위한 변수 
    private float mLastMotionX = 0;
    private float mLastMotionY = 0;
    // 마우스 move 로 일정범위 벗어나면 취소하기 위한 값
    private int mTouchSlop;
 
    // long click을 위한 변수들 
    private boolean mHasPerformedLongPress;
    private CheckForLongPress mPendingCheckForLongPress;
 
    private Handler mHandler = null;

	private RealDoubleFFT transformer;
	int blockSize = 256;
	Button startStopButton;
	boolean started = false;
	int sampleSize = 10;
	int sampleCount = 0;

	double[] pitches = { 523.25, 587.32 - 25, 659.25, 698.45 - 10,
			783.99 - 35, 880.00 - 40, 987.76 - 90, 1046.50 - 15, 1174.66 };
	String[] pitchName = { "C", "D", "E", "F", "G", "A", "B", "C6", "D6" };
	String[] musicSheet_code = { "C", "C", "G", "G", "A", "A", "G", "F", "F",
			"E", "E", "D", "D", "C", "end" };
	
	// XXX
	double[][] calibratedPitches = new double[9][NUM_FREQUENCIES];
	boolean isCalibrated = false;

	int currentCount = 0;
	int currentError = 0;
	int currentPosition = 0;

	RecordAudio recordTask;

	ImageView trackingView;
	ImageView currentSpec;
	Bitmap curBitmap, rec1Bitmap, rec2Bitmap;
	Canvas curCanvas, rec1Canvas, rec2Canvas;
	Paint paint;
	
	int width, height;
	int lastNoteIndex;
	int side_padding = 40;

	TextView resultText, debugText;

	double[][] toTransformSample = new double[blockSize * 2 + 1][sampleSize];

	ArrayList<Note> music_sheet;
	ArrayList<ImageView> noteViews;

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
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);  
	    //set up full screen
	    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
	                WindowManager.LayoutParams.FLAG_FULLSCREEN);  
		
		setContentView(R.layout.activity_display);

		// added
		resultText = (TextView) findViewById(R.id.resultText);
		debugText = (TextView) findViewById(R.id.debugText);

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
		
		music_sheet = new ArrayList<Note>();
		
		music_sheet.add(new Note(-1));
		music_sheet.add(new Note(0));
		music_sheet.add(new Note(1));
		music_sheet.add(new Note(-1));
		music_sheet.add(new Note(3,3));
		music_sheet.add(new Note(1));
		
		music_sheet.add(new Note(0,2));
		music_sheet.add(new Note(3,2));
		music_sheet.add(new Note(0,2));
		music_sheet.add(new Note(-1));
		music_sheet.add(new Note(-3));
		music_sheet.add(new Note(1,3));
		music_sheet.add(new Note(-1));
		music_sheet.add(new Note(-2,3));
		music_sheet.add(new Note(-1, 1, true));
		
		music_sheet.add(new Note(-1));
		music_sheet.add(new Note(-2));
		music_sheet.add(new Note(-3,2));
		music_sheet.add(new Note(-2,2));
		music_sheet.add(new Note(-1));
		music_sheet.add(new Note(0));
		music_sheet.add(new Note(-4,2));
		music_sheet.add(new Note(-1,2));
		music_sheet.add(new Note(0));
		music_sheet.add(new Note(1));
		
		music_sheet.add(new Note(2,2));
		music_sheet.add(new Note(2));
		music_sheet.add(new Note(1));
		music_sheet.add(new Note(0));
		music_sheet.add(new Note(-1));
		music_sheet.add(new Note(0,3));
		music_sheet.add(new Note(-1, 1, true));
		
		music_sheet.add(new Note(-1));
		music_sheet.add(new Note(0));
		music_sheet.add(new Note(-1, 1, true));	

		/* school bell dangdangdang
		 * 4/4 beat. hak gyo jong E DDangDDANGADNAGDSNGADSf
		 *
		music_sheet.add(new Note(0));
		music_sheet.add(new Note(0));
		music_sheet.add(new Note(1));
		music_sheet.add(new Note(1));

		music_sheet.add(new Note(0));
		music_sheet.add(new Note(0));
		music_sheet.add(new Note(-2, 2));

		music_sheet.add(new Note(0));
		music_sheet.add(new Note(0));
		music_sheet.add(new Note(-2));
		music_sheet.add(new Note(-2));

		music_sheet.add(new Note(-3, 3));
		music_sheet.add(new Note(-1, 1, true));

		music_sheet.add(new Note(0));
		music_sheet.add(new Note(0));
		music_sheet.add(new Note(1));
		music_sheet.add(new Note(1));

		music_sheet.add(new Note(0));
		music_sheet.add(new Note(0));
		music_sheet.add(new Note(-2, 2));

		music_sheet.add(new Note(0));
		music_sheet.add(new Note(-2));
		music_sheet.add(new Note(-3));
		music_sheet.add(new Note(-2));

		music_sheet.add(new Note(-4, 3));
		music_sheet.add(new Note(-1, 1, true));
		*/

		// get dimension of device
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		width = size.x;
		height = size.y;
		height = height / 2; // added

		FrameLayout l = (FrameLayout) findViewById(R.id.music_sheet);

		musicSheet_code = new String[music_sheet.size()];
		for (int pt = 0; pt < music_sheet.size(); pt++) {
			Note note = music_sheet.get(pt);
			if (!note.isRest())
				musicSheet_code[pt] = pitchName[note.getPitch() + 4]; // /// 0 for G
			else
				musicSheet_code[pt] = "_";
		}

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
		

        mHandler = new Handler();
 
        mTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
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
 
 
    /**
     * Remove the longpress detection timer.
     * 중간에 취소하는 용도입니다.
     */
    private void removeLongPressCallback() {
        if (mPendingCheckForLongPress != null) {
            mHandler.removeCallbacks(mPendingCheckForLongPress);
        }
    }
 
    public boolean performLongClick() {
    	Intent foo = new Intent(this, TextEntryActivity.class);
    	foo.putExtra("value", "");
    	this.startActivityForResult(foo, MEMO_ADD);
    	
        return true;
    }
     
    private void performOneClick() {
    	// do nothing.
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
                    		opacity, 1, value, 1); // XXX : to be fixed : page, musicsheet id, id
                    DBMemoHelper helper = new DBMemoHelper(this);
                    int id = (int) helper.addMemo(memo);
                    memo.setId(id);
                    
                    memoView.setOnClickListener(new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
					    	Intent foo = new Intent(DisplayActivity.this, TextEntryActivity.class);
					    	foo.putExtra("value", ((TextView) v).getText().toString());
					    	DisplayActivity.this.startActivityForResult(foo, MEMO_MODIFY);
						}
					});
                    
                    f.addView(memoView);
                }
            } catch (Exception e) {
            }
            break;
        case MEMO_MODIFY:
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
			if (note.isRest()) {
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
			debugText.setText(f2note(MajorF) + " : " + MajorF + " "
					+ currentCount);

			// tracking
			if (!isCalibrated) {
				if (maxIntensity < 5)
					currentError++;
				else if (currentError > 1
						&& currentCount > 3
						&& musicSheet_code[currentPosition + 1]
								.equals(f2note(MajorF))) {
					currentPosition++;
					currentCount = 0;
					currentError = 0;
	
				} else if (musicSheet_code[currentPosition].equals(f2note(MajorF))) {
					currentCount++;
					currentError = 0;
				} 
				else if (currentError>2 && currentCount>5 
						&& musicSheet_code[currentPosition + 1]
								.equals("_"))
				{
					currentPosition++;
					currentCount = 0;
					currentError = 0;
				}
				else {
					currentError++;
				}
			} else {
				double score = 0;
				
			}

			trackingView.setX(music_sheet.get(currentPosition).x-5);
			trackingView.setY(music_sheet.get(currentPosition).y+80);
			
			if (lastNoteIndex >= 0 && currentPosition >= lastNoteIndex) {
				displayMusicSheet(lastNoteIndex+1);
			}

			String musicShow = "<font color='#000000'>";
			for (int i = 0; i < currentPosition; i++) {
				musicShow += musicSheet_code[i];
			}
			musicShow += "</font> <font color = #FF8080>"
					+ musicSheet_code[currentPosition]
							+ "</font> <font color='#000000'>";
			for (int i = currentPosition + 1; i < musicSheet_code.length - 1; i++) {
				musicShow += musicSheet_code[i];
			}
			musicShow += "</font>";
			resultText.setText(Html.fromHtml(musicShow));

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

		private String f2note(double frequency) {
			for (int i = 1; i < pitches.length; i++) {
				if (pitches[i] > frequency) {
					if ((pitches[i] - frequency) < (pitches[i] - pitches[i - 1]) / 2)
						return pitchName[i];
					else if (i >= 1)
						return pitchName[i - 1];
					else
						return "out of range1";
				}
			}
			return "out of range2";
		}
	}

}