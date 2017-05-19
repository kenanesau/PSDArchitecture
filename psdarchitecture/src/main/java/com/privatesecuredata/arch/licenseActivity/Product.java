package com.privatesecuredata.arch.licenseActivity;

import android.app.Activity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by kenan on 3/24/17.
 */

public class Product {
    private String dir;
    private String name;
    private String version;
    private License license;

    public Product(String dir, String name, String version, License license) {
        setDir(dir);
        setName(name);
        setVersion(version);
        setLicense(license);
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getVersion() {
        return version;
    }
    public void setVersion(String version) {
        this.version = version;
    }
    public License getLicense() {
        return license;
    }
    public void setLicense(License license) {
        this.license = license;
    }
    public String getDir() { return dir; }
    public void setDir(String dir) { this.dir = dir; }

    public static Product createProduct(Activity ctx, String prod) throws IOException {
        InputStream versionStream = ctx.getAssets().open("licenses/" + prod + "/version");
        BufferedReader reader = new BufferedReader(new InputStreamReader(versionStream));
        String version = reader.readLine();
        reader.close();

        InputStream metaStream = ctx.getAssets().open("licenses/" + prod + "/meta");
        reader = new BufferedReader(new InputStreamReader(metaStream));
        String productName = reader.readLine();
        reader.close();

        return new Product(prod, productName, version, new License("licenses/" + prod + "/license"));
    }
}
