package com.pimpimmobile.librealarm.shareddata;

public class PredictionData extends GlucoseData {

    public enum Result {
        OK,
        ERROR_NO_NFC,
        ERROR_NFC_READ
    }

    public double prediction = -1;
    public double confidence = -1;
    public Result errorCode;
    public int attempt;

    public PredictionData() {}

    public PredictionData(String transferString) {
        super(transferString);
        String[] split = transferString.split(":");
        prediction = Double.valueOf(split[4]);
        confidence = Double.valueOf(split[5]);
        errorCode = Result.values()[Integer.valueOf(split[6])];
        attempt = Integer.valueOf(split[7]);
    }

    public String toTransferString() {
        return super.toTransferString() + ":" + prediction + ":" + confidence + ":" + errorCode.ordinal() + ":" + attempt;
    }
}
