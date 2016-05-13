package com.pimpimmobile.librealarm.shareddata;

import java.text.DecimalFormat;

public class GlucoseData {

    public long realDate;
    public String sensorId;
    public long sensorTime;
    public int glucoseLevel = -1;
    public long databaseId;

    public GlucoseData(){}

    public GlucoseData(String transferString) {
        String[] split = transferString.split(":");
        sensorId = String.valueOf(split[0]);
        sensorTime = Long.valueOf(split[1]);
        glucoseLevel = Integer.valueOf(split[2]);
        realDate = Long.valueOf(split[3]);
    }

    public String toTransferString() {
        return sensorId + ":" + sensorTime + ":" + glucoseLevel + ":" + realDate;
    }

    public String mmolGlucose() {
        return new DecimalFormat("##.#").format(glucoseLevel/18f);
    }

}
