package com.techreviewsandhelp.goodthings;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "good_things.db";
    private static final int SCHEMA = 1; // database version
    public static final String TABLE_NAME = "good_things";
    public static final String DATE = "date";
    public static final String THINGS = "things";
    public static final String DEALS = "deeds";
    public static final String BETTER = "better";

    public DbHelper(Context context)
    {
        super(context,DATABASE_NAME,null,SCHEMA);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sqlString = "CREATE TABLE " + TABLE_NAME + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DATE + " TEXT, " + THINGS + " TEXT, " + DEALS + " TEXT, "
                + BETTER + " TEXT);";
        db.execSQL(sqlString);
        Log.v("dbString", sqlString);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXIST " + TABLE_NAME);
        onCreate(db);
    }

}
