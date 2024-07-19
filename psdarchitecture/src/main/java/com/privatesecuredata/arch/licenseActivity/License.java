package com.privatesecuredata.arch.licenseActivity;

/**
 * Created by kenan on 3/24/17.
 */

public class License {
    private String file;

    public License(String file)
    {
        setFile(file);
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
