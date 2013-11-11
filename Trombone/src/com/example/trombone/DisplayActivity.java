package com.example.trombone;

import java.util.ArrayList;

import com.example.trombone.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class DisplayActivity extends Activity {
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display);
		
		// temporary music sheet 
		// TODO : consider beat
		// 4/4 beat. hak gyo jong E  DDangDDANGADNAGDSNGADSf
		ArrayList<Note> music_sheet = new ArrayList<Note>();
		
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
		music_sheet.add(new Note(-2,2));
		
		music_sheet.add(new Note(0));
		music_sheet.add(new Note(-2));
		music_sheet.add(new Note(-3));
		music_sheet.add(new Note(-2));
		
		music_sheet.add(new Note(-4, 3));
		music_sheet.add(new Note(-1, 1, true));
		
		// get dimension of device
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;
		
		FrameLayout l = (FrameLayout)findViewById(R.id.music_sheet);
		
		Paint paint = new Paint();
		paint.setColor(Color.BLACK);
		
		// Display music sheet
		int note_index = 0;
		int y = 0;
		while (y < height) {
			ImageView fiveLine = new ImageView(getBaseContext());
			Bitmap bitmap = Bitmap.createBitmap((int)width, (int)150, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			fiveLine.setImageBitmap(bitmap);
			
			int side_padding = 40;
			for (int i=20; i<=100; i+=20)
				canvas.drawLine(side_padding, i, width-side_padding, i, paint);

			canvas.drawLine((int)((width-side_padding)/2), 20, (int)((width-side_padding)/2), 100, paint);
			canvas.drawLine(width-side_padding, 20, width-side_padding, 100, paint);
			
	        fiveLine.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
	        fiveLine.setPadding(0, y, 0, 0);
	        fiveLine.setScaleType(ScaleType.MATRIX);
	         
	        l.addView(fiveLine);
	        
	        
			ImageView clef = new ImageView(getBaseContext());
			Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.high);
			clef.setImageBitmap(bm);
			clef.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			clef.setPadding(side_padding, y, 0, 0);
	        
	        Matrix m = new Matrix();
	        m.postScale((float)0.24, (float)0.24);
	        clef.setScaleType(ScaleType.MATRIX);
	        clef.setImageMatrix(m); 
	         
	        l.addView(clef); 
	        
	        if (note_index >= 0)
	        	note_index = DrawNotes(note_index, 150, y, music_sheet);
	        if (note_index >= 0)
	        	note_index = DrawNotes(note_index, (int)((width-side_padding)/2)+50, y, music_sheet);
	        
	        y += 150;
		}
		

		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(R.id.fullscreen_content);

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, contentView,
				HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider
				.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
					// Cached values.
					int mControlsHeight;
					int mShortAnimTime;

					@Override
					@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
					public void onVisibilityChange(boolean visible) {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
							// If the ViewPropertyAnimator API is available
							// (Honeycomb MR2 and later), use it to animate the
							// in-layout UI controls at the bottom of the
							// screen.
							if (mControlsHeight == 0) {
								mControlsHeight = controlsView.getHeight();
							}
							if (mShortAnimTime == 0) {
								mShortAnimTime = getResources().getInteger(
										android.R.integer.config_shortAnimTime);
							}
							controlsView
									.animate()
									.translationY(visible ? 0 : mControlsHeight)
									.setDuration(mShortAnimTime);
						} else {
							// If the ViewPropertyAnimator APIs aren't
							// available, simply show or hide the in-layout UI
							// controls.
							controlsView.setVisibility(visible ? View.VISIBLE
									: View.GONE);
						}

						if (visible && AUTO_HIDE) {
							// Schedule a hide().
							delayedHide(AUTO_HIDE_DELAY_MILLIS);
						}
					}
				});

		// Set up the user interaction to manually show or hide the system UI.
		contentView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		});

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		findViewById(R.id.dummy_button).setOnTouchListener(
				mDelayHideTouchListener);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
	}

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			mSystemUiHider.hide();
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}
	
	class Note {
		private int pitch;  // temporary set 0 as G
		private int beat;   // no saenggak yet
		private boolean isRest;
		// TODO : sharp? flat?
		
		public Note(int pitch, int beat, boolean rest) {
			this.pitch = pitch;
			this.beat = beat;
			this.isRest = rest;
		}
		public Note(int pitch, int beat) {
			this.pitch = pitch;
			this.beat = beat;
			this.isRest = false;
		}
		public Note(int pitch) {
			this.pitch = pitch;
			this.beat = 1;
			this.isRest = false;
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
	
	public int DrawNotes(int pt, int x, int y, ArrayList<Note> notes)
	{
		FrameLayout l = (FrameLayout)findViewById(R.id.music_sheet);
		
		int beatSum = 0;
		while ( pt < notes.size() )
		{
			Note note = notes.get(pt++);
			beatSum += note.getBeat();
			
			if ( beatSum > 4 )
			{
				return pt - 1;
			}
			
			ImageView noteImage = new ImageView(getBaseContext());
			Bitmap bmNote;
			if (note.isRest) {
				bmNote = BitmapFactory.decodeResource(getResources(), R.drawable.rest_1);
			} else {
				switch (note.getBeat()) {
				case 1:
					bmNote = BitmapFactory.decodeResource(getResources(), R.drawable.note_1);
					break;
				case 2:
					bmNote = BitmapFactory.decodeResource(getResources(), R.drawable.note_2);
					break;
				case 3:
					bmNote = BitmapFactory.decodeResource(getResources(), R.drawable.note_3);
					break;
				case 4:
					bmNote = BitmapFactory.decodeResource(getResources(), R.drawable.note_4);
					break;
				default:
					bmNote = BitmapFactory.decodeResource(getResources(), R.drawable.note_1);
				}
			}
			
    		noteImage.setImageBitmap(bmNote);
    		noteImage.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    		noteImage.setPadding(x, getNotePosition(note)+y, 0, 0);
            
            Matrix mNote = new Matrix();
            mNote.postScale((float)0.5, (float)0.5);
            noteImage.setScaleType(ScaleType.MATRIX);
            noteImage.setImageMatrix(mNote); 
             
            l.addView(noteImage); 
            
			x += 60 * note.getBeat();
		}
		return -1;
	}
	
}
