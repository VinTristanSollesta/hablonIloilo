package com.example.habloniloilo.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ColorGroupDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "ColorGroupDatabaseHelper";
    private static final String DATABASE_NAME = "color_groups.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_GROUPS = "color_groups";
    private static final String TABLE_GROUP_COLORS = "group_colors";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_GROUP_ID = "group_id";
    private static final String COLUMN_COLOR = "color";
    private static final String COLUMN_TIMESTAMP = "timestamp";

    public ColorGroupDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_GROUPS_TABLE = "CREATE TABLE " + TABLE_GROUPS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ")";
        db.execSQL(CREATE_GROUPS_TABLE);

        String CREATE_GROUP_COLORS_TABLE = "CREATE TABLE " + TABLE_GROUP_COLORS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_GROUP_ID + " INTEGER,"
                + COLUMN_COLOR + " INTEGER,"
                + "FOREIGN KEY(" + COLUMN_GROUP_ID + ") REFERENCES " + TABLE_GROUPS + "(" + COLUMN_ID + ")"
                + ")";
        db.execSQL(CREATE_GROUP_COLORS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROUP_COLORS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROUPS);
        onCreate(db);
    }

    public long addColorGroup(String name, List<Integer> colors) {
        SQLiteDatabase db = this.getWritableDatabase();
        long groupId = -1;

        try {
            db.beginTransaction();

            // Insert group
            ContentValues groupValues = new ContentValues();
            groupValues.put(COLUMN_NAME, name);
            groupId = db.insert(TABLE_GROUPS, null, groupValues);

            if (groupId != -1) {
                // Insert colors
                for (int color : colors) {
                    ContentValues colorValues = new ContentValues();
                    colorValues.put(COLUMN_GROUP_ID, groupId);
                    colorValues.put(COLUMN_COLOR, color);
                    db.insert(TABLE_GROUP_COLORS, null, colorValues);
                }
            }

            db.setTransactionSuccessful();
        } catch (Exception e) {
            groupId = -1;
        } finally {
            db.endTransaction();
            db.close();
        }

        return groupId;
    }

    public List<ColorGroup> getAllColorGroups() {
        List<ColorGroup> groups = new ArrayList<>();
        String selectQuery = "SELECT g." + COLUMN_ID + ", g." + COLUMN_NAME + ", g." + COLUMN_TIMESTAMP + 
                           " FROM " + TABLE_GROUPS + " g ORDER BY g." + COLUMN_TIMESTAMP + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                long groupId = cursor.getLong(0);
                String name = cursor.getString(1);
                String timestamp = cursor.getString(2);
                List<Integer> colors = getGroupColors(groupId);
                groups.add(new ColorGroup(groupId, name, colors, timestamp));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return groups;
    }

    public ColorGroup getColorGroup(long groupId) {
        String selectQuery = "SELECT g." + COLUMN_ID + ", g." + COLUMN_NAME + ", g." + COLUMN_TIMESTAMP + 
                           " FROM " + TABLE_GROUPS + " g WHERE g." + COLUMN_ID + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(groupId)});

        ColorGroup group = null;
        if (cursor.moveToFirst()) {
            String name = cursor.getString(1);
            String timestamp = cursor.getString(2);
            List<Integer> colors = getGroupColors(groupId);
            group = new ColorGroup(groupId, name, colors, timestamp);
        }
        cursor.close();
        db.close();
        return group;
    }

    private List<Integer> getGroupColors(long groupId) {
        List<Integer> colors = new ArrayList<>();
        String selectQuery = "SELECT " + COLUMN_COLOR + " FROM " + TABLE_GROUP_COLORS + 
                           " WHERE " + COLUMN_GROUP_ID + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(groupId)});

        if (cursor.moveToFirst()) {
            do {
                colors.add(cursor.getInt(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return colors;
    }

    public boolean updateColorGroup(long groupId, String name, List<Integer> colors) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean success = false;

        try {
            db.beginTransaction();

            // Update group name
            ContentValues groupValues = new ContentValues();
            groupValues.put(COLUMN_NAME, name);
            int rowsAffected = db.update(TABLE_GROUPS, groupValues, COLUMN_ID + " = ?", 
                new String[]{String.valueOf(groupId)});

            if (rowsAffected > 0) {
                // Delete existing colors
                db.delete(TABLE_GROUP_COLORS, COLUMN_GROUP_ID + " = ?", 
                    new String[]{String.valueOf(groupId)});

                // Insert new colors
                for (int color : colors) {
                    ContentValues colorValues = new ContentValues();
                    colorValues.put(COLUMN_GROUP_ID, groupId);
                    colorValues.put(COLUMN_COLOR, color);
                    db.insert(TABLE_GROUP_COLORS, null, colorValues);
                }

                success = true;
            }

            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error updating color group", e);
        } finally {
            db.endTransaction();
            db.close();
        }

        return success;
    }

    public void deleteColorGroup(long groupId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();
            db.delete(TABLE_GROUP_COLORS, COLUMN_GROUP_ID + " = ?", new String[]{String.valueOf(groupId)});
            db.delete(TABLE_GROUPS, COLUMN_ID + " = ?", new String[]{String.valueOf(groupId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public void clearAllColorGroups() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();
            db.delete(TABLE_GROUP_COLORS, null, null);
            db.delete(TABLE_GROUPS, null, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public static class ColorGroup {
        private final long id;
        private final String name;
        private final List<Integer> colors;
        private final String timestamp;

        public ColorGroup(long id, String name, List<Integer> colors, String timestamp) {
            this.id = id;
            this.name = name;
            this.colors = colors;
            this.timestamp = timestamp;
        }

        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public List<Integer> getColors() {
            return colors;
        }

        public String getTimestamp() {
            return timestamp;
        }
    }
} 