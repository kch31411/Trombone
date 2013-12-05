package db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import classes.*;

public class DBHelper extends SQLiteOpenHelper {
	// Database Version
	private static final int DATABASE_VERSION = 1;
		
	// Database Name
	private static final String DATABASE_NAME = "tromboneDB";
	
	// Sheets table name
	private static final String MUSICSHEET_TABLE_SHEETS = "musicsheets";
	private static final String NOTE_TABLE_SHEETS = "notes";
	private static final String MEMO_TABLE_SHEETS = "memos";
	private static final String HISTORY_TABLE_SHEETS = "histories";
	private static final String CALIB_TABLE_SHEETS = "calibration_data";
	
	// Sheets Table Columns names
	private static final String MUSICSHEET_KEY_ID = "id";
	private static final String MUSICSHEET_KEY_NAME = "name";
	private static final String MUSICSHEET_KEY_BEAT = "beat";
	private static final String MUSICSHEET_KEY_PLAYCOUNT = "playcount";
	private static final String MUSICSHEET_KEY_KEYNUMBER = "keyNumber";
	private static final String MUSICSHEET_KEY_PAGES = "pages";
	
	private static final String NOTE_KEY_ID = "id"; // primary key
	private static final String NOTE_KEY_PAGE = "page";
	private static final String NOTE_KEY_ORDER = "order_in_page";
	private static final String NOTE_KEY_PITCH = "pitch";
	private static final String NOTE_KEY_BEAT = "beat";
	private static final String NOTE_KEY_ISREST = "isRest";
	private static final String NOTE_KEY_ISADCCIDENTAL = "isAccidental";
	private static final String NOTE_KEY_X = "x";
	private static final String NOTE_KEY_Y = "y";
	private static final String NOTE_KEY_MUSICSHEET = "musicsheet_id"; // foreign key
	
	private static final String MEMO_KEY_ID = "id"; // primary key
	private static final String MEMO_KEY_X = "x";
	private static final String MEMO_KEY_Y = "y";
	private static final String MEMO_KEY_OPACITY = "opacity";
	private static final String MEMO_KEY_PAGE = "page";
	private static final String MEMO_KEY_CONTENT = "content";
	private static final String MEMO_KEY_MUSICSHEET = "musicsheet_id"; // foreign key
	
	private static final String HISTORY_KEY_ID = "id"; // primary key
	private static final String HISTORY_KEY_DATE = "date";
	private static final String HISTORY_KEY_SCORE = "score";
	private static final String HISTORY_KEY_MUSICSHEET = "musicsheet_id"; // foreign key

	private static final String CALIB_KEY_ID = "id"; // primary key
	private static final String CALIB_KEY_NAME = "name";
	private static final String CALIB_KEY_PATH = "file_path";
	
	public DBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	// Creating Tables
	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d("aaa", "SHEETHELPER onCreate above MUSICSHEET");
		String CREATE_SHEETS_TABLE = "CREATE TABLE " + MUSICSHEET_TABLE_SHEETS + "("
				+ MUSICSHEET_KEY_ID + " INTEGER PRIMARY KEY," 
				+ MUSICSHEET_KEY_NAME + " TEXT," 
				+ MUSICSHEET_KEY_BEAT + " INTEGER," 
				+ MUSICSHEET_KEY_PLAYCOUNT + " INTEGER," 
				+ MUSICSHEET_KEY_KEYNUMBER + " INTEGER,"
				+ MUSICSHEET_KEY_PAGES + " INTEGER" + ")";
		db.execSQL(CREATE_SHEETS_TABLE);
		
		Log.d("aaa", "SHEETHELPER onCreate above NOTE");
		CREATE_SHEETS_TABLE = "CREATE TABLE " + NOTE_TABLE_SHEETS + "("
				+ NOTE_KEY_ID + " INTEGER PRIMARY KEY," 
				+ NOTE_KEY_PAGE + " INTEGER,"
				+ NOTE_KEY_ORDER + " INTEGER,"
				+ NOTE_KEY_PITCH + " INTEGER,"
				+ NOTE_KEY_BEAT + " INTEGER,"
				+ NOTE_KEY_ISREST + " INTEGER,"
				+ NOTE_KEY_ISADCCIDENTAL + " INTEGER,"
				+ NOTE_KEY_X + " INTEGER,"
				+ NOTE_KEY_Y + " INTEGER,"
				+ NOTE_KEY_MUSICSHEET + " INTEGER,"
				+ "FOREIGN KEY(" + NOTE_KEY_MUSICSHEET + ") REFERENCES musicsheets(id)" 
				+ ")";
		db.execSQL(CREATE_SHEETS_TABLE);
		
		Log.d("aaa", "SHEETHELPER onCreate above MEMO");
		CREATE_SHEETS_TABLE = "CREATE TABLE " + MEMO_TABLE_SHEETS + "("
				+ MEMO_KEY_ID + " INTEGER PRIMARY KEY," 
				+ MEMO_KEY_X + " FLOAT,"
				+ MEMO_KEY_Y + " FLOAT,"
				+ MEMO_KEY_OPACITY + " INTEGER,"
				+ MEMO_KEY_PAGE + " INTEGER,"
				+ MEMO_KEY_CONTENT + " TEXT,"
				+ MEMO_KEY_MUSICSHEET + " INTEGER,"
				+ "FOREIGN KEY(" + MEMO_KEY_MUSICSHEET + ") REFERENCES musicsheets(id)" 
				+ ")";
		db.execSQL(CREATE_SHEETS_TABLE);
		
		Log.d("aaa", "SHEETHELPER onCreate above HISTORY");
		CREATE_SHEETS_TABLE = "CREATE TABLE " + HISTORY_TABLE_SHEETS + "("
				+ HISTORY_KEY_ID + " INTEGER PRIMARY KEY," 
				+ HISTORY_KEY_DATE + " TEXT,"
				+ HISTORY_KEY_SCORE + " INTEGER,"
				+ HISTORY_KEY_MUSICSHEET + " INTEGER,"
				+ "FOREIGN KEY(" + HISTORY_KEY_MUSICSHEET + ") REFERENCES musicsheets(id)" 
				+ ")";
		db.execSQL(CREATE_SHEETS_TABLE);
		
		Log.d("aaa", "SHEETHELPER onCreate above CALIB");
		CREATE_SHEETS_TABLE = "CREATE TABLE " + CALIB_TABLE_SHEETS + "("
				+ CALIB_KEY_ID + " INTEGER PRIMARY KEY," 
				+ CALIB_KEY_NAME + " TEXT,"
				+ CALIB_KEY_PATH + " TEXT"
				+ ")";
		db.execSQL(CREATE_SHEETS_TABLE);
	}
	
	// Upgrading database
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older table if existed
		db.execSQL("DROP TABLE IF EXISTS " + MUSICSHEET_TABLE_SHEETS);
		db.execSQL("DROP TABLE IF EXISTS " + NOTE_TABLE_SHEETS);
		db.execSQL("DROP TABLE IF EXISTS " + MEMO_TABLE_SHEETS);
		db.execSQL("DROP TABLE IF EXISTS " + HISTORY_TABLE_SHEETS);;
		db.execSQL("DROP TABLE IF EXISTS " + CALIB_TABLE_SHEETS);
		
		// Create tables again
		onCreate(db);
	}
	
	/**
	 * CRUD 함수
	 */
	
	/*/////////////////////////////////////////////////////////////////////////
	 * MUSICSHEET
	 */////////////////////////////////////////////////////////////////////////
	// 새로운  Sheet 함수 추가
	public long addMusicSheet(MusicSheet sheet) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(MUSICSHEET_KEY_NAME, sheet.getName());
		values.put(MUSICSHEET_KEY_BEAT, sheet.getBeat());
		values.put(MUSICSHEET_KEY_PAGES, sheet.getPages());
		values.put(MUSICSHEET_KEY_PLAYCOUNT, sheet.getPlayCount());
		values.put(MUSICSHEET_KEY_KEYNUMBER, sheet.getKeyNumber());
		
		// Inserting Row
		long id = db.insert(MUSICSHEET_TABLE_SHEETS, null, values);
		db.close();
		
		return id;
	}
	
	// id 에 해당하는 Sheet 객체 가져오기
	public MusicSheet getMusicSheet(int id) {
		SQLiteDatabase db = this.getReadableDatabase();
		
		Cursor cursor = db.query(MUSICSHEET_TABLE_SHEETS, new String[] { MUSICSHEET_KEY_ID, 
				MUSICSHEET_KEY_NAME, MUSICSHEET_KEY_BEAT, MUSICSHEET_KEY_PAGES, MUSICSHEET_KEY_PLAYCOUNT, MUSICSHEET_KEY_KEYNUMBER }, MUSICSHEET_KEY_ID + "=?",
				new String[] { String.valueOf(id) }, null, null, null, null);
		if ( cursor != null )
			cursor.moveToFirst();

		// memo 가져오기, note 가져오기
		int pages = Integer.parseInt(cursor.getString(3)); 
		
		ArrayList<ArrayList<Note>> note = new ArrayList<ArrayList<Note>>();
		ArrayList<ArrayList<Memo>> memo = new ArrayList<ArrayList<Memo>>();
		
		for (int p = 1; p <= pages; p++) {
			note.set(p, getNotes(id, p));
			memo.set(p, getMemos(id, p));
		}
		
		MusicSheet sheet = new MusicSheet(Integer.parseInt(cursor.getString(0)),
				cursor.getString(1),
				Integer.parseInt(cursor.getString(2)),
				pages,
				Integer.parseInt(cursor.getString(4)),
				Integer.parseInt(cursor.getString(5)),
				note,
				memo);
		
		
		
		return sheet;
	}
	
	// 모든 Sheet 정보 가져오기
	public List<MusicSheet> getAllMusicSheets() {
		List<MusicSheet> sheetList = new ArrayList<MusicSheet>();
		// Select All Query
		String selectQuery = "SELECT * FROM " + MUSICSHEET_TABLE_SHEETS;
		
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		
		// looping through all rows and adding to list
		if ( cursor.moveToFirst() ) {
			do {
				// memo 가져오기, note 가져오기
				int id = Integer.parseInt(cursor.getString(0));
				int pages = Integer.parseInt(cursor.getString(3)); 
				
				ArrayList<ArrayList<Note>> note = new ArrayList<ArrayList<Note>>();
				ArrayList<ArrayList<Memo>> memo = new ArrayList<ArrayList<Memo>>();
				
				for (int p = 1; p <= pages; p++) {
					note.set(p, getNotes(id, p));
					memo.set(p, getMemos(id, p));
				}
				
				MusicSheet sheet = new MusicSheet(Integer.parseInt(cursor.getString(0)),
						cursor.getString(1), Integer.parseInt(cursor.getString(2)), Integer.parseInt(cursor.getString(3)),
						Integer.parseInt(cursor.getString(4)), Integer.parseInt(cursor.getString(5)),
						note, memo);
				
				// Adding sheets to list
				sheetList.add(sheet);
			} while (cursor.moveToNext());
		}
		
		// return sheet list
		return sheetList;
	}
	
	// MusicSheet 정보 업데이트
	public int updateMusicSheet(MusicSheet sheet) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(MUSICSHEET_KEY_NAME, sheet.getName());
		values.put(MUSICSHEET_KEY_BEAT, sheet.getBeat());
		values.put(MUSICSHEET_KEY_PAGES, sheet.getPages());
		values.put(MUSICSHEET_KEY_PLAYCOUNT, sheet.getPlayCount());
		values.put(MUSICSHEET_KEY_KEYNUMBER, sheet.getKeyNumber());
		
		return db.update(MUSICSHEET_TABLE_SHEETS, values, MUSICSHEET_KEY_ID + " = ?",
				new String[] { String.valueOf(sheet.getId()) });
	}
	
	// MusicSheet 정보 삭제하기
	public void deleteMusicSheet(MusicSheet sheet) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(MUSICSHEET_TABLE_SHEETS, MUSICSHEET_KEY_ID + " = ?", 
				new String[] { String.valueOf(sheet.getId()) });
		db.close();
	}
	
	// MusicSheet 정보 숫자
	public int getMusicSheetsCount() {
		String countQuery = "SELECT * FROM " + MUSICSHEET_TABLE_SHEETS;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);
		
		// return count
		return cursor.getCount();
	}
	/////////////////////////////////////////////////////////////////////////
	
	/*/////////////////////////////////////////////////////////////////////////
	 * NOTE
	 */////////////////////////////////////////////////////////////////////////
	// 새로운 Note를 Sheet에 추가
	public long addNote(Note note) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(NOTE_KEY_PAGE, note.getPage());
		values.put(NOTE_KEY_ORDER, note.getOrder());
		values.put(NOTE_KEY_PITCH, note.getPitch());
		values.put(NOTE_KEY_BEAT, note.getBeat());
		values.put(NOTE_KEY_ISREST, note.getIsRest());
		values.put(NOTE_KEY_ISADCCIDENTAL, note.getIsRest());
		values.put(NOTE_KEY_X, note.getX());
		values.put(NOTE_KEY_Y, note.getY());
		values.put(NOTE_KEY_MUSICSHEET, note.getMusicsheet_id());
		Log.d("aaa", "aaaa9999");
		// Inserting Row
		long id = db.insert(NOTE_TABLE_SHEETS, null, values);
		db.close();
		
		return id;
	}
	
	// musicsheet_id와 page가 주어질 떄 순서대로 Note 정보 가져오기
	public ArrayList<Note> getNotes(int musicsheet_id, int page) {
		ArrayList<Note> noteList = new ArrayList<Note>();
		// Select All Query
		String selectQuery = "SELECT * FROM " + NOTE_TABLE_SHEETS + 
				" WHERE musicsheet_id= '" + musicsheet_id + "'"
				+ " AND page= '" + page + "'"
				+ " ORDER BY " + NOTE_KEY_ORDER + " ASC";
		
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
				note.setIsAccidental(Integer.parseInt(cursor.getString(6)));
				note.setX(Integer.parseInt(cursor.getString(7)));
				note.setY(Integer.parseInt(cursor.getString(8)));
				note.setMusicsheet_id(Integer.parseInt(cursor.getString(9)));
				
				// Adding sheets to list
				noteList.add(note);
			} while (cursor.moveToNext());
		}
		
		// return sheet list
		return noteList;
	}
	/////////////////////////////////////////////////////////////////////////
	
	/*/////////////////////////////////////////////////////////////////////////
	 * MEMO
	 */////////////////////////////////////////////////////////////////////////
	// 새로운 Memo를 Sheet에 추가
	public long addMemo(Memo memo) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(MEMO_KEY_X, memo.getX());
		values.put(MEMO_KEY_Y, memo.getY());
		values.put(MEMO_KEY_OPACITY, memo.getOpacity());
		values.put(MEMO_KEY_PAGE, memo.getPage());
		values.put(MEMO_KEY_CONTENT, memo.getContent());
		values.put(MEMO_KEY_MUSICSHEET, memo.getMusicsheet_id());
		
		// Inserting Row
		long id = db.insert(MEMO_TABLE_SHEETS, null, values);
		db.close();
		
		return id;
	}
	
	// id 에 해당하는 Sheet 객체 가져오기
	public Memo getMemo(int id) {
		SQLiteDatabase db = this.getReadableDatabase();
		
		Cursor cursor = db.query(MEMO_TABLE_SHEETS, new String[] { MEMO_KEY_ID, 
				MEMO_KEY_X, MEMO_KEY_Y, MEMO_KEY_OPACITY, MEMO_KEY_PAGE, MEMO_KEY_CONTENT, MEMO_KEY_MUSICSHEET }, MEMO_KEY_ID + "=?",
				new String[] { String.valueOf(id) }, null, null, null, null);
		if ( cursor != null )
			cursor.moveToFirst();
		
		Memo memo = new Memo(Integer.parseInt(cursor.getString(0)),
				Float.parseFloat(cursor.getString(1)),
				Float.parseFloat(cursor.getString(2)),
				Integer.parseInt(cursor.getString(3)),
				Integer.parseInt(cursor.getString(4)),
				cursor.getString(5),
				Integer.parseInt(cursor.getString(6)),
				null
				);
		
		return memo;
	}
	
	// musicsheet_id와 page가 주어질 떄 Memo 정보 가져오기
	public ArrayList<Memo> getMemos(int musicsheet_id, int page) {
		ArrayList<Memo> memoList = new ArrayList<Memo>();
		// Select All Query
		String selectQuery = "SELECT * FROM " + MEMO_TABLE_SHEETS + 
				" WHERE musicsheet_id= '" + musicsheet_id + "'"
				+ " AND page= '" + page + "'";
		
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		
		// looping through all rows and adding to list
		if ( cursor.moveToFirst() ) {
			do {
				Memo memo = new Memo();
				memo.setId(Integer.parseInt(cursor.getString(0)));
				memo.setX(Float.parseFloat(cursor.getString(1)));
				memo.setY(Float.parseFloat(cursor.getString(2)));
				memo.setOpacity(Integer.parseInt(cursor.getString(3)));
				memo.setPage(Integer.parseInt(cursor.getString(4)));
				memo.setContent(cursor.getString(5));
				memo.setMusicsheet_id(Integer.parseInt(cursor.getString(6)));
				
				// Adding sheets to list
				memoList.add(memo);
			} while (cursor.moveToNext());
		}
		
		// return sheet list
		return memoList;
	}
	
	// Memo 정보 업데이트
	public int updateMemo(Memo memo) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(MEMO_KEY_X, memo.getX());
		values.put(MEMO_KEY_Y, memo.getY());
		values.put(MEMO_KEY_OPACITY, memo.getOpacity());
		values.put(MEMO_KEY_PAGE, memo.getPage());
		values.put(MEMO_KEY_CONTENT, memo.getContent());
		values.put(MEMO_KEY_MUSICSHEET, memo.getMusicsheet_id());
		
		return db.update(MEMO_TABLE_SHEETS, values, MEMO_KEY_ID + " = ?",
				new String[] { String.valueOf(memo.getId()) });
	}
	
	// Memo 정보 삭제하기
	public void deleteMemo(Memo memo) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(MEMO_TABLE_SHEETS, MEMO_KEY_ID + " = ?", 
				new String[] { String.valueOf(memo.getId()) });
		db.close();
	}
	/////////////////////////////////////////////////////////////////////////
	
	/*/////////////////////////////////////////////////////////////////////////
	 * HISTORY
	 */////////////////////////////////////////////////////////////////////////
	// 새로운 History를 Sheet에 추가
	public long addHistory(History history) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(HISTORY_KEY_DATE, history.getDate());
		values.put(HISTORY_KEY_SCORE, history.getScore());
		values.put(HISTORY_KEY_MUSICSHEET, history.getMusicsheet_id());
		
		// Inserting Row
		long id = db.insert(HISTORY_TABLE_SHEETS, null, values);
		db.close();
		
		return id;
	}
	
	// musicsheet_id가 주어질 떄 Memo 정보 가져오기
	public List<History> getMemos(int musicsheet_id) {
			List<History> historyList = new ArrayList<History>();
			// Select All Query
			String selectQuery = "SELECT * FROM " + HISTORY_TABLE_SHEETS + 
					" WHERE musicsheet_id= '" + musicsheet_id + "'";
			
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
	/////////////////////////////////////////////////////////////////////////
	
	/*/////////////////////////////////////////////////////////////////////////
	 * Calibration
	 */////////////////////////////////////////////////////////////////////////
	// 새로운 Calibration data를 추가
	public long addCalibrationData(CalibrationData calibrationData) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(CALIB_KEY_NAME, calibrationData.getName());
		values.put(CALIB_KEY_PATH, calibrationData.getFile_path());
		
		// Inserting Row
		long id = db.insert(CALIB_TABLE_SHEETS, null, values);
		db.close();
		
		return id;
	}
	
	// id 에 해당하는 Calibration data 객체 가져오기
	public CalibrationData getCalibrationData(int id) {
		SQLiteDatabase db = this.getReadableDatabase();
		
		Cursor cursor = db.query(CALIB_TABLE_SHEETS, new String[] { CALIB_KEY_ID, 
				CALIB_KEY_NAME, CALIB_KEY_PATH }, CALIB_KEY_ID + "=?",
				new String[] { String.valueOf(id) }, null, null, null, null);
		if ( cursor != null )
			cursor.moveToFirst();
		
		CalibrationData cd = new CalibrationData(
				Integer.parseInt(cursor.getString(0)),
				cursor.getString(1),
				cursor.getString(2));
		
		return cd;
	}
	
	// get all id list from calibration
	public List<Integer> getAllCalibrationId() {
		List<Integer> idList = new ArrayList<Integer>();
		// Select All Query
		String selectQuery = "SELECT * FROM " + CALIB_TABLE_SHEETS;
		
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		
		// looping through all rows and adding to list
		if ( cursor.moveToFirst() ) {
			do {
				idList.add(Integer.parseInt(cursor.getString(0)));
			} while (cursor.moveToNext());
		}
		
		// return sheet list
		return idList;
	}	
	
	public List<String> getAllCalibrationName() {
		List<String> nameList = new ArrayList<String>();
		// Select All Query
		String selectQuery = "SELECT * FROM " + CALIB_TABLE_SHEETS;
		
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		
		// looping through all rows and adding to list
		if ( cursor.moveToFirst() ) {
			do {
				nameList.add(cursor.getString(1));
			} while (cursor.moveToNext());
		}
		
		// return sheet list
		return nameList;
	}	
	
	public void deleteCalibration(int id) {
		SQLiteDatabase db = this.getWritableDatabase();	
		db.delete(CALIB_TABLE_SHEETS, CALIB_KEY_ID + "=?", 
				new String[] { String.valueOf(id)});
		db.close();
	}
}
