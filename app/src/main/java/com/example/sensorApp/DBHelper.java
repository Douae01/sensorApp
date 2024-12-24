package com.example.sensorApp;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Users.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "users";
    private static final String COL_1 = "ID";
    private static final String COL_2 = "EMAIL";
    private static final String COL_3 = "PASSWORD";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        // Manually call the method to ensure table creation
        checkAndCreateTable(getWritableDatabase());
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Typically called when the database is first created
        createTable(db);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    private void createTable(SQLiteDatabase db) {
        // Create table SQL command
        String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                + COL_1 + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_2 + " TEXT, "
                + COL_3 + " TEXT)";

        // Execute the SQL command
        db.execSQL(CREATE_TABLE);
        Log.d("DBHelper", "Table created or already exists: " + TABLE_NAME);
    }

    public void checkAndCreateTable(SQLiteDatabase db) {
        try {
            // Run the SQL command manually during instantiation to ensure table creation
            createTable(db);
        } catch (Exception e) {
            Log.e("DBHelper", "Error ensuring table creation: " + e.getMessage());
        }
    }

    public boolean insertUser(String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2, email);
        contentValues.put(COL_3, password);
        long result = db.insert(TABLE_NAME, null, contentValues);
        return result != -1;
    }

    public boolean checkUserExists(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE EMAIL = ? AND PASSWORD = ?", new String[]{email, password});
        return cursor.getCount() > 0;
    }

    public List<String> getAllEmails() {
        List<String> emailList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_2 + " FROM " + TABLE_NAME, null);

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String email = cursor.getString(cursor.getColumnIndex(COL_2));
                emailList.add(email);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return emailList;
    }

}



