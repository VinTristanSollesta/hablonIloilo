package com.example.habloniloilo.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

public class ColorDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "colors.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_COLORS = "colors";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_COLOR = "color";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final int COLOR_SIMILARITY_THRESHOLD = 30; // Threshold for considering colors similar

    public ColorDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_COLORS_TABLE = "CREATE TABLE " + TABLE_COLORS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_COLOR + " INTEGER,"
                + COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ")";
        db.execSQL(CREATE_COLORS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COLORS);
        onCreate(db);
    }

    public void addColor(int color) {
        if (!isSimilarColorExists(color)) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_COLOR, color);
            db.insert(TABLE_COLORS, null, values);
            db.close();
        }
    }

    private boolean isSimilarColorExists(int newColor) {
        List<Integer> existingColors = getAllColors();
        for (int existingColor : existingColors) {
            if (areColorsSimilar(newColor, existingColor)) {
                return true;
            }
        }
        return false;
    }

    private boolean areColorsSimilar(int color1, int color2) {
        int r1 = Color.red(color1);
        int g1 = Color.green(color1);
        int b1 = Color.blue(color1);
        
        int r2 = Color.red(color2);
        int g2 = Color.green(color2);
        int b2 = Color.blue(color2);
        
        int diff = Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);
        return diff <= COLOR_SIMILARITY_THRESHOLD;
    }

    public List<Integer> getAllColors() {
        List<Integer> colors = new ArrayList<>();
        String selectQuery = "SELECT " + COLUMN_COLOR + " FROM " + TABLE_COLORS + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                colors.add(cursor.getInt(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return colors;
    }

    public void deleteColor(int color) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_COLORS, COLUMN_COLOR + " = ?", new String[]{String.valueOf(color)});
        db.close();
    }

    public void clearAllColors() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_COLORS, null, null);
        db.close();
    }
} 