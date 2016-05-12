package com.pimpimmobile.librealarm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.pimpimmobile.librealarm.shareddata.GlucoseData;
import com.pimpimmobile.librealarm.shareddata.PredictionData;
import com.pimpimmobile.librealarm.shareddata.ReadingData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SimpleDatabase extends SQLiteOpenHelper {

    private DatabaseListener mListener;

    public SimpleDatabase(Context context) {
        super(context, "data", null, 1);
    }

    public interface Prediction extends Glucose {
        String ERROR_CODE = "error_code";
        String CONFIDENCE = "confidence";
        String PREDICTION = "prediction";
    }

    public interface Trend extends Glucose {
        String OWNER_ID = "owner_id";
    }

    public interface Glucose {
        String ID = "id";
        String SENSOR_ID = "sensor_id";
        String SENSOR_TIME = "sensor_time";
        String GLUCOSE = "glucose";
        String REAL_DATE_MS = "real_ms";
    }

    public static final String TABLE_PREDICTIONS = "Predictions";
    public static final String TABLE_TREND = "Trend";
    public static final String TABLE_HISTORY = "History";

    private static final String CREATE_HISTORY_TABLE =
            "create table " + TABLE_HISTORY + "("
                    + Glucose.ID + " integer primary key,"
                    + Glucose.SENSOR_ID + " text,"
                    + Glucose.SENSOR_TIME + " integer,"
                    + Glucose.GLUCOSE + " integer,"
                    + Glucose.REAL_DATE_MS + " integer);";

    private static final String CREATE_TREND_TABLE =
            "create table " + TABLE_TREND + "("
                    + Trend.ID + " integer primary key,"
                    + Trend.SENSOR_ID + " text,"
                    + Trend.SENSOR_TIME + " integer,"
                    + Trend.REAL_DATE_MS + " integer,"
                    + Trend.GLUCOSE + " integer,"
                    + Trend.OWNER_ID + " integer);";

    private static final String CREATE_PREDICTION_TABLE =
            "create table " + TABLE_PREDICTIONS + "("
                    + Prediction.ID + " integer primary key,"
                    + Prediction.SENSOR_ID + " text,"
                    + Prediction.SENSOR_TIME + " integer,"
                    + Prediction.GLUCOSE + " integer,"
                    + Prediction.REAL_DATE_MS + " text,"
                    + Prediction.CONFIDENCE + " real,"
                    + Prediction.PREDICTION + " real,"
                    + Prediction.ERROR_CODE + " integer);";

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_HISTORY_TABLE);
        db.execSQL(CREATE_TREND_TABLE);
        db.execSQL(CREATE_PREDICTION_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public void setListener(DatabaseListener listener) {
        mListener = listener;
    }

    public void storeReading(ReadingData data) {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        ContentValues predictionValues = getGlucoseContentValues(data.prediction, -1);
        long predictionId = database.insert(TABLE_PREDICTIONS, null, predictionValues);
        for (Object trend : data.trend) {
            database.insert(TABLE_TREND, null, getGlucoseContentValues((GlucoseData) trend, predictionId));
        }
        for (Object history : data.history) {
            database.insert(TABLE_HISTORY, null, getGlucoseContentValues((GlucoseData) history, -1));
        }

        database.setTransactionSuccessful();
        database.endTransaction();
        mListener.onDatabaseChange();
    }

    public List<PredictionData> getPredictions() {
        List<PredictionData> prediction = new ArrayList<>();
        SQLiteDatabase database = getReadableDatabase();
        Cursor c = database.query(TABLE_PREDICTIONS, null, null, null, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                int idIndex = c.getColumnIndex(Prediction.ID);
                int glucoseIndex = c.getColumnIndex(Prediction.GLUCOSE);
                int realDateIndex = c.getColumnIndex(Prediction.REAL_DATE_MS);
                int sensorIdIndex = c.getColumnIndex(Prediction.SENSOR_ID);
                int sensorTimeIndex = c.getColumnIndex(Prediction.SENSOR_TIME);
                int confidenceIndex = c.getColumnIndex(Prediction.CONFIDENCE);
                int predictionIndex = c.getColumnIndex(Prediction.PREDICTION);
                int errorIndex = c.getColumnIndex(Prediction.ERROR_CODE);
                while (!c.isAfterLast()) {
                    PredictionData data = new PredictionData();
                    data.databaseId = c.getLong(idIndex);
                    data.glucoseLevel = c.getInt(glucoseIndex);
                    data.realDate = c.getLong(realDateIndex);
                    data.sensorId = c.getLong(sensorIdIndex);
                    data.sensorTime = c.getLong(sensorTimeIndex);
                    data.confidence = c.getFloat(confidenceIndex);
                    data.prediction = c.getFloat(predictionIndex);
                    data.errorCode = PredictionData.Result.values()[c.getInt(errorIndex)];
                    prediction.add(data);
                    c.moveToNext();
                }
            }
            c.close();
        }
        Collections.reverse(prediction);
        return prediction;
    }

    public List<GlucoseData> getTrend(long predictionId) {
        List<GlucoseData> trend = new ArrayList<>();
        SQLiteDatabase database = getReadableDatabase();
        Cursor c = database.query(TABLE_TREND, null, Trend.OWNER_ID + "=?",
                new String[] { Long.toString(predictionId) }, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                int glucoseIndex = c.getColumnIndex(Trend.GLUCOSE);
                int realDateIndex = c.getColumnIndex(Trend.REAL_DATE_MS);
                int sensorIdIndex = c.getColumnIndex(Trend.SENSOR_ID);
                int sensorTimeIndex = c.getColumnIndex(Trend.SENSOR_TIME);
                while (!c.isAfterLast()) {
                    GlucoseData data = new GlucoseData();
                    data.glucoseLevel = c.getInt(glucoseIndex);
                    data.realDate = c.getLong(realDateIndex);
                    data.sensorId = c.getLong(sensorIdIndex);
                    data.sensorTime = c.getLong(sensorTimeIndex);
                    trend.add(data);
                    c.moveToNext();
                }
            }
            c.close();
        }
        Collections.sort(trend, new Comparator<GlucoseData>() {
            @Override
            public int compare(GlucoseData lhs, GlucoseData rhs) {
                return (int) (rhs.sensorTime - lhs.sensorTime);
            }
        });
        return trend;
    }

    private ContentValues getGlucoseContentValues(GlucoseData g, long ownerId) {
        ContentValues values = new ContentValues();
        values.put(Glucose.GLUCOSE, g.glucoseLevel);
        values.put(Glucose.REAL_DATE_MS, g.realDate);
        values.put(Glucose.SENSOR_ID, g.sensorId);
        values.put(Glucose.SENSOR_TIME, g.sensorTime);
        if (g instanceof PredictionData) {
            PredictionData p = (PredictionData) g;
            values.put(Prediction.CONFIDENCE, p.confidence);
            values.put(Prediction.PREDICTION, p.prediction);
            values.put(Prediction.ERROR_CODE, p.errorCode.ordinal());
        }
        if (ownerId != -1) values.put(Trend.OWNER_ID, ownerId);
        return values;
    }

    interface DatabaseListener {
        void onDatabaseChange();
    }
}
