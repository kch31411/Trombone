package db;

import java.util.ArrayList;
import java.util.List;

import classes.Memo;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBMemoHelper extends SQLiteOpenHelper {
	
		// Database Version
		private static final int DATABASE_VERSION = 1;
		
		// Database Name
		private static final String DATABASE_NAME = "tromboneDB";
		
		// Sheets table name
		private static final String TABLE_SHEETS = "memos";
		
		// Sheets Table Columns names
		private static final String KEY_ID = "id"; // primary key
		private static final String KEY_X = "x";
		private static final String KEY_Y = "y";
		private static final String KEY_OPACITY = "opacity";
		private static final String KEY_PAGE = "page";
		private static final String KEY_CONTENT = "content";
		private static final String KEY_MUSICSHEET = "musicsheet_id"; // foreign key
		
		public DBMemoHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		// Crating Tables
		@Override
		public void onCreate(SQLiteDatabase db) {
			String CREATE_SHEETS_TABLE = "CREATE TABLE " + TABLE_SHEETS + "("
					+ KEY_ID + " INTEGER PRIMARY KEY," 
					+ KEY_X + " INTEGER,"
					+ KEY_Y + " INTEGER,"
					+ KEY_OPACITY + " INTEGER,"
					+ KEY_PAGE + " INTEGER,"
					+ KEY_CONTENT + " TEXT,"
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
		
		// 새로운 Memo를 Sheet에 추가
		public long addMemo(Memo memo) {
			SQLiteDatabase db = this.getWritableDatabase();
			
			ContentValues values = new ContentValues();
			values.put(KEY_X, memo.getX());
			values.put(KEY_Y, memo.getY());
			values.put(KEY_OPACITY, memo.getOpacity());
			values.put(KEY_PAGE, memo.getPage());
			values.put(KEY_CONTENT, memo.getContent());
			values.put(KEY_MUSICSHEET, memo.getMusicsheet_id());
			
			// Inserting Row
			long id = db.insert(TABLE_SHEETS, null, values);
			db.close();
			
			return id;
		}
		
		// id 에 해당하는 Sheet 객체 가져오기
		public Memo getMemo(int id) {
			SQLiteDatabase db = this.getReadableDatabase();
			
			Cursor cursor = db.query(TABLE_SHEETS, new String[] { KEY_ID, 
					KEY_X, KEY_Y, KEY_OPACITY, KEY_PAGE, KEY_CONTENT, KEY_MUSICSHEET }, KEY_ID + "=?",
					new String[] { String.valueOf(id) }, null, null, null, null);
			if ( cursor != null )
				cursor.moveToFirst();
			
			Memo memo = new Memo(Integer.parseInt(cursor.getString(0)),
					Integer.parseInt(cursor.getString(1)),
					Integer.parseInt(cursor.getString(2)),
					Integer.parseInt(cursor.getString(3)),
					Integer.parseInt(cursor.getString(4)),
					cursor.getString(5),
					Integer.parseInt(cursor.getString(6))
					);
			
			return memo;
		}
		
		// musicsheet_id와 page가 주어질 떄 Memo 정보 가져오기
		public List<Memo> getMemos(int musicsheet_id, int page) {
			List<Memo> memoList = new ArrayList<Memo>();
			// Select All Query
			String selectQuery = "SELECT * FROM " + TABLE_SHEETS + 
					"WHERE musicsheet_id=" + musicsheet_id
					+ " AND page=" + page;
			
			SQLiteDatabase db = this.getWritableDatabase();
			Cursor cursor = db.rawQuery(selectQuery, null);
			
			// looping through all rows and adding to list
			if ( cursor.moveToFirst() ) {
				do {
					Memo memo = new Memo();
					memo.setId(Integer.parseInt(cursor.getString(0)));
					memo.setX(Integer.parseInt(cursor.getString(1)));
					memo.setY(Integer.parseInt(cursor.getString(2)));
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
			values.put(KEY_X, memo.getX());
			values.put(KEY_Y, memo.getY());
			values.put(KEY_OPACITY, memo.getOpacity());
			values.put(KEY_PAGE, memo.getPage());
			values.put(KEY_CONTENT, memo.getContent());
			values.put(KEY_MUSICSHEET, memo.getMusicsheet_id());
			
			return db.update(TABLE_SHEETS, values, KEY_ID + " = ?",
					new String[] { String.valueOf(memo.getId()) });
		}
		
		// Memo 정보 삭제하기
		public void deleteMemo(Memo memo) {
			SQLiteDatabase db = this.getWritableDatabase();
			db.delete(TABLE_SHEETS, KEY_ID + " = ?", 
					new String[] { String.valueOf(memo.getId()) });
			db.close();
		}
}
>>>>>>> origin/origin
