package com.privatesecuredata.arch.db;

/**
 * Created by kenan on 5/13/16.
 */
public class StatusMessage {
    private PersistanceManager.Status status;
    private String text;
    private Throwable ex;

    public StatusMessage(PersistanceManager.Status status) {
        this.status = status;
    }

    public StatusMessage(PersistanceManager.Status status, String txt) {
        this.status = status;
        this.text = txt;
    }

    public StatusMessage(PersistanceManager.Status status, String txt, Throwable ex) {
        this.status = status;
        this.text = txt;
        this.ex = ex;
    }

    public StatusMessage(PersistanceManager.Status status, Throwable ex) {
        this.status = status;
        this.ex = ex;
    }

    public PersistanceManager.Status getStatus() {
        return status;
    }

    public String getText() {
        return text;
    }

    public Throwable getException() {
        return ex;
    }
}
