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

    @Override
    public String toString() {

        String status = null;
        switch (this.status) {
            case UNINITIALIZED:
                status = "UNINITIALIZED";
                break;
            case INITIALIZINGPM:
                status = "INITIALIZINGPM";
                break;
            case INITIALIZEDPM:
                status = "INITIALIZEDPM";
                break;
            case CREATINGDB:
                status = "CREATINGDB";
                break;
            case CREATEDDB:
                status = "CREATEDDB";
                break;
            case UPGRADINGDB:
                status = "UPGRADINGDB";
                break;
            case OPERATIONAL:
                status = "OPERATIONAL";
                break;
            case INFO:
                status = "INFO";
                break;
            case ERROR:
                status = "ERROR";
                break;
        }

        if (ex != null) {
            return String.format("%s: %s:\n%s", status, text != null ? text : "", ex.toString());
        }
        else
        {
            return String.format("%s: %s", status, text != null ? text : "");
        }
    }
}
