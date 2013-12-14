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

import static classes.Constants.*;

public class MainUIActivity extends Activity {
	private int calib_id = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);  
	    //set up full screen
	    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
	                WindowManager.LayoutParams.FLAG_FULLSCREEN);  
		setContentView(R.layout.activity_main_ui);

		Button playBtnCall = (Button)findViewById(R.id.playbutton);
		playBtnCall.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(MainUIActivity.this, demoActivity.class);
				intent.putExtra("calib_id2play", calib_id);
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
				Intent intent = new Intent(MainUIActivity.this, CalibrationActivity.class);
				intent.putExtra("calib_id", calib_id);
		
				startActivityForResult(intent, CALIBRATION);
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
