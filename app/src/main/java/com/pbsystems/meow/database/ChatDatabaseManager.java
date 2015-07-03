package com.pbsystems.meow.database;

import java.util.ArrayList;

import com.pbsystems.meow.data.CMessage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ChatDatabaseManager extends SQLiteOpenHelper {

	public static final int DB_VERSION = 1;

	public static final String TABLE_NAME = "chats";

	public static final String KEY_FRIENDID = "friendid";
	public static final String KEY_MESSAGE = "message";
	public static final String KEY_SENTBY = "sentby";
	public static final String KEY_RECORDID = "recordid";

	private String createStatement = "CREATE TABLE " + TABLE_NAME + "("
			+ KEY_FRIENDID + " TEXT," + KEY_MESSAGE + " TEXT," + KEY_SENTBY
			+ " TEXT," + KEY_RECORDID + " BIGINT)";

	public ChatDatabaseManager(Context context, String nameofDatabse) {
		super(context, nameofDatabse + "_pbs.sql", null, DB_VERSION);
		Log.i("Databse location", context.getDatabasePath(nameofDatabse+"_pbs.sql").getAbsolutePath());
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		db.execSQL(createStatement);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		onCreate(db);
	}

	public boolean insertMessage(String frndid, String sentby, String message,
			long recordid) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues contentValues = new ContentValues();

		contentValues.put(KEY_FRIENDID, frndid);
		contentValues.put(KEY_MESSAGE, message);
		contentValues.put(KEY_SENTBY, sentby);
		contentValues.put(KEY_RECORDID, recordid);

		db.insert(TABLE_NAME, null, contentValues);
		return true;
	}

	public int delete(String strFriendId) {
		SQLiteDatabase db = this.getWritableDatabase();
		return db.delete(TABLE_NAME, KEY_FRIENDID + "=?",
				new String[] { strFriendId });

	}

	public Cursor getData() {
		String selectQuery = "SELECT  * FROM " + TABLE_NAME;
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		return cursor;
	}

	public ArrayList<CMessage> getmessagesofFriend(String frndid) {
		ArrayList<CMessage> msgs = new ArrayList<CMessage>();
		String selectQuery = "SELECT  * FROM " + TABLE_NAME + " WHERE "
				+ KEY_FRIENDID + " = \"" + frndid + "\"";
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		cursor.moveToFirst();
		while (cursor.isAfterLast() == false) {
			boolean isLeft;
			if (cursor.getString(cursor.getColumnIndex(KEY_SENTBY))
					.equals("me")) {
				isLeft = false;
			} else {
				isLeft = true;
			}
			msgs.add(new CMessage(cursor.getString(cursor
					.getColumnIndex(KEY_MESSAGE)), isLeft));
			cursor.moveToNext();
		}
		if (msgs.size() > 0) {
			return msgs;
		} else {
			return null;
		}
	}

	public boolean checkIfMessageIsAlreadyInserted(String recid) {
		SQLiteDatabase sqldb = this.getWritableDatabase();
		String Query = "SELECT * FROM " + TABLE_NAME + " WHERE " + KEY_RECORDID
				+ " = " + recid;
		Cursor cursor = sqldb.rawQuery(Query, null);
		if (cursor.getCount() <= 0) {
			return false;
		}
		return true;
	}

}
