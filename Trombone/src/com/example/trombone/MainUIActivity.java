package com.example.trombone;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class MainUIActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main_ui);

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.


		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		Button playBtnCall = (Button)findViewById(R.id.playbutton);
		playBtnCall.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(MainUIActivity.this, DisplayActivity.class);
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
	}
}
