package com.pimpimmobile.librealarm.shareddata;

public class Status {

    public enum Type {
        ATTEMPTING,
        ATTENPT_FAILED,
        WAITING,
        NOT_RUNNING,
        ALARM_HIGH,
        ALARM_LOW,
        ALARM_OTHER
    }

    public Type status;
    public int attempt;
    public int maxAttempts;
    public long nextCheck;
    public int alarmExtraValue;
    public int alarmExtraTrendOrdinal;

    public Status(Type type, int attempt, int maxAttempts, long nextCheck) {
        this.status = type;
        this.attempt = attempt;
        this.maxAttempts = maxAttempts;
        this.nextCheck = nextCheck;
    }

    public Status(Type type, int attempt, int maxAttempts, long nextCheck, int alarmExtraValue,
                  AlgorithmUtil.TrendArrow alarmExtraTrend) {
        this.status = type;
        this.attempt = attempt;
        this.maxAttempts = maxAttempts;
        this.nextCheck = nextCheck;
        this.alarmExtraValue = alarmExtraValue;
        this.alarmExtraTrendOrdinal = alarmExtraTrend.ordinal();
    }

    public Status() {
    }
}
