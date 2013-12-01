package com.example.trombone;

import com.example.trombone.util.SystemUiHider;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.util.Log;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class MusicsheetSelectActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);  
	    //set up full screen
	    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
	                WindowManager.LayoutParams.FLAG_FULLSCREEN);  

		setContentView(R.layout.activity_musicsheet_select);
		
		ImageButton realplayBtnCall = (ImageButton)findViewById(R.id.realplaybutton);
		realplayBtnCall.setOnClickListener(new ImageButton.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(MusicsheetSelectActivity.this, DisplayActivity.class);
				startActivity(intent);
			}
			
		});
		/*Button realplayBtnCall = (Button)findViewById(R.id.realplaybutton);
		Log.d("ww","Click7");
		realplayBtnCall.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				Log.d("ww","Click");
				Intent intent = new Intent(MusicsheetSelectActivity.this, DisplayActivity.class);
				startActivity(intent);
			}
		});
*/	}
}
