package db;

import java.util.ArrayList;
import java.util.List;

import classes.History;
import classes.Memo;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHistoryHelper extends SQLiteOpenHelper {
	
	// Database Version
	private static final int DATABASE_VERSION = 1;
	
	// Database Name
	private static final String DATABASE_NAME = "tromboneDB";
	
	// Sheets table name
	private static final String TABLE_SHEETS = "histories";
	
	// Sheets Table Columns names
	private static final String KEY_ID = "id"; // primary key
	private static final String KEY_DATE = "date";
	private static final String KEY_SCORE = "score";
	private static final String KEY_MUSICSHEET = "musicsheet_id"; // foreign key
	
	public DBHistoryHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	// Crating Tables
	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_SHEETS_TABLE = "CREATE TABLE " + TABLE_SHEETS + "("
				+ KEY_ID + " INTEGER PRIMARY KEY," 
				+ KEY_DATE + " TEXT,"
				+ KEY_SCORE + " INTEGER,"
				+ "FOREIGN KEY(" + KEY_MUSICSHEET + ") REFERENCES musicsheets(id)" 
				+ ")";
		db.execSQL(CREATE_SHEETS_TABLE);
	}
	
	// Upgrading database
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older table if existed
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SHEETS);
		
		// Create tables again
		onCreate(db);
	}
	
	/**
	 * CRUD 함수
	 */
	
	// 새로운 History를 Sheet에 추가
	public void addHistory(History history) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_DATE, history.getDate());
		values.put(KEY_SCORE, history.getScore());
		values.put(KEY_MUSICSHEET, history.getMusicsheet_id());
		
		// Inserting Row
		db.insert(TABLE_SHEETS, null, values);
		db.close();
	}
	
	// musicsheet_id가 주어질 떄 Memo 정보 가져오기
	public List<History> getMemos(int musicsheet_id) {
		List<History> historyList = new ArrayList<History>();
		// Select All Query
		String selectQuery = "SELECT * FROM " + TABLE_SHEETS + 
				"WHERE musicsheet_id=" + musicsheet_id;
		
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		
		// looping through all rows and adding to list
		if ( cursor.moveToFirst() ) {
			do {
				History history = new History();
				history.setId(Integer.parseInt(cursor.getString(0)));
				history.setDate(cursor.getString(1));
				history.setScore(Integer.parseInt(cursor.getString(2)));
				history.setMusicsheet_id(Integer.parseInt(cursor.getString(3)));
				
				// Adding sheets to list
				historyList.add(history);
			} while (cursor.moveToNext());
		}
		
		// return sheet list
		return historyList;
	}
}
