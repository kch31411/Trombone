package com.example.trombone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import classes.MusicSheet;

import com.example.trombone.util.SystemUiHider;

import db.DBSheetHelper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.util.Log;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class MusicsheetSelectActivity extends Activity {

	private class SpecialAdapter extends ArrayAdapter<String> {
		public SpecialAdapter(Context context, int resource,
				List<String> objects) {
			super(context, resource, objects);
			// TODO Auto-generated constructor stub
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if ( convertView == null ) {
				TextView tv = new TextView(getApplicationContext());
				tv.setTextColor(getResources().getColor(R.color.black_overlay));
				tv.setTextSize(getResources().getDimension(R.dimen.musicsheetname));
				convertView = tv;
			}
			
			return super.getView(position, convertView, parent);
		}
	}
	
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
		
		DBSheetHelper db = new DBSheetHelper(this); 
		
		Log.d("Reading: ", "Reading all contacts..");
		List<MusicSheet> sheets = db.getAllMusicSheets();
		ArrayList<String> sheetNames = new ArrayList<String>();
		SpecialAdapter adapter;
		adapter = new SpecialAdapter(this, android.R.layout.simple_list_item_1, sheetNames);
		Log.d("Reading: ", "Reading 1");
		
		for (MusicSheet sheet : sheets) {
			Log.d("Reading: ", "wwwwwww");
			String log = "Id: " + sheet.getId() + ", Name: " + sheet.getName() + ", Beat: " + sheet.getBeat() + ", Pages: " + sheet.getPages();
			Log.d("read - ", log);
			sheetNames.add(sheet.getName());
			adapter.notifyDataSetChanged();
 		}
		
		ListView lv = (ListView)findViewById(R.id.musicsheetlistview);
		lv.setAdapter(adapter);
	}
}
