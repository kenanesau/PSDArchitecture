package com.privatesecuredata.arch.tools;

import android.util.Base64;


/**
 * Utility-class which does an XOR-operation on a messag
 *
 * Created by kenan on 11/17/15.
 *
 */
public class KeyUtil {

    public static String encode(String str, String key) {
        return Base64.encodeToString(xor(str.getBytes(), key.getBytes()), Base64.DEFAULT);
    }

    public static String decode(String str, String key) {
        return new String(xor(Base64.decode(str, Base64.DEFAULT), key.getBytes()));
    }

    private static byte[] xor(byte[] msg, byte[] key) {
        byte[] ret = new byte[msg.length];
        for (int i = 0; i < msg.length; i++) {
            ret[i] = (byte) (msg[i] ^ key[i % key.length]);
        }
        return ret;
    }
}
