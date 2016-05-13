package com.privatesecuredata.arch.db;

/**
 * Created by kenan on 5/13/16.
 */
public class StatusMessage {
    private PersistanceManager.Status status;
    private String errorText;
    private Exception ex;

    public StatusMessage(PersistanceManager.Status status) {
        this.status = status;
    }

    public StatusMessage(PersistanceManager.Status status, String txt) {
        this.status = status;
        this.errorText = txt;
    }

    public StatusMessage(PersistanceManager.Status status, String txt, Exception ex) {
        this.status = status;
        this.errorText = txt;
        this.ex = ex;
    }

    public StatusMessage(PersistanceManager.Status status, Exception ex) {
        this.status = status;
        this.ex = ex;
    }

    public PersistanceManager.Status getStatus() {
        return status;
    }

    public String getErrorText() {
        return errorText;
    }

    public Exception getException() {
        return ex;
    }
}
