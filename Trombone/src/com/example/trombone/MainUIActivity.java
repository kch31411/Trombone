package com.example.trombone;


import java.util.List;

import classes.MusicSheet;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import classes.MusicSheet;
import android.view.Window;
import android.view.WindowManager;

import db.*;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class MainUIActivity extends Activity {
	
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
		
				startActivityForResult(intent,3);
			}
		});
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (resultCode == RESULT_OK && requestCode == 3) {
	        if (data.hasExtra("calib2main")) {
	        	pitches = data.getExtras().getDoubleArray("calib2main");
	            /*Toast.makeText(this, pitches[0]+"",
	                Toast.LENGTH_SHORT).show();*/
	        }
	    }
	}
}
