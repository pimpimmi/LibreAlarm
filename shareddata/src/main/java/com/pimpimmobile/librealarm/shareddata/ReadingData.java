package com.pimpimmobile.librealarm.shareddata;

import java.util.ArrayList;
import java.util.List;

public class ReadingData {

    public final PredictionData prediction;
    public final List<GlucoseData> trend;
    public final List<GlucoseData> history;

    public ReadingData(PredictionData.Result result) {
        this.prediction = new PredictionData();
        this.prediction.realDate = System.currentTimeMillis();
        this.prediction.errorCode = result;
        this.trend = new ArrayList<>();
        this.history = new ArrayList<>();
    }

    public ReadingData(PredictionData prediction, List<GlucoseData> trend, List<GlucoseData> history) {
        this.prediction = prediction;
        this.trend = trend;
        this.history = history;
    }

    public ReadingData(String data) {
        trend = new ArrayList<>();
        history = new ArrayList<>();
        String[] lines = data.split("\n");
        int i = 0;
        prediction = new PredictionData(lines[i++]);
        String line = lines[i++];
        for (; !line.equals("history"); i++) {
            trend.add(new GlucoseData(line));
            line = lines[i];
        }
        for (;i < lines.length && line != null && !"".equals(line);i++) {
            line = lines[i];
            history.add(new GlucoseData(line));
        }
    }

    public String readingToString() {
        StringBuilder builder = new StringBuilder();
        builder.append(prediction.toTransferString()).append("\n");
        for (GlucoseData trend : this.trend) {
            builder.append(trend.toTransferString()).append("\n");
        }
        builder.append("history").append("\n");
        for (GlucoseData history : this.history) {
            builder.append(history.toTransferString()).append("\n");
        }
        return builder.toString();
    }

    public static class TransferObject {
        public final long id;
        public final ReadingData data;

        public TransferObject(String data) {
            id = Long.valueOf(data.split("\n")[0]);
            this.data = new ReadingData(data.substring(data.indexOf("\n") + 1));
        }

        public TransferObject(long id, String data) {
            this.id = id;
            this.data = new ReadingData(data);
        }

        public String toString() {
            return "" + id + "\n" + data.readingToString();
        }
    }
}
