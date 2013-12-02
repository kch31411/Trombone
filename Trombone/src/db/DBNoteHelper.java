package db;

import java.util.ArrayList;
import java.util.List;

import classes.Memo;
import classes.Note;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBNoteHelper extends SQLiteOpenHelper {
	// Database Version
	private static final int DATABASE_VERSION = 1;
	
	// Database Name
	private static final String DATABASE_NAME = "tromboneDB";
	
	// Sheets table name
	private static final String TABLE_SHEETS = "notes";
	
	// Sheets Table Columns names
	private static final String KEY_ID = "id"; // primary key
	private static final String KEY_PAGE = "page";
	private static final String KEY_ORDER = "order";
	private static final String KEY_PITCH = "pitch";
	private static final String KEY_BEAT = "beat";
	private static final String KEY_ISREST = "isRest";
	private static final String KEY_X = "x";
	private static final String KEY_Y = "y";
	private static final String KEY_MUSICSHEET = "musicsheet_id"; // foreign key
	
	public DBNoteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	// Crating Tables
	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_SHEETS_TABLE = "CREATE TABLE " + TABLE_SHEETS + "("
				+ KEY_ID + " INTEGER PRIMARY KEY," 
				+ KEY_PAGE + " INTEGER,"
				+ KEY_ORDER + " INTEGER,"
				+ KEY_PITCH + " INTEGER,"
				+ KEY_BEAT + " INTEGER,"
				+ KEY_ISREST + " INTEGER,"
				+ KEY_X + " INTEGER,"
				+ KEY_Y + " INTEGER,"
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
	
	// 새로운 Note를 Sheet에 추가
	public long addNote(Note note) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_ID, note.getId());
		values.put(KEY_PAGE, note.getPage());
		values.put(KEY_ORDER, note.getOrder());
		values.put(KEY_PITCH, note.getPitch());
		values.put(KEY_BEAT, note.getBeat());
		values.put(KEY_ISREST, note.getIsRest());
		values.put(KEY_X, note.getX());
		values.put(KEY_Y, note.getY());
		values.put(KEY_MUSICSHEET, note.getMusicsheet_id());
		
		// Inserting Row
		long id = db.insert(TABLE_SHEETS, null, values);
		db.close();
		
		return id;
	}
	
	// musicsheet_id와 page가 주어질 떄 순서대로 Note 정보 가져오기
	public List<Note> getMemos(int musicsheet_id, int page) {
		List<Note> noteList = new ArrayList<Note>();
		// Select All Query
		String selectQuery = "SELECT * FROM " + TABLE_SHEETS + 
				"WHERE musicsheet_id=" + musicsheet_id
				+ " AND page=" + page
				+ "ORDER BY" + KEY_ORDER + "ASC";
		
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		
		// looping through all rows and adding to list
		if ( cursor.moveToFirst() ) {
			do {
				Note note = new Note();
				note.setId(Integer.parseInt(cursor.getString(0)));
				note.setPage(Integer.parseInt(cursor.getString(1)));
				note.setOrder(Integer.parseInt(cursor.getString(2)));
				note.setPitch(Integer.parseInt(cursor.getString(3)));
				note.setBeat(Integer.parseInt(cursor.getString(4)));
				note.setIsRest(Integer.parseInt(cursor.getString(5)));
				note.setX(Integer.parseInt(cursor.getString(6)));
				note.setY(Integer.parseInt(cursor.getString(7)));
				note.setMusicsheet_id(Integer.parseInt(cursor.getString(8)));
				
				// Adding sheets to list
				noteList.add(note);
			} while (cursor.moveToNext());
		}
		
		// return sheet list
		return noteList;
	}
}
