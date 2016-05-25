package com.pimpimmobile.librealarm.shareddata;

public class Status {

    public enum Type {
        ATTEMPTING,
        ATTENPT_FAILED,
        WAITING,
        NOT_RUNNING,
        ALARM
    }

    public final Type status;
    public final int attempt;
    public final int maxAttempts;
    public final long nextCheck;

    public Status(Type type, int attempt, int maxAttempts, long nextCheck) {
        this.status = type;
        this.attempt = attempt;
        this.maxAttempts = maxAttempts;
        this.nextCheck = nextCheck;
    }

    public Status(String transferString) {
        String[] split = transferString.split(":");
        status = Type.values()[Integer.valueOf(split[0])];
        attempt = Integer.valueOf(split[1]);
        maxAttempts = Integer.valueOf(split[2]);
        nextCheck = Long.valueOf(split[3]);
    }

    public String toTransferString() {
        return "" + status.ordinal() + ":" + attempt + ":" + maxAttempts + ":" + nextCheck;
    }
}
