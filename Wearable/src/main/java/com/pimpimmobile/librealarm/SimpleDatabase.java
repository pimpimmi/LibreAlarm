package com.pimpimmobile.librealarm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.gson.Gson;
import com.pimpimmobile.librealarm.shareddata.ReadingData;

import java.util.ArrayList;
import java.util.List;

/**
 * This database only stores glucose readings until it has made sure the phone got them.
 */
public class SimpleDatabase extends SQLiteOpenHelper {

    public SimpleDatabase(Context context) {
        super(context, "data", null, 2);
    }

    public interface Message {
        String ID = "id";
        String MESSAGE = "message";
    }

    public static final String TABLE_TRANSFER_MESSAGES = "messages";

    private static final String CREATE_PREDICTION_TABLE =
            "create table " + TABLE_TRANSFER_MESSAGES + "("
                    + Message.ID + " integer primary key, "
                    + Message.MESSAGE + " text);";

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_PREDICTION_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public List<ReadingData.TransferObject> getMessages() {
        SQLiteDatabase database = getReadableDatabase();
        List<ReadingData.TransferObject> list = new ArrayList<>();
        Cursor c = database.query(TABLE_TRANSFER_MESSAGES, null, null, null, null, null, null);
        if (c != null && c.moveToFirst()) {
            while (!c.isAfterLast()) {
                list.add(new ReadingData.TransferObject(c.getLong(c.getColumnIndex(Message.ID)),
                        new Gson().fromJson(c.getString(c.getColumnIndex(Message.MESSAGE)), ReadingData.class)));
                c.moveToNext();
            }
        }
        database.close();
        return list;
    }

    public void deleteMessage(long id) {
        SQLiteDatabase database = getWritableDatabase();
        database.delete(TABLE_TRANSFER_MESSAGES, Message.ID + "=?", new String[] {String.valueOf(id)});
        database.close();
    }

    public long saveMessage(ReadingData message) {
        SQLiteDatabase database = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Message.MESSAGE, new Gson().toJson(message));
        long id = database.insert(TABLE_TRANSFER_MESSAGES, null, values);
        database.close();
        return id;
    }

}
