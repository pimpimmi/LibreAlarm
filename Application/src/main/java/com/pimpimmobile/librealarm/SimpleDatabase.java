package com.pimpimmobile.librealarm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.pimpimmobile.librealarm.shareddata.GlucoseData;
import com.pimpimmobile.librealarm.shareddata.PredictionData;
import com.pimpimmobile.librealarm.shareddata.ReadingData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class SimpleDatabase extends SQLiteOpenHelper {

    private static final String TAG = "GLUCOSE::" + SimpleDatabase.class.getSimpleName();

    private DatabaseListener mListener;

    public SimpleDatabase(Context context) {
        super(context, "data", null, 2);
    }

    public interface Prediction extends Glucose {
        String ERROR_CODE = "error_code";
        String CONFIDENCE = "confidence";
        String PREDICTION = "prediction";
        String ATTEMPT = "attempt";
        String NIGHTSCOUT_SYNC = "ns_uploaded";
    }

    public interface Glucose {
        String ID = "id";
        String SENSOR_ID = "sensor_id";
        String SENSOR_TIME = "sensor_time";
        String GLUCOSE = "glucose";
        String REAL_DATE_MS = "real_ms";
        String OWNER_ID = "owner_id";
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
                    + Glucose.REAL_DATE_MS + " integer,"
                    + Glucose.OWNER_ID + " integer);";

    private static final String CREATE_TREND_TABLE =
            "create table " + TABLE_TREND + "("
                    + Glucose.ID + " integer primary key,"
                    + Glucose.SENSOR_ID + " text,"
                    + Glucose.SENSOR_TIME + " integer,"
                    + Glucose.REAL_DATE_MS + " integer,"
                    + Glucose.GLUCOSE + " integer,"
                    + Glucose.OWNER_ID + " integer);";

    private static final String CREATE_PREDICTION_TABLE =
            "create table " + TABLE_PREDICTIONS + "("
                    + Prediction.ID + " integer primary key,"
                    + Prediction.SENSOR_ID + " text,"
                    + Prediction.SENSOR_TIME + " integer,"
                    + Prediction.GLUCOSE + " integer,"
                    + Prediction.REAL_DATE_MS + " text,"
                    + Prediction.CONFIDENCE + " real,"
                    + Prediction.PREDICTION + " real,"
                    + Prediction.ERROR_CODE + " integer,"
                    + Prediction.ATTEMPT + " integer,"
                    + Prediction.NIGHTSCOUT_SYNC + " integer default 0);";

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_HISTORY_TABLE);
        db.execSQL(CREATE_TREND_TABLE);
        db.execSQL(CREATE_PREDICTION_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2 && newVersion >= 2) {
            db.execSQL("ALTER TABLE " + TABLE_PREDICTIONS + " ADD " + Prediction.NIGHTSCOUT_SYNC + " integer default 0");
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public void setListener(DatabaseListener listener) {
        mListener = listener;
    }

    public void storeReading(ReadingData data) {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();

        // Checks for duplicates.
        if (data.prediction.glucoseLevel != -1) {
            Cursor c = null;
            try {
                c = database.query(TABLE_PREDICTIONS, null,
                        Glucose.SENSOR_ID + "=? AND ?=" + Glucose.SENSOR_TIME,
                        new String[]{data.prediction.sensorId, String.valueOf(data.prediction.sensorTime)},
                        null, null, null);
                if (c.getCount() > 0) {
                    Log.i(TAG, "Data already exist, sensor id: " + data.prediction.sensorId +
                            ", sensor time: " + data.prediction.sensorTime);
                    return;
                }
            } finally {
                if (c != null) c.close();
            }
        }

        ContentValues predictionValues = getGlucoseContentValues(data.prediction, -1);
        long predictionId = database.insert(TABLE_PREDICTIONS, null, predictionValues);
        for (Object trend : data.trend) {
            database.insert(TABLE_TREND, null, getGlucoseContentValues((GlucoseData) trend, predictionId));
        }
        for (Object history : data.history) {
            database.insert(TABLE_HISTORY, null, getGlucoseContentValues((GlucoseData) history, predictionId));
        }

        database.setTransactionSuccessful();
        database.endTransaction();
        if (mListener != null) mListener.onDatabaseChange();
    }

    public List<PredictionData> getPredictions() {
        return getPredictions(null, null);
    }

    private List<PredictionData> getPredictions(String selection, String[] selectionArgs) {
        List<PredictionData> prediction = new ArrayList<>();
        SQLiteDatabase database = getReadableDatabase();
        Cursor c = database.query(TABLE_PREDICTIONS, null, selection, selectionArgs, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                int idIndex = c.getColumnIndex(Prediction.ID);
                int glucoseIndex = c.getColumnIndex(Prediction.GLUCOSE);
                int realDateIndex = c.getColumnIndex(Prediction.REAL_DATE_MS);
                int sensorIdIndex = c.getColumnIndex(Prediction.SENSOR_ID);
                int sensorTimeIndex = c.getColumnIndex(Prediction.SENSOR_TIME);
                int confidenceIndex = c.getColumnIndex(Prediction.CONFIDENCE);
                int predictionIndex = c.getColumnIndex(Prediction.PREDICTION);
                int attemptIndex = c.getColumnIndex(Prediction.ATTEMPT);
                int errorIndex = c.getColumnIndex(Prediction.ERROR_CODE);
                while (!c.isAfterLast()) {
                    PredictionData data = new PredictionData();
                    data.phoneDatabaseId = c.getLong(idIndex);
                    data.glucoseLevel = c.getInt(glucoseIndex);
                    data.realDate = c.getLong(realDateIndex);
                    data.sensorId = c.getString(sensorIdIndex);
                    data.sensorTime = c.getLong(sensorTimeIndex);
                    data.confidence = c.getFloat(confidenceIndex);
                    data.trend = c.getFloat(predictionIndex);
                    data.attempt = c.getInt(attemptIndex);
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


    public List<PredictionData> getNsSyncData() {
        return getPredictions(Prediction.NIGHTSCOUT_SYNC + "=0 AND -1 !=" + Prediction.GLUCOSE, null);
    }

    public void setNsSynced(List<PredictionData> list) {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        for (PredictionData data : list) {
            ContentValues values = new ContentValues();
            values.put(Prediction.NIGHTSCOUT_SYNC, 1);
            database.update(TABLE_PREDICTIONS, values, Prediction.ID + "=?", new String[] {String.valueOf(data.phoneDatabaseId)});
        }
        database.setTransactionSuccessful();
        database.endTransaction();
    }

    public List<GlucoseData> getTrend(long predictionId) {
        List<GlucoseData> trend = new ArrayList<>();
        SQLiteDatabase database = getReadableDatabase();
        Cursor c = database.query(TABLE_TREND, null, Glucose.OWNER_ID + "=?",
                new String[] { Long.toString(predictionId) }, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                int glucoseIndex = c.getColumnIndex(Glucose.GLUCOSE);
                int realDateIndex = c.getColumnIndex(Glucose.REAL_DATE_MS);
                int sensorIdIndex = c.getColumnIndex(Glucose.SENSOR_ID);
                int sensorTimeIndex = c.getColumnIndex(Glucose.SENSOR_TIME);
                while (!c.isAfterLast()) {
                    GlucoseData data = new GlucoseData();
                    data.glucoseLevel = c.getInt(glucoseIndex);
                    data.realDate = c.getLong(realDateIndex);
                    data.sensorId = c.getString(sensorIdIndex);
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
            values.put(Prediction.PREDICTION, p.trend);
            values.put(Prediction.ERROR_CODE, p.errorCode.ordinal());
            values.put(Prediction.ATTEMPT, p.attempt);
        }
        if (ownerId != -1) values.put(Glucose.OWNER_ID, ownerId);
        return values;
    }

    public List<GlucoseData> getTrends() {

        HashMap<Long, List<GlucoseData>> trend = new HashMap<>();
        Cursor c = getReadableDatabase().query(TABLE_TREND, null, null, null, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                int glucoseIndex = c.getColumnIndex(Glucose.GLUCOSE);
                int sensorTimeIndex = c.getColumnIndex(Glucose.SENSOR_TIME);
                while (!c.isAfterLast()) {
                    GlucoseData data = new GlucoseData();
                    data.glucoseLevel = c.getInt(glucoseIndex);
                    data.sensorTime = c.getLong(sensorTimeIndex);
                    if (!trend.containsKey(data.sensorTime)) {
                        trend.put(data.sensorTime, new ArrayList<GlucoseData>());
                    }
                    trend.get(data.sensorTime).add(data);
                    c.moveToNext();
                }
            }
            c.close();
        }

        List<GlucoseData> trendList = new ArrayList<>();
        for (List<GlucoseData> glucose : trend.values()) {
            HashMap<Integer, Integer> count = new HashMap<>();
            for (GlucoseData data : glucose) {
                if (!count.containsKey(data.glucoseLevel)) {
                    count.put(data.glucoseLevel, 1);
                } else {
                    count.put(data.glucoseLevel, count.get(data.glucoseLevel) + 1);
                }
            }
            GlucoseData trendValue = new GlucoseData();
            trendValue.sensorTime = glucose.get(0).sensorTime;
            int max = 0;
            for (int key : count.keySet()) {
                if (count.get(key) > max) {
                    trendValue.glucoseLevel = key;
                    max = count.get(key);
                }
            }
            trendList.add(trendValue);
        }
        return trendList;
    }

    public List<GlucoseData> getHistory() {
        HashMap<Long, List<GlucoseData>> history = new HashMap<>();
        Cursor c = getReadableDatabase().query(TABLE_HISTORY, null, null, null, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                int glucoseIndex = c.getColumnIndex(Glucose.GLUCOSE);
                int sensorTimeIndex = c.getColumnIndex(Glucose.SENSOR_TIME);
                while (!c.isAfterLast()) {
                    GlucoseData data = new GlucoseData();
                    data.glucoseLevel = c.getInt(glucoseIndex);
                    data.sensorTime = c.getLong(sensorTimeIndex);
                    if (!history.containsKey(data.sensorTime)) {
                        history.put(data.sensorTime, new ArrayList<GlucoseData>());
                    }
                    history.get(data.sensorTime).add(data);
                    c.moveToNext();
                }
            }
            c.close();
        }

        List<GlucoseData> historyList = new ArrayList<>();

        for (List<GlucoseData> glucose : history.values()) {
            HashMap<Integer, Integer> count = new HashMap<>();
            for (GlucoseData data : glucose) {
                if (!count.containsKey(data.glucoseLevel)) {
                    count.put(data.glucoseLevel, 1);
                } else {
                    count.put(data.glucoseLevel, count.get(data.glucoseLevel) + 1);
                }
            }
            GlucoseData historyValue = new GlucoseData();
            historyValue.sensorTime = glucose.get(0).sensorTime;
            int max = 0;
            for (int key : count.keySet()) {
                if (count.get(key) > max) {
                    historyValue.glucoseLevel = key;
                    max = count.get(key);
                }
            }
            historyList.add(historyValue);
        }
        return historyList;
    }

    interface DatabaseListener {
        void onDatabaseChange();
    }
}
