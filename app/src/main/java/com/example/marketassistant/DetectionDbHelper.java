package com.example.marketassistant;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 检测历史数据库帮助类
 */
public class DetectionDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "market_assistant.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "detection_history";

    // 列名
    private static final String COL_ID = "_id";
    private static final String COL_DETECT_TIME = "detect_time";
    private static final String COL_THUMBNAIL_PATH = "thumbnail_path";
    private static final String COL_FRUIT_TYPE = "fruit_type";
    private static final String COL_SCORE = "score";
    private static final String COL_LEVEL = "level";
    private static final String COL_COLOR_INFO = "color_info";
    private static final String COL_SUGGESTION = "suggestion";

    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " ("
                    + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_DETECT_TIME + " INTEGER NOT NULL, "
                    + COL_THUMBNAIL_PATH + " TEXT, "
                    + COL_FRUIT_TYPE + " TEXT NOT NULL, "
                    + COL_SCORE + " INTEGER NOT NULL, "
                    + COL_LEVEL + " TEXT NOT NULL, "
                    + COL_COLOR_INFO + " TEXT, "
                    + COL_SUGGESTION + " TEXT)";

    public DetectionDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    /**
     * 插入一条检测记录
     */
    public long insertRecord(DetectionRecord record) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_DETECT_TIME, record.getDetectTime());
        values.put(COL_THUMBNAIL_PATH, record.getThumbnailPath());
        values.put(COL_FRUIT_TYPE, record.getFruitType());
        values.put(COL_SCORE, record.getScore());
        values.put(COL_LEVEL, record.getLevel());
        values.put(COL_COLOR_INFO, record.getColorInfo());
        values.put(COL_SUGGESTION, record.getSuggestion());
        long id = db.insert(TABLE_NAME, null, values);
        db.close();
        return id;
    }

    /**
     * 获取所有记录，按时间倒序
     */
    public List<DetectionRecord> getAllRecords() {
        List<DetectionRecord> records = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = getReadableDatabase();
            cursor = db.query(TABLE_NAME, null, null, null, null, null,
                    COL_DETECT_TIME + " DESC");
            while (cursor.moveToNext()) {
                records.add(cursorToRecord(cursor));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
        return records;
    }

    /**
     * 按水果类型筛选
     */
    public List<DetectionRecord> getRecordsByFruitType(String fruitType) {
        List<DetectionRecord> records = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = getReadableDatabase();
            cursor = db.query(TABLE_NAME, null,
                    COL_FRUIT_TYPE + " = ?", new String[]{fruitType},
                    null, null, COL_DETECT_TIME + " DESC");
            while (cursor.moveToNext()) {
                records.add(cursorToRecord(cursor));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
        return records;
    }

    /**
     * 搜索记录（按水果类型模糊匹配）
     */
    public List<DetectionRecord> searchRecords(String keyword) {
        List<DetectionRecord> records = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = getReadableDatabase();
            cursor = db.query(TABLE_NAME, null,
                    COL_FRUIT_TYPE + " LIKE ?", new String[]{"%" + keyword + "%"},
                    null, null, COL_DETECT_TIME + " DESC");
            while (cursor.moveToNext()) {
                records.add(cursorToRecord(cursor));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
        return records;
    }

    /**
     * 按分数排序获取记录
     */
    public List<DetectionRecord> getRecordsSortedByScore(boolean ascending) {
        List<DetectionRecord> records = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = getReadableDatabase();
            String order = COL_SCORE + (ascending ? " ASC" : " DESC");
            cursor = db.query(TABLE_NAME, null, null, null, null, null, order);
            while (cursor.moveToNext()) {
                records.add(cursorToRecord(cursor));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
        return records;
    }

    /**
     * 获取所有不重复的水果类型（用于筛选下拉菜单）
     */
    public List<String> getDistinctFruitTypes() {
        List<String> types = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = getReadableDatabase();
            cursor = db.rawQuery(
                    "SELECT DISTINCT " + COL_FRUIT_TYPE + " FROM " + TABLE_NAME
                            + " ORDER BY " + COL_FRUIT_TYPE, null);
            while (cursor.moveToNext()) {
                types.add(cursor.getString(0));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
        return types;
    }

    /**
     * 删除单条记录，同时删除缩略图文件
     */
    public boolean deleteRecord(long id) {
        SQLiteDatabase db = getWritableDatabase();
        // 先获取缩略图路径
        Cursor cursor = db.query(TABLE_NAME, new String[]{COL_THUMBNAIL_PATH},
                COL_ID + " = ?", new String[]{String.valueOf(id)},
                null, null, null);
        if (cursor.moveToFirst()) {
            String path = cursor.getString(0);
            if (path != null) {
                File file = new File(path);
                if (file.exists()) {
                    file.delete();
                }
            }
        }
        cursor.close();
        int rows = db.delete(TABLE_NAME, COL_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return rows > 0;
    }

    /**
     * 清空所有记录，同时删除所有缩略图文件
     */
    public int clearAllRecords() {
        SQLiteDatabase db = getWritableDatabase();
        // 先获取所有缩略图路径
        Cursor cursor = db.query(TABLE_NAME, new String[]{COL_THUMBNAIL_PATH},
                null, null, null, null, null);
        while (cursor.moveToNext()) {
            String path = cursor.getString(0);
            if (path != null) {
                File file = new File(path);
                if (file.exists()) {
                    file.delete();
                }
            }
        }
        cursor.close();
        int rows = db.delete(TABLE_NAME, null, null);
        db.close();
        return rows;
    }

    /**
     * 获取记录总数
     */
    public int getRecordCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    /**
     * 将 Cursor 转换为 DetectionRecord 对象
     */
    private DetectionRecord cursorToRecord(Cursor cursor) {
        DetectionRecord record = new DetectionRecord();
        try {
            int idIndex = cursor.getColumnIndex(COL_ID);
            int timeIndex = cursor.getColumnIndex(COL_DETECT_TIME);
            int pathIndex = cursor.getColumnIndex(COL_THUMBNAIL_PATH);
            int typeIndex = cursor.getColumnIndex(COL_FRUIT_TYPE);
            int scoreIndex = cursor.getColumnIndex(COL_SCORE);
            int levelIndex = cursor.getColumnIndex(COL_LEVEL);
            int colorIndex = cursor.getColumnIndex(COL_COLOR_INFO);
            int suggestionIndex = cursor.getColumnIndex(COL_SUGGESTION);

            if (idIndex >= 0) record.setId(cursor.getLong(idIndex));
            if (timeIndex >= 0) record.setDetectTime(cursor.getLong(timeIndex));
            if (pathIndex >= 0) record.setThumbnailPath(cursor.getString(pathIndex));
            if (typeIndex >= 0) record.setFruitType(cursor.getString(typeIndex));
            if (scoreIndex >= 0) record.setScore(cursor.getInt(scoreIndex));
            if (levelIndex >= 0) record.setLevel(cursor.getString(levelIndex));
            if (colorIndex >= 0) record.setColorInfo(cursor.getString(colorIndex));
            if (suggestionIndex >= 0) record.setSuggestion(cursor.getString(suggestionIndex));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return record;
    }
}
