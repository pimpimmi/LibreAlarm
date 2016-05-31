package com.pimpimmobile.librealarm.shareddata;

public class Status {

    public enum Type {
        ATTEMPTING,
        ATTENPT_FAILED,
        WAITING,
        NOT_RUNNING,
        ALARM
    }

    public Type status;
    public int attempt;
    public int maxAttempts;
    public long nextCheck;

    public Status(Type type, int attempt, int maxAttempts, long nextCheck) {
        this.status = type;
        this.attempt = attempt;
        this.maxAttempts = maxAttempts;
        this.nextCheck = nextCheck;
    }

    public Status() {
    }
}
