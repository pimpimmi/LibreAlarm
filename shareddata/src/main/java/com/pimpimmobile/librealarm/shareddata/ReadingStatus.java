package com.pimpimmobile.librealarm.shareddata;

public class ReadingStatus {

    public final int attempt;
    public final int maxAttempts;
    public final boolean running;

    public ReadingStatus(int attempt, int maxAttempts, boolean running) {
        this.attempt = attempt;
        this.maxAttempts = maxAttempts;
        this.running = running;
    }

    public ReadingStatus(String transferString) {
        String[] split = transferString.split(":");
        attempt = Integer.valueOf(split[0]);
        maxAttempts = Integer.valueOf(split[1]);
        running = Integer.valueOf(split[2]) == 1;
    }

    public String toTransferString() {
        return "" + attempt + ":" + maxAttempts + ":" + (running ? "1" : "0");
    }
}
