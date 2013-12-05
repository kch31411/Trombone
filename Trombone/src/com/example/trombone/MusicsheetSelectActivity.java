package com.example.trombone;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import classes.MusicSheet;
import classes.Note;

import com.example.trombone.util.SystemUiHider;

import db.DBHelper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
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
	public static final int ADD_SHEET = 1;
	
	public int selectedPos = -1;
	public ArrayList<Integer> ids = new ArrayList<Integer>();
	public DBHelper db = new DBHelper(this); 
	int calibId;
	
	private class SpecialAdapter extends ArrayAdapter<String> {
		public SpecialAdapter(Context context, int resource,
				List<String> objects) {
			super(context, resource, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if ( convertView == null ) {
				TextView tv = new TextView(getApplicationContext());
				tv.setTextColor(getResources().getColor(R.color.black_overlay));
				tv.setTextSize(getResources().getDimension(R.dimen.musicsheetname));
				
				if ( selectedPos == position ) {
					tv.setBackgroundColor(getResources().getColor(R.color.selecteditem));
				}
				
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

		ImageView realplayBtnCall = (ImageView)findViewById(R.id.realplaybutton);
		realplayBtnCall.setOnClickListener(new ImageView.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if ( selectedPos == -1 )
					return;
				
				int id = ids.get(selectedPos);
				MusicSheet musicsheet = db.getMusicSheet(id);
				musicsheet.setPlayCount(musicsheet.getPlayCount() + 1);
				db.updateMusicSheet(musicsheet);
				
				Intent intent = new Intent(MusicsheetSelectActivity.this, DisplayActivity.class);
				intent.putExtra("main2display", getIntent().getDoubleArrayExtra("main2display"));
				intent.putExtra("musicsheet_id", id);
				intent.putExtra("calib_id2play",  getIntent().getIntExtra("calib_id2play", -1));
				startActivity(intent);
			}
		});

		// Add musicsheet
		ImageView addmusicsheetBtnCall = (ImageView)findViewById(R.id.addmusicsheetbutton);
		addmusicsheetBtnCall.setOnClickListener(new ImageView.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("file/*");
				startActivityForResult(intent, ADD_SHEET);
			}
		});
		
		// Delete musicsheet
		ImageView deletemusicsheetBtnCall = (ImageView)findViewById(R.id.deletemusicsheetbutton);
		deletemusicsheetBtnCall.setOnClickListener(new ImageView.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if ( selectedPos == -1 )
					return;
				
				int id = ids.get(selectedPos);
				MusicSheet musicsheet = db.getMusicSheet(id);
				db.deleteMusicSheet(musicsheet);
				selectedPos = -1;
				
				refreshListView();
			}
		});
			
		ListView lv = (ListView)findViewById(R.id.musicsheetlistview);
		lv.setOnItemClickListener( new ListViewItemClickListener() );
		//lv.setOnItemLongClickListener( new ListViewItemClickListener() );
		refreshListView();
	}
	
	private class ListViewItemClickListener implements AdapterView.OnItemClickListener {		
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			if ( selectedPos == position )
				selectedPos =-1;
			else
				selectedPos = position;
			
			refreshListView();
		}
		
	}
	
	public void refreshListView() {
		ids.clear();

		List<MusicSheet> sheets = db.getAllMusicSheets();
		ArrayList<String> sheetNames = new ArrayList<String>();
		SpecialAdapter adapter;
		adapter = new SpecialAdapter(this, android.R.layout.simple_list_item_1, sheetNames);

		int index = 0;
		for (MusicSheet sheet : sheets) {
			ids.add(index, sheet.getId()); // musicsheet id를 모아놓은 ArrayList
			index++;
			
			sheetNames.add(sheet.getName());
			adapter.notifyDataSetChanged();
		}

		ListView lv = (ListView)findViewById(R.id.musicsheetlistview);
		lv.setAdapter(adapter);
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent result) 
	{
		Log.d("aaa", "aaaa");
		if (resultCode == RESULT_OK) 
		{
			Log.d("aaa", "aaaa1");
			if (requestCode == ADD_SHEET) 
			{
				Log.d("aaa", "aaaa2");
				Uri data = result.getData();
				File musicsheet = new File(data.toString());
				
				readFile(musicsheet);
			}
		}
		super.onActivityResult(requestCode, resultCode, result);
	}
	
	private void readFile(File file) {
		Log.d("aaa", file.getAbsolutePath().substring(6));
		//if ( file != null && file.exists() ) {
		if ( true ) {
			try {
				Log.d("aaa", "aaaa4");
				FileInputStream fileinputstream =  new FileInputStream(file.getAbsolutePath().substring(6));
				Scanner scan = new Scanner(fileinputstream);
				
				String name = scan.nextLine();
				int keyNumber = scan.nextInt();
				int beat = scan.nextInt();
				int numberOfNote = scan.nextInt();
				int page = 1;
				int order = 1;
				int currentPageBeat = 0;
				ArrayList<Note> notes = new ArrayList<Note>();
				
				// Note class 생성
				for ( int i = 0; i < numberOfNote; i++ ) {
					Note current = new Note(scan.nextInt(), scan.nextInt());
					current.setIsRest(scan.nextInt());
					current.setIsAccidental(scan.nextInt());
					current.setPage(page);
					current.setOrder(order);
					notes.add(current);
					
					order++;
					currentPageBeat += current.getBeat();
					if ( currentPageBeat == beat * 6 ) {
						page++;
						order = 1;
						currentPageBeat = 0;
					}
				}
				
				scan.close();
				
				// TODO : KEY NUMBER
				
				// Make MusicSheet DB
				DBHelper db = new DBHelper(this); 
				Log.d("Insert: ", "Inserting ..");
				int musicsheet_id = (int)db.addMusicSheet(new MusicSheet(name, beat, page, keyNumber));
				Log.d("Insert: ", "After inserting MUSICSHEET to DB");
				
				// Make Note DB
				for (Note note : notes) {
					Log.d("Insert: ", "Inserting ..");
					note.setMusicsheet_id(musicsheet_id);
					db.addNote(note);
					Log.d("Insert: ", "After inserting NOTE to DB");
		 		}
				
				// DB TEST
				for ( int i = 1; i <= page; i++ ) {
					List<Note> notesOnDB = db.getNotes(musicsheet_id, i);
					for (Note note : notesOnDB ) {
						String log = "Id: " + note.getId() + 
								", Page: " + note.getPage() + 
								", Order: " + note.getOrder() +
								", Pitch: " + note.getPitch() +
								", Beat: " + note.getBeat() +
								", isRest: " + note.getIsRest() +
								", isAccidental: " + note.getIsAccidental() +
								", X: " + note.getX() +
								", Y: " + note.getY() +
								", Musicsheet_id: " + note.getMusicsheet_id();
						Log.d("read - ", log);
			 		}
				}
		 		//////
		 		refreshListView();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
