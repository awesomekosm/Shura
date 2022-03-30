package com.bots.shura.db.entities;

public enum TrackPlayStatus {
    QUEUED(0),
    SKIPPED(1),
    PLAYING(2),
    PAUSED(3),
    FINISHED(4),
    LOAD_FAILED(5);

    final int status;

    TrackPlayStatus(int status) {
        this.status = status;
    }

    public int status() {
        return status;
    }
}
