package com.privatesecuredata.arch.tools;

import android.os.Build;

/**
 * Created by kenan on 12/4/15.
 */
public class Environment {
    public static boolean isEmulator()
    {
        return (Build.PRODUCT.startsWith("sdk") &&
                (Build.HARDWARE.equals("goldfish")) &&
                (Build.MODEL.contains("Android SDK")));
    }
}
