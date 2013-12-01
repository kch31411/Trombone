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

public class DBSheetHelper extends SQLiteOpenHelper {
	
	// Database Version
	private static final int DATABASE_VERSION = 1;
	
	// Database Name
	private static final String DATABASE_NAME = "tromboneDB";
	
	// Sheets table name
	private static final String TABLE_SHEETS = "musicsheets";
	
	// Sheets Table Columns names
	private static final String KEY_ID = "id";
	private static final String KEY_NAME = "name";
	private static final String KEY_BEAT = "beat";
	private static final String KEY_PAGES = "pages";
	
	public DBSheetHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	// Creating Tables
	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_SHEETS_TABLE = "CREATE TABLE " + TABLE_SHEETS + "("
				+ KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT," 
				+ KEY_BEAT + " INTEGER," + KEY_PAGES + " INTEGER" + ")";
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
	
	// 새로운  Sheet 함수 추가
	public void addMusicSheet(MusicSheet sheet) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_NAME, sheet.getName());
		values.put(KEY_BEAT, sheet.getBeat());
		values.put(KEY_PAGES, sheet.getPages());
		
		// Inserting Row
		db.insert(TABLE_SHEETS, null, values);
		db.close();
	}
	
	// id 에 해당하는 Sheet 객체 가져오기
	public MusicSheet getMusicSheet(int id) {
		SQLiteDatabase db = this.getReadableDatabase();
		
		Cursor cursor = db.query(TABLE_SHEETS, new String[] { KEY_ID, 
				KEY_NAME, KEY_BEAT, KEY_PAGES }, KEY_ID + "=?",
				new String[] { String.valueOf(id) }, null, null, null, null);
		if ( cursor != null )
			cursor.moveToFirst();
		
		MusicSheet sheet = new MusicSheet(Integer.parseInt(cursor.getString(0)),
				cursor.getString(1),
				Integer.parseInt(cursor.getString(2)),
				Integer.parseInt(cursor.getString(3)));
		
		return sheet;
	}
	
	// 모든 Sheet 정보 가져오기
	public List<MusicSheet> getAllMusicSheets() {
		List<MusicSheet> sheetList = new ArrayList<MusicSheet>();
		// Select All Query
		String selectQuery = "SELECT * FROM " + TABLE_SHEETS;
		
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		
		// looping through all rows and adding to list
		if ( cursor.moveToFirst() ) {
			do {
				MusicSheet sheet = new MusicSheet();
				sheet.setId(Integer.parseInt(cursor.getString(0)));
				sheet.setName(cursor.getString(1));
				sheet.setBeat(Integer.parseInt(cursor.getString(2)));
				sheet.setPages(Integer.parseInt(cursor.getString(3)));
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
		values.put(KEY_NAME, sheet.getName());
		values.put(KEY_BEAT, sheet.getBeat());
		values.put(KEY_PAGES, sheet.getPages());
		
		return db.update(TABLE_SHEETS, values, KEY_ID + " = ?",
				new String[] { String.valueOf(sheet.getId()) });
	}
	
	// MusicSheet 정보 삭제하기
	public void deleteMusicSheet(MusicSheet sheet) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_SHEETS, KEY_ID + " = ?", 
				new String[] { String.valueOf(sheet.getId()) });
		db.close();
	}
	
	// MusicSheet 정보 숫자
	public int getMusicSheetsCount() {
		String countQuery = "SELECT * FROM " + TABLE_SHEETS;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);
		cursor.close();
		
		// return count
		return cursor.getCount();
	}
}
