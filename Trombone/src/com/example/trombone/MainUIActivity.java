package com.example.trombone;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.example.trombone.util.SystemUiHider;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class MainUIActivity extends Activity {
	public final static int CALIBRATION = 3;
	
	private int calib_id = 1;
	
	double[] pitches = {261.626, 277.183, 293.665, 311.127, 329.628, 
			349.228, 369.994, 391.995, 415.305, 440.000, 466.164,
			493.883};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);  
	    //set up full screen
	    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
	                WindowManager.LayoutParams.FLAG_FULLSCREEN);  

		setContentView(R.layout.activity_main_ui);

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		Button playBtnCall = (Button)findViewById(R.id.playbutton);
		playBtnCall.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				Log.d("ww","Click");
				Intent intent = new Intent(MainUIActivity.this, MusicsheetSelectActivity.class);
				intent.putExtra("calib_id2play", calib_id);
				intent.putExtra("main2display", pitches);
				startActivity(intent);
			}
		});
		
		Button historyBtnCall = (Button)findViewById(R.id.historybutton);
		historyBtnCall.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(MainUIActivity.this, HistoryActivity.class);
				startActivity(intent);
			}
		});
		
		Button calibBtnCall = (Button) findViewById(R.id.calibrationbutton);
		calibBtnCall.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(MainUIActivity.this,
						CalibrationActivity.class);
				intent.putExtra("calib_id", calib_id);  // XXX : to check whether it works. move it to music sheet activity later
		
				startActivityForResult(intent,CALIBRATION);
			}
		});
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (resultCode == RESULT_OK && requestCode == CALIBRATION) {
	        if (data.hasExtra("calib_id2main")) {
	        	calib_id = data.getIntExtra("calib_id2main", -1);
	            Toast.makeText(this, calib_id+"", Toast.LENGTH_SHORT).show();
	        }
	    }
	}
}
